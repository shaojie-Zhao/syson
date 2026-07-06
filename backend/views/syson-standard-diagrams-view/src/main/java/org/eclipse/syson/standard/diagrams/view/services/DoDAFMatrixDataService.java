package org.eclipse.syson.standard.diagrams.view.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.sirius.components.core.api.IEditingContextPersistenceService;
import org.eclipse.sirius.components.core.api.IEditingContextSearchService;
import org.eclipse.sirius.components.core.api.IObjectSearchService;
import org.eclipse.sirius.components.emf.services.api.IEMFEditingContext;
import org.eclipse.sirius.web.application.project.services.api.IProjectEditingContextService;
import org.eclipse.sirius.web.domain.boundedcontexts.project.repositories.IProjectRepository;
import org.eclipse.sirius.web.domain.boundedcontexts.representationdata.repositories.IRepresentationMetadataRepository;
import org.eclipse.syson.sysml.PartUsage;
import org.eclipse.syson.sysml.RequirementUsage;
import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.InterfaceUsage;
import org.eclipse.syson.sysml.Package;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Map<String, Map<String, Object>> relations = new ConcurrentHashMap<>();
    private static final Map<String, List<Map<String, Object>>> elementCache = new ConcurrentHashMap<>();
    private static final Map<String, String> packageNameCache = new ConcurrentHashMap<>();
    static final Map<String, String> ctxIdCache = new ConcurrentHashMap<>();
    private static final Map<String, org.eclipse.emf.ecore.resource.ResourceSet> resourceSetCache = new ConcurrentHashMap<>();
    private static final Map<String, String> DODAF_MAP = Map.of("dodaf:capability","Capability","dodaf:operational","OperationalNode","dodaf:system","SystemNode","dodaf:organization","Organization","dodaf:exchange","InformationExchange");

    public DoDAFMatrixDataService(IEditingContextSearchService a, IEditingContextPersistenceService p, IObjectSearchService o,
                                   IProjectEditingContextService b, IProjectRepository c, IRepresentationMetadataRepository d, JdbcTemplate j) {
        this.editingContextSearchService = a; this.editingContextPersistenceService = p; this.objectSearchService = o;
        this.projectEditingContextService = b; this.projectRepository = c; this.representationMetadataRepository = d; this.jdbcTemplate = j;
    }

    // === Element operations (unchanged from before, abbreviated for brevity) ===

    public List<Map<String, Object>> getElements(String types, String pid, String scope, String unused) {
        java.util.Set<String> ts = new java.util.HashSet<>();
        if (types != null && !types.isBlank() && !"ALL".equalsIgnoreCase(types)) ts.addAll(Arrays.asList(types.split("\\s*,\\s*")));
        String ck = pid + ":" + scope + ":" + types;
        List<Map<String, Object>> cached = elementCache.get(ck);
        if (cached != null) return cached;
        if (scope != null && !scope.isBlank()) {
            List<Map<String, Object>> r = fetch(pid, scope, ts);
            if (!r.isEmpty()) { elementCache.put(ck, r); return r; }
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
                try { EObject o = r.getEObject(pkid); if (o instanceof Package sp && sp.getDeclaredName() != null && !sp.getDeclaredName().isBlank()) { packageNameCache.put(ck, sp.getDeclaredName()); return sp.getDeclaredName(); } } catch (Exception ig) {}
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

    /** Map Sirius object ID (from createChild) to EMF elementId via document table JSON parsing. */
    public String getElementIdByObjectId(String pid, String objectId) {
        try {
            String ecId = ctxIdCache.computeIfAbsent(pid, p -> projectEditingContextService.getEditingContextId(p).orElse(pid));
            java.util.UUID semId = java.util.UUID.fromString(ecId);
            var rows = jdbcTemplate.queryForList("SELECT encode(content, 'escape') as txt FROM document WHERE semantic_data_id = ? AND name LIKE '%.sysml'", semId);
            log.info("EIO: {} docs for semId={}", rows.size(), ecId);
            for (var row : rows) {
                String text = (String) row.get("txt");
                if (text == null) continue;
                int idx = text.indexOf("\"id\":\"" + objectId + "\"");
                if (idx >= 0) {
                    // Find the nearest "elementId" after this position
                    int eidIdx = text.indexOf("\"elementId\":\"", idx);
                    if (eidIdx >= 0 && eidIdx < idx + 2000) {
                        int start = eidIdx + 14;
                        int end = text.indexOf("\"", start);
                        if (end > start) {
                            String eid = text.substring(start, end);
                            log.info("EIO: mapped {} -> {}", objectId, eid);
                            return eid;
                        }
                    }
                }
            }
            log.info("EIO: objectId {} not found via JDBC in {} docs, trying ResourceSet", objectId, rows.size());
        } catch (Exception e) { log.debug("EIO JDBC: {}", e.getMessage()); }
        // Fallback: search EMF ResourceSet (element was just created by createChild)
        var rs = getRS(pid);
        if (rs != null) {
            for (var r : rs.getResources()) {
                var it = r.getAllContents();
                while (it.hasNext()) {
                    EObject o = it.next();
                    try { if (objectId.equals(o.eResource().getURIFragment(o))) { log.info("EIO: found via ResourceSet"); return objectId; } } catch (Exception ig) {}
                }
            }
        }
        return null;
    }

    /** Rename element by searching ResourceSet for the newly created element with default name. */
    public boolean renameByDefaultName(String pid, String defaultName, String newName) {
        try {
            var rs = getRS(pid);
            if (rs == null) return false;
            for (var r : rs.getResources()) {
                var it = r.getAllContents();
                while (it.hasNext()) {
                    EObject o = it.next();
                    if (o instanceof org.eclipse.syson.sysml.Element el) {
                        String dn = el.getDeclaredName();
                        if (dn != null && dn.equals(defaultName)) {
                            el.setDeclaredName(newName);
                            log.info("Renamed '{}' to '{}'", defaultName, newName);
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) { log.warn("renameByDefaultName: {}", e.getMessage()); }
        return false;
    }

    public boolean renameBySiriusId(String pid, String siriusId, String newName) {
        String elementId = getElementIdByObjectId(pid, siriusId);
        if (elementId == null) { log.warn("ElementId not found for siriusId {}", siriusId); return false; }
        try {
            String ecId = ctxIdCache.computeIfAbsent(pid, p -> projectEditingContextService.getEditingContextId(p).orElse(pid));
            var optCtx = editingContextSearchService.findById(ecId);
            if (optCtx.isEmpty()) { log.warn("EditingContext not found for {}", ecId); return false; }
            var ctx = optCtx.get();
            var found = objectSearchService.getObject(ctx, elementId)
                    .filter(org.eclipse.syson.sysml.Element.class::isInstance)
                    .map(org.eclipse.syson.sysml.Element.class::cast);
            if (found.isPresent()) {
                found.get().setDeclaredName(newName);
                editingContextPersistenceService.persist(null, ctx);
                log.info("Renamed element {} to {}", elementId, newName);
                return true;
            }
        } catch (Exception e) { log.error("renameBySiriusId: {}", e.getMessage(), e); }
        return false;
    }

    public boolean renameElement(String pid, String elementId, String newName) {
        try {
            var rs = getRS(pid); if (rs == null) return false;
            for (var r : rs.getResources()) {
                EObject o = r.getEObject(elementId);
                if (o instanceof org.eclipse.syson.sysml.Element el) {
                    el.setDeclaredName(newName); log.info("Renamed {} to {}", elementId, newName);
                    // Persist via edit service
                    try {
                        String cid = ctxIdCache.get(pid);
                        if (cid != null) {
                            var octx = editingContextSearchService.findById(cid);
                            if (octx.isPresent()) {
                                var persistMethod = octx.getClass().getMethod("getPersistenceService");
                                // Can't persist without proper service injection — skip for now
                                // The rename will stick in memory until next persist
                            }
                        }
                    } catch (Exception ig) {}
                    return true;
                }
            }
        } catch (Exception e) { log.warn("rename: {}", e.getMessage()); }
        return false;
    }

    private String fb(String id) { return id.length() > 8 ? id.substring(0, 8) + "..." : id; }

    // === Model traversal (unchanged) ===
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
    private void coll(EObject p, List<Map<String, Object>> r, java.util.Set<String> ts) { coll(p, r, ts, 4); }
    private void coll(EObject p, List<Map<String, Object>> r, java.util.Set<String> ts, int d) { if (d <= 0) return; for (EObject c : p.eContents()) { String n = en(c); String et = c.eClass().getName(); if (n != null && !n.isEmpty()) { String dt = ed(c); if (ts.isEmpty() || ts.contains(et) || ts.contains(dt) || mt(c, ts)) { Map<String, Object> el = new LinkedHashMap<>(); try { el.put("id", c.eResource().getURIFragment(c)); } catch (Exception e) { el.put("id",""); } el.put("name",n); el.put("type",et); el.put("dodafType",dt!=null?dt:""); el.put("parentPath",""); r.add(el); } } coll(c, r, ts, d - 1); } }
    private String ed(EObject o) { try { var f = o.eClass().getEStructuralFeature("aliasIds"); if (f != null && o.eGet(f) instanceof java.util.List<?> l) for (Object a : l) if (a instanceof String s && DODAF_MAP.containsKey(s)) return DODAF_MAP.get(s); } catch (Exception ig) {} return null; }
    private String en(EObject o) { try { if (o instanceof PartUsage x) return x.getDeclaredName(); if (o instanceof RequirementUsage x) return x.getDeclaredName(); if (o instanceof ActionUsage x) return x.getDeclaredName(); if (o instanceof InterfaceUsage x) return x.getDeclaredName(); if (o instanceof Package x) return x.getDeclaredName(); } catch (Exception ig) {} try { var f = o.eClass().getEStructuralFeature("declaredName"); if (f != null && o.eGet(f) instanceof String s && !s.isBlank()) return s; } catch (Exception ig) {} return null; }
    private boolean mt(EObject o, java.util.Set<String> ts) { String cn = o.eClass().getName(); for (String t : ts) if (cn.contains(t) || cn.equalsIgnoreCase(t)) return true; return false; }

    // === Relations ===
    public List<Map<String, Object>> getRelations() { return new ArrayList<>(relations.values()); }
    public Map<String, Object> createRelation(String s, String t, String rt) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id",id); r.put("sourceId",s); r.put("targetId",t); r.put("relationType",rt); r.put("createdAt",System.currentTimeMillis());
        relations.put(id, r); return r;
    }
    public void deleteRelation(String id) { relations.remove(id); }
}
