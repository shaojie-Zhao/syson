package org.eclipse.syson.application.nodes.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.eclipse.sirius.components.collaborative.api.ChangeDescription;
import org.eclipse.sirius.components.collaborative.api.ChangeKind;
import org.eclipse.sirius.components.collaborative.api.Monitoring;
import org.eclipse.sirius.components.collaborative.diagrams.DiagramChangeKind;
import org.eclipse.sirius.components.collaborative.diagrams.DiagramContext;
import org.eclipse.sirius.components.collaborative.diagrams.api.IDiagramEventHandler;
import org.eclipse.sirius.components.collaborative.diagrams.api.IDiagramInput;
import org.eclipse.sirius.components.collaborative.diagrams.api.IDiagramQueryService;
import org.eclipse.sirius.components.collaborative.diagrams.messages.ICollaborativeDiagramMessageService;
import org.eclipse.sirius.components.core.api.ErrorPayload;
import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.sirius.components.core.api.IPayload;
import org.eclipse.sirius.components.core.api.SuccessPayload;
import org.eclipse.sirius.components.diagrams.Node;
import org.eclipse.sirius.components.diagrams.events.appearance.EditAppearanceEvent;
import org.eclipse.sirius.components.diagrams.events.appearance.IAppearanceChange;
import org.eclipse.sirius.components.diagrams.events.appearance.NodeBackgroundAppearanceChange;
import org.eclipse.sirius.components.diagrams.events.appearance.NodeBorderColorAppearanceChange;
import org.eclipse.sirius.components.diagrams.events.appearance.NodeBorderSizeAppearanceChange;
import org.eclipse.sirius.components.diagrams.events.appearance.NodeBorderStyleAppearanceChange;
import org.eclipse.syson.application.nodes.dto.EditOv1NodeAppearanceInput;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import reactor.core.publisher.Sinks;

@Service
public class EditOv1NodeAppearanceEventHandler implements IDiagramEventHandler {

    private final ICollaborativeDiagramMessageService messageService;
    private final IDiagramQueryService diagramQueryService;
    private final Counter counter;

    public EditOv1NodeAppearanceEventHandler(ICollaborativeDiagramMessageService messageService, IDiagramQueryService diagramQueryService, MeterRegistry meterRegistry) {
        this.messageService = Objects.requireNonNull(messageService);
        this.diagramQueryService = Objects.requireNonNull(diagramQueryService);
        this.counter = Counter.builder(Monitoring.EVENT_HANDLER).tag(Monitoring.NAME, this.getClass().getSimpleName()).register(meterRegistry);
    }

    @Override
    public boolean canHandle(IEditingContext editingContext, IDiagramInput diagramInput) {
        return diagramInput instanceof EditOv1NodeAppearanceInput;
    }

    @Override
    public void handle(Sinks.One<IPayload> payloadSink, Sinks.Many<ChangeDescription> changeDescriptionSink, IEditingContext editingContext, DiagramContext diagramContext, IDiagramInput diagramInput) {
        this.counter.increment();
        String message = this.messageService.invalidInput(diagramInput.getClass().getSimpleName(), EditOv1NodeAppearanceInput.class.getSimpleName());
        IPayload payload = new ErrorPayload(diagramInput.id(), message);
        ChangeDescription changeDescription = new ChangeDescription(ChangeKind.NOTHING, diagramInput.representationId(), diagramInput);

        if (diagramInput instanceof EditOv1NodeAppearanceInput editInput) {
            List<String> nodeIds = editInput.nodeIds();
            List<IAppearanceChange> changes = new ArrayList<>();
            List<String> notFound = new ArrayList<>();

            for (String nodeId : nodeIds) {
                Optional<Node> optNode = this.diagramQueryService.findNodeById(diagramContext.diagram(), nodeId);
                if (optNode.isPresent()) {
                    var app = editInput.appearance();
                    Optional.ofNullable(app.background()).ifPresent(v -> changes.add(new NodeBackgroundAppearanceChange(nodeId, v)));
                    Optional.ofNullable(app.borderColor()).ifPresent(v -> changes.add(new NodeBorderColorAppearanceChange(nodeId, v)));
                    Optional.ofNullable(app.borderSize()).ifPresent(v -> changes.add(new NodeBorderSizeAppearanceChange(nodeId, v)));
                    Optional.ofNullable(app.borderStyle()).ifPresent(v -> changes.add(new NodeBorderStyleAppearanceChange(nodeId, v)));
                    if (app.iconId() != null) {
                        changes.add(new NodeIconAppearanceChange(nodeId, app.iconId()));
                    }
                } else {
                    notFound.add(nodeId);
                }
            }
            if (!notFound.isEmpty()) {
                payload = new ErrorPayload(diagramInput.id(), this.messageService.nodeNotFound(String.join(" - ", notFound)));
            } else {
                diagramContext.diagramEvents().add(new EditAppearanceEvent(changes));
                payload = new SuccessPayload(diagramInput.id());
                changeDescription = new ChangeDescription(DiagramChangeKind.DIAGRAM_APPEARANCE_CHANGE, diagramInput.representationId(), diagramInput);
            }
        }

        payloadSink.tryEmitValue(payload);
        changeDescriptionSink.tryEmitNext(changeDescription);
    }
}
