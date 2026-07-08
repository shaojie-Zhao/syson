package org.eclipse.syson.application.nodes.dto;

import java.util.UUID;
import org.eclipse.sirius.components.core.api.IInput;

public record DeleteOv1PartUsageInput(UUID id, String editingContextId, String elementId) implements IInput {}
