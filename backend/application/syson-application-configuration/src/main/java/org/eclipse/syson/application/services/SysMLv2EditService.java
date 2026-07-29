/*******************************************************************************
 * Copyright (c) 2023, 2026 Obeo.
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
package org.eclipse.syson.application.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.sirius.components.collaborative.api.IRepresentationMetadataPersistenceService;
import org.eclipse.sirius.components.collaborative.api.IRepresentationPersistenceService;
import org.eclipse.sirius.components.collaborative.diagrams.api.IDiagramCreationService;
import org.eclipse.sirius.components.core.RepresentationMetadata;
import org.eclipse.sirius.components.core.api.ChildCreationDescription;
import org.eclipse.sirius.components.core.api.IDefaultEditService;
import org.eclipse.sirius.components.core.api.IEditServiceDelegate;
import org.eclipse.sirius.components.core.api.IEditingContext;
import org.eclipse.sirius.components.core.api.ILabelService;
import org.eclipse.sirius.components.core.api.IObjectSearchService;
import org.eclipse.sirius.components.core.api.IRepresentationDescriptionSearchService;
import org.eclipse.sirius.components.core.api.labels.StyledString;
import org.eclipse.sirius.components.diagrams.Diagram;
import org.eclipse.sirius.components.diagrams.description.DiagramDescription;
import org.eclipse.sirius.components.emf.services.api.IEMFEditingContext;
import org.eclipse.sirius.components.representations.VariableManager;
import org.eclipse.syson.services.DeleteService;
import org.eclipse.syson.services.UtilService;
import org.eclipse.syson.services.api.ISysONResourceService;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.Membership;
import org.eclipse.syson.sysml.Namespace;
import org.eclipse.syson.sysml.Relationship;
import org.eclipse.syson.sysml.SysmlFactory;
import org.eclipse.syson.sysml.SysmlPackage;
import org.eclipse.syson.sysml.ViewUsage;
import org.eclipse.syson.sysml.metamodel.services.ElementInitializerSwitch;
import org.eclipse.syson.util.GetIntermediateContainerCreationSwitch;
import org.eclipse.syson.util.SysMLMetamodelHelper;
import org.eclipse.syson.util.SysONRepresentationDescriptionIdentifiers;
import org.springframework.stereotype.Service;

/**
 * Specific {@link IEditServiceDelegate} to handle edition of SysML elements.
 *
 * @author arichard
 */
@Service
public class SysMLv2EditService implements IEditServiceDelegate {

    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(SysMLv2EditService.class.getName());

    public static final String ID_PREFIX = "SysMLv2EditService-";

    private final IDefaultEditService defaultEditService;

    private final ILabelService labelService;

    private final IObjectSearchService objectSearchService;

    private final IDiagramCreationService diagramCreationService;

    private final IRepresentationDescriptionSearchService representationDescriptionSearchService;

    private final IRepresentationMetadataPersistenceService representationMetadataPersistenceService;

    private final IRepresentationPersistenceService representationPersistenceService;

    private final ISysONResourceService sysONResourceService;

    private final DeleteService deleteService;

    private final UtilService utilService;

    public SysMLv2EditService(IDefaultEditService defaultEditService, ILabelService labelService, IObjectSearchService objectSearchService, ISysONResourceService sysONResourceService,
            SysMLv2EditServiceExtraServices extraServices) {
        this.defaultEditService = Objects.requireNonNull(defaultEditService);
        this.labelService = Objects.requireNonNull(labelService);
        this.objectSearchService = Objects.requireNonNull(objectSearchService);
        this.sysONResourceService = Objects.requireNonNull(sysONResourceService);

        Objects.requireNonNull(extraServices);
        this.diagramCreationService = extraServices.diagramCreationService();
        this.representationDescriptionSearchService = extraServices.representationDescriptionSearchService();
        this.representationMetadataPersistenceService = extraServices.representationMetadataPersistenceService();
        this.representationPersistenceService = extraServices.representationPersistenceService();

        this.deleteService = new DeleteService();
        this.utilService = new UtilService();
    }

    @Override
    public boolean canHandle(Object object) {
        return object instanceof Element;
    }

    @Override
    public boolean canHandle(IEditingContext editingContext) {
        return true;
    }

