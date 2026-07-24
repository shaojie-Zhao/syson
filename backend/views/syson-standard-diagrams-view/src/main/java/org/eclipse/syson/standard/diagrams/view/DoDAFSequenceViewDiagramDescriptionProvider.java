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

import org.eclipse.sirius.components.view.RepresentationDescription;
import org.eclipse.sirius.components.view.ViewFactory;
import org.eclipse.sirius.components.view.builder.generated.diagram.DiagramBuilders;
import org.eclipse.sirius.components.view.builder.providers.IColorProvider;
import org.eclipse.sirius.components.view.builder.providers.IRepresentationDescriptionProvider;
import org.eclipse.sirius.components.view.diagram.ArrangeLayoutDirection;
import org.eclipse.sirius.components.view.diagram.DiagramLayoutOption;
import org.eclipse.sirius.components.view.diagram.DropTool;
import org.eclipse.syson.common.view.api.IViewDescriptionProvider;
import org.eclipse.syson.standard.diagrams.view.services.DoDAFSequenceViewCreateService;
import org.eclipse.syson.sysml.SysmlPackage;
import org.eclipse.syson.util.ServiceMethod;
import org.eclipse.syson.util.SysMLMetamodelHelper;
import org.springframework.stereotype.Service;

/**
 * Allows to register the DoDAF Sequence View diagram in the application. Used for DoDAF sequence/event trace views
 * (OV-6c).
 * The actual diagram created is the SDV one.
 *
 * @author dousheng
 */
@Service
public class DoDAFSequenceViewDiagramDescriptionProvider implements IViewDescriptionProvider {

    public static final String DESCRIPTION_NAME = "DoDAF Sequence View";

    @Override
    public String getViewId() {
        return "DoDAFSequenceViewDiagram";
    }

    @Override
    public IRepresentationDescriptionProvider getRepresentationDescriptionProvider() {
        return colorProvider -> {
            String originalName = this.sdvProvider.descriptionName;
            this.sdvProvider.descriptionName = DESCRIPTION_NAME;
            try {
                return this.sdvProvider.create(colorProvider);
            } finally {
                this.sdvProvider.descriptionName = originalName;
            }
        };
    }
}
