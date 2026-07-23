/*******************************************************************************
 * Copyright (c) 2026 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.syson.standard.diagrams.view;

import org.eclipse.sirius.components.view.builder.generated.diagram.DiagramBuilders;
import org.eclipse.sirius.components.view.builder.providers.IColorProvider;
import org.eclipse.sirius.components.view.builder.providers.IRepresentationDescriptionProvider;
import org.eclipse.syson.common.view.api.IViewDescriptionProvider;
import org.eclipse.syson.sysml.SysmlPackage;
import org.eclipse.syson.util.SysMLMetamodelHelper;
import org.springframework.stereotype.Service;

/**
 * Registers the Sequence View (时序图) based on General View.
 */
@Service
public class SequenceViewDiagramDescriptionProvider implements IViewDescriptionProvider {

    @Override
    public String getViewId() {
        return "SequenceView";
    }

    @Override
    public IRepresentationDescriptionProvider getRepresentationDescriptionProvider() {
        return (IColorProvider colorProvider) -> {
            var diagramBuilders = new DiagramBuilders();
            return diagramBuilders.newDiagramDescription()
                    .name("Sequence Diagram View")
                    .titleExpression("aql:self.name")
                    .domainType(SysMLMetamodelHelper.buildQualifiedName(SysmlPackage.eINSTANCE.getPartUsage()))
                    .build();
        };
    }
}