    @Override
    public List<ChildCreationDescription> getRootCreationDescriptions(IEditingContext editingContext, String domainId, boolean suggested, String referenceKind) {
        final List<ChildCreationDescription> rootObjectCreationDescription = new ArrayList<>();
        if (this.isSysMLDomainId(domainId)) {
            if (suggested) {
                List<String> iconURL = this.labelService.getImagePaths(EcoreUtil.create(SysmlPackage.eINSTANCE.getPackage()));
                StyledString styledLabel = this.labelService.getStyledLabel(SysmlPackage.eINSTANCE.getPackage());
                String label = "";
                if (styledLabel != null) {
                    label = styledLabel.toString();
                }
                rootObjectCreationDescription.add(new ChildCreationDescription(ID_PREFIX + SysmlPackage.eINSTANCE.getPackage().getName(), label, iconURL));
            } else {
                List<EClass> childrenCandidates = new GetChildCreationSwitch().doSwitch(SysmlPackage.eINSTANCE.getNamespace());
                childrenCandidates.forEach(candidate -> {
                    List<String> iconURL = java.util.List.of("/icons/full/obj16/" + candidate.getName() + ".svg");
                    StyledString styledLabel = this.labelService.getStyledLabel(candidate);
                    String label = "";
                    if (styledLabel != null) {
                        label = styledLabel.toString();
                    }
                    ChildCreationDescription childCreationDescription = new ChildCreationDescription(ID_PREFIX + candidate.getName(), label, iconURL);
                    rootObjectCreationDescription.add(childCreationDescription);
                });
            }
        } else {
            rootObjectCreationDescription.addAll(this.defaultEditService.getRootCreationDescriptions(editingContext, domainId, suggested, referenceKind));
        }
        Collections.sort(rootObjectCreationDescription, Comparator.comparing(ChildCreationDescription::label, String.CASE_INSENSITIVE_ORDER));
        return rootObjectCreationDescription;
    }

