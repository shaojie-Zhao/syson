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
package org.eclipse.syson.application.sysmlv2;

import java.util.Objects;
import java.util.Optional;

import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.sirius.components.core.api.IEditingContextPersistenceService;
import org.eclipse.sirius.components.emf.services.api.IEMFEditingContext;
import org.eclipse.sirius.web.domain.boundedcontexts.semanticdata.events.SemanticDataUpdatedEvent;
import org.eclipse.syson.diagram.services.DiagramMutationDiagramService;
import org.eclipse.syson.model.services.ModelMutationElementService;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.Namespace;
import org.eclipse.syson.sysml.Package;
import org.eclipse.syson.sysml.ViewUsage;
import org.eclipse.syson.util.StandardDiagramsConstants;
import org.eclipse.syson.util.SysONRepresentationDescriptionIdentifiers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Used to create the representation of a newly created SysMLv2 project.
 *
 * @author arichard
 */
@Service
public class SysMLv2TemplatesRepresentationInitializer {

    private final Logger logger = LoggerFactory.getLogger(SysMLv2TemplatesRepresentationInitializer.class);

    private final IEditingContextPersistenceService editingContextPersistenceService;

    private final DiagramMutationDiagramService diagramMutationDiagramService;

    private final ModelMutationElementService modelMutationElementService;

    private final org.eclipse.sirius.components.core.api.IRepresentationDescriptionSearchService representationDescriptionSearchService;

    private final org.eclipse.sirius.components.collaborative.tables.services.TableCreationService tableCreationService;

    private final org.eclipse.sirius.components.collaborative.gantt.service.GanttCreationService ganttCreationService;

    private final org.eclipse.sirius.components.collaborative.api.IRepresentationMetadataPersistenceService representationMetadataPersistenceService;

    private final org.eclipse.sirius.components.collaborative.api.IRepresentationPersistenceService representationPersistenceService;

