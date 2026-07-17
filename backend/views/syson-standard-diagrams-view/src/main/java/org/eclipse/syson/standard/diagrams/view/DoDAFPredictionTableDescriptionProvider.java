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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.sirius.components.view.RepresentationDescription;
import org.eclipse.sirius.components.view.builder.generated.table.TableBuilders;
import org.eclipse.sirius.components.view.builder.providers.IColorProvider;
import org.eclipse.sirius.components.view.builder.providers.IRepresentationDescriptionProvider;
import org.eclipse.sirius.components.view.table.CellDescription;
import org.eclipse.sirius.components.view.table.ColumnDescription;
import org.eclipse.sirius.components.view.table.RowContextMenuEntry;
import org.eclipse.syson.sysml.SysmlPackage;
import org.eclipse.syson.util.AQLConstants;
import org.eclipse.syson.util.SysMLMetamodelHelper;

/**
 * Table description for DoDAF Prediction View (技术和技能预测).
 * Columns: 技术和技能领域, 技术和技能, 短期, 中期, 长期
 */
public class DoDAFPredictionTableDescriptionProvider implements IRepresentationDescriptionProvider {

    public static final String DESCRIPTION_NAME = "DoDAF Prediction View";

    private final TableBuilders tableBuilders = new TableBuilders();

    @Override
    public RepresentationDescription create(IColorProvider colorProvider) {
        String domainType = SysMLMetamodelHelper.buildQualifiedName(SysmlPackage.eINSTANCE.getNamespace());

        var rowDescription = this.tableBuilders.newRowDescription()
                .name("DoDAFPrediction-Row")
                .semanticCandidatesExpression("aql:self")
                .depthLevelExpression("0")
                .headerLabelExpression("")
                .initialHeightExpression("-1")
                .isResizableExpression(AQLConstants.AQL_FALSE)
                .build();

        return this.tableBuilders.newTableDescription()
                .name(DESCRIPTION_NAME)
                .titleExpression("aql:self.name")
                .domainType(domainType)
                .columnDescriptions(this.createColumns().toArray(ColumnDescription[]::new))
                .cellDescriptions(this.createCells().toArray(CellDescription[]::new))
                .rowDescription(rowDescription)
                .pageSizeOptionsExpression("aql:Sequence{10,20,50}")
                .useStripedRowsExpression("aql:true")
                .build();
    }

    private List<ColumnDescription> createColumns() {
        List<ColumnDescription> columns = new ArrayList<>();

        columns.add(this.tableBuilders.newColumnDescription()
                .name("DoDAFPrediction-Col-Domain")
                .semanticCandidatesExpression("aql:'Domain'")
                .headerLabelExpression("技术和技能领域")
                .initialWidthExpression("200")
                .isResizableExpression(AQLConstants.AQL_TRUE)
                .build());

        columns.add(this.tableBuilders.newColumnDescription()
                .name("DoDAFPrediction-Col-Skill")
                .semanticCandidatesExpression("aql:'Skill'")
                .headerLabelExpression("技术和技能")
                .initialWidthExpression("200")
                .isResizableExpression(AQLConstants.AQL_TRUE)
                .build());

        columns.add(this.tableBuilders.newColumnDescription()
                .name("DoDAFPrediction-Col-ShortTerm")
                .semanticCandidatesExpression("aql:'ShortTerm'")
                .headerLabelExpression("短期")
                .initialWidthExpression("200")
                .isResizableExpression(AQLConstants.AQL_TRUE)
                .build());

        columns.add(this.tableBuilders.newColumnDescription()
                .name("DoDAFPrediction-Col-MidTerm")
                .semanticCandidatesExpression("aql:'MidTerm'")
                .headerLabelExpression("中期")
                .initialWidthExpression("200")
                .isResizableExpression(AQLConstants.AQL_TRUE)
                .build());

        columns.add(this.tableBuilders.newColumnDescription()
                .name("DoDAFPrediction-Col-LongTerm")
                .semanticCandidatesExpression("aql:'LongTerm'")
                .headerLabelExpression("长期")
                .initialWidthExpression("200")
                .isResizableExpression(AQLConstants.AQL_TRUE)
                .build());

        return columns;
    }

    private List<CellDescription> createCells() {
        List<CellDescription> cells = new ArrayList<>();

        for (String col : new String[]{"Domain", "Skill", "ShortTerm", "MidTerm", "LongTerm"}) {
            cells.add(this.tableBuilders.newCellDescription()
                    .name("DoDAFPrediction-Cell-" + col)
                    .preconditionExpression("aql:columnTargetObject == '" + col + "'")
                    .valueExpression("aql:''")
                    .cellWidgetDescription(this.tableBuilders.newCellTextfieldWidgetDescription().build())
                    .build());
        }

        return cells;
    }
}