    @Override
    public List<ChildCreationDescription> getChildCreationDescriptions(IEditingContext editingContext, String containerId, String referenceKind) {
        List<ChildCreationDescription> result = new ArrayList<>();

        var optionalContainer = this.objectSearchService.getObject(editingContext, containerId)
                .filter(EObject.class::isInstance)
                .map(EObject.class::cast);

        if (optionalContainer.isPresent()) {
            EObject container = optionalContainer.get();
            EClass eClass = container.eClass();
            EPackage ePackage = eClass.getEPackage();
            if (SysmlPackage.eNS_PREFIX.equals(ePackage.getNsPrefix())) {
                List<ChildCreationDescription> childCreationDescriptions = new ArrayList<>();
                List<EClass> childrenCandidates = new GetChildCreationSwitch().doSwitch(eClass);
                // For DoDAF projects, filter to show only DoDAF-relevant types
                if (this.isDoDAFProject(container)) {
                    childrenCandidates = filterDoDAFChildren(childrenCandidates);
                }
                childrenCandidates.forEach(candidate -> {
                    List<String> iconURL = java.util.List.of("/icons/full/obj16/" + candidate.getName() + ".svg");
                    StyledString styledLabel = this.labelService.getStyledLabel(candidate);
                    String label = "";
                    if (styledLabel != null) {
                        label = styledLabel.toString();
                    }
                    ChildCreationDescription childCreationDescription = new ChildCreationDescription(ID_PREFIX + candidate.getName(), label, iconURL);
                    childCreationDescriptions.add(childCreationDescription);
                });
                result = childCreationDescriptions;
            } else {
                result = this.defaultEditService.getChildCreationDescriptions(editingContext, containerId, referenceKind);
            }
        }
        Collections.sort(result, Comparator.comparing(ChildCreationDescription::label, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    @Override
    public Optional<Object> createChild(IEditingContext editingContext, Object object, String childCreationDescriptionId) {
        // Support name parameter: SysMLv2EditService-PartUsage:MyName
        String initName = null;
        String resolvedId = childCreationDescriptionId;
        int colonIdx = childCreationDescriptionId.lastIndexOf(':');
        if (colonIdx > 0 && childCreationDescriptionId.startsWith(ID_PREFIX)) {
            initName = childCreationDescriptionId.substring(colonIdx + 1);
            resolvedId = childCreationDescriptionId.substring(0, colonIdx);
        }
        if (resolvedId.startsWith(ID_PREFIX) && object instanceof Element container) {
            EClass eClass = SysMLMetamodelHelper.toEClass(resolvedId.substring(ID_PREFIX.length()));
            EObject eObject = SysmlFactory.eINSTANCE.create(eClass);
            Optional<EClass> intermediateContainerClass = new GetIntermediateContainerCreationSwitch(container).doSwitch(eClass);
            if (intermediateContainerClass.isPresent() && eObject instanceof Element newElement) {
                EObject intermediateContainerEObject = SysmlFactory.eINSTANCE.create(intermediateContainerClass.get());
                if (intermediateContainerEObject instanceof Relationship intermediateContainer) {
                    container.getOwnedRelationship().add(intermediateContainer);
                    intermediateContainer.getOwnedRelatedElement().add(newElement);
                }
            } else if (eObject instanceof Relationship newElement) {
                container.getOwnedRelationship().add(newElement);
            } else if (container instanceof Membership membership && eObject instanceof Element newElement) {
                membership.getOwnedRelatedElement().add(newElement);
            }
            new ElementInitializerSwitch().doSwitch(eObject);
            // Tag DoDAF elements with appropriate aliasId
            System.err.println("=== CREATE-CHILD: descId=" + childCreationDescriptionId + " isEl=" + (eObject instanceof Element) + " isDoDAF=" + this.isDoDAFProject(container));
            if (childCreationDescriptionId.contains("Lifeline") || childCreationDescriptionId.contains("CombinedFragment") || childCreationDescriptionId.contains("StateInvariant") || childCreationDescriptionId.contains("Message")) {
                String alias;
                if (childCreationDescriptionId.contains("Lifeline")) alias = "dodaf:Lifeline";
                else if (childCreationDescriptionId.contains("CombinedFragment")) alias = "dodaf:CombinedFragment";
                else if (childCreationDescriptionId.contains("StateInvariant")) alias = "dodaf:StateInvariant";
                else alias = "dodaf:Message";
                System.err.println("=== SEQUENCE-ELEMENT: setting alias " + alias);
                if (eObject instanceof Element created) setAlias(created, alias);
            } else if (childCreationDescriptionId.contains("OperationalCapability")) {
                System.err.println("=== OP-CAP: setting alias dodaf:OperationalCapability");
                if (eObject instanceof Element created) setAlias(created, "dodaf:OperationalCapability");
            } else if (childCreationDescriptionId.contains("TaskStage")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:TaskStage");
            } else if (childCreationDescriptionId.contains("EquipCapability")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:EquipCapability");
            } else if (childCreationDescriptionId.contains("TimeScale")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:TimeScale");
            } else if (childCreationDescriptionId.contains("TaskIntent")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:TaskIntent");
            } else if (childCreationDescriptionId.contains("ImplementationPhase")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:ImplementationPhase");
            } else if (childCreationDescriptionId.contains("Vision") && !childCreationDescriptionId.contains("VisionDescription")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:Vision");
            } else if (childCreationDescriptionId.contains("VisionDescription")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:VisionDescription");
            } else if (childCreationDescriptionId.contains("Target")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:Target");
            } else if (childCreationDescriptionId.contains("Performer")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:Performer");
            } else if (childCreationDescriptionId.contains("Location")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:Location");
            } else if (childCreationDescriptionId.contains("Condition")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:Condition");
            } else if (childCreationDescriptionId.contains("OperationalTask")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:OperationalTask");
            } else if (childCreationDescriptionId.contains("OperationalModel")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:OperationalModel");
            } else if (childCreationDescriptionId.contains("CommunicationActivity")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:CommunicationActivity");
            } else if (childCreationDescriptionId.contains("TransmitActivity")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:TransmitActivity");
            } else if (childCreationDescriptionId.contains("ReceiveActivity")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:ReceiveActivity");
            } else if (childCreationDescriptionId.contains("ManeuverActivity")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:ManeuverActivity");
            } else if (childCreationDescriptionId.contains("Task") && !childCreationDescriptionId.contains("TaskStage") && !childCreationDescriptionId.contains("TaskIntent")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:Task");
            } else if (childCreationDescriptionId.contains("Force")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:Force");
            } else if (childCreationDescriptionId.contains("Role")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:Role");
            } else if (childCreationDescriptionId.contains("Equipment")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:Equipment");
            } else if (childCreationDescriptionId.contains("OperationalAction")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:OperationalAction");
            } else if (childCreationDescriptionId.contains("ActualOrganization")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:ActualOrganization");
            } else if (childCreationDescriptionId.contains("ActualPerson")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:ActualPerson");
            } else if (childCreationDescriptionId.contains("Duty")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:Duty");
            } else if (childCreationDescriptionId.contains("MissionPhase")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:MissionPhase");
            } else if (childCreationDescriptionId.contains("OperationalProblem")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:OperationalProblem");
            } else if (childCreationDescriptionId.contains("OperationalActivity")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:OperationalActivity");
            } else if (childCreationDescriptionId.contains("Person") && !childCreationDescriptionId.contains("PersonType")) {
                if (eObject instanceof Element created) setAlias(created, "dodaf:Person");
            } else if (eObject instanceof Element created && this.isDoDAFProject(container)) {
                this.tagDoDAFAlias(created, childCreationDescriptionId);
            }
            if (initName != null && !initName.isEmpty() && eObject instanceof Element newElement) {
                newElement.setDeclaredName(initName);
            }
            if (eObject instanceof ViewUsage viewUsage) {
                this.createDiagram(editingContext, viewUsage);
            }
            return Optional.of(eObject);
        }
        return this.defaultEditService.createChild(editingContext, object, childCreationDescriptionId);
    }

    @Override
    public Optional<Object> createRootObject(IEditingContext editingContext, UUID documentId, String domainId, String rootObjectCreationDescriptionId) {
        Optional<Object> createdObjectOptional = Optional.empty();

        var optionalEditingDomain = Optional.of(editingContext)
                .filter(IEMFEditingContext.class::isInstance)
                .map(IEMFEditingContext.class::cast)
                .map(IEMFEditingContext::getDomain);

        if (optionalEditingDomain.isPresent()) {
            AdapterFactoryEditingDomain editingDomain = optionalEditingDomain.get();

            var optionalResource = editingDomain.getResourceSet().getResources().stream()
                    .filter(resource -> documentId.toString().equals(resource.getURI().path().substring(1)))
                    .findFirst();

            if (optionalResource.isPresent()) {
                var resource = optionalResource.get();

                createdObjectOptional = this.createRootObjectInResource(editingContext, documentId, domainId, rootObjectCreationDescriptionId, resource);
            }
        } else {
            // Delegate to the default behavior for non-EMF editing contexts.
            createdObjectOptional = this.defaultCreateRootObject(editingContext, documentId, domainId, rootObjectCreationDescriptionId);
        }
        return createdObjectOptional;
    }

    private Optional<Object> createRootObjectInResource(IEditingContext editingContext, UUID documentId, String domainId, String rootObjectCreationDescriptionId, Resource resource) {
        final Optional<Object> createdObjectOptional;
        if (this.isSysMLDomainId(domainId)) {
            var rootNamespace = resource.getContents().stream()
                    .filter(Element.class::isInstance)
                    .map(Element.class::cast)
                    .filter(this.utilService::isRootNamespace)
                    .findFirst()
                    .orElseGet(() -> {
                        // Only create the missing root Namespace if the resource looks like a SysML one.
                        final boolean isSysMLResource = this.sysONResourceService.isSysML(resource);
                        if (isSysMLResource) {
                            final Namespace namespace = SysmlFactory.eINSTANCE.createNamespace();
                            resource.getContents().add(namespace);
                            return namespace;
                        } else {
                            return null;
                        }
                    });
            if (rootNamespace != null) {
                createdObjectOptional = this.createChild(editingContext, rootNamespace, rootObjectCreationDescriptionId);
            } else {
                // Delegate to the default behavior when trying to create a SysML element in a non-sysml
                // resource.
                createdObjectOptional = this.defaultCreateRootObject(editingContext, documentId, domainId, rootObjectCreationDescriptionId);
            }
        } else {
            // Delegate to the default behavior for non-SysML root object creation.
            createdObjectOptional = this.defaultCreateRootObject(editingContext, documentId, domainId, rootObjectCreationDescriptionId);
        }
        return createdObjectOptional;
    }

    private boolean isSysMLDomainId(String domainId) {
        return SysmlPackage.eNS_URI.equals(domainId);
    }

    private Optional<Object> defaultCreateRootObject(IEditingContext editingContext, UUID documentId, String domainId, String rootObjectCreationDescriptionId) {
        final String rootObjectCreationDescriptionIdForDefaultEditService;
        if (rootObjectCreationDescriptionId.startsWith(ID_PREFIX)) {
            rootObjectCreationDescriptionIdForDefaultEditService = rootObjectCreationDescriptionId.substring(ID_PREFIX.length());
        } else {
            rootObjectCreationDescriptionIdForDefaultEditService = rootObjectCreationDescriptionId;
        }
        return this.defaultEditService.createRootObject(editingContext, documentId, domainId, rootObjectCreationDescriptionIdForDefaultEditService);
    }

    @Override
    public void delete(Object object) {
        Optional<Element> optionalElement = Optional.of(object)
                .filter(Element.class::isInstance)
                .map(Element.class::cast);

        optionalElement.ifPresent(element -> this.deleteService.deleteFromModel(element));
    }

    /**
     * Create a General View diagram and associate it to the given ViewUsage.
     */
    private void createDiagram(IEditingContext editingContext, ViewUsage viewUsage) {
        this.representationDescriptionSearchService.findById(editingContext, SysONRepresentationDescriptionIdentifiers.GENERAL_VIEW_DIAGRAM_DESCRIPTION_ID)
                .filter(DiagramDescription.class::isInstance)
                .map(DiagramDescription.class::cast)
                .ifPresent(diagramDescription -> {
                    var variableManager = new VariableManager();
                    variableManager.put(VariableManager.SELF, viewUsage);
                    variableManager.put(DiagramDescription.LABEL, viewUsage.getDeclaredName());
                    String label = diagramDescription.getLabelProvider().apply(variableManager);
                    List<String> iconURLs = diagramDescription.getIconURLsProvider().apply(variableManager);

                    Diagram diagram = this.diagramCreationService.create(editingContext, diagramDescription, viewUsage);
                    var representationMetadata = RepresentationMetadata.newRepresentationMetadata(diagram.getId())
                            .kind(diagram.getKind())
                            .label(label)
                            .descriptionId(diagram.getDescriptionId())
                            .iconURLs(iconURLs)
                            .build();
                    this.representationMetadataPersistenceService.save(null, editingContext, representationMetadata, diagram.getTargetObjectId());
                    this.representationPersistenceService.save(null, editingContext, diagram);
                });
    }

    /** DoDAF-relevant SysML types for the "New" menu filter. */
    private static final Set<String> DODAF_CHILD_TYPES = Set.of(
        "PartDefinition", "PartUsage", "ActionUsage", "Dependency", "Package",
        "ViewUsage", "Documentation", "Comment", "TextualRepresentation",
        "PortUsage", "PortDefinition", "InterfaceUsage", "InterfaceDefinition",
        "RequirementUsage", "RequirementDefinition", "SatisfyRequirementUsage",
        "AllocationUsage", "AllocationDefinition"
    );

    private boolean isDoDAFProject(EObject container) {
        try {
            Resource resource = container.eResource();
            if (resource != null) {
                var it = resource.getAllContents();
                while (it.hasNext()) {
                    EObject obj = it.next();
                    if (obj instanceof Element e && e.getAliasIds().stream().anyMatch(a -> a.startsWith("dodaf:"))) {
                        System.out.println("isDoDAFProject: TRUE");
                        return true;
                    }
                }
            }
        } catch (Exception ignored) {}
        log.info("isDoDAFProject: FALSE (no dodaf: alias found)");
        return false;
    }

    private List<EClass> filterDoDAFChildren(List<EClass> candidates) {
        return candidates.stream()
            .filter(eClass -> DODAF_CHILD_TYPES.contains(eClass.getName()))
            .toList();
    }

    private static final Map<String, String> DODAF_TYPE_TO_ALIAS = Map.ofEntries(
        Map.entry("SysMLv2EditService-PartUsage", "dodaf:node"),
        Map.entry("SysMLv2EditService-PartDefinition", "dodaf:node"),
        Map.entry("SysMLv2EditService-ActionUsage", "dodaf:capability"),
        Map.entry("SysMLv2EditService-ActionDefinition", "dodaf:capability"),
        Map.entry("SysMLv2EditService-SatisfyRequirementUsage", "dodaf:satisfy"),
        Map.entry("SysMLv2EditService-AllocationUsage", "dodaf:allocate"),
        Map.entry("SysMLv2EditService-AllocationDefinition", "dodaf:allocate"),
        Map.entry("SysMLv2EditService-InterfaceUsage", "dodaf:exchange"),
        Map.entry("SysMLv2EditService-InterfaceDefinition", "dodaf:exchange"),
        Map.entry("SysMLv2EditService-RequirementUsage", "dodaf:requirement"),
        Map.entry("SysMLv2EditService-RequirementDefinition", "dodaf:requirement"),
        Map.entry("SysMLv2EditService-ConcernUsage", "dodaf:requirement"),
        Map.entry("SysMLv2EditService-ConcernDefinition", "dodaf:requirement"),
        Map.entry("SysMLv2EditService-ConstraintUsage", "dodaf:requirement"),
        Map.entry("SysMLv2EditService-ConstraintDefinition", "dodaf:requirement"),
        Map.entry("SysMLv2EditService-AttributeUsage", "dodaf:node"),
        Map.entry("SysMLv2EditService-AttributeDefinition", "dodaf:node"),
        Map.entry("SysMLv2EditService-ItemUsage", "dodaf:node"),
        Map.entry("SysMLv2EditService-ItemDefinition", "dodaf:node"),
        Map.entry("SysMLv2EditService-PortUsage", "dodaf:node"),
        Map.entry("SysMLv2EditService-PortDefinition", "dodaf:node"),
        Map.entry("SysMLv2EditService-ConnectionDefinition", "dodaf:node"),
        Map.entry("SysMLv2EditService-EnumerationDefinition", "dodaf:node"),
        Map.entry("SysMLv2EditService-AcceptActionUsage", "dodaf:capability"),
        Map.entry("SysMLv2EditService-OccurrenceUsage", "dodaf:capability"),
        Map.entry("SysMLv2EditService-OccurrenceDefinition", "dodaf:capability"),
        Map.entry("SysMLv2EditService-StateUsage", "dodaf:capability"),
        Map.entry("SysMLv2EditService-StateDefinition", "dodaf:capability"),
        Map.entry("SysMLv2EditService-ViewUsage", "dodaf:capability"),
        Map.entry("SysMLv2EditService-ReferenceUsage", "dodaf:node"),
        Map.entry("SysMLv2EditService-CaseUsage", "dodaf:capability"),
        Map.entry("SysMLv2EditService-CaseDefinition", "dodaf:capability"),
        Map.entry("SysMLv2EditService-UseCaseUsage", "dodaf:capability"),
        Map.entry("SysMLv2EditService-UseCaseDefinition", "dodaf:capability"),
        Map.entry("SysMLv2EditService-MetadataDefinition", "dodaf:metadata"),
        Map.entry("SysMLv2EditService-AssignmentActionUsage", "dodaf:capability")
    );

    private void tagDoDAFAlias(Element element, String childCreationDescriptionId) {
        System.err.println("=== tagDoDAFAlias: descId=" + childCreationDescriptionId);
        if (childCreationDescriptionId != null && childCreationDescriptionId.startsWith("SysMLv2EditService-PartDefinition")) {
            String n = childCreationDescriptionId.contains(":") ? childCreationDescriptionId.substring(childCreationDescriptionId.lastIndexOf(':')+1) : "";
            System.err.println("=== tagDoDAFAlias PartDef: n=" + n);
            String aliasedId = n.isEmpty() ? childCreationDescriptionId : childCreationDescriptionId.substring(0, childCreationDescriptionId.lastIndexOf(':'));
            // Map by menu item base name (strip trailing numbers from auto-numbering)
            if (n.startsWith("Capability")) setAlias(element, "dodaf:capability");
            else if (n.startsWith("OperationalCapability")) setAlias(element, "dodaf:OperationalCapability");
            else if (n.startsWith("Task") && !n.startsWith("TaskStage") && !n.startsWith("TaskIntent")) setAlias(element, "dodaf:Task");
            else if (n.startsWith("Force")) setAlias(element, "dodaf:Force");
            else if (n.startsWith("Role")) setAlias(element, "dodaf:Role");
            else if (n.startsWith("TaskStage")) setAlias(element, "dodaf:TaskStage");
            else if (n.startsWith("TaskIntent")) setAlias(element, "dodaf:TaskIntent");
            else if (n.startsWith("ImplementationPhase")) setAlias(element, "dodaf:ImplementationPhase");
            else if (n.startsWith("Vision") && !n.startsWith("VisionDescription")) setAlias(element, "dodaf:Vision");
            else if (n.startsWith("VisionDescription")) setAlias(element, "dodaf:VisionDescription");
            else if (n.startsWith("Target")) setAlias(element, "dodaf:Target");
            else if (n.startsWith("Performer")) setAlias(element, "dodaf:Performer");
            else if (n.startsWith("Location")) setAlias(element, "dodaf:Location");
            else if (n.startsWith("Condition")) setAlias(element, "dodaf:Condition");
            else if (n.startsWith("TimeScale")) setAlias(element, "dodaf:TimeScale");
            else if (n.startsWith("Equipment")) setAlias(element, "dodaf:Equipment");
            else if (n.startsWith("EquipCapability")) setAlias(element, "dodaf:EquipCapability");
            else if (n.startsWith("OperationalAction")) setAlias(element, "dodaf:OperationalAction");
            else if (n.startsWith("OperationalProblem")) setAlias(element, "dodaf:OperationalProblem");
            else if (n.startsWith("OperationalActivity")) setAlias(element, "dodaf:OperationalActivity");
            else if (n.startsWith("OperationalTask")) setAlias(element, "dodaf:OperationalTask");
            else if (n.startsWith("OperationalModel")) setAlias(element, "dodaf:OperationalModel");
            else if (n.startsWith("CommunicationActivity")) setAlias(element, "dodaf:CommunicationActivity");
            else if (n.startsWith("TransmitActivity")) setAlias(element, "dodaf:TransmitActivity");
            else if (n.startsWith("ReceiveActivity")) setAlias(element, "dodaf:ReceiveActivity");
            else if (n.startsWith("ManeuverActivity")) setAlias(element, "dodaf:ManeuverActivity");
            else if (n.startsWith("ActualOrganization")) setAlias(element, "dodaf:ActualOrganization");
            else if (n.startsWith("ActualPerson")) setAlias(element, "dodaf:ActualPerson");
            else if (n.startsWith("Duty")) setAlias(element, "dodaf:Duty");
            else if (n.startsWith("MissionPhase")) setAlias(element, "dodaf:MissionPhase");
            else if (n.startsWith("Person") && !n.startsWith("PersonType")) setAlias(element, "dodaf:Person");
            else if (n.startsWith("Lifeline")) setAlias(element, "dodaf:Lifeline");
            else if (n.startsWith("OperationalNode")) setAlias(element, "dodaf:operational");
            else if (n.startsWith("SystemNode")) setAlias(element, "dodaf:system");
            else if (n.startsWith("Organization")) setAlias(element, "dodaf:organization");
            else {
                String alias = DODAF_TYPE_TO_ALIAS.get(aliasedId);
                if (alias != null && !element.getAliasIds().contains(alias)) element.getAliasIds().add(alias);
            }
            return;
        }
        // Special case: PartUsage with "Lifeline" name → dodaf:Lifeline
        if (childCreationDescriptionId.contains("Lifeline")) {
            setAlias(element, "dodaf:Lifeline");
            return;
        }
        String alias = DODAF_TYPE_TO_ALIAS.get(childCreationDescriptionId);
        if (alias == null && childCreationDescriptionId.contains(":")) {
            alias = DODAF_TYPE_TO_ALIAS.get(childCreationDescriptionId.substring(0, childCreationDescriptionId.lastIndexOf(':')));
        }
        if (alias != null && !element.getAliasIds().contains(alias)) {
            element.getAliasIds().add(alias);
        }
    }
    private void setAlias(Element element, String alias) {
        if (!element.getAliasIds().contains(alias)) {
            element.getAliasIds().add(alias);
            System.err.println("=== setAlias: ADDED " + alias + " -> " + element.getAliasIds());
        }
    }
}
