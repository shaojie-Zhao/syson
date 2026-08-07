package org.eclipse.syson.standard.diagrams.view.nodes;

import org.eclipse.sirius.components.view.builder.IViewDiagramElementFinder;
import org.eclipse.sirius.components.view.builder.providers.IColorProvider;
import org.eclipse.sirius.components.view.diagram.DiagramDescription;
import org.eclipse.sirius.components.view.diagram.InsideLabelPosition;
import org.eclipse.sirius.components.view.diagram.LabelTextAlign;
import org.eclipse.sirius.components.view.diagram.NodeDescription;
import org.eclipse.sirius.components.view.diagram.SynchronizationPolicy;
import org.eclipse.sirius.components.view.diagram.UserResizableDirection;
import org.eclipse.syson.diagram.common.view.nodes.AbstractNodeDescriptionProvider;
import org.eclipse.syson.model.services.aql.ModelQueryAQLService;
import org.eclipse.syson.standard.diagrams.view.SDVDescriptionNameGenerator;
import org.eclipse.syson.sysml.SysmlPackage;
import org.eclipse.syson.util.ServiceMethod;
import org.eclipse.syson.util.ViewConstants;

public class LifelineNodeDescriptionProvider extends AbstractNodeDescriptionProvider {

    public static final String NAME = SDVDescriptionNameGenerator.PREFIX + " Node Lifeline";

    public LifelineNodeDescriptionProvider(IColorProvider colorProvider) {
        super(colorProvider);
    }

    @Override
    public NodeDescription create() {
        System.err.println("=== LifelineNodeDescriptionProvider.create() called");
        return this.diagramBuilderHelper.newNodeDescription()
                .collapsible(false)
                .defaultHeightExpression("150")
                .defaultWidthExpression("150")
                .domainType(org.eclipse.syson.util.SysMLMetamodelHelper.buildQualifiedName(SysmlPackage.eINSTANCE.getPartDefinition()))
                .insideLabel(this.diagramBuilderHelper.newInsideLabelDescription()
                        .labelExpression("aql:self.declaredName")
                        .position(org.eclipse.sirius.components.view.diagram.InsideLabelPosition.TOP_CENTER)
                        .style(this.diagramBuilderHelper.newInsideLabelStyle()
                                .fontSize(10)
                                .labelColor(this.colorProvider.getColor(ViewConstants.DEFAULT_LABEL_COLOR))
                                .build())
                        .textAlign(org.eclipse.sirius.components.view.diagram.LabelTextAlign.CENTER)
                        .build())
                .name(NAME)
                .preconditionExpression(ServiceMethod.of0(ModelQueryAQLService::isLifeline).aqlSelf())
                .style(this.diagramBuilderHelper.newImageNodeStyleDescription()
                        .borderSize(0)
                        .shape("images/lifeline.svg")
                        .build())
                .userResizable(UserResizableDirection.NONE)
                .keepAspectRatio(false)
                .synchronizationPolicy(SynchronizationPolicy.SYNCHRONIZED)
                .build();
    }

    @Override
    public void link(DiagramDescription diagramDescription, IViewDiagramElementFinder cache) {
        cache.getNodeDescription(NAME).ifPresent(nd -> {
            nd.setPalette(this.diagramBuilderHelper.newNodePalette().build());
            // Register the Lifeline node description in the diagram so edges can connect
            // to it and its size (head + dashed tail) is honored by the layout.
            if (!diagramDescription.getNodeDescriptions().contains(nd)) {
                diagramDescription.getNodeDescriptions().add(nd);
            }
        });
    }
}
