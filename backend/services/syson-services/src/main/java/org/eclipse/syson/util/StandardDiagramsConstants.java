/*******************************************************************************
 * Copyright (c) 2025, 2026 Obeo.
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
package org.eclipse.syson.util;

import java.util.HashMap;
import java.util.Map;

/**
 * StandardDiagrams-related constants.
 *
 * @author arichard
 */
public class StandardDiagramsConstants {

    public static final String GV = "General View";

    public static final String IV = "Interconnection View";

    public static final String AFV = "ActionFlow View";

    public static final String STV = "StateTransition View";

    public static final String GV_QN = "StandardViewDefinitions::GeneralView";

    public static final String IV_QN = "StandardViewDefinitions::InterconnectionView";

    public static final String AFV_QN = "StandardViewDefinitions::ActionFlowView";

    public static final String STV_QN = "StandardViewDefinitions::StateTransitionView";

    // DoDAF-specific ViewDefinition qualified names
    public static final String DODAF_MATRIX_QN = "DoDAFv2_Library::MatrixView";
    public static final String DODAF_GANTT_QN = "DoDAFv2_Library::GanttView";
    public static final String DODAF_SEQUENCE_QN = "DoDAFv2_Library::SequenceView";
    public static final String DODAF_TABLE_QN = "DoDAFv2_Library::TableView";

    public static final String DODAF_OV1_QN = "DoDAFv2_Library::OV1View";

    public static final Map<String, String> SHORT_NAME_TO_VALUE = new HashMap<>();

    static {
        SHORT_NAME_TO_VALUE.put("gv", GV);
        SHORT_NAME_TO_VALUE.put("iv", IV);
        SHORT_NAME_TO_VALUE.put("afv", AFV);
        SHORT_NAME_TO_VALUE.put("stv", STV);
    }

    public static String getValueFromShortName(String shortName) {
        return SHORT_NAME_TO_VALUE.get(shortName.toLowerCase());
    }

}
