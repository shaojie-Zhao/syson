package org.eclipse.syson.standard.diagrams.view.services;

import java.util.*;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.syson.sysml.Dependency;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.SysmlFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sequence")
public class DoDAFSequenceEdgeController {

    private static final Logger log = LoggerFactory.getLogger(DoDAFSequenceEdgeController.class);
    private final DoDAFMatrixDataService dataService;

    public DoDAFSequenceEdgeController(DoDAFMatrixDataService dataService) {
        this.dataService = dataService;
    }

    public static class CreateEdgeRequest {
        public String editingContextId;
        public String sourceElementId;
        public String targetElementId;
        public String type;
    }

    @PostMapping("/edge/create")
    public Map<String, Object> createEdge(@RequestBody CreateEdgeRequest req) {
        log.info("POST /api/sequence/edge/create: src={} tgt={}", req.sourceElementId, req.targetElementId);
        Map<String, Object> result = new HashMap<>();
        try {
            var rs = dataService.getRSByEcId(req.editingContextId);
            if (rs == null) {
                result.put("error", "ResourceSet not found for: " + req.editingContextId);
                return result;
            }
            // Search for elements by name (from Lifeline label) rather than fragment ID
            Element src = null, tgt = null;
            System.out.println("Searching for srcName=" + req.sourceElementId + " tgtName=" + req.targetElementId);
            for (Resource r : rs.getResources()) {
                var it = r.getAllContents();
                while (it.hasNext()) {
                    var obj = it.next();
                    if (obj instanceof Element e) {
                        String name = e.getName();
                        if (name != null && name.equals(req.sourceElementId)) src = e;
                        if (name != null && name.equals(req.targetElementId)) tgt = e;
                        // Also try matching by ID if name-based fails
                        String fid = r.getURIFragment(e);
                        if (fid != null) {
                            if (fid.equals(req.sourceElementId)) src = e;
                            if (fid.equals(req.targetElementId)) tgt = e;
                        }
                    }
                }
            }
            if (src == null || tgt == null) {
                result.put("error", "Elements not found");
                return result;
            }
            Dependency dep = SysmlFactory.eINSTANCE.createDependency();
            dep.getClient().add(src);
            dep.getSupplier().add(tgt);
            EObject container = src.eContainer();
            if (container instanceof Element ce) {
                ce.getOwnedRelationship().add(dep);
            }
            String depId = dep.eResource() != null ? dep.eResource().getURIFragment(dep) : "";
            result.put("id", depId);
            result.put("success", true);
            log.info("Dependency created: id={}", depId);
        } catch (Exception e) {
            log.error("Edge create failed: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
        }
        return result;
    }
}
