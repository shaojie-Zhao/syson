package org.eclipse.syson.application.nodes.dto;

import java.util.List;
import java.util.UUID;
import org.eclipse.sirius.components.collaborative.diagrams.api.IDiagramInput;

public record EditOv1NodeAppearanceInput(UUID id, String editingContextId, String representationId, List<String> nodeIds, Ov1NodeAppearanceInput appearance) implements IDiagramInput {}
