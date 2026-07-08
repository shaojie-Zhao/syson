package org.eclipse.syson.standard.diagrams.view.services;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.sirius.components.collaborative.api.IEditingContextEventProcessorRegistry;
import org.eclipse.sirius.components.core.api.*;
import org.eclipse.sirius.components.emf.services.api.IEMFEditingContext;
import org.eclipse.sirius.web.application.project.services.api.IProjectEditingContextService;
import org.eclipse.sirius.web.domain.boundedcontexts.project.repositories.IProjectRepository;
import org.eclipse.sirius.web.domain.boundedcontexts.representationdata.repositories.IRepresentationMetadataRepository;
import org.eclipse.syson.sysml.*;
import org.slf4j.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DoDAFMatrixDataService {
    private static final Logger log = LoggerFactory.getLogger(DoDAFMatrixDataService.class);
    private final IEditingContextSearchService editingContextSearchService;
    private final IEditingContextPersistenceService editingContextPersistenceService;
    private final IObjectSearchService objectSearchService;
    private final IProjectEditingContextService projectEditingContextService;
    private final IProjectRepository projectRepository;
    private final IRepresentationMetadataRepository representationMetadataRepository;
    private final JdbcTemplate jdbcTemplate;
    private final IEditingContextEventProcessorRegistry eventProcessorRegistry;
    private static final Map<String, List<Map<String, Object>>> elementCache = new ConcurrentHashMap<>();
    private static final Map<String, String> packageNameCache = new ConcurrentHashMap<>();
    static final Map<String, String> ctxIdCache = new ConcurrentHashMap<>();
    private static final Map<String, org.eclipse.emf.ecore.resource.ResourceSet> resourceSetCache = new ConcurrentHashMap<>();
    private volatile boolean tableReady = false;
    private static final Map<String, String> DODAF_MAP = Map.of("dodaf:capability","Capability","dodaf:operational","OperationalNode","dodaf:system","SystemNode","dodaf:organization","Organization","dodaf:exchange","InformationExchange");

    public DoDAFMatrixDataService(IEditingContextSearchService a, IEditingContextPersistenceService p, IObjectSearchService o,
                                   IProjectEditingContextService b, IProjectRepository c, IRepresentationMetadataRepository d, JdbcTemplate j,
                                   IEditingContextEventProcessorRegistry evt) {
        this.editingContextSearchService = a; this.editingContextPersistenceService = p; this.objectSearchService = o;
        this.projectEditingContextService = b; this.projectRepository = c; this.representationMetadataRepository = d; this.jdbcTemplate = j;
        this.eventProcessorRegistry = evt;
    }

    public List<Map<String, Object>> getElements(String types, String pid, String scope, String unused) {
        java.util.Set<String> ts = new java.util.HashSet<>();
        if (types != null && !types.isBlank() && !"ALL".equalsIgnoreCase(types)) ts.addAll(Arrays.asList(types.split("\\s*,\\s*")));
        String ck = pid + ":" + scope + ":" + types;
        List<Map<String, Object>> cached = elementCache.get(ck);
        if (cached != null) return cached;
        if (scope != null && !scope.isBlank()) {
            List<Map<String, Object>> r = fetch(pid, scope, ts);
            String scopeName = getPackageName(pid, scope);
            List<String> parentChain = getParentChain(pid, scope);
            if (!r.isEmpty()) {
                Map<String, Object> meta = new LinkedHashMap<>();
                meta.put("id","__scope_meta__"); meta.put("name",scopeName); meta.put("type","__meta__"); meta.put("parentPath",scope);
                meta.put("parentChain", parentChain);
                r.add(0, meta); elementCache.put(ck, r); return r;
            }
            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("id","__scope_meta__"); meta.put("name",scopeName); meta.put("type","__meta__"); meta.put("parentPath",scope);
            meta.put("parentChain", parentChain);
            elementCache.put(ck, List.of(meta)); return List.of(meta);
        }
        return List.of();
    }

    private org.eclipse.emf.ecore.resource.ResourceSet getRS(String pid) {
        return resourceSetCache.computeIfAbsent(pid, p -> {
            try {
                String cid = ctxIdCache.computeIfAbsent(p, k -> projectEditingContextService.getEditingContextId(k).orElse(null));
                if (cid == null) return null;
                var o = editingContextSearchService.findById(cid);
                if (o.isPresent() && o.get() instanceof IEMFEditingContext e) return e.getDomain().getResourceSet();
            } catch (Exception ex) { log.error("RS: {}", ex.getMessage()); }
            return null;
        });
    }

    public String getEditingContextId(String pid) { return ctxIdCache.computeIfAbsent(pid, p -> projectEditingContextService.getEditingContextId(p).orElse(null)); }
    public String getPackageName(String pid, String pkid) {
        String ck = pid + ":" + pkid; String c = packageNameCache.get(ck); if (c != null) return c;
        try { var rs = getRS(pid); if (rs == null) return fb(pkid);
            for (var r : rs.getResources()) {
                try { EObject o = r.getEObject(pkid); if ((o instanceof org.eclipse.syson.sysml.Package || o instanceof org.eclipse.syson.sysml.ViewUsage)) { String nm = en(o); if (nm != null && !nm.isBlank()) { packageNameCache.put(ck, nm); return nm; } } } catch (Exception ig) {}
            }
        } catch (Exception ig) {}
        return fb(pkid);
    }

    public String getTargetObjectId(String pid, String repId) {
        try {
            String s = ctxIdCache.computeIfAbsent(pid, p -> projectEditingContextService.getEditingContextId(p).orElse(pid));
            var metas = representationMetadataRepository.findAllRepresentationMetadataBySemanticDataId(java.util.UUID.fromString(s));
            for (var m : metas) { if (m.getId().contains(repId) && m.getTargetObjectId() != null) return m.getTargetObjectId(); }
            for (var m : metas) { if (m.getTargetObjectId() != null) return m.getTargetObjectId(); }
        } catch (Exception e) { log.warn("TOI: {}", e.getMessage()); }
        return null;
    }

    /** Map Sirius Object ID (from createChild) to EMF elementId */
    public String getElementIdByObjectId(String pid, String objectId) {
        try {
            String ecId = ctxIdCache.computeIfAbsent(pid, p -> projectEditingContextService.getEditingContextId(p).orElse(pid));
            log.info("EIO: looking up objectId={} ecId={}", objectId, ecId);
            var rows = jdbcTemplate.queryForList(
                "SELECT content AS txt FROM document WHERE semantic_data_id = ?::uuid AND name LIKE '%sysml'",
                java.util.UUID.fromString(ecId));
            log.info("EIO: {} docs found", rows.size());
            for (var row : rows) {
                String text = (String) row.get("txt");
                if (text != null && text.contains(objectId)) {
                    // Robust regex: find "elementId": "..." near the objectId
                    int idx = text.indexOf("\"id\":\"" + objectId + "\"");
                    if (idx > 0) {
                        int searchEnd = Math.min(idx + 2000, text.length());
                        var m = java.util.regex.Pattern.compile("\"elementId\"\\s*:\\s*\"([^\"]+)\"")
                                .matcher(text.substring(idx, searchEnd));
                        if (m.find()) {
                            String eid = m.group(1);
                            log.info("EIO: found elementId {}", eid);
                            return eid;
                        }
                    }
                }
            }
        } catch (Exception e) { log.warn("EIO: {}", e.getMessage(), e); }
        return null;
    }

    public boolean renameBySiriusId(String pid, String siriusId, String newName) {
        log.info("renameBySiriusId: pid={} siriusId={} newName={}", pid, siriusId, newName);
        String elementId = getElementIdByObjectId(pid, siriusId);
        if (elementId == null) { log.warn("elementId not found"); return false; }
        try {
            String ecId = ctxIdCache.computeIfAbsent(pid, p -> projectEditingContextService.getEditingContextId(p).orElse(pid));
            // Dispatch through the editing context event processor so the change is applied on the LIVE
            // editing context, triggers a refresh of the explorer tree, and is persisted.
            var input = new RenameMatrixElementInput(java.util.UUID.randomUUID(), ecId, elementId, newName);
            var payload = eventProcessorRegistry.dispatchEvent(ecId, input).block();
            boolean ok = payload instanceof org.eclipse.sirius.components.core.api.SuccessPayload;
            log.info("renameBySiriusId dispatch result: {} (payload={})", ok, payload);
            return ok;
        } catch (Exception e) { log.error("renameBySiriusId: {}", e.getMessage(), e); }
        return false;
    }

    public List<String> getParentChain(String pid, String scopeId) {
        List<String> chain = new ArrayList<>();
        try { var rs = getRS(pid); if (rs == null) return chain;
            for (var r : rs.getResources()) {
                EObject obj = r.getEObject(scopeId);
                if (obj != null) { EObject p = obj.eContainer(); while (p != null) { String n = en(p); if (n != null && !n.isBlank()) chain.add(0, n); p = p.eContainer(); } break; }
            }
        } catch (Exception e) { log.warn("getParentChain: {}", e.getMessage()); }
        return chain;
    }

    public boolean renameElement(String pid, String elementId, String newName) {
        try { var rs = getRS(pid); if (rs == null) return false;
            for (var r : rs.getResources()) { EObject o = r.getEObject(elementId); if (o instanceof org.eclipse.syson.sysml.Element el) { el.setDeclaredName(newName); return true; } }
        } catch (Exception e) { log.warn("rename: {}", e.getMessage()); }
        return false;
    }

    private String fb(String id) { return id.length() > 8 ? id.substring(0, 8) + "..." : id; }

    private List<Map<String, Object>> fetch(String pid, String pkid, java.util.Set<String> ts) {
        List<Map<String, Object>> r = new ArrayList<>(); var rs = getRS(pid); if (rs == null) return r;
        for (var res : rs.getResources()) {
            try { EObject o = res.getEObject(pkid); if (o != null) { coll(o, r, ts); if (!r.isEmpty()) return r; }
                for (EObject x : res.getContents()) { EObject f = findR(x, pkid); if (f != null) { coll(f, r, ts); if (!r.isEmpty()) return r; } }
            } catch (Exception e) {}
        }
        return r;
    }
    private EObject findR(EObject o, String tid) { if (o == null) return null; try { if (tid.equals(o.eResource().getURIFragment(o))) return o; } catch (Exception ig) {} for (EObject c : o.eContents()) { EObject f = findR(c, tid); if (f != null) return f; } return null; }
    private void coll(EObject p, List<Map<String, Object>> r, java.util.Set<String> ts) { coll(p, r, ts, 50); }
    private void coll(EObject p, List<Map<String, Object>> r, java.util.Set<String> ts, int d) { if (d <= 0) return; for (EObject c : p.eContents()) { String n = en(c); String et = c.eClass().getName(); if (n != null && !n.isEmpty()) { String dt = ed(c); if (ts.isEmpty() || ts.contains(et) || ts.contains(dt) || mt(c, ts)) { Map<String, Object> el = new LinkedHashMap<>(); try { el.put("id", c.eResource().getURIFragment(c)); } catch (Exception e) { el.put("id",""); } el.put("name",n); el.put("type",et); el.put("dodafType",dt!=null?dt:""); el.put("parentPath",""); r.add(el); } } coll(c, r, ts, d - 1); } }
    private String ed(EObject o) { try { var f = o.eClass().getEStructuralFeature("aliasIds"); if (f != null && o.eGet(f) instanceof java.util.List<?> l) for (Object a : l) if (a instanceof String s && DODAF_MAP.containsKey(s)) return DODAF_MAP.get(s); } catch (Exception ig) {} return null; }
    private String en(EObject o) { try { if (o instanceof PartUsage x) return x.getDeclaredName(); if (o instanceof RequirementUsage x) return x.getDeclaredName(); if (o instanceof ActionUsage x) return x.getDeclaredName(); if (o instanceof InterfaceUsage x) return x.getDeclaredName(); if (o instanceof org.eclipse.syson.sysml.Package x) return x.getDeclaredName(); } catch (Exception ig) {} try { var f = o.eClass().getEStructuralFeature("declaredName"); if (f != null && o.eGet(f) instanceof String s && !s.isBlank()) return s; } catch (Exception ig) {} return null; }
    private boolean mt(EObject o, java.util.Set<String> ts) { String cn = o.eClass().getName(); for (String t : ts) if (cn.contains(t) || cn.equalsIgnoreCase(t)) return true; return false; }

    private String resolveEc(String pid) { return ctxIdCache.computeIfAbsent(pid, p -> projectEditingContextService.getEditingContextId(p).orElse(pid)); }

    /** Lazily create the persistence table for matrix relations (idempotent). */
    private void ensureTable() {
        if (tableReady) { return; }
        synchronized (this) {
            if (tableReady) { return; }
            try {
                jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS dodaf_matrix_relation (id text PRIMARY KEY, editing_context_id text, source_id text NOT NULL, target_id text NOT NULL, relation_type text NOT NULL, sirius_id text, created_at bigint NOT NULL)");
                tableReady = true;
            } catch (Exception e) { log.error("ensureTable: {}", e.getMessage(), e); }
        }
    }

    private Map<String, Object> rowToRelation(Map<String, Object> row) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", row.get("id"));
        m.put("sourceId", row.get("source_id"));
        m.put("targetId", row.get("target_id"));
        m.put("relationType", row.get("relation_type"));
        Object sid = row.get("sirius_id");
        if (sid instanceof String s && !s.isBlank()) { m.put("siriusId", s); }
        m.put("createdAt", row.get("created_at"));
        return m;
    }

    public List<Map<String, Object>> getRelations() {
        ensureTable();
        List<Map<String, Object>> out = new ArrayList<>();
        try {
            for (var row : jdbcTemplate.queryForList("SELECT * FROM dodaf_matrix_relation ORDER BY created_at")) { out.add(rowToRelation(row)); }
        } catch (Exception e) { log.error("getRelations: {}", e.getMessage(), e); }
        return out;
    }

    /**
     * Return the relations for an editing context, first pruning any relation whose backing SysML element no longer
     * exists in the model (e.g. it was deleted from the Explorer tree). A grace period protects freshly-created
     * relations whose element may not be persisted yet.
     */
    public List<Map<String, Object>> getRelations(String pid) {
        ensureTable();
        if (pid == null || pid.isBlank()) { return getRelations(); }
        String ecId = resolveEc(pid);
        List<Map<String, Object>> rows;
        try {
            rows = jdbcTemplate.queryForList("SELECT * FROM dodaf_matrix_relation WHERE editing_context_id = ? OR editing_context_id IS NULL ORDER BY created_at", ecId);
        } catch (Exception e) { log.error("getRelations(pid): {}", e.getMessage(), e); return new ArrayList<>(); }
        java.util.Set<Object> pruned = new java.util.HashSet<>();
        var withSirius = rows.stream().filter(r -> r.get("sirius_id") instanceof String sid && !sid.isBlank()).toList();
        if (!withSirius.isEmpty()) {
            String docText = loadDocumentText(pid);
            if (docText != null) {
                long now = System.currentTimeMillis();
                for (var r : withSirius) {
                    long createdAt = (r.get("created_at") instanceof Number n) ? n.longValue() : 0L;
                    if (now - createdAt < 3_000L) { continue; } // grace period: element may not be persisted yet
                    String sid = (String) r.get("sirius_id");
                    if (!docText.contains(sid)) {
                        try { jdbcTemplate.update("DELETE FROM dodaf_matrix_relation WHERE id = ?", r.get("id")); } catch (Exception ig) {}
                        pruned.add(r.get("id"));
                        log.info("getRelations: pruned relation {} (element {} no longer in model)", r.get("id"), sid);
                    }
                }
            }
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (var row : rows) { if (!pruned.contains(row.get("id"))) { out.add(rowToRelation(row)); } }
        return out;
    }

    private String loadDocumentText(String pid) {
        try {
            String ecId = ctxIdCache.computeIfAbsent(pid, p -> projectEditingContextService.getEditingContextId(p).orElse(pid));
            var rows = jdbcTemplate.queryForList(
                "SELECT content AS txt FROM document WHERE semantic_data_id = ?::uuid AND name LIKE '%sysml'",
                java.util.UUID.fromString(ecId));
            StringBuilder sb = new StringBuilder();
            for (var row : rows) { Object t = row.get("txt"); if (t != null) { sb.append((String) t); } }
            return sb.length() > 0 ? sb.toString() : null;
        } catch (Exception e) { log.warn("loadDocumentText: {}", e.getMessage()); return null; }
    }

    public Map<String, Object> createRelation(String s, String t, String rt, String siriusId, String ctxId) {
        ensureTable();
        String id = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();
        String ecId = (ctxId != null && !ctxId.isBlank()) ? resolveEc(ctxId) : null;
        String sid = (siriusId != null && !siriusId.isBlank()) ? siriusId : null;
        try {
            jdbcTemplate.update("INSERT INTO dodaf_matrix_relation (id, editing_context_id, source_id, target_id, relation_type, sirius_id, created_at) VALUES (?,?,?,?,?,?,?)",
                    id, ecId, s, t, rt, sid, now);
        } catch (Exception e) { log.error("createRelation: {}", e.getMessage(), e); }
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", id); r.put("sourceId", s); r.put("targetId", t); r.put("relationType", rt);
        if (sid != null) { r.put("siriusId", sid); }
        r.put("createdAt", now);
        return r;
    }

    public void deleteRelation(String id) {
        ensureTable();
        try { jdbcTemplate.update("DELETE FROM dodaf_matrix_relation WHERE id = ?", id); } catch (Exception e) { log.error("deleteRelation: {}", e.getMessage(), e); }
    }

    /** Delete the matrix relation and, if it is backed by a SysML element, delete that element from the model (syncing the Explorer tree). */
    public boolean deleteRelationAndElement(String pid, String id) {
        ensureTable();
        boolean elementDeleted = false;
        try {
            var rows = jdbcTemplate.queryForList("SELECT sirius_id FROM dodaf_matrix_relation WHERE id = ?", id);
            if (!rows.isEmpty() && rows.get(0).get("sirius_id") instanceof String sid && !sid.isBlank()) {
                elementDeleted = deleteBySiriusId(pid, sid);
            }
        } catch (Exception e) { log.error("deleteRelationAndElement: {}", e.getMessage(), e); }
        deleteRelation(id);
        return elementDeleted;
    }

    /** Delete the SysML element identified (indirectly) by a Sirius object id, through the live editing context event processor. */
    public boolean deleteBySiriusId(String pid, String siriusId) {
        log.info("deleteBySiriusId: pid={} siriusId={}", pid, siriusId);
        String elementId = getElementIdByObjectId(pid, siriusId);
        if (elementId == null) { log.warn("deleteBySiriusId: elementId not found"); return false; }
        try {
            String ecId = ctxIdCache.computeIfAbsent(pid, p -> projectEditingContextService.getEditingContextId(p).orElse(pid));
            var input = new DeleteMatrixElementInput(java.util.UUID.randomUUID(), ecId, elementId);
            var payload = eventProcessorRegistry.dispatchEvent(ecId, input).block();
            boolean ok = payload instanceof org.eclipse.sirius.components.core.api.SuccessPayload;
            log.info("deleteBySiriusId dispatch result: {} (payload={})", ok, payload);
            return ok;
        } catch (Exception e) { log.error("deleteBySiriusId: {}", e.getMessage(), e); }
        return false;
    }

    public List<String> getAllElementTypes(String pid) { java.util.LinkedHashSet<String> types = new java.util.LinkedHashSet<>(); var rs = getRS(pid); if (rs == null) return List.of(); for (var r : rs.getResources()) { var it = r.getAllContents(); while (it.hasNext()) types.add(it.next().eClass().getName()); } types.removeIf(t -> t.startsWith("org.eclipse") || t.equals("Usage") || t.contains("Impl")); return new ArrayList<>(types); }
}
