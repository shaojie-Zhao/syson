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
package org.eclipse.syson.standard.diagrams.view.services;

import java.util.List;

import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.SysmlFactory;
import org.eclipse.syson.sysml.ViewUsage;
import org.eclipse.syson.sysml.metamodel.util.ElementUtil;

/**
 * Query services for the DoDAF Matrix View table.
 */
public class DoDAFMatrixQueryServices {

    /**
     * Returns all PartUsage/PartDefinition elements owned by the ViewUsage.
     */
    public List<Element> getMatrixElements(ViewUsage viewUsage) {
        return viewUsage.getOwnedElement().stream()
                .filter(e -> e instanceof org.eclipse.syson.sysml.PartUsage || e instanceof org.eclipse.syson.sysml.PartDefinition)
                .toList();
    }

    /**
     * Creates a new PartUsage element inside the ViewUsage with a default name.
     * Used when the table context menu does not provide AQL variables.
     */
    public Element createDefaultMatrixElement(ViewUsage viewUsage) {
        return this.createMatrixElement(viewUsage, "新元素");
    }

    /**
     * Creates a new PartUsage element inside the ViewUsage and returns it.
     */
    public Element createMatrixElement(ViewUsage viewUsage, String name) {
        var partUsage = SysmlFactory.eINSTANCE.createPartUsage();
        partUsage.setDeclaredName(name != null && !name.isBlank() ? name : "New Element");
        partUsage.setElementId(ElementUtil.generateUUID(partUsage).toString());
        partUsage.getAliasIds().add("dodaf:exchange");
        var owningMembership = SysmlFactory.eINSTANCE.createOwningMembership();
        viewUsage.getOwnedRelationship().add(owningMembership);
        owningMembership.getOwnedRelatedElement().add(partUsage);
        return partUsage;
    }
}
