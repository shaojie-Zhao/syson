package org.eclipse.syson.standard.diagrams.view.services;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rules/default-rules")
public class DoDAFRulesController {
    private static final Logger log = LoggerFactory.getLogger(DoDAFRulesController.class);
    private final Map<String, Rule> rules = new ConcurrentHashMap<>();
    private final AtomicInteger seq = new AtomicInteger(0);

    public static class Rule {
        public String id, name, applied, description, ruleType, owner, parentId;
        public List<Rule> children;
    }

    public DoDAFRulesController() {
        // Demo data
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

    @PostMapping("/sync")
    public Map<String, String> sync(@RequestParam(defaultValue = "") String ctxId) {
        log.info("POST /sync ctxId={}", ctxId);
        // Simulate sync - in real impl would fetch from remote
        return Map.of("result", "ok", "message", "同步完成");
    }
}
