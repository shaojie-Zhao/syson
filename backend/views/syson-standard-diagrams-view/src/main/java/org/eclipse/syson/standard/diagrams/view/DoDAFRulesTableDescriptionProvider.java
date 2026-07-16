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
import org.eclipse.sirius.components.view.builder.generated.view.ViewBuilders;
import org.eclipse.sirius.components.view.builder.providers.IColorProvider;
import org.eclipse.sirius.components.view.builder.providers.IRepresentationDescriptionProvider;
import org.eclipse.sirius.components.view.table.CellDescription;
import org.eclipse.sirius.components.view.table.ColumnDescription;
import org.eclipse.sirius.components.view.table.RowContextMenuEntry;
import org.eclipse.syson.standard.diagrams.view.services.DoDAFRulesMutationServices;
import org.eclipse.syson.standard.diagrams.view.services.DoDAFRulesQueryServices;
import org.eclipse.syson.sysml.SysmlPackage;
import org.eclipse.syson.util.AQLConstants;
import org.eclipse.syson.util.ServiceMethod;
import org.eclipse.syson.util.SysMLMetamodelHelper;

/**
 * Table description for DoDAF Rules View.
 * Columns: 序号, 应用于, 名称, 规则说明, 规则种类, 所有者
 */
public class DoDAFRulesTableDescriptionProvider implements IRepresentationDescriptionProvider {

    public static final String DESCRIPTION_NAME = "DoDAF Rules View";

    private final TableBuilders tableBuilders = new TableBuilders();
    private final ViewBuilders viewBuilders = new ViewBuilders();

    @Override
    public RepresentationDescription create(IColorProvider colorProvider) {
        String domainType = SysMLMetamodelHelper.buildQualifiedName(SysmlPackage.eINSTANCE.getNamespace());

        var rowDescription = this.tableBuilders.newRowDescription()
                .name("DoDAFRules-Row")
                .semanticCandidatesExpression("aql:self.getRulesElements()->toPaginatedData(cursor,direction,size)")
                .depthLevelExpression("0")
                .headerLabelExpression("")
                .initialHeightExpression("-1")
                .isResizableExpression(AQLConstants.AQL_FALSE)
                .contextMenuEntries(this.createContextMenuEntries().toArray(RowContextMenuEntry[]::new))
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
                .name("DoDAFRules-Col-Index")
                .semanticCandidatesExpression("aql:'Index'")
                .headerLabelExpression("序号")
                .initialWidthExpression("60")
                .isResizableExpression(AQLConstants.AQL_FALSE)
                .build());

        columns.add(this.tableBuilders.newColumnDescription()
                .name("DoDAFRules-Col-Applied")
                .semanticCandidatesExpression("aql:'Applied'")
                .headerLabelExpression("应用于")
                .initialWidthExpression("180")
                .isResizableExpression(AQLConstants.AQL_TRUE)
                .isSortableExpression(AQLConstants.AQL_TRUE)
                .build());

        columns.add(this.tableBuilders.newColumnDescription()
                .name("DoDAFRules-Col-Name")
                .semanticCandidatesExpression("aql:'Name'")
                .headerLabelExpression("名称")
                .initialWidthExpression("200")
                .isResizableExpression(AQLConstants.AQL_TRUE)
                .isSortableExpression(AQLConstants.AQL_TRUE)
                .build());

        columns.add(this.tableBuilders.newColumnDescription()
                .name("DoDAFRules-Col-Description")
                .semanticCandidatesExpression("aql:'Description'")
                .headerLabelExpression("规则说明")
                .initialWidthExpression("300")
                .isResizableExpression(AQLConstants.AQL_TRUE)
                .build());

        columns.add(this.tableBuilders.newColumnDescription()
                .name("DoDAFRules-Col-Type")
                .semanticCandidatesExpression("aql:'Type'")
                .headerLabelExpression("规则种类")
                .initialWidthExpression("120")
                .isResizableExpression(AQLConstants.AQL_TRUE)
                .isSortableExpression(AQLConstants.AQL_TRUE)
                .build());

        columns.add(this.tableBuilders.newColumnDescription()
                .name("DoDAFRules-Col-Owner")
                .semanticCandidatesExpression("aql:'Owner'")
                .headerLabelExpression("所有者")
                .initialWidthExpression("120")
                .isResizableExpression(AQLConstants.AQL_TRUE)
                .build());

