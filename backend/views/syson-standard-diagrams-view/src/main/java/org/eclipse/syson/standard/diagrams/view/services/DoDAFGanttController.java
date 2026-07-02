package org.eclipse.syson.standard.diagrams.view.services;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controllers for DoDAF Gantt and Matrix views.
 */
@RestController
public class DoDAFGanttController {

    private static final Logger log = LoggerFactory.getLogger(DoDAFGanttController.class);
    private final DoDAFMatrixDataService matrixDataService;

    public DoDAFGanttController(DoDAFMatrixDataService matrixDataService) {
        this.matrixDataService = matrixDataService;
    }

    // ===== Gantt API =====

    @GetMapping("/api/gantt/{repId}/tasks")
    public List<DoDAFGanttDataService.GanttTaskData> getTasks(@PathVariable String repId) {
        return DoDAFGanttDataService.getTasks(repId);
    }

    @PostMapping("/api/gantt/{repId}/tasks")
    public DoDAFGanttDataService.GanttTaskData createTask(@PathVariable String repId, @RequestParam(defaultValue = "") String parentId) {
        return DoDAFGanttDataService.createTask(repId, parentId.isEmpty() ? null : parentId);
    }

    @PutMapping("/api/gantt/{repId}/tasks/{taskId}")
    public DoDAFGanttDataService.GanttTaskData updateTask(@PathVariable String repId, @PathVariable String taskId,
            @RequestBody DoDAFGanttDataService.GanttTaskData updated) {
        return DoDAFGanttDataService.updateTask(repId, updated);
    }

    @DeleteMapping("/api/gantt/{repId}/tasks/{taskId}")
    public void deleteTask(@PathVariable String repId, @PathVariable String taskId) {
        DoDAFGanttDataService.deleteTask(repId, taskId);
    }

    // ===== Matrix API =====

    @GetMapping("/api/matrix/{repId}/elements")
    public List<Map<String, Object>> getMatrixElements(@PathVariable String repId,
            @RequestParam(defaultValue = "ALL") String types,
            @RequestParam(defaultValue = "") String scope,
            @RequestParam(defaultValue = "") String ctxId) {
        log.info("GET /api/matrix/{}/elements types={} scope={} ctxId={}", repId, types, scope, ctxId);
        var result = matrixDataService.getElements(types, ctxId, scope, "");
        // Attach scope name as the first element (special marker entry)
        if (scope != null && !scope.isBlank() && !result.isEmpty()) {
            String scopeName = matrixDataService.getPackageName(ctxId, scope);
            // Prepend a __meta__ entry with scope name
            Map<String, Object> meta = new java.util.LinkedHashMap<>();
            meta.put("id", "__scope_meta__");
            meta.put("name", scopeName);
            meta.put("type", "__meta__");
            meta.put("parentPath", scope);
            result.add(0, meta);
        }
        return result;
    }

    @GetMapping("/api/matrix/{repId}/scope-name")
    public Map<String, String> getScopeName(@PathVariable String repId,
            @RequestParam String id,
            @RequestParam(defaultValue = "") String ctxId) {
        log.info("GET /api/matrix/{}/scope-name id={} ctxId={}", repId, id, ctxId);
        String name = matrixDataService.getPackageName(ctxId, id);
        return Map.of("id", id, "name", name);
    }

    @GetMapping("/api/matrix/{repId}/relations")
    public List<Map<String, Object>> getRelations(@PathVariable String repId) {
        return matrixDataService.getRelations();
    }

    @PostMapping("/api/matrix/{repId}/relations")
    public Map<String, Object> createRelation(@PathVariable String repId, @RequestBody Map<String, Object> rel) {
        return matrixDataService.createRelation(
                (String) rel.get("sourceId"),
                (String) rel.get("targetId"),
                (String) rel.get("relationType"));
    }

    @DeleteMapping("/api/matrix/{repId}/relations/{relId}")
    public void deleteRelation(@PathVariable String repId, @PathVariable String relId) {
        matrixDataService.deleteRelation(relId);
    }
}
