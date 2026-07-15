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

import java.util.List;

import org.eclipse.sirius.components.view.View;
import org.eclipse.sirius.components.view.emf.IJavaServiceProvider;
import org.eclipse.syson.services.DeleteService;
import org.eclipse.syson.services.UtilService;
import org.eclipse.syson.standard.diagrams.view.services.DoDAFRulesMutationServices;
import org.eclipse.syson.standard.diagrams.view.services.DoDAFRulesQueryServices;
import org.springframework.context.annotation.Configuration;

/**
 * Java services for the DoDAF Rules View table.
 */
@Configuration
public class DoDAFRulesViewJavaServiceProvider implements IJavaServiceProvider {

    @Override
    public List<Class<?>> getServiceClasses(View view) {
        var descriptions = view.getDescriptions();
        var optDesc = descriptions.stream()
                .filter(desc -> DoDAFRulesTableDescriptionProvider.DESCRIPTION_NAME.equals(desc.getName()))
                .findFirst();
        if (optDesc.isPresent()) {
            return List.of(
                    DeleteService.class,
                    UtilService.class,
                    DoDAFRulesMutationServices.class,
                    DoDAFRulesQueryServices.class);
        }
        return List.of();
    }
}
