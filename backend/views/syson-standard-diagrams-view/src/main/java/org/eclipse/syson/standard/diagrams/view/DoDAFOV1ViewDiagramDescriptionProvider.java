package org.eclipse.syson.standard.diagrams.view;

import org.eclipse.sirius.components.view.RepresentationDescription;
import org.eclipse.sirius.components.view.builder.providers.IColorProvider;
import org.eclipse.sirius.components.view.builder.providers.IRepresentationDescriptionProvider;
import org.eclipse.syson.common.view.api.IViewDescriptionProvider;
import org.springframework.stereotype.Service;

/**
 * Register OV-1 diagram type with full SDV engine (nodes, edges, tools, drag-drop).
 * Extends SDVDiagramDescriptionProvider and overrides only the name and menu filter.
 */
@Service
public class DoDAFOV1ViewDiagramDescriptionProvider implements IViewDescriptionProvider {
    public static final String DESCRIPTION_NAME = "DoDAF OV-1 View";

    @Override public String getViewId() { return "DoDAFOV1ViewDiagram"; }

    @Override
    public IRepresentationDescriptionProvider getRepresentationDescriptionProvider() {
        return new IRepresentationDescriptionProvider() {
            @Override
            public RepresentationDescription create(IColorProvider colorProvider) {
                var provider = new SDVDiagramDescriptionProvider();
                provider.descriptionName = DESCRIPTION_NAME;
                provider.showViewAsTools = false;
                var desc = provider.create(colorProvider);
                // Clear precondition to avoid "do not work" issues
                desc.setPreconditionExpression(null);
                // Point at the correct domain type for OV-1 (same as GeneralView)
                return desc;
            }
        };
    }
}
