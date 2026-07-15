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
import org.eclipse.sirius.components.view.builder.providers.IColorProvider;
import org.eclipse.sirius.components.view.builder.providers.IRepresentationDescriptionProvider;
import org.eclipse.syson.common.view.api.IViewDescriptionProvider;
import org.springframework.stereotype.Service;

/**
 * Allows to register the DoDAF Rules View in the application.
 * Table-based view for managing combat status rules.
 *
 * @author dousheng
 */
@Service
public class DoDAFRulesViewDiagramDescriptionProvider implements IViewDescriptionProvider {

    public static final String DESCRIPTION_NAME = "DoDAF Rules View";

    @Override
    public String getViewId() {
        return "DoDAFRulesViewTable";
    }

    @Override
    public IRepresentationDescriptionProvider getRepresentationDescriptionProvider() {
        return new DoDAFRulesTableDescriptionProvider();
    }
}
