package org.eclipse.syson.standard.diagrams.view.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.sirius.components.core.api.IEditingContextSearchService;
import org.eclipse.sirius.components.emf.services.api.IEMFEditingContext;
import org.eclipse.sirius.web.application.project.services.api.IProjectEditingContextService;
import org.eclipse.sirius.web.domain.boundedcontexts.project.repositories.IProjectRepository;
import org.eclipse.syson.sysml.PartUsage;
import org.eclipse.syson.sysml.RequirementUsage;
import org.eclipse.syson.sysml.ActionUsage;
import org.eclipse.syson.sysml.InterfaceUsage;
import org.eclipse.syson.sysml.Package;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Data service for DoDAF Matrix View.
 * Queries the SysML model via Sirius Web EditingContext when a Package scope is provided.
 */
@Service
public class DoDAFMatrixDataService {

    private static final Logger log = LoggerFactory.getLogger(DoDAFMatrixDataService.class);

    private final IEditingContextSearchService editingContextSearchService;
    private final IProjectEditingContextService projectEditingContextService;
    private final IProjectRepository projectRepository;

    private static final Map<String, Map<String, Object>> relations = new ConcurrentHashMap<>();
    private static final Map<String, List<Map<String, Object>>> elementCache = new ConcurrentHashMap<>();
    private static final Map<String, String> packageNameCache = new ConcurrentHashMap<>();
    private static final Map<String, String> ctxIdCache = new ConcurrentHashMap<>();
    private static final Map<String, org.eclipse.emf.ecore.resource.ResourceSet> resourceSetCache = new ConcurrentHashMap<>();

    public DoDAFMatrixDataService(IEditingContextSearchService editingContextSearchService,
                                   IProjectEditingContextService projectEditingContextService,
                                   IProjectRepository projectRepository) {
        this.editingContextSearchService = editingContextSearchService;
        this.projectEditingContextService = projectEditingContextService;
        this.projectRepository = projectRepository;
    }

    public List<Map<String, Object>> getElements(String typesParam, String projectId, String scope, String unused) {
        java.util.Set<String> typeSet = new java.util.HashSet<>();
        if (typesParam != null && !typesParam.isBlank() && !"ALL".equalsIgnoreCase(typesParam)) {
            typeSet.addAll(Arrays.asList(typesParam.split("\\s*,\\s*")));
        }

        String cacheKey = projectId + ":" + scope + ":" + typesParam;
        List<Map<String, Object>> cached = elementCache.get(cacheKey);
        if (cached != null) {
            log.debug("Cache hit for {}", cacheKey);
            return cached;
        }

        List<Map<String, Object>> result;
        if (scope != null && !scope.isBlank()) {
            result = fetchFromModel(projectId, scope, typeSet);
            if (!result.isEmpty()) {
                log.info("Found {} model elements, caching", result.size());
                elementCache.put(cacheKey, result);
                return result;
            }
        }
        return List.of();
    }

    private org.eclipse.emf.ecore.resource.ResourceSet getResourceSet(String projectId) {
        return resourceSetCache.computeIfAbsent(projectId, pid -> {
            try {
                String ctxId = ctxIdCache.computeIfAbsent(pid,
                    p -> projectEditingContextService.getEditingContextId(p).orElse(null));
                if (ctxId == null) return null;
                var optCtx = editingContextSearchService.findById(ctxId);
                if (optCtx.isPresent() && optCtx.get() instanceof IEMFEditingContext emfCtx) {
                    log.info("ResourceSet loaded & cached for project {}", pid);
                    return emfCtx.getDomain().getResourceSet();
                }
            } catch (Exception e) {
                log.error("Failed to load ResourceSet: {}", e.getMessage());
            }
            return null;
        });
    }

