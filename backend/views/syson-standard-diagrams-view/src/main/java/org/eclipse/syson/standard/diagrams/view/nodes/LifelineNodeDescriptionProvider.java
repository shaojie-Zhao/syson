package org.eclipse.syson.standard.diagrams.view.nodes;

import org.eclipse.sirius.components.view.builder.IViewDiagramElementFinder;
import org.eclipse.sirius.components.view.builder.providers.IColorProvider;
import org.eclipse.sirius.components.view.diagram.DiagramDescription;
import org.eclipse.sirius.components.view.diagram.NodeDescription;
import org.eclipse.sirius.components.view.diagram.SynchronizationPolicy;
import org.eclipse.sirius.components.view.diagram.UserResizableDirection;
import org.eclipse.syson.diagram.common.view.nodes.AbstractNodeDescriptionProvider;
import org.eclipse.syson.model.services.aql.ModelQueryAQLService;
import org.eclipse.syson.standard.diagrams.view.SDVDescriptionNameGenerator;
import org.eclipse.syson.sysml.SysmlPackage;
import org.eclipse.syson.util.ServiceMethod;

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
                .defaultHeightExpression("100")
                .defaultWidthExpression("150")
                .domainType(org.eclipse.syson.util.SysMLMetamodelHelper.buildQualifiedName(SysmlPackage.eINSTANCE.getPartDefinition()))
                .insideLabel(null)
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
        });
    }
}
