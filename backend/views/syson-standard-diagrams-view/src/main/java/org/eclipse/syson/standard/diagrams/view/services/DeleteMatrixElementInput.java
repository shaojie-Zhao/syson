package org.eclipse.syson.standard.diagrams.view.services;

import java.util.UUID;

import org.eclipse.sirius.components.core.api.IInput;

/**
 * Input for deleting a matrix element (the SysML element backing a matrix relation) through the editing context event
 * processor, so that the deletion is applied on the live editing context, triggers a tree/diagram refresh and is
 * persisted.
 *
 * @param id the request id
 * @param editingContextId the editing context id
 * @param elementId the semantic element id (EMF elementId) to delete
 */
public record DeleteMatrixElementInput(UUID id, String editingContextId, String elementId) implements IInput {
}
