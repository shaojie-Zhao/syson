package org.eclipse.syson.standard.diagrams.view.services;

import java.util.UUID;

import org.eclipse.sirius.components.core.api.IInput;

/**
 * Input for renaming a matrix element's declaredName through the editing context event processor,
 * so that the change is applied on the live editing context, triggers a tree/diagram refresh and is
 * persisted.
 *
 * @param id the request id
 * @param editingContextId the editing context id
 * @param elementId the semantic element id (EMF elementId)
 * @param newName the new declaredName
 */
public record RenameMatrixElementInput(UUID id, String editingContextId, String elementId, String newName) implements IInput {
}
