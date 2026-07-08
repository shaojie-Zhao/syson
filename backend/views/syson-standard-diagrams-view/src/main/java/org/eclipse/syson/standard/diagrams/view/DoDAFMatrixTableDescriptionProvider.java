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
import org.eclipse.syson.services.DeleteService;
import org.eclipse.syson.standard.diagrams.view.services.DoDAFMatrixMutationServices;
import org.eclipse.syson.standard.diagrams.view.services.DoDAFMatrixQueryServices;
import org.eclipse.syson.sysml.SysmlPackage;
import org.eclipse.syson.util.AQLConstants;
import org.eclipse.syson.util.ServiceMethod;
import org.eclipse.syson.util.SysMLMetamodelHelper;

/**
 * Table description for DoDAF Matrix views (OV-3, SV-3, CV-5~7, DIV-3, PV-3,
 * SvcV-3a/b, SvcV-5~7, SV-5a/b, SV-6).
 * Displays elements from the ViewUsage in a table with name/source/target columns.
 */
public class DoDAFMatrixTableDescriptionProvider implements IRepresentationDescriptionProvider {

    public static final String DESCRIPTION_NAME = "DoDAF Matrix View";

    private final TableBuilders tableBuilders = new TableBuilders();
    private final ViewBuilders viewBuilders = new ViewBuilders();

    @Override
    public RepresentationDescription create(IColorProvider colorProvider) {
        String domainType = SysMLMetamodelHelper.buildQualifiedName(SysmlPackage.eINSTANCE.getNamespace());

        var rowDescription = this.tableBuilders.newRowDescription()
                .name("DoDAFMatrix-Row")
                .semanticCandidatesExpression("aql:self.getMatrixElements()->toPaginatedData(cursor,direction,size)")
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
                .name("DoDAFMatrix-Col-Name")
                .semanticCandidatesExpression("aql:'Name'")
                .headerLabelExpression("名称")
                .initialWidthExpression("280")
                .isResizableExpression(AQLConstants.AQL_TRUE)
                .isSortableExpression(AQLConstants.AQL_TRUE)
                .build());

        columns.add(this.tableBuilders.newColumnDescription()
                .name("DoDAFMatrix-Col-Type")
                .semanticCandidatesExpression("aql:'Type'")
                .headerLabelExpression("类型")
                .initialWidthExpression("120")
                .isResizableExpression(AQLConstants.AQL_TRUE)
                .build());

        columns.add(this.tableBuilders.newColumnDescription()
                .name("DoDAFMatrix-Col-Description")
                .semanticCandidatesExpression("aql:'Description'")
                .headerLabelExpression("描述")
                .initialWidthExpression("300")
                .isResizableExpression(AQLConstants.AQL_TRUE)
                .build());

        return columns;
    }

    private List<CellDescription> createCells() {
        List<CellDescription> cells = new ArrayList<>();

        // Name cell - editable
        cells.add(this.tableBuilders.newCellDescription()
                .name("DoDAFMatrix-Cell-Name")
                .preconditionExpression("aql:columnTargetObject == 'Name'")
                .valueExpression("aql:self.declaredName")
                .cellWidgetDescription(this.tableBuilders.newCellTextfieldWidgetDescription()
                        .body(this.viewBuilders.newSetValue()
                                .featureName("declaredName")
                                .valueExpression("aql:newValue")
                                .build())
                        .build())
                .build());

        // Type cell - read-only
        cells.add(this.tableBuilders.newCellDescription()
                .name("DoDAFMatrix-Cell-Type")
                .preconditionExpression("aql:columnTargetObject == 'Type'")
                .valueExpression("aql:self.eClass().name")
                .cellWidgetDescription(this.tableBuilders.newCellLabelWidgetDescription()
                        .build())
                .build());

        // Description cell - editable, persisted via Documentation element
        cells.add(this.tableBuilders.newCellDescription()
                .name("DoDAFMatrix-Cell-Description")
                .preconditionExpression("aql:columnTargetObject == 'Description'")
                .valueExpression(ServiceMethod.of0(DoDAFMatrixMutationServices::getDocumentationBody).aqlSelf())
                .cellWidgetDescription(this.tableBuilders.newCellTextareaWidgetDescription()
                        .body(this.viewBuilders.newChangeContext()
                                .expression(ServiceMethod.of1(DoDAFMatrixMutationServices::editDocumentation).aqlSelf("newValue"))
                                .build())
                        .build())
                .build());

        return cells;
    }

    private List<RowContextMenuEntry> createContextMenuEntries() {
        List<RowContextMenuEntry> entries = new ArrayList<>();

        entries.add(this.tableBuilders.newRowContextMenuEntry()
                .name("DoDAFMatrix-CreateRow")
                .labelExpression("新建")
                .iconURLExpression("/images/graphicalAdd.svg")
                .body(this.viewBuilders.newChangeContext()
                        .expression(ServiceMethod.of0(DoDAFMatrixQueryServices::createDefaultMatrixElement).aqlSelf())
                        .build())
                .build());

        entries.add(this.tableBuilders.newRowContextMenuEntry()
                .name("DoDAFMatrix-DeleteRow")
                .labelExpression("从模型中删除")
                .iconURLExpression("/images/semanticDelete.svg")
                .body(this.viewBuilders.newChangeContext()
                        .expression(ServiceMethod.of0(DeleteService::deleteFromModel).aqlSelf())
                        .build())
                .build());

        return entries;
    }
}
