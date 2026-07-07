package org.eclipse.syson.application.ov1;

import java.util.Objects;
import java.util.UUID;

import org.eclipse.sirius.components.events.ICause;
import org.eclipse.sirius.components.core.api.IEditingContextPersistenceService;
import org.eclipse.sirius.components.core.api.IEditingContextSearchService;
import org.eclipse.sirius.components.core.api.IObjectSearchService;
import org.eclipse.syson.sysml.Element;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Ov1ElementRenameController {

    private final IEditingContextSearchService editingContextSearchService;
    private final IEditingContextPersistenceService editingContextPersistenceService;
    private final IObjectSearchService objectSearchService;

    public Ov1ElementRenameController(IEditingContextSearchService editingContextSearchService,
            IEditingContextPersistenceService editingContextPersistenceService,
            IObjectSearchService objectSearchService) {
        this.editingContextSearchService = Objects.requireNonNull(editingContextSearchService);
        this.editingContextPersistenceService = Objects.requireNonNull(editingContextPersistenceService);
        this.objectSearchService = Objects.requireNonNull(objectSearchService);
    }

    @PostMapping("/api/ov1/rename")
    public ResponseEntity<?> renameElement(@RequestBody RenameRequest request) {
        var ctx = this.editingContextSearchService.findById(request.editingContextId());
        if (ctx.isEmpty()) return ResponseEntity.badRequest().body("ctx_not_found");
        var editingContext = ctx.get();

        var found = this.objectSearchService.getObject(editingContext, request.elementId())
                .filter(Element.class::isInstance).map(Element.class::cast);
        if (found.isEmpty()) return ResponseEntity.badRequest().body("element_not_found");

        var element = found.get();
        element.setDeclaredName(request.newName());
        this.editingContextPersistenceService.persist(new Ov1RenameCause(UUID.randomUUID()), editingContext);
        return ResponseEntity.ok().build();
    }

    public record RenameRequest(String editingContextId, String elementId, String newName) {}

    private record Ov1RenameCause(UUID id) implements ICause {}
}
