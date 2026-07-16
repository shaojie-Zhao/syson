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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.SysmlFactory;
import org.eclipse.syson.sysml.ViewUsage;
import org.eclipse.syson.sysml.metamodel.util.ElementUtil;

/**
 * Query services for the DoDAF Rules View table.
 */
public class DoDAFRulesQueryServices {

    /**
     * Returns all PartUsage elements under the ViewUsage (rules data).
     */
    public List<Element> getRulesElements(ViewUsage viewUsage) {
        return viewUsage.getOwnedElement().stream()
                .filter(e -> e instanceof org.eclipse.syson.sysml.PartUsage || e instanceof org.eclipse.syson.sysml.PartDefinition)
                .toList();
    }

    /**
     * Returns the 1-based index of the element among siblings.
     */
    public int getRuleIndex(Element element) {
        if (element.eContainer() instanceof org.eclipse.syson.sysml.Relationship rel) {
            return rel.getOwnedRelatedElement().indexOf(element) + 1;
        }
        var view = element.getOwningNamespace();
        if (view instanceof ViewUsage vu) {
            return vu.getOwnedElement().indexOf(element) + 1;
        }
        return 0;
    }

    /**
     * Creates a new default PartUsage under the ViewUsage.
     */
    public Element createDefaultRuleElement(ViewUsage viewUsage) {
        return this.createRuleElement(viewUsage, "新规则");
    }

    /**
     * Creates a new PartUsage element under the ViewUsage.
     */
    public Element createRuleElement(ViewUsage viewUsage, String name) {
        var partUsage = SysmlFactory.eINSTANCE.createPartUsage();
        partUsage.setDeclaredName(name != null && !name.isBlank() ? name : "新规则");
        partUsage.setElementId(ElementUtil.generateUUID(partUsage).toString());
        partUsage.getAliasIds().add("dodaf:rule");
        var owningMembership = SysmlFactory.eINSTANCE.createOwningMembership();
        viewUsage.getOwnedRelationship().add(owningMembership);
        owningMembership.getOwnedRelatedElement().add(partUsage);
        return partUsage;
    }

    /**
     * Deletes the given rule element from its parent.
     */
    public boolean deleteRuleElement(Element element) {
        var parent = element.eContainer();
        if (parent instanceof org.eclipse.syson.sysml.Relationship rel) {
            rel.getOwnedRelatedElement().remove(element);
            var grandParent = rel.eContainer();
            if (grandParent instanceof ViewUsage vu) {
                vu.getOwnedRelationship().remove(rel);
            }
            return true;
        }
        return false;
    }
}
