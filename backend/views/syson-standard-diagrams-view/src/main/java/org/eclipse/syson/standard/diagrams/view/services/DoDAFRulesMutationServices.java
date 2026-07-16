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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.syson.sysml.Documentation;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.SysmlFactory;
import org.eclipse.syson.sysml.Usage;

/**
 * Mutation services for DoDAF Rules View table.
 * Stores rule name, type, and owner as aliasIds metadata on the element.
 */
public class DoDAFRulesMutationServices {

    private static final String PREFIX_RULE_NAME = "dodaf:rule:name:";
    private static final String PREFIX_RULE_TYPE = "dodaf:rule:type:";
    private static final String PREFIX_RULE_OWNER = "dodaf:rule:owner:";

    /**
     * Returns the body of the first Documentation owned by the element.
     */
    public String getRuleDescription(Element element) {
        return element.getDocumentation().stream()
                .map(Documentation::getBody)
                .findFirst()
                .orElse("");
    }

    /**
     * Sets or updates the Documentation body for the element.
     */
    public boolean editRuleDescription(Element element, String newValue) {
        var documentation = element.getDocumentation().stream().findFirst().orElse(null);
        if (documentation != null) {
            documentation.setBody(newValue);
        } else if (newValue != null && !newValue.isEmpty()) {
            var newDoc = SysmlFactory.eINSTANCE.createDocumentation();
            newDoc.setBody(newValue);
            var owningMembership = SysmlFactory.eINSTANCE.createOwningMembership();
            element.getOwnedRelationship().add(owningMembership);
            owningMembership.getOwnedRelatedElement().add(newDoc);
        }
        return true;
    }

    /**
     * Returns the rule name from aliasIds.
     */
    public String getRuleName(Element element) {
        if (element instanceof Usage usage && usage.getDeclaredShortName() != null) {
            return usage.getDeclaredShortName();
        }
        for (String alias : element.getAliasIds()) {
            if (alias.startsWith(PREFIX_RULE_NAME)) {
                return alias.substring(PREFIX_RULE_NAME.length());
            }
        }
        return "";
    }

    /**
     * Sets the rule name via declaredShortName.
     */
    public boolean editRuleName(Element element, String newValue) {
        if (element instanceof Usage usage) {
            usage.setDeclaredShortName(newValue);
        }
        // Also store in aliasIds as fallback
        element.getAliasIds().removeIf(a -> a.startsWith(PREFIX_RULE_NAME));
        if (newValue != null && !newValue.isEmpty()) {
            element.getAliasIds().add(PREFIX_RULE_NAME + newValue);
        }
        return true;
    }

    /**
     * Returns the rule type (规则种类) from aliasIds.
     */
    public String getRuleType(Element element) {
        for (String alias : element.getAliasIds()) {
            if (alias.startsWith(PREFIX_RULE_TYPE)) {
                return alias.substring(PREFIX_RULE_TYPE.length());
            }
        }
        return "";
    }

    /**
     * Sets the rule type (规则种类).
     */
    public boolean editRuleType(Element element, String newValue) {
        element.getAliasIds().removeIf(a -> a.startsWith(PREFIX_RULE_TYPE));
        if (newValue != null && !newValue.isEmpty()) {
            element.getAliasIds().add(PREFIX_RULE_TYPE + newValue);
        }
        return true;
    }

    /**
     * Returns the rule owner from aliasIds.
     */
    public String getRuleOwner(Element element) {
        for (String alias : element.getAliasIds()) {
            if (alias.startsWith(PREFIX_RULE_OWNER)) {
                return alias.substring(PREFIX_RULE_OWNER.length());
            }
        }
        return "";
    }

    /**
     * Sets the rule owner.
     */
    public boolean editRuleOwner(Element element, String newValue) {
        element.getAliasIds().removeIf(a -> a.startsWith(PREFIX_RULE_OWNER));
        if (newValue != null && !newValue.isEmpty()) {
            element.getAliasIds().add(PREFIX_RULE_OWNER + newValue);
        }
        return true;
    }
}
