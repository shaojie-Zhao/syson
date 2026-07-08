package org.eclipse.syson.standard.diagrams.view.services;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
public class DoDAFGanttController {
    private static final Logger log = LoggerFactory.getLogger(DoDAFGanttController.class);
    private final DoDAFMatrixDataService s;

    public DoDAFGanttController(DoDAFMatrixDataService s) { this.s = s; }

    // Gantt
    @GetMapping("/api/gantt/{repId}/tasks") public List<DoDAFGanttDataService.GanttTaskData> getTasks(@PathVariable String repId) { return DoDAFGanttDataService.getTasks(repId); }
    @PostMapping("/api/gantt/{repId}/tasks") public DoDAFGanttDataService.GanttTaskData createTask(@PathVariable String repId, @RequestParam(defaultValue="") String parentId) { return DoDAFGanttDataService.createTask(repId, parentId.isEmpty()?null:parentId); }
    @PutMapping("/api/gantt/{repId}/tasks/{taskId}") public DoDAFGanttDataService.GanttTaskData updateTask(@PathVariable String repId, @PathVariable String taskId, @RequestBody DoDAFGanttDataService.GanttTaskData u) { return DoDAFGanttDataService.updateTask(repId, u); }
    @DeleteMapping("/api/gantt/{repId}/tasks/{taskId}") public void deleteTask(@PathVariable String repId, @PathVariable String taskId) { DoDAFGanttDataService.deleteTask(repId, taskId); }

    // Matrix
    @GetMapping("/api/matrix/{repId}/elements")
    public List<Map<String, Object>> getElements(@PathVariable String repId, @RequestParam(defaultValue="ALL") String types, @RequestParam(defaultValue="") String scope, @RequestParam(defaultValue="") String ctxId) {
        return s.getElements(types, ctxId, scope, "");
    }
    @GetMapping("/api/matrix/{repId}/target-object-id") public Map<String,String> getTargetObjectId(@PathVariable String repId, @RequestParam String ctxId, @RequestParam(defaultValue="") String matrixRepId) { String effectiveRepId = matrixRepId.isBlank() ? repId : matrixRepId; String toi = s.getTargetObjectId(ctxId, effectiveRepId); String ec = s.getEditingContextId(ctxId); Map<String,String> r = new java.util.HashMap<>(); r.put("targetObjectId", toi == null ? "" : toi); r.put("editingContextId", ec == null ? "" : ec); return r; }
    @GetMapping("/api/matrix/{repId}/scope-name") public Map<String,String> getScopeName(@PathVariable String repId, @RequestParam String id, @RequestParam(defaultValue="") String ctxId) { return Map.of("id",id,"name",s.getPackageName(ctxId,id)); }
    @GetMapping("/api/matrix/{repId}/element-id") public Map<String,String> getElementId(@PathVariable String repId, @RequestParam String ctxId, @RequestParam String objectId) { return Map.of("elementId",s.getElementIdByObjectId(ctxId,objectId)); }
    @PostMapping("/api/matrix/{repId}/rename-by-sirius") public Map<String,String> renameBySirius(@PathVariable String repId, @RequestBody Map<String,String> b) { return Map.of("result",s.renameBySiriusId(b.get("ctxId"),b.get("siriusId"),b.get("newName"))?"ok":"failed"); }
    @PostMapping("/api/matrix/{repId}/rename") public Map<String,String> renameElement(@PathVariable String repId, @RequestBody Map<String,String> b) { return Map.of("result",s.renameElement(b.get("ctxId"),b.get("elementId"),b.get("newName"))?"ok":"failed"); }
    @GetMapping("/api/matrix/{repId}/relations") public List<Map<String,Object>> getRelations(@PathVariable String repId, @RequestParam(defaultValue="") String ctxId) { return ctxId.isEmpty() ? s.getRelations() : s.getRelations(ctxId); }
    @PostMapping("/api/matrix/{repId}/relations") public Map<String,Object> createRelation(@PathVariable String repId, @RequestBody Map<String,Object> r) { return s.createRelation((String)r.get("sourceId"),(String)r.get("targetId"),(String)r.get("relationType"),(String)r.get("siriusId"),(String)r.get("ctxId")); }
    @DeleteMapping("/api/matrix/{repId}/relations/{relId}") public void deleteRelation(@PathVariable String repId, @PathVariable String relId, @RequestParam(defaultValue="") String ctxId) { if (ctxId.isEmpty()) { s.deleteRelation(relId); } else { s.deleteRelationAndElement(ctxId, relId); } }
    @GetMapping("/api/projects/{projectId}/element-types") public List<String> getElementTypes(@PathVariable String projectId) { return s.getAllElementTypes(projectId); }
}