        return columns;
    }

    private List<CellDescription> createCells() {
        List<CellDescription> cells = new ArrayList<>();

        // Index cell - auto sequence number
        cells.add(this.tableBuilders.newCellDescription()
                .name("DoDAFRules-Cell-Index")
                .preconditionExpression("aql:columnTargetObject == 'Index'")
                .valueExpression("aql:self.getRuleIndex()")
                .cellWidgetDescription(this.tableBuilders.newCellLabelWidgetDescription().build())
                .build());

        // Applied cell - editable (declaredName)
        cells.add(this.tableBuilders.newCellDescription()
                .name("DoDAFRules-Cell-Applied")
                .preconditionExpression("aql:columnTargetObject == 'Applied'")
                .valueExpression("aql:self.declaredName")
                .cellWidgetDescription(this.tableBuilders.newCellTextfieldWidgetDescription()
                        .body(this.viewBuilders.newSetValue()
                                .featureName("declaredName")
                                .valueExpression("aql:newValue")
                                .build())
                        .build())
                .build());

        // Name cell - editable via Documentation or shortName
        cells.add(this.tableBuilders.newCellDescription()
                .name("DoDAFRules-Cell-Name")
                .preconditionExpression("aql:columnTargetObject == 'Name'")
                .valueExpression("aql:self.getRuleName()")
                .cellWidgetDescription(this.tableBuilders.newCellTextfieldWidgetDescription()
                        .body(this.viewBuilders.newChangeContext()
                                .expression(ServiceMethod.of1(DoDAFRulesMutationServices::editRuleName).aqlSelf("newValue"))
                                .build())
                        .build())
                .build());

        // Description cell - editable via Documentation
        cells.add(this.tableBuilders.newCellDescription()
                .name("DoDAFRules-Cell-Description")
                .preconditionExpression("aql:columnTargetObject == 'Description'")
                .valueExpression(ServiceMethod.of0(DoDAFRulesMutationServices::getRuleDescription).aqlSelf())
                .cellWidgetDescription(this.tableBuilders.newCellTextareaWidgetDescription()
                        .body(this.viewBuilders.newChangeContext()
                                .expression(ServiceMethod.of1(DoDAFRulesMutationServices::editRuleDescription).aqlSelf("newValue"))
                                .build())
                        .build())
                .build());

        // Type cell - editable (uses a custom metadata or alias)
        cells.add(this.tableBuilders.newCellDescription()
                .name("DoDAFRules-Cell-Type")
                .preconditionExpression("aql:columnTargetObject == 'Type'")
                .valueExpression("aql:self.getRuleType()")
                .cellWidgetDescription(this.tableBuilders.newCellTextfieldWidgetDescription()
                        .body(this.viewBuilders.newChangeContext()
                                .expression(ServiceMethod.of1(DoDAFRulesMutationServices::editRuleType).aqlSelf("newValue"))
                                .build())
                        .build())
                .build());

        // Owner cell - editable
        cells.add(this.tableBuilders.newCellDescription()
                .name("DoDAFRules-Cell-Owner")
                .preconditionExpression("aql:columnTargetObject == 'Owner'")
                .valueExpression("aql:self.getRuleOwner()")
                .cellWidgetDescription(this.tableBuilders.newCellTextfieldWidgetDescription()
                        .body(this.viewBuilders.newChangeContext()
                                .expression(ServiceMethod.of1(DoDAFRulesMutationServices::editRuleOwner).aqlSelf("newValue"))
                                .build())
                        .build())
                .build());

        return cells;
    }

    private List<RowContextMenuEntry> createContextMenuEntries() {
        List<RowContextMenuEntry> entries = new ArrayList<>();

        entries.add(this.tableBuilders.newRowContextMenuEntry()
                .name("DoDAFRules-ContextMenu-Add")
                .labelExpression("aql:'+ 添加行'")
                .preconditionExpression("aql:true")
                .body(this.viewBuilders.newChangeContext()
                        .expression(ServiceMethod.of0(DoDAFRulesQueryServices::createDefaultRuleElement).aqlSelf())
                        .build())
                .build());

        entries.add(this.tableBuilders.newRowContextMenuEntry()
                .name("DoDAFRules-ContextMenu-Delete")
                .labelExpression("aql:'删除行'")
                .preconditionExpression("aql:true")
                .body(this.viewBuilders.newDeleteElement()
                        .build())
                .build());

        return entries;
    }
}