    public String getPackageName(String projectId, String packageId) {
        String cacheKey = projectId + ":" + packageId;
        String cached = packageNameCache.get(cacheKey);
        if (cached != null) return cached;

        try {
            var resourceSet = getResourceSet(projectId);
            if (resourceSet == null) return fallbackName(packageId);
            for (var resource : resourceSet.getResources()) {
                try {
                    EObject pkg = resource.getEObject(packageId);
                    String name = null;
                    if (pkg instanceof Package sp && sp.getDeclaredName() != null) {
                        name = sp.getDeclaredName();
                    } else if (pkg != null) {
                        name = extractName(pkg);
                    }
                    if (name != null && !name.isBlank()) {
                        packageNameCache.put(cacheKey, name);
                        return name;
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return fallbackName(packageId);
    }

    private String fallbackName(String id) {
        return id.length() > 8 ? id.substring(0, 8) + "..." : id;
    }

    private List<Map<String, Object>> fetchFromModel(String projectId, String packageId, java.util.Set<String> typeSet) {
        List<Map<String, Object>> result = new ArrayList<>();
        var resourceSet = getResourceSet(projectId);
        if (resourceSet == null) return result;

        for (var resource : resourceSet.getResources()) {
            try {
                EObject pkg = resource.getEObject(packageId);
                if (pkg != null) {
                    String pkgName = extractName(pkg);
                    if (pkgName != null) packageNameCache.put(projectId + ":" + packageId, pkgName);
                    collectChildren(pkg, result, typeSet);
                    if (!result.isEmpty()) return result;
                }
                for (EObject obj : resource.getContents()) {
                    EObject found = findPackageRecursive(obj, packageId);
                    if (found != null) {
                        String pkgName = extractName(found);
                        if (pkgName != null) packageNameCache.put(projectId + ":" + packageId, pkgName);
                        collectChildren(found, result, typeSet);
                        if (!result.isEmpty()) return result;
                    }
                }
            } catch (Exception e) {
                log.debug("Error searching resource: {}", e.getMessage());
            }
        }
        return result;
    }

    private EObject findPackageRecursive(EObject obj, String targetId) {
        if (obj == null) return null;
        try {
            if (targetId.equals(obj.eResource().getURIFragment(obj))) return obj;
        } catch (Exception ignored) {}
        for (EObject child : obj.eContents()) {
            EObject found = findPackageRecursive(child, targetId);
            if (found != null) return found;
        }
        return null;
    }

    private void collectChildren(EObject parent, List<Map<String, Object>> result, java.util.Set<String> typeSet) {
        collectChildren(parent, result, typeSet, 4);
    }

    private void collectChildren(EObject parent, List<Map<String, Object>> result, java.util.Set<String> typeSet, int maxDepth) {
        if (maxDepth <= 0) return;
        for (EObject child : parent.eContents()) {
            String name = extractName(child);
            String emfType = child.eClass().getName();
            if (name != null && !name.isEmpty()) {
                // Extract DoDAF semantic type from aliasIds
                String dodafType = extractDodafType(child);
                String displayType = dodafType != null ? dodafType : emfType;
                if (typeSet.isEmpty() || typeSet.contains(emfType) || typeSet.contains(dodafType) || matchesType(child, typeSet)) {
                    Map<String, Object> el = new LinkedHashMap<>();
                    try { el.put("id", child.eResource().getURIFragment(child)); } catch (Exception e) { el.put("id", ""); }
                    el.put("name", name);
                    el.put("type", emfType);
                    el.put("dodafType", dodafType != null ? dodafType : "");
                    el.put("displayType", displayType);
                    el.put("parentPath", "");
                    result.add(el);
                }
            }
            collectChildren(child, result, typeSet, maxDepth - 1);
        }
    }

    /** Extract DoDAF semantic type from aliasIds (e.g., dodaf:capability → Capability) */
    private static final Map<String, String> DODAF_ALIAS_MAP = Map.of(
        "dodaf:capability", "Capability",
        "dodaf:operational", "OperationalNode",
        "dodaf:system", "SystemNode",
        "dodaf:organization", "Organization",
        "dodaf:exchange", "InformationExchange"
    );

    private String extractDodafType(EObject obj) {
        try {
            var aliasFeature = obj.eClass().getEStructuralFeature("aliasIds");
            if (aliasFeature != null) {
                Object aliases = obj.eGet(aliasFeature);
                if (aliases instanceof java.util.List<?> list) {
                    for (Object alias : list) {
                        if (alias instanceof String s) {
                            String mapped = DODAF_ALIAS_MAP.get(s);
                            if (mapped != null) return mapped;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String extractName(EObject obj) {
        try {
            if (obj instanceof PartUsage pu) return pu.getDeclaredName();
            if (obj instanceof RequirementUsage ru) return ru.getDeclaredName();
            if (obj instanceof ActionUsage au) return au.getDeclaredName();
            if (obj instanceof InterfaceUsage iu) return iu.getDeclaredName();
            if (obj instanceof Package pkg) return pkg.getDeclaredName();
        } catch (Exception ignored) {}
        try {
            var f = obj.eClass().getEStructuralFeature("declaredName");
            if (f != null) { Object v = obj.eGet(f); if (v instanceof String s && !s.isBlank()) return s; }
            var f2 = obj.eClass().getEStructuralFeature("name");
            if (f2 != null) { Object v = obj.eGet(f2); if (v instanceof String s && !s.isBlank()) return s; }
        } catch (Exception ignored) {}
        return null;
    }

    private boolean matchesType(EObject obj, java.util.Set<String> typeSet) {
        String cn = obj.eClass().getName();
        for (String t : typeSet) {
            if (cn.contains(t) || cn.equalsIgnoreCase(t)) return true;
        }
        return false;
    }

    public List<Map<String, Object>> getRelations() {
        return new ArrayList<>(relations.values());
    }

    public Map<String, Object> createRelation(String sourceId, String targetId, String relationType) {
        String id = UUID.randomUUID().toString();
        Map<String, Object> rel = new LinkedHashMap<>();
        rel.put("id", id);
        rel.put("sourceId", sourceId);
        rel.put("targetId", targetId);
        rel.put("relationType", relationType);
        rel.put("createdAt", System.currentTimeMillis());
        relations.put(id, rel);
        return rel;
    }

    public void deleteRelation(String id) {
        relations.remove(id);
    }
}
