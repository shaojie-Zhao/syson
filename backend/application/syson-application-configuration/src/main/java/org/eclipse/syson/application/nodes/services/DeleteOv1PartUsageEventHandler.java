package org.eclipse.syson.application.nodes.services;

import java.util.Objects;

import org.eclipse.sirius.components.collaborative.api.ChangeDescription;
import org.eclipse.sirius.components.collaborative.api.ChangeKind;
import org.eclipse.sirius.components.collaborative.api.IEditingContextEventHandler;
import org.eclipse.sirius.components.collaborative.messages.ICollaborativeMessageService;
import org.eclipse.sirius.components.core.api.ErrorPayload;
import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.sirius.components.core.api.IInput;
import org.eclipse.sirius.components.core.api.IObjectSearchService;
import org.eclipse.sirius.components.core.api.IPayload;
import org.eclipse.sirius.components.core.api.SuccessPayload;
import org.eclipse.syson.application.nodes.dto.DeleteOv1PartUsageInput;
import org.eclipse.syson.services.DeleteService;
import org.eclipse.syson.sysml.Element;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Sinks;

@Service
public class DeleteOv1PartUsageEventHandler implements IEditingContextEventHandler {

    private final IObjectSearchService objectSearchService;
    private final ICollaborativeMessageService messageService;
    private final DeleteService deleteService;

    public DeleteOv1PartUsageEventHandler(IObjectSearchService objectSearchService, ICollaborativeMessageService messageService) {
        this.objectSearchService = Objects.requireNonNull(objectSearchService);
        this.messageService = Objects.requireNonNull(messageService);
        this.deleteService = new DeleteService();
    }

    @Override
    public boolean canHandle(IEditingContext editingContext, IInput input) {
        return input instanceof DeleteOv1PartUsageInput;
    }

    @Override
    public void handle(Sinks.One<IPayload> payloadSink, Sinks.Many<ChangeDescription> changeDescriptionSink, IEditingContext editingContext, IInput input) {
        if (input instanceof DeleteOv1PartUsageInput deleteInput) {
            var optionalElement = this.objectSearchService.getObject(editingContext, deleteInput.elementId())
                    .filter(Element.class::isInstance)
                    .map(Element.class::cast);
            if (optionalElement.isPresent()) {
                this.deleteService.deleteFromModel(optionalElement.get());
                changeDescriptionSink.tryEmitNext(new ChangeDescription(ChangeKind.SEMANTIC_CHANGE, editingContext.getId(), input));
                payloadSink.tryEmitValue(new SuccessPayload(input.id()));
            } else {
                payloadSink.tryEmitValue(new ErrorPayload(input.id(), this.messageService.notFound()));
            }
        }
    }
}
