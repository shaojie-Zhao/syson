package org.eclipse.syson.standard.diagrams.view.services;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.syson.sysml.util.SysmlSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rules/default-rules")
public class DoDAFRulesController {
    private static final Logger log = LoggerFactory.getLogger(DoDAFRulesController.class);
    private final Map<String, Rule> rules = new ConcurrentHashMap<>();
    private final AtomicInteger seq = new AtomicInteger(0);
    private final DoDAFMatrixDataService dataService;

    public static class Rule {
        public String id, name, applied, description, ruleType, owner, parentId;
        public List<Rule> children;
    }

    public static class TreeNode {
        public String id, label, type;
    }

    public DoDAFRulesController(DoDAFMatrixDataService dataService) {
        this.dataService = dataService;
        var r1 = new Rule(); r1.id = "1"; r1.applied = "待战状态"; r1.name = ""; r1.description = ""; r1.ruleType = "红方状态"; r1.owner = "";
        rules.put(r1.id, r1);
        seq.set(1);
    }

    @GetMapping("/list")
    public List<Rule> list(@RequestParam(defaultValue = "") String ctxId) {
        log.info("GET /list ctxId={}", ctxId);
        List<Rule> result = new ArrayList<>(rules.values());
        // Build tree: children grouped by parentId
        Map<String, List<Rule>> byParent = new HashMap<>();
        for (Rule r : result) {
            if (r.parentId != null) {
                byParent.computeIfAbsent(r.parentId, k -> new ArrayList<>()).add(r);
            }
        }
        for (Rule r : result) {
            r.children = byParent.get(r.id);
        }
        // Return root nodes only
        return result.stream().filter(r -> r.parentId == null).toList();
    }

    @PostMapping("/add")
    public Rule add(@RequestBody Map<String, String> body) {
        String parentId = body.getOrDefault("parentId", null);
        var r = new Rule();
        r.id = String.valueOf(seq.incrementAndGet());
        r.name = "";
        r.applied = "";
        r.description = "";
        r.ruleType = "";
        r.owner = "";
        r.parentId = parentId;
        rules.put(r.id, r);
        log.info("POST /add id={} parent={}", r.id, parentId);
        return r;
    }

    @PutMapping("/{id}")
    public Rule update(@PathVariable String id, @RequestBody Map<String, String> body) {
        var r = rules.get(id);
        if (r == null) return null;
        String field = body.get("field");
        String value = body.get("value");
        if (field != null && value != null) {
            switch (field) {
                case "name": r.name = value; break;
                case "applied": r.applied = value; break;
                case "nodeId": r.applied = body.getOrDefault("value", r.applied); break; // store nodeId, display uses label
                case "description": r.description = value; break;
                case "ruleType": r.ruleType = value; break;
                case "owner": r.owner = value; break;
            }
        }
        log.info("PUT /{}/{} = {}", id, field, value);
        return r;
    }

    @DeleteMapping("/{id}")
    public Map<String, String> delete(@PathVariable String id, @RequestBody Map<String, String> body) {
        rules.remove(id);
        // Also remove children
        rules.values().removeIf(r -> id.equals(r.parentId));
        log.info("DELETE /" + id);
        return Map.of("result", "ok");
    }

    @GetMapping("/elements")
    public List<Map<String, Object>> elements(@RequestParam(defaultValue = "") String ctxId, @RequestParam(defaultValue = "") String pid,
          @RequestParam(defaultValue = "") String types, @RequestParam(defaultValue = "") String scope) {
        log.info("GET /elements ctxId={} pid={} types={} scope={}", ctxId, pid, types, scope);
        return dataService.getElements(types.isEmpty() ? null : types, pid.isEmpty() ? ctxId : pid, scope.isEmpty() ? null : scope, null);
    }

    @GetMapping("/scope-name")
    public Map<String, String> scopeName(@RequestParam(defaultValue = "") String ctxId, @RequestParam(defaultValue = "") String id) {
        String name = dataService.getPackageName(ctxId, id);
        return Map.of("id", id, "name", name == null ? "" : name);
    }

    @GetMapping("/tree-nodes")
    public List<TreeNode> treeNodes(@RequestParam(defaultValue = "") String ctxId, @RequestParam(defaultValue = "") String pid) {
        log.info("GET /tree-nodes ctxId={} pid={}", ctxId, pid);
        List<TreeNode> result = new ArrayList<>();
        try {
            var elements = dataService.getElements(null, pid.isEmpty() ? ctxId : pid, null, null);
            if (elements != null) {
                for (var e : elements) {
                    var tn = new TreeNode(); tn.id = (String) e.get("id"); tn.label = (String) e.get("label"); tn.type = (String) e.getOrDefault("kind", "");
                    result.add(tn);
                }
            }
        } catch (Exception ex) { log.warn("tree-nodes: {}", ex.getMessage()); }
        return result;
    }

    @PostMapping("/sync")
    public Map<String, String> sync(@RequestParam(defaultValue = "") String ctxId) {
        log.info("POST /sync ctxId={}", ctxId);
        return Map.of("result", "ok", "message", "同步完成");
    }
}
