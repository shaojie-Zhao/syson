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
import org.eclipse.syson.services.DeleteService;
import org.eclipse.syson.sysml.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Sinks;

/**
 * Event handler for {@link DeleteMatrixElementInput}. It runs inside the editing context event processor so it operates
 * on the live editing context, deletes the target element from the model and emits a {@link ChangeKind#SEMANTIC_CHANGE}
 * change description that triggers a refresh of all representations (including the explorer tree) and the persistence of
 * the model.
 *
 * @author syson-ds
 */
@Service
public class DeleteMatrixElementEventHandler implements IEditingContextEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteMatrixElementEventHandler.class);

    private final IObjectSearchService objectSearchService;

    private final DeleteService deleteService;

    public DeleteMatrixElementEventHandler(IObjectSearchService objectSearchService) {
        this.objectSearchService = Objects.requireNonNull(objectSearchService);
        this.deleteService = new DeleteService();
    }

    @Override
    public boolean canHandle(IEditingContext editingContext, IInput input) {
        return editingContext instanceof IEMFEditingContext && input instanceof DeleteMatrixElementInput;
    }

    @Override
    public void handle(Sinks.One<IPayload> payloadSink, Sinks.Many<ChangeDescription> changeDescriptionSink, IEditingContext editingContext, IInput input) {
        IPayload payload;
        ChangeDescription changeDescription = new ChangeDescription(ChangeKind.NOTHING, editingContext.getId(), input);

        if (input instanceof DeleteMatrixElementInput deleteInput) {
            Optional<Element> optionalElement = this.objectSearchService.getObject(editingContext, deleteInput.elementId())
                    .filter(Element.class::isInstance)
                    .map(Element.class::cast);
            if (optionalElement.isPresent()) {
                this.deleteService.deleteFromModel(optionalElement.get());
                changeDescription = new ChangeDescription(ChangeKind.SEMANTIC_CHANGE, editingContext.getId(), input);
                payload = new SuccessPayload(input.id());
                LOGGER.info("DeleteMatrixElement: deleted {}", deleteInput.elementId());
            } else {
                payload = new ErrorPayload(input.id(), "Element not found: " + deleteInput.elementId());
                LOGGER.warn("DeleteMatrixElement: element not found {}", deleteInput.elementId());
            }
        } else {
            payload = new ErrorPayload(input.id(), "Invalid input");
        }

        payloadSink.tryEmitValue(payload);
        changeDescriptionSink.tryEmitNext(changeDescription);
    }
}
