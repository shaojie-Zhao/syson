package org.eclipse.syson.standard.diagrams.view.services;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.sirius.components.collaborative.api.ChangeDescription;
import org.eclipse.sirius.components.collaborative.api.ChangeKind;
import org.eclipse.sirius.components.collaborative.api.IEditingContextEventHandler;
import org.eclipse.sirius.components.core.api.ErrorPayload;
import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.sirius.components.core.api.IInput;
import org.eclipse.sirius.components.core.api.IObjectSearchService;
import org.eclipse.sirius.components.core.api.IPayload;
import org.eclipse.sirius.components.core.api.SuccessPayload;
import org.eclipse.sirius.components.emf.services.api.IEMFEditingContext;
import org.eclipse.syson.sysml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Sinks;

/**
 * Event handler for {@link RenameMatrixElementInput}. It runs inside the editing context event processor so it operates
 * on the live editing context, sets the {@code declaredName} of the target element and emits a
 * {@link ChangeKind#SEMANTIC_CHANGE} change description that triggers a refresh of all representations (including the
 * explorer tree) and the persistence of the model.
 *
 * @author syson-ds
 */
@Service
public class RenameMatrixElementEventHandler implements IEditingContextEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenameMatrixElementEventHandler.class);

    private final IObjectSearchService objectSearchService;

    public RenameMatrixElementEventHandler(IObjectSearchService objectSearchService) {
        this.objectSearchService = Objects.requireNonNull(objectSearchService);
    }

    @Override
    public boolean canHandle(IEditingContext editingContext, IInput input) {
        return editingContext instanceof IEMFEditingContext && input instanceof RenameMatrixElementInput;
    }

    @Override
    public void handle(Sinks.One<IPayload> payloadSink, Sinks.Many<ChangeDescription> changeDescriptionSink, IEditingContext editingContext, IInput input) {
        IPayload payload;
        ChangeDescription changeDescription = new ChangeDescription(ChangeKind.NOTHING, editingContext.getId(), input);

        if (input instanceof RenameMatrixElementInput renameInput) {
            Optional<Element> optionalElement = this.objectSearchService.getObject(editingContext, renameInput.elementId())
                    .filter(Element.class::isInstance)
                    .map(Element.class::cast);
            if (optionalElement.isPresent()) {
                optionalElement.get().setDeclaredName(renameInput.newName());
                changeDescription = new ChangeDescription(ChangeKind.SEMANTIC_CHANGE, editingContext.getId(), input);
                payload = new SuccessPayload(input.id());
                LOGGER.info("RenameMatrixElement: renamed {} to {}", renameInput.elementId(), renameInput.newName());
            } else {
                payload = new ErrorPayload(input.id(), "Element not found: " + renameInput.elementId());
                LOGGER.warn("RenameMatrixElement: element not found {}", renameInput.elementId());
            }
        } else {
            payload = new ErrorPayload(input.id(), "Invalid input");
        }

        payloadSink.tryEmitValue(payload);
        changeDescriptionSink.tryEmitNext(changeDescription);
    }
}