    public SysMLv2TemplatesRepresentationInitializer(DiagramMutationDiagramService diagramMutationDiagramService, ModelMutationElementService modelMutationElementService,
            IEditingContextPersistenceService editingContextPersistenceService,
            org.eclipse.sirius.components.core.api.IRepresentationDescriptionSearchService representationDescriptionSearchService,
            org.eclipse.sirius.components.collaborative.tables.services.TableCreationService tableCreationService,
            org.eclipse.sirius.components.collaborative.gantt.service.GanttCreationService ganttCreationService,
            org.eclipse.sirius.components.collaborative.api.IRepresentationMetadataPersistenceService representationMetadataPersistenceService,
            org.eclipse.sirius.components.collaborative.api.IRepresentationPersistenceService representationPersistenceService) {
        this.diagramMutationDiagramService = Objects.requireNonNull(diagramMutationDiagramService);
        this.modelMutationElementService = Objects.requireNonNull(modelMutationElementService);
        this.editingContextPersistenceService = Objects.requireNonNull(editingContextPersistenceService);
        this.representationDescriptionSearchService = Objects.requireNonNull(representationDescriptionSearchService);
        this.tableCreationService = Objects.requireNonNull(tableCreationService);
        this.ganttCreationService = Objects.requireNonNull(ganttCreationService);
        this.representationMetadataPersistenceService = Objects.requireNonNull(representationMetadataPersistenceService);
        this.representationPersistenceService = Objects.requireNonNull(representationPersistenceService);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    public void onSemanticDataUpdatedEvent(SemanticDataUpdatedEvent semanticDataUpdatedEvent) {
        if (semanticDataUpdatedEvent.causedBy() instanceof SysMLv2TemplatesInitialization templateInitialization) {
            var editingContext = templateInitialization.editingContext();
            var resource = templateInitialization.resource();

            // Check if this is a DoDAF template (has 有人无人协同反潜系统 as root)
            var rootElement = this.getRootElement(resource);
            boolean isDoDAF = rootElement.isPresent() && rootElement.get() instanceof Package
                    && "有人无人协同反潜系统".equals(((Package) rootElement.get()).getName());

            if (!isDoDAF) {
                // Standard SysMLv2 templates: create a single "view1" ViewUsage with GeneralView diagram
                var optViewUsage = this.getOrCreateViewUsage(resource);
                if (optViewUsage.isPresent()) {
                    this.diagramMutationDiagramService.createDiagram(optViewUsage.get(), editingContext, SysONRepresentationDescriptionIdentifiers.GENERAL_VIEW_DIAGRAM_DESCRIPTION_ID);
                    this.editingContextPersistenceService.persist(semanticDataUpdatedEvent, editingContext);
                }
            } else {
                // DoDAF templates: process ViewUsage elements with proper ViewDefinitions
                this.createDodafv2Diagrams(resource, editingContext, semanticDataUpdatedEvent);
            }
        }
    }

    private void createDodafv2Diagrams(Resource resource, IEMFEditingContext editingContext, SemanticDataUpdatedEvent event) {
        var optRoot = this.getRootElement(resource);
        if (optRoot.isEmpty()) return;

        var rootElement = optRoot.get();
        for (var child : rootElement.getOwnedElement()) {
            if (child instanceof Namespace ns) {
                this.processDoDAFViews(ns, editingContext);
            }
        }
    }

    /**
     * Maps a DoDAF view name to the appropriate ViewDefinition qualified name.
     * Based on DoDAF v2.0 specifications and UPDM symbol standards.
     *
     * Categories:
     * - GeneralView: standard node-link diagrams (OV-1, OV-2, CV-1 etc.)
     * - ActionFlowView: activity/function flow (OV-5b, SV-4, SvcV-4)
     * - InterconnectionView: system/service interfaces (SV-1, SV-2, SvcV-1, SvcV-2)
     * - MatrixView: matrix/table (OV-3, SV-3, CV-5~7, DIV-3, PV-3, SvcV-3a/b, SvcV-5~7, SV-5a/b, SV-6)
     * - GanttView: gantt/timeline (CV-3, PV-2, SV-8)
     * - SequenceView: sequence/event trace (OV-6c)
     * - TableView: plain table/list (StdV-1, StdV-2, AV-2)
     */
    private String getViewDefinitionForDoDAFView(String viewName) {
        if (viewName == null) return StandardDiagramsConstants.GV_QN;

        // === Gantt Chart views ===
        if (viewName.contains("CV-3")) return StandardDiagramsConstants.DODAF_GANTT_QN;      // Capability phasing
        if (viewName.contains("PV-2")) return StandardDiagramsConstants.DODAF_GANTT_QN;      // Project timeline
        if (viewName.contains("SV-8")) return StandardDiagramsConstants.DODAF_GANTT_QN;      // System evolution

        // === OV-1 High Level Concept View ===
        if (viewName.contains("OV-1")) return StandardDiagramsConstants.DODAF_OV1_QN;

        // === Sequence/Event Trace views ===
        if (viewName.contains("OV-6c")) return StandardDiagramsConstants.DODAF_SEQUENCE_QN;   // Event trace

        // === Plain Table views ===
        if (viewName.contains("StdV-1") || viewName.contains("StdV-2")) return StandardDiagramsConstants.DODAF_TABLE_QN; // Standards
        if (viewName.contains("AV-2")) return StandardDiagramsConstants.DODAF_TABLE_QN;       // Integrated dictionary

        // === Matrix/Table views ===
        // OV matrices
        if (viewName.contains("OV-3")) return StandardDiagramsConstants.DODAF_MATRIX_QN;      // Resource flow matrix
        // SV matrices
        if (viewName.contains("SV-3")) return StandardDiagramsConstants.DODAF_MATRIX_QN;      // System-system matrix
        if (viewName.contains("SV-5a")) return StandardDiagramsConstants.DODAF_MATRIX_QN;     // Activity-function trace
        if (viewName.contains("SV-5b")) return StandardDiagramsConstants.DODAF_MATRIX_QN;     // Activity-system trace
        if (viewName.contains("SV-6")) return StandardDiagramsConstants.DODAF_MATRIX_QN;      // Resource flow matrix
        // CV matrices
        if (viewName.contains("CV-5")) return StandardDiagramsConstants.DODAF_MATRIX_QN;      // Capability-org mapping
        if (viewName.contains("CV-6")) return StandardDiagramsConstants.DODAF_MATRIX_QN;      // Capability-activity mapping
        if (viewName.contains("CV-7")) return StandardDiagramsConstants.DODAF_MATRIX_QN;      // Capability-service mapping
        // DIV matrices
        if (viewName.contains("DIV-3")) return StandardDiagramsConstants.DODAF_MATRIX_QN;     // Physical data model
        // PV matrices
        if (viewName.contains("PV-3")) return StandardDiagramsConstants.DODAF_MATRIX_QN;      // Project-capability mapping
        // SvcV matrices
        if (viewName.contains("SvcV-3a")) return StandardDiagramsConstants.DODAF_MATRIX_QN;   // Service-system matrix
        if (viewName.contains("SvcV-3b")) return StandardDiagramsConstants.DODAF_MATRIX_QN;   // Service-service matrix
        if (viewName.contains("SvcV-5")) return StandardDiagramsConstants.DODAF_MATRIX_QN;    // Service-activity trace
        if (viewName.contains("SvcV-6")) return StandardDiagramsConstants.DODAF_MATRIX_QN;    // Service resource flow matrix
        if (viewName.contains("SvcV-7")) return StandardDiagramsConstants.DODAF_MATRIX_QN;    // Service measures

        // === Activity Flow views ===
        if (viewName.contains("OV-5b")) return StandardDiagramsConstants.AFV_QN;              // Activity model
        if (viewName.contains("SV-4")) return StandardDiagramsConstants.AFV_QN;               // System function
        if (viewName.contains("SvcV-4")) return StandardDiagramsConstants.AFV_QN;             // Service function

        // === Interconnection views ===
        if (viewName.contains("SV-1")) return StandardDiagramsConstants.IV_QN;                // System interface
        if (viewName.contains("SV-2")) return StandardDiagramsConstants.IV_QN;                // System resource flow
        if (viewName.contains("SvcV-1")) return StandardDiagramsConstants.IV_QN;              // Service context
        if (viewName.contains("SvcV-2")) return StandardDiagramsConstants.IV_QN;              // Service resource flow

        // === Default: General View ===
        return StandardDiagramsConstants.GV_QN;
    }

    private void processDoDAFViews(Namespace ns, IEMFEditingContext editingContext) {
        for (var child : ns.getOwnedElement()) {
            if (child instanceof ViewUsage vu) {
                var name = vu.getDeclaredName();
                if (name != null) {
                    try {
                        // Set ViewDefinition type
                        String viewDefQN = this.getViewDefinitionForDoDAFView(name);
                        this.modelMutationElementService.featureTypeViewUsage(vu, viewDefQN);
                        // Pre-create diagram for all DoDAF views using full SDV engine
                        if (name != null && (viewDefQN.equals(StandardDiagramsConstants.GV_QN)
                                || viewDefQN.equals(StandardDiagramsConstants.IV_QN)
                                || viewDefQN.equals(StandardDiagramsConstants.AFV_QN)
                                || viewDefQN.equals(StandardDiagramsConstants.STV_QN)
                                || viewDefQN.equals(StandardDiagramsConstants.DODAF_OV1_QN)
                                || viewDefQN.equals(StandardDiagramsConstants.DODAF_SEQUENCE_QN))) {
                            this.diagramMutationDiagramService.createDiagram(vu, editingContext,
                                    SysONRepresentationDescriptionIdentifiers.GENERAL_VIEW_DIAGRAM_DESCRIPTION_ID);
                        }
                    } catch (Exception e) {
                        this.logger.warn("Failed to process DoDAF view {}: {}", name, e.getMessage());
                    }
                }
            }
            if (child instanceof Namespace childNs) {
                this.processDoDAFViews(childNs, editingContext);
            }
        }
    }

    private void createRepresentationForView(ViewUsage vu, String viewName, String viewDefQN, IEMFEditingContext editingContext) {
        if (viewDefQN.equals(StandardDiagramsConstants.DODAF_MATRIX_QN)) {
            this.createTableRepresentation(vu, viewName, "DoDAF Matrix View", editingContext);
        } else if (viewDefQN.equals(StandardDiagramsConstants.DODAF_GANTT_QN)) {
            this.createGanttRepresentation(vu, viewName, "DoDAF Gantt View", editingContext);
        } else if (viewDefQN.equals(StandardDiagramsConstants.DODAF_TABLE_QN)) {
            this.createTableRepresentation(vu, viewName, "DoDAF Table View", editingContext);
        } else {
            // GeneralView / InterconnectionView / ActionFlowView / SequenceView → standard diagram
            this.diagramMutationDiagramService.createDiagram(vu, editingContext,
                    SysONRepresentationDescriptionIdentifiers.GENERAL_VIEW_DIAGRAM_DESCRIPTION_ID);
        }
    }

    private void createTableRepresentation(ViewUsage vu, String viewName, String descName, IEMFEditingContext editingContext) {
        try {
            var desc = this.representationDescriptionSearchService.findAll(editingContext).values().stream()
                    .filter(org.eclipse.sirius.components.tables.descriptions.TableDescription.class::isInstance)
                    .map(org.eclipse.sirius.components.tables.descriptions.TableDescription.class::cast)
                    .filter(d -> descName.equals(d.getLabel()))
                    .findFirst();
            if (desc.isPresent()) {
                var tableDesc = desc.get();
                String id = java.util.UUID.randomUUID().toString();
                var table = this.tableCreationService.create(id, vu, tableDesc, editingContext);
                var metadata = org.eclipse.sirius.components.core.RepresentationMetadata.newRepresentationMetadata(table.getId())
                        .kind("Table")
                        .label(viewName + " [表格]")
                        .descriptionId(table.getDescriptionId())
                        .iconURLs(java.util.List.of())
                        .build();
                this.representationMetadataPersistenceService.save(null, editingContext, metadata, table.getTargetObjectId());
                this.representationPersistenceService.save(null, editingContext, table);
                this.logger.info("Created {} table for DoDAF view: {}", descName, viewName);
            } else {
                this.logger.warn("Table description '{}' not found for view: {}", descName, viewName);
            }
        } catch (Exception e) {
            this.logger.warn("Failed to create table for {}: {}", viewName, e.getMessage());
        }
    }

    private void createGanttRepresentation(ViewUsage vu, String viewName, String descName, IEMFEditingContext editingContext) {
        try {
            var desc = this.representationDescriptionSearchService.findAll(editingContext).values().stream()
                    .filter(org.eclipse.sirius.components.gantt.description.GanttDescription.class::isInstance)
                    .map(org.eclipse.sirius.components.gantt.description.GanttDescription.class::cast)
                    .filter(d -> descName.equals(d.getLabel()))
                    .findFirst();
            if (desc.isPresent()) {
                var ganttDesc = desc.get();
                var gantt = this.ganttCreationService.create(vu, ganttDesc, editingContext);
                var metadata = org.eclipse.sirius.components.core.RepresentationMetadata.newRepresentationMetadata(gantt.getId())
                        .kind("Gantt")
                        .label(viewName + " [甘特图]")
                        .descriptionId(gantt.getDescriptionId())
                        .iconURLs(java.util.List.of())
                        .build();
                this.representationMetadataPersistenceService.save(null, editingContext, metadata, gantt.getTargetObjectId());
                this.representationPersistenceService.save(null, editingContext, gantt);
                this.logger.info("Created {} gantt for DoDAF view: {}", descName, viewName);
            } else {
                this.logger.warn("Gantt description '{}' not found for view: {}", descName, viewName);
            }
        } catch (Exception e) {
            this.logger.warn("Failed to create gantt for {}: {}", viewName, e.getMessage());
        }
    }

    private Optional<Element> getRootElement(Resource resource) {
        Object rootElement = resource.getContents().get(0);
        if (rootElement instanceof Namespace rootNamespace) {
            if (!rootNamespace.getOwnedMember().isEmpty()) {
                return Optional.of(rootNamespace.getOwnedMember().get(0));
            }
        }
        return Optional.empty();
    }

    private Optional<Package> getRootPackage(Resource resource) {
        Object rootElement = resource.getContents().get(0);
        if (rootElement instanceof Namespace rootNamespace) {
            Element rootMember = rootNamespace.getOwnedMember().get(0);
            if (rootMember instanceof Package rootPackage) {
                return Optional.of(rootPackage);
            }
        }
        return Optional.empty();
    }

    private Optional<ViewUsage> getOrCreateViewUsage(Resource resource) {
        Optional<Package> optRootPackage = this.getRootPackage(resource);
        if (optRootPackage.isPresent()) {
            var rootPackage = optRootPackage.get();
            var viewUsage = this.getViewUsage(rootPackage);
            if (viewUsage.isEmpty()) {
                viewUsage = this.modelMutationElementService.createViewUsage(rootPackage, "view1");
                viewUsage.ifPresent(view -> {
                    this.modelMutationElementService.featureTypeViewUsage(view, StandardDiagramsConstants.GV_QN);
                });
            }
            return viewUsage;
        }
        return Optional.empty();
    }

    private Optional<ViewUsage> getViewUsage(Element element) {
        return element.getOwnedElement().stream()
                .filter(ViewUsage.class::isInstance)
                .map(ViewUsage.class::cast)
                .filter(vu -> Objects.equals(vu.getDeclaredName(), "view1"))
                .findFirst();
    }
}
