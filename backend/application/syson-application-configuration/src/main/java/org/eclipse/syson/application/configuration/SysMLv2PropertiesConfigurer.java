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
package org.eclipse.syson.application.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.acceleo.query.services.EObjectServices;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory.Descriptor;
import org.eclipse.sirius.components.collaborative.forms.services.api.IPropertiesDescriptionRegistry;
import org.eclipse.sirius.components.collaborative.forms.services.api.IPropertiesDescriptionRegistryConfigurer;
import org.eclipse.sirius.components.core.api.IFeedbackMessageService;
import org.eclipse.sirius.components.core.api.ILabelService;
import org.eclipse.sirius.components.core.api.IReadOnlyObjectPredicate;
import org.eclipse.sirius.components.emf.services.IDAdapter;
import org.eclipse.sirius.components.emf.services.api.IEMFEditingContext;
import org.eclipse.sirius.components.interpreter.AQLInterpreter;
import org.eclipse.sirius.components.view.ChangeContext;
import org.eclipse.sirius.components.view.View;
import org.eclipse.sirius.components.view.ViewFactory;
import org.eclipse.sirius.components.view.emf.ViewConverterResult;
import org.eclipse.sirius.components.view.emf.form.ViewFormDescriptionConverter;
import org.eclipse.sirius.components.view.form.CheckboxDescription;
import org.eclipse.sirius.components.view.form.DateTimeDescription;
import org.eclipse.sirius.components.view.form.DateTimeType;
import org.eclipse.sirius.components.view.form.MultiSelectDescription;
import org.eclipse.sirius.components.view.form.FormDescription;
import org.eclipse.sirius.components.view.form.FormElementDescription;
import org.eclipse.sirius.components.view.form.FormElementFor;
import org.eclipse.sirius.components.view.form.FormElementIf;
import org.eclipse.sirius.components.view.form.FormFactory;
import org.eclipse.sirius.components.view.form.GroupDescription;
import org.eclipse.sirius.components.view.form.GroupDisplayMode;
import org.eclipse.sirius.components.view.form.LabelDescription;
import org.eclipse.sirius.components.view.form.PageDescription;
import org.eclipse.sirius.components.view.form.RadioDescription;
import org.eclipse.sirius.components.view.form.TextAreaDescription;
import org.eclipse.sirius.components.view.form.TextfieldDescription;
import org.eclipse.sirius.components.view.form.WidgetDescription;
import org.eclipse.sirius.components.view.widget.reference.ReferenceFactory;
import org.eclipse.sirius.components.view.widget.reference.ReferenceWidgetDescription;
import org.eclipse.syson.application.services.DetailsViewService;
import org.eclipse.syson.application.services.DoDAFProperties;
import org.eclipse.syson.form.services.api.IDetailsViewHelpTextProvider;
import org.eclipse.syson.form.services.aql.FormMutationAQLService;
import org.eclipse.syson.form.services.aql.FormQueryAQLService;
import org.eclipse.syson.model.services.ModelMutationElementService;
import org.eclipse.syson.model.services.aql.ModelMutationAQLService;
import org.eclipse.syson.model.services.aql.ModelQueryAQLService;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.SysmlPackage;
import org.eclipse.syson.sysml.metamodel.services.MetamodelQueryElementService;
import org.eclipse.syson.util.AQLConstants;
import org.eclipse.syson.util.AQLUtils;
import org.eclipse.syson.util.ServiceMethod;
import org.eclipse.syson.util.SysMLMetamodelHelper;
import org.springframework.context.annotation.Configuration;

/**
 * Provides custom Details view for SysML elements.
 *
 * @author arichard
 */
@Configuration
public class SysMLv2PropertiesConfigurer implements IPropertiesDescriptionRegistryConfigurer {

    private static final String CUSTOM_EXPRESSION_WIDGET_KEY = "syson:expression-value-widget";

    private static final String CORE_PROPERTIES = "核心属性";

    private static final String ADVANCED_PROPERTIES = "高级属性";

    private static final String REFERENCE_SUBSETTING_PROPERTIES = "引用子集属性";

    private static final String REDEFINITION_PROPERTIES = "Redefinition Properties";

    private static final String SUBCLASSIFICATION_PROPERTIES = "Subclassification Properties";

    private static final String STATESUBACTIONKIND_PROPERTIES = "Statesubaction Properties";

    private static final String TRANSITION_SOURCETARGET_PROPERTIES = "Transition Source and Target Properties";

    private static final String SUBSETTING_PROPERTIES = "Subsetting Properties";

    private static final String TYPING_PROPERTIES = "Typing Properties";

    private static final String MEMBERSHIP_PROPERTIES = "Membership Properties";

    private static final String REQUIREMENT_CONSTRAINT_KIND_PROPERTIES = "Kind Properties";

    private static final String ACCEPT_ACTION_USAGE_PROPERTIES = "Accept Action Usage Properties";

    private static final String AQL_NOT_SELF_IS_READ_ONLY = "aql:not(self.isReadOnly())";

    private static final String AQL_NOT_SELF_IS_READ_ONLY_E_STRUCTURAL_FEATURE = "aql:not(self.isReadOnly(eStructuralFeature))";

    private static final String E_STRUCTURAL_FEATURE = "eStructuralFeature";

    private static final String CLOSING_QUOTE_CLOSING_PARENTHESIS = "')";

    private final List<Descriptor> composedAdapterFactoryDescriptors;

    private final ViewFormDescriptionConverter converter;

    private final IFeedbackMessageService feedbackMessageService;

    private final ILabelService labelService;

    private final IReadOnlyObjectPredicate readOnlyObjectPredicate;

    private final MetamodelQueryElementService metamodelQueryElementService;

    private final List<IDetailsViewHelpTextProvider> detailViewHelpTextProviders;

    public SysMLv2PropertiesConfigurer(List<Descriptor> composedAdapterFactoryDescriptors, ViewFormDescriptionConverter converter, IFeedbackMessageService feedbackMessageService,
            ILabelService labelService, List<IDetailsViewHelpTextProvider> detailViewHelpTextProviders, final IReadOnlyObjectPredicate readOnlyObjectPredicate) {
        this.composedAdapterFactoryDescriptors = Objects.requireNonNull(composedAdapterFactoryDescriptors);
        this.converter = Objects.requireNonNull(converter);
        this.feedbackMessageService = Objects.requireNonNull(feedbackMessageService);
        this.labelService = Objects.requireNonNull(labelService);
        this.readOnlyObjectPredicate = Objects.requireNonNull(readOnlyObjectPredicate);
        this.detailViewHelpTextProviders = Objects.requireNonNull(detailViewHelpTextProviders);
        this.metamodelQueryElementService = new MetamodelQueryElementService();
    }

    @Override
    public void addPropertiesDescriptions(IPropertiesDescriptionRegistry registry) {
        // Build the actual FormDescription that will be used in Detail view.
        FormDescription viewFormDescription = this.createDetailsViewForElement();

        // The FormDescription must be part of View inside a proper EMF Resource to be correctly handled
        URI uri = URI.createURI(IEMFEditingContext.RESOURCE_SCHEME + ":///" + UUID.nameUUIDFromBytes(SysMLv2PropertiesConfigurer.class.getCanonicalName().getBytes()));
        Resource resource = new XMIResourceImpl(uri);
        View view = ViewFactory.eINSTANCE.createView();

        view.eAllContents().forEachRemaining(eObject -> {
            eObject.eAdapters().add(new IDAdapter(UUID.nameUUIDFromBytes(EcoreUtil.getURI(eObject).toString().getBytes())));
        });

        resource.getContents().add(view);
        view.getDescriptions().add(viewFormDescription);

        // Convert the View-based FormDescription and register the result into the system
        AQLInterpreter interpreter = new AQLInterpreter(List.of(),
                List.of(new DetailsViewService(this.composedAdapterFactoryDescriptors, this.feedbackMessageService, this.readOnlyObjectPredicate, this.metamodelQueryElementService,
                        this.detailViewHelpTextProviders), this.labelService,
                        new ModelMutationAQLService(new ModelMutationElementService()), new ModelQueryAQLService(), new FormMutationAQLService(), new FormQueryAQLService()),
                List.of(SysmlPackage.eINSTANCE));
        ViewConverterResult converterResult = this.converter.convert(viewFormDescription, List.of(), interpreter);
        if (converterResult != null && converterResult.representationDescription() instanceof org.eclipse.sirius.components.forms.description.FormDescription formDescription) {
            formDescription.getPageDescriptions().forEach(registry::add);
        }
    }

    private FormDescription createDetailsViewForElement() {
        String domainType = SysMLMetamodelHelper.buildQualifiedName(SysmlPackage.eINSTANCE.getElement());
        FormDescription form = FormFactory.eINSTANCE.createFormDescription();
        form.setName("SysON Details View");
        form.setDomainType(domainType);
        form.setTitleExpression("SysON Details View");

        PageDescription pageCore = FormFactory.eINSTANCE.createPageDescription();
        pageCore.setName("SysON-DetailsView-Core");
        pageCore.setDomainType(domainType);
        pageCore.setPreconditionExpression("");
        pageCore.setLabelExpression("核心");
        pageCore.getGroups().add(this.createCorePropertiesGroup());
        pageCore.getGroups().add(this.createDoDAFCapabilityPropertiesGroup());
        pageCore.getGroups().add(this.createDoDAFOperationalCapabilityPropertiesGroup());
        pageCore.getGroups().add(this.createDoDAFTaskStagePropertiesGroup());
        pageCore.getGroups().add(this.createDoDAFGenericPropertiesGroup());
        pageCore.getGroups().add(this.createVisibilityPropertyGroup());
        pageCore.getGroups().add(this.createExtraReferenceSubsettingPropertiesGroup());
        pageCore.getGroups().add(this.createExtraRedefinitionPropertiesGroup());
        pageCore.getGroups().add(this.createExtraStatesubactionMembershipKindPropertiesGroup());
        pageCore.getGroups().add(this.createExtraSubclassificationPropertiesGroup());
        pageCore.getGroups().add(this.createExtraSubsettingPropertiesGroup());
        pageCore.getGroups().add(this.createExtraFeatureTypingPropertiesGroup());
        pageCore.getGroups().add(this.createExtraRequirementConstraintMembershipPropertiesGroup());
        pageCore.getGroups().add(this.createExtraAcceptActionUsagePropertiesGroup());
        pageCore.getGroups().add(this.createExtraTransitionSourceTargetPropertiesGroup());
        pageCore.getGroups().add(this.createFeatureValuePropertiesGroup());
        pageCore.getGroups().add(this.createExpressionPropertiesGroup());

        PageDescription pageAdvanced = FormFactory.eINSTANCE.createPageDescription();
        pageAdvanced.setName("SysON-DetailsView-Advanced");
        pageAdvanced.setDomainType(domainType);
        pageAdvanced.setPreconditionExpression("");
        pageAdvanced.setLabelExpression("高级");
        pageAdvanced.getGroups().add(this.createAdvancedPropertiesGroup());

        form.getPages().add(pageCore);
        form.getPages().add(pageAdvanced);

        return form;
    }

    /**
     * Creates a group to display the value of an Expression.
     *
     * @return a {@link GroupDescription}
     */
    private GroupDescription createExpressionPropertiesGroup() {
        GroupDescription group = FormFactory.eINSTANCE.createGroupDescription();
        group.setDisplayMode(GroupDisplayMode.LIST);
        group.setName("Expression Value");
        group.setLabelExpression("");
        group.setSemanticCandidatesExpression(ServiceMethod.of0(DetailsViewService::getExpression).aqlSelf());

        TextAreaDescription expressionWidget = FormFactory.eINSTANCE.createTextAreaDescription();
        expressionWidget.setName("Expression");
        expressionWidget.setLabelExpression(CUSTOM_EXPRESSION_WIDGET_KEY);
        expressionWidget.setValueExpression(ServiceMethod.of0(DetailsViewService::getExpressionTextualRepresentation).aqlSelf());
        expressionWidget.setIsEnabledExpression(AQLConstants.AQL_FALSE);

        group.getChildren().add(expressionWidget);

        return group;
    }

    /**
     * Creates a group to display the value of a Feature or FeatureValue.
     *
     * @return a {@link GroupDescription}
     */
    private GroupDescription createFeatureValuePropertiesGroup() {
        GroupDescription group = FormFactory.eINSTANCE.createGroupDescription();
        group.setDisplayMode(GroupDisplayMode.LIST);
        group.setName("Value");
        group.setLabelExpression("");
        group.setSemanticCandidatesExpression(ServiceMethod.of0(DetailsViewService::getFeatureValue).aqlSelf());

        TextAreaDescription expressionWidget = FormFactory.eINSTANCE.createTextAreaDescription();
        expressionWidget.setName("ValueExpression");
        expressionWidget.setLabelExpression("值");
        expressionWidget.setValueExpression(ServiceMethod.of0(DetailsViewService::getValueExpressionTextualRepresentation).aqlSelf());
        expressionWidget.setIsEnabledExpression(AQLConstants.AQL_FALSE);

        group.getChildren().add(expressionWidget);

        return group;
    }

    /**
     * Creates the generic DoDAF properties group: visible for every element carrying a
     * {@code dodaf:} alias that does not own a dedicated group. The property list comes
     * from the OWL-derived configuration table ({@link DoDAFProperties}).
     *
     * @return a {@link GroupDescription}
     */
    private GroupDescription createDoDAFGenericPropertiesGroup() {
        GroupDescription group = FormFactory.eINSTANCE.createGroupDescription();
        group.setDisplayMode(GroupDisplayMode.LIST);
        group.setName("DoDAF 属性");
        group.setLabelExpression("DoDAF 属性");
        group.setSemanticCandidatesExpression(ServiceMethod.of0(DetailsViewService::getDoDAFElement).aqlSelf());

        FormElementFor forElt = FormFactory.eINSTANCE.createFormElementFor();
        forElt.setName("Widgets for DoDAF Generic Group");
        forElt.setIterator("prop");
        forElt.setIterableExpression(ServiceMethod.of0(DetailsViewService::getDoDAFProps).aqlSelf());
        forElt.getChildren().addAll(this.createDoDAFPropWidgets());
        group.getChildren().add(forElt);

        return group;
    }

    private List<FormElementIf> createDoDAFPropWidgets() {
        List<FormElementIf> widgets = new ArrayList<>();

        // String / number properties -> textfield
        FormElementIf textfield = FormFactory.eINSTANCE.createFormElementIf();
        textfield.setName("DoDAF String Properties");
        textfield.setPredicateExpression(ServiceMethod.of1(DetailsViewService::isDodafStringProp).aqlSelf("prop"));
        TextfieldDescription tf = FormFactory.eINSTANCE.createTextfieldDescription();
        tf.setName("DoDAFTextfieldWidget");
        tf.setLabelExpression(ServiceMethod.of1(DetailsViewService::getDodafPropLabel).aqlSelf("prop"));
        tf.setValueExpression(ServiceMethod.of1(DetailsViewService.class, DetailsViewService::getDodafPropValue, Element.class, DoDAFProperties.DodafProp.class).aqlSelf("prop"));
        ChangeContext setTf = ViewFactory.eINSTANCE.createChangeContext();
        setTf.setExpression(ServiceMethod.of2(DetailsViewService.class, DetailsViewService::setDodafPropValue, Element.class, DoDAFProperties.DodafProp.class, Object.class)
                .aqlSelf("prop", ViewFormDescriptionConverter.NEW_VALUE));
        tf.getBody().add(setTf);
        textfield.getChildren().add(tf);
        widgets.add(textfield);

        // Boolean properties -> checkbox
        FormElementIf checkbox = FormFactory.eINSTANCE.createFormElementIf();
        checkbox.setName("DoDAF Boolean Properties");
        checkbox.setPredicateExpression(ServiceMethod.of1(DetailsViewService::isDodafBooleanProp).aqlSelf("prop"));
        CheckboxDescription cb = FormFactory.eINSTANCE.createCheckboxDescription();
        cb.setName("DoDAFCheckboxWidget");
        cb.setLabelExpression(ServiceMethod.of1(DetailsViewService::getDodafPropLabel).aqlSelf("prop"));
        cb.setValueExpression(ServiceMethod.of1(DetailsViewService::isDodafBoolValue).aqlSelf("prop"));
        ChangeContext setCb = ViewFactory.eINSTANCE.createChangeContext();
        setCb.setExpression(ServiceMethod.of2(DetailsViewService.class, DetailsViewService::setDodafPropValue, Element.class, DoDAFProperties.DodafProp.class, Object.class)
                .aqlSelf("prop", ViewFormDescriptionConverter.NEW_VALUE));
        cb.getBody().add(setCb);
        checkbox.getChildren().add(cb);
        widgets.add(checkbox);

        // Enum properties -> radio
        FormElementIf radio = FormFactory.eINSTANCE.createFormElementIf();
        radio.setName("DoDAF Enum Properties");
        radio.setPredicateExpression(ServiceMethod.of1(DetailsViewService::isDodafEnumProp).aqlSelf("prop"));
        RadioDescription rd = FormFactory.eINSTANCE.createRadioDescription();
        rd.setName("DoDAFRadioWidget");
        rd.setLabelExpression(ServiceMethod.of1(DetailsViewService::getDodafPropLabel).aqlSelf("prop"));
        rd.setCandidatesExpression(ServiceMethod.of1(DetailsViewService::getDodafEnumCandidates).aqlSelf("prop"));
        rd.setCandidateLabelExpression("aql:candidate");
        rd.setValueExpression(ServiceMethod.of1(DetailsViewService.class, DetailsViewService::getDodafPropValue, Element.class, DoDAFProperties.DodafProp.class).aqlSelf("prop"));
        ChangeContext setRd = ViewFactory.eINSTANCE.createChangeContext();
        setRd.setExpression(ServiceMethod.of2(DetailsViewService.class, DetailsViewService::setDodafPropValue, Element.class, DoDAFProperties.DodafProp.class, Object.class)
                .aqlSelf("prop", ViewFormDescriptionConverter.NEW_VALUE));
        rd.getBody().add(setRd);
        radio.getChildren().add(rd);
        widgets.add(radio);

        // Date properties -> date-time picker
        FormElementIf datePicker = FormFactory.eINSTANCE.createFormElementIf();
        datePicker.setName("DoDAF Date Properties");
        datePicker.setPredicateExpression(ServiceMethod.of1(DetailsViewService::isDodafDateProp).aqlSelf("prop"));
        DateTimeDescription dt = FormFactory.eINSTANCE.createDateTimeDescription();
        dt.setName("DoDAFDateTimeWidget");
        dt.setLabelExpression(ServiceMethod.of1(DetailsViewService::getDodafPropLabel).aqlSelf("prop"));
        dt.setStringValueExpression(ServiceMethod.of1(DetailsViewService.class, DetailsViewService::getDodafPropValue, Element.class, DoDAFProperties.DodafProp.class).aqlSelf("prop"));
        dt.setType(DateTimeType.DATE);
        dt.setIsEnabledExpression(AQLConstants.AQL_TRUE);
        ChangeContext setDt = ViewFactory.eINSTANCE.createChangeContext();
        setDt.setExpression(ServiceMethod.of2(DetailsViewService.class, DetailsViewService::setDodafPropValue, Element.class, DoDAFProperties.DodafProp.class, Object.class)
                .aqlSelf("prop", ViewFormDescriptionConverter.NEW_VALUE));
        dt.getBody().add(setDt);
        datePicker.getChildren().add(dt);
        widgets.add(datePicker);

        // Reference properties -> multi-select of model elements
        FormElementIf ref = FormFactory.eINSTANCE.createFormElementIf();
        ref.setName("DoDAF Reference Properties");
        ref.setPredicateExpression(ServiceMethod.of1(DetailsViewService::isDodafRefProp).aqlSelf("prop"));
        MultiSelectDescription ms = FormFactory.eINSTANCE.createMultiSelectDescription();
        ms.setName("DoDAFMultiSelectWidget");
        ms.setLabelExpression(ServiceMethod.of1(DetailsViewService::getDodafPropLabel).aqlSelf("prop"));
        ms.setCandidatesExpression(ServiceMethod.of0(DetailsViewService::getDoDAFReferenceCandidates).aqlSelf());
        ms.setCandidateLabelExpression(ServiceMethod.of1(DetailsViewService::getDoDAFRefCandidateLabel).aqlSelf("candidate"));
        ms.setValueExpression(ServiceMethod.of1(DetailsViewService.class, DetailsViewService::getDoDAFRefValue, Element.class, DoDAFProperties.DodafProp.class).aqlSelf("prop"));
        ms.setIsEnabledExpression(AQLConstants.AQL_TRUE);
        ChangeContext setMs = ViewFactory.eINSTANCE.createChangeContext();
        setMs.setExpression(ServiceMethod.of2(DetailsViewService.class, DetailsViewService::setDoDAFRefValue, Element.class, DoDAFProperties.DodafProp.class, Object.class)
                .aqlSelf("prop", ViewFormDescriptionConverter.NEW_VALUE));
        ms.getBody().add(setMs);
        ref.getChildren().add(ms);
        widgets.add(ref);

        return widgets;
    }

    /**
     * Creates the DoDAF TaskStage properties group: only visible for elements whose
     * aliasIds contain {@code dodaf:TaskStage}. Inherits the generic Classifier property
     * set (name, isAbstract) plus the DoDAF-specific booleans stored in the
     * {@code dodaf} EAnnotation.
     *
     * @return a {@link GroupDescription}
     */
    private GroupDescription createDoDAFTaskStagePropertiesGroup() {
        GroupDescription group = FormFactory.eINSTANCE.createGroupDescription();
        group.setDisplayMode(GroupDisplayMode.LIST);
        group.setName("DoDAF 任务阶段属性");
        group.setLabelExpression("任务阶段属性");
        group.setSemanticCandidatesExpression(ServiceMethod.of0(DetailsViewService::getDoDAFTaskStage).aqlSelf());

        FormElementFor forElt = FormFactory.eINSTANCE.createFormElementFor();
        forElt.setName("Widgets for DoDAF TaskStage Group");
        forElt.setIterator(E_STRUCTURAL_FEATURE);
        forElt.setIterableExpression(ServiceMethod.of0(DetailsViewService::getDoDAFCapabilityFeatures).aqlSelf());
        forElt.getChildren().addAll(this.createWidgets());
        group.getChildren().add(forElt);

        // OWL-configured properties (data + reference + generic) for the task stage element
        FormElementFor forProps = FormFactory.eINSTANCE.createFormElementFor();
        forProps.setName("Widgets for DoDAF TaskStage OWL Props");
        forProps.setIterator("prop");
        forProps.setIterableExpression(ServiceMethod.of0(DetailsViewService::getDoDAFProps).aqlSelf());
        forProps.getChildren().addAll(this.createDoDAFPropWidgets());
        group.getChildren().add(forProps);

        return group;
    }

    /**
     * Creates the DoDAF OperationalCapability properties group: only visible for elements
     * whose aliasIds contain {@code dodaf:OperationalCapability}. Inherits the Capability
     * property set per the DoDAF ontology (OperationalCapability subClassOf Capability).
     *
     * @return a {@link GroupDescription}
     */
    private GroupDescription createDoDAFOperationalCapabilityPropertiesGroup() {
        GroupDescription group = FormFactory.eINSTANCE.createGroupDescription();
        group.setDisplayMode(GroupDisplayMode.LIST);
        group.setName("DoDAF 作战能力属性");
        group.setLabelExpression("作战能力属性");
        group.setSemanticCandidatesExpression(ServiceMethod.of0(DetailsViewService::getDoDAFOperationalCapability).aqlSelf());

        FormElementFor forElt = FormFactory.eINSTANCE.createFormElementFor();
        forElt.setName("Widgets for DoDAF OperationalCapability Group");
        forElt.setIterator(E_STRUCTURAL_FEATURE);
        forElt.setIterableExpression(ServiceMethod.of0(DetailsViewService::getDoDAFCapabilityFeatures).aqlSelf());
        forElt.getChildren().addAll(this.createWidgets());
        group.getChildren().add(forElt);

        group.getChildren().add(this.createDodafTextField("aCapabilityDescription", "能力描述"));
        group.getChildren().add(this.createDodafBooleanCheckbox("isLeaf", "是否叶属性"));
        group.getChildren().add(this.createDodafBooleanCheckbox("isActive", "是否为活动对象"));
        group.getChildren().add(this.createDodafBooleanCheckbox("isFinalSpecialization", "是否为final类"));

        return group;
    }

    /**
     * Creates the DoDAF Capability properties group: only visible for elements whose
     * aliasIds contain {@code dodaf:capability}. Displays the SysML-backed features
     * (name, visibility, isAbstract) plus the DoDAF-specific booleans stored in the
     * {@code dodaf} EAnnotation (isLeaf, isActive, isFinalSpecialization).
     *
     * @return a {@link GroupDescription}
     */
    private GroupDescription createDoDAFCapabilityPropertiesGroup() {
        GroupDescription group = FormFactory.eINSTANCE.createGroupDescription();
        group.setDisplayMode(GroupDisplayMode.LIST);
        group.setName("DoDAF 能力属性");
        group.setLabelExpression("能力属性");
        group.setSemanticCandidatesExpression(ServiceMethod.of0(DetailsViewService::getDoDAFCapability).aqlSelf());

        FormElementFor forElt = FormFactory.eINSTANCE.createFormElementFor();
        forElt.setName("Widgets for DoDAF Capability Group");
        forElt.setIterator(E_STRUCTURAL_FEATURE);
        forElt.setIterableExpression(ServiceMethod.of0(DetailsViewService::getDoDAFCapabilityFeatures).aqlSelf());
        forElt.getChildren().addAll(this.createWidgets());
        group.getChildren().add(forElt);

        group.getChildren().add(this.createDodafTextField("aCapabilityDescription", "能力描述"));
        group.getChildren().add(this.createDodafBooleanCheckbox("isLeaf", "是否叶属性"));
        group.getChildren().add(this.createDodafBooleanCheckbox("isActive", "是否为活动对象"));
        group.getChildren().add(this.createDodafBooleanCheckbox("isFinalSpecialization", "是否为final类"));

        return group;
    }

    private TextfieldDescription createDodafTextField(String key, String label) {
        TextfieldDescription textfield = FormFactory.eINSTANCE.createTextfieldDescription();
        textfield.setName("DoDAF_" + key);
        textfield.setLabelExpression(label);
        textfield.setValueExpression(ServiceMethod.of1(DetailsViewService.class, DetailsViewService::getDodafPropValueByKey, Element.class, String.class).aqlSelf("'" + key + "'"));
        textfield.setIsEnabledExpression(AQLConstants.AQL_TRUE);
        ChangeContext setNewValueOperation = ViewFactory.eINSTANCE.createChangeContext();
        setNewValueOperation.setExpression(ServiceMethod.of2(DetailsViewService.class, DetailsViewService::setDodafPropValueByKey, Element.class, String.class, Object.class)
                .aqlSelf("'" + key + "'", ViewFormDescriptionConverter.NEW_VALUE));
        textfield.getBody().add(setNewValueOperation);
        return textfield;
    }

    private CheckboxDescription createDodafBooleanCheckbox(String key, String label) {        CheckboxDescription checkbox = FormFactory.eINSTANCE.createCheckboxDescription();
        checkbox.setName("DoDAF_" + key);
        checkbox.setLabelExpression(label);
        checkbox.setValueExpression(ServiceMethod.of1(DetailsViewService.class, DetailsViewService::getDodafBooleanProperty, Element.class, String.class).aqlSelf("'" + key + "'"));
        checkbox.setIsEnabledExpression(AQLConstants.AQL_TRUE);
        ChangeContext setNewValueOperation = ViewFactory.eINSTANCE.createChangeContext();
        setNewValueOperation.setExpression(ServiceMethod.of2(DetailsViewService.class, DetailsViewService::setDodafBooleanProperty, Element.class, String.class, boolean.class)
                .aqlSelf("'" + key + "'", ViewFormDescriptionConverter.NEW_VALUE));
        checkbox.getBody().add(setNewValueOperation);
        return checkbox;
    }

    private GroupDescription createCorePropertiesGroup() {        GroupDescription group = FormFactory.eINSTANCE.createGroupDescription();
        group.setDisplayMode(GroupDisplayMode.LIST);
        group.setName(CORE_PROPERTIES);
        group.setLabelExpression("aql:self.eClass().getStyledLabel() + ' 属性'");
        group.setSemanticCandidatesExpression(AQLConstants.AQL_SELF);

        group.getChildren().add(this.createCoreWidgets());
        group.getChildren().add(this.createCommentWidget());
        group.getChildren().add(this.createDocumentationWidget());

        return group;
    }

    private GroupDescription createAdvancedPropertiesGroup() {
        GroupDescription group = FormFactory.eINSTANCE.createGroupDescription();
        group.setDisplayMode(GroupDisplayMode.LIST);
        group.setName(ADVANCED_PROPERTIES);
        group.setLabelExpression("aql:self.eClass().getStyledLabel() + ' 属性'");
        group.setSemanticCandidatesExpression(AQLConstants.AQL_SELF);

        group.getChildren().add(this.createAdvancedWidgets());

        return group;
    }

    // should handle multiple Redefinition
    private GroupDescription createExtraRedefinitionPropertiesGroup() {
        GroupDescription group = FormFactory.eINSTANCE.createGroupDescription();
        group.setDisplayMode(GroupDisplayMode.LIST);
        group.setName(REDEFINITION_PROPERTIES);
        group.setLabelExpression("");
        group.setSemanticCandidatesExpression("aql:self.ownedRelationship->filter(sysml::Redefinition)");

        ReferenceWidgetDescription refWidget = ReferenceFactory.eINSTANCE.createReferenceWidgetDescription();
        refWidget.setName("ExtraReferenceWidget");
        refWidget.setLabelExpression("重定义");
        refWidget.setReferenceNameExpression(SysmlPackage.eINSTANCE.getRedefinition_RedefinedFeature().getName());
        refWidget.setReferenceOwnerExpression(AQLConstants.AQL_SELF);
        refWidget.setIsEnabledExpression(AQL_NOT_SELF_IS_READ_ONLY);
        ChangeContext setRefWidget = ViewFactory.eINSTANCE.createChangeContext();
        setRefWidget.setExpression(ServiceMethod.of2(DetailsViewService::handleReferenceWidgetNewValue)
                .aqlSelf(AQLUtils.aqlString(SysmlPackage.eINSTANCE.getRedefinition_RedefinedFeature().getName()), ViewFormDescriptionConverter.NEW_VALUE));
        refWidget.getBody().add(setRefWidget);

        group.getChildren().add(refWidget);

        return group;
    }

    // should handle multiple ReferenceSubsetting
    private GroupDescription createExtraReferenceSubsettingPropertiesGroup() {
        GroupDescription group = FormFactory.eINSTANCE.createGroupDescription();
        group.setDisplayMode(GroupDisplayMode.LIST);
        group.setName(REFERENCE_SUBSETTING_PROPERTIES);
        group.setLabelExpression("");
        group.setSemanticCandidatesExpression("aql:self.ownedRelationship->filter(sysml::ReferenceSubsetting)");

        ReferenceWidgetDescription refWidget = ReferenceFactory.eINSTANCE.createReferenceWidgetDescription();
        refWidget.setName("ExtraReferenceWidget");
        refWidget.setLabelExpression("引用");
        refWidget.setReferenceNameExpression(SysmlPackage.eINSTANCE.getReferenceSubsetting_ReferencedFeature().getName());
        refWidget.setReferenceOwnerExpression(AQLConstants.AQL_SELF);
        refWidget.setIsEnabledExpression(AQL_NOT_SELF_IS_READ_ONLY);
        ChangeContext setRefWidget = ViewFactory.eINSTANCE.createChangeContext();
        setRefWidget.setExpression(ServiceMethod.of2(DetailsViewService::handleReferenceWidgetNewValue)
                .aqlSelf(AQLUtils.aqlString(SysmlPackage.eINSTANCE.getReferenceSubsetting_ReferencedFeature().getName()), ViewFormDescriptionConverter.NEW_VALUE));
        refWidget.getBody().add(setRefWidget);

        group.getChildren().add(refWidget);

        return group;
    }

    private GroupDescription createExtraStatesubactionMembershipKindPropertiesGroup() {
        GroupDescription group = FormFactory.eINSTANCE.createGroupDescription();
        group.setDisplayMode(GroupDisplayMode.LIST);
        group.setName(STATESUBACTIONKIND_PROPERTIES);
        group.setLabelExpression("");
        group.setSemanticCandidatesExpression("aql:self.eContainer()->filter(sysml::StateSubactionMembership)");

        RadioDescription radio = FormFactory.eINSTANCE.createRadioDescription();
        radio.setName("ExtraRadioKindWidget");
        radio.setLabelExpression("类型");
        radio.setCandidatesExpression(ServiceMethod.of1(DetailsViewService.class, DetailsViewService::getEnumCandidates, Element.class, String.class)
                .aqlSelf(AQLUtils.aqlString(SysmlPackage.eINSTANCE.getStateSubactionMembership_Kind().getName())));
        radio.setCandidateLabelExpression("aql:candidate.name");
        radio.setValueExpression(ServiceMethod.of1(DetailsViewService.class, DetailsViewService::getEnumValue, Element.class, String.class)
                .aqlSelf(AQLUtils.aqlString(SysmlPackage.eINSTANCE.getStateSubactionMembership_Kind().getName())));
        radio.setIsEnabledExpression(AQL_NOT_SELF_IS_READ_ONLY);
        ChangeContext setNewValueOperation = ViewFactory.eINSTANCE.createChangeContext();
        setNewValueOperation.setExpression(ServiceMethod.of2(DetailsViewService.class, DetailsViewService::setNewValue, Element.class, String.class, Object.class)
                .aqlSelf(AQLUtils.aqlString(SysmlPackage.eINSTANCE.getStateSubactionMembership_Kind().getName()), "newValue.instance"));
        radio.getBody().add(setNewValueOperation);

        group.getChildren().add(radio);

        return group;
    }

    // should handle multiple Subclassification
    private GroupDescription createExtraSubclassificationPropertiesGroup() {
        GroupDescription group = FormFactory.eINSTANCE.createGroupDescription();
        group.setDisplayMode(GroupDisplayMode.LIST);
        group.setName(SUBCLASSIFICATION_PROPERTIES);
        group.setLabelExpression("");
        group.setSemanticCandidatesExpression("aql:self.ownedRelationship->filter(sysml::Subclassification)");

        ReferenceWidgetDescription refWidget = ReferenceFactory.eINSTANCE.createReferenceWidgetDescription();
        refWidget.setName("ExtraReferenceWidget");
        refWidget.setLabelExpression("特化");
        refWidget.setReferenceNameExpression(SysmlPackage.eINSTANCE.getSubclassification_Superclassifier().getName());
        refWidget.setReferenceOwnerExpression(AQLConstants.AQL_SELF);
        refWidget.setIsEnabledExpression(AQL_NOT_SELF_IS_READ_ONLY);
        ChangeContext setRefWidget = ViewFactory.eINSTANCE.createChangeContext();
        setRefWidget.setExpression(ServiceMethod.of2(DetailsViewService::handleReferenceWidgetNewValue)
                .aqlSelf(AQLUtils.aqlString(SysmlPackage.eINSTANCE.getSubclassification_Superclassifier().getName()), ViewFormDescriptionConverter.NEW_VALUE));
        refWidget.getBody().add(setRefWidget);

        group.getChildren().add(refWidget);

        return group;
    }

    // should handle multiple Subsetting
    private GroupDescription createExtraSubsettingPropertiesGroup() {
        GroupDescription group = FormFactory.eINSTANCE.createGroupDescription();
        group.setDisplayMode(GroupDisplayMode.LIST);
        group.setName(SUBSETTING_PROPERTIES);
        group.setLabelExpression("");
        group.setSemanticCandidatesExpression("aql:self.ownedRelationship->select(r | r.oclIsTypeOf(sysml::Subsetting))");

        ReferenceWidgetDescription refWidget = ReferenceFactory.eINSTANCE.createReferenceWidgetDescription();
        refWidget.setName("ExtraReferenceWidget");
        refWidget.setLabelExpression("子集");
        refWidget.setReferenceNameExpression(SysmlPackage.eINSTANCE.getSubsetting_SubsettedFeature().getName());
        refWidget.setReferenceOwnerExpression(AQLConstants.AQL_SELF);
        refWidget.setIsEnabledExpression(AQL_NOT_SELF_IS_READ_ONLY);
        ChangeContext setRefWidget = ViewFactory.eINSTANCE.createChangeContext();
        setRefWidget.setExpression(ServiceMethod.of2(DetailsViewService::handleReferenceWidgetNewValue)
                .aqlSelf(AQLUtils.aqlString(SysmlPackage.eINSTANCE.getSubsetting_SubsettedFeature().getName()), ViewFormDescriptionConverter.NEW_VALUE));
        refWidget.getBody().add(setRefWidget);

        group.getChildren().add(refWidget);

        return group;
    }

    // should handle multiple FeatureTyping
    private GroupDescription createExtraFeatureTypingPropertiesGroup() {
        GroupDescription group = FormFactory.eINSTANCE.createGroupDescription();
        group.setDisplayMode(GroupDisplayMode.LIST);
        group.setName(TYPING_PROPERTIES);
        group.setLabelExpression("");
        // This widget is declared on the Feature.type derived many-valuated reference.
        // It allows to display the Typed By reference widget even if the Feature does not have a FeatureTyping yet.
        // Feature.type is a derived many-valuated reference, containing the union of all mono-valuated
        // "FeatureTyping.type" references of FeatureTyping children of this Feature
        group.setSemanticCandidatesExpression("aql:self->filter(sysml::Feature)");

        ReferenceWidgetDescription refWidget = ReferenceFactory.eINSTANCE.createReferenceWidgetDescription();
        refWidget.setName("ExtraReferenceWidget");
        refWidget.setLabelExpression("类型引用");
        refWidget.setReferenceNameExpression(SysmlPackage.eINSTANCE.getFeature_Type().getName());
        refWidget.setReferenceOwnerExpression(AQLConstants.AQL_SELF);
        refWidget.setIsEnabledExpression(AQL_NOT_SELF_IS_READ_ONLY);
        ChangeContext setRefWidget = ViewFactory.eINSTANCE.createChangeContext();
        setRefWidget.setExpression(ServiceMethod.of1(DetailsViewService::handleFeatureTypingNewValue).aqlSelf(ViewFormDescriptionConverter.NEW_VALUE));
        refWidget.getBody().add(setRefWidget);

        group.getChildren().add(refWidget);

        return group;
    }

    private GroupDescription createExtraRequirementConstraintMembershipPropertiesGroup() {
        GroupDescription group = FormFactory.eINSTANCE.createGroupDescription();
        group.setDisplayMode(GroupDisplayMode.LIST);
        group.setName(REQUIREMENT_CONSTRAINT_KIND_PROPERTIES);
        group.setLabelExpression("");
        group.setSemanticCandidatesExpression("aql:self.eContainer()->filter(sysml::RequirementConstraintMembership)");

        RadioDescription radio = FormFactory.eINSTANCE.createRadioDescription();
        radio.setName("ExtraRadioKindWidget");
        radio.setLabelExpression("类型");
        radio.setCandidatesExpression("aql:self.getEnumCandidates('" + SysmlPackage.eINSTANCE.getRequirementConstraintMembership_Kind().getName() + CLOSING_QUOTE_CLOSING_PARENTHESIS);
        radio.setCandidateLabelExpression("aql:candidate.name");
        radio.setValueExpression("aql:self.getEnumValue('" + SysmlPackage.eINSTANCE.getRequirementConstraintMembership_Kind().getName() + CLOSING_QUOTE_CLOSING_PARENTHESIS);
        radio.setIsEnabledExpression(AQL_NOT_SELF_IS_READ_ONLY);
        ChangeContext setNewValueOperation = ViewFactory.eINSTANCE.createChangeContext();
        setNewValueOperation.setExpression("aql:self.setNewValue('" + SysmlPackage.eINSTANCE.getRequirementConstraintMembership_Kind().getName() + "', newValue.instance)");
        radio.getBody().add(setNewValueOperation);

        group.getChildren().add(radio);

        return group;
    }

    private GroupDescription createVisibilityPropertyGroup() {
        GroupDescription group = FormFactory.eINSTANCE.createGroupDescription();
        group.setDisplayMode(GroupDisplayMode.LIST);
        group.setName(MEMBERSHIP_PROPERTIES);
        group.setLabelExpression("");
        group.setSemanticCandidatesExpression(ServiceMethod.of0(DetailsViewService::getVisibilityPropertyOwner).aqlSelf());

        RadioDescription radio = FormFactory.eINSTANCE.createRadioDescription();
        radio.setName("ExtraRadioVisibilityWidget");
        radio.setLabelExpression("可见性");
        radio.setCandidatesExpression(ServiceMethod.of0(DetailsViewService::getVisibilityEnumLiterals).aqlSelf());
        radio.setCandidateLabelExpression("aql:candidate.name");
        radio.setValueExpression(ServiceMethod.of0(DetailsViewService::getVisibilityValue).aqlSelf());
        radio.setIsEnabledExpression(AQL_NOT_SELF_IS_READ_ONLY);
        ChangeContext setNewValueOperation = ViewFactory.eINSTANCE.createChangeContext();
        setNewValueOperation.setExpression(ServiceMethod.of1(DetailsViewService::setVisibilityValue).aqlSelf("newValue.instance"));
        radio.getBody().add(setNewValueOperation);

        group.getChildren().add(radio);

        return group;
    }

    private GroupDescription createExtraAcceptActionUsagePropertiesGroup() {
        GroupDescription group = FormFactory.eINSTANCE.createGroupDescription();
        group.setDisplayMode(GroupDisplayMode.LIST);
        group.setName(ACCEPT_ACTION_USAGE_PROPERTIES);
        group.setLabelExpression("");
        group.setSemanticCandidatesExpression(ServiceMethod.of0(DetailsViewService::getAcceptActionUsage).aqlSelf());

        ReferenceWidgetDescription payloadRefWidget = ReferenceFactory.eINSTANCE.createReferenceWidgetDescription();
        payloadRefWidget.setName("ExtraPayloadWidget");
        payloadRefWidget.setLabelExpression("载荷");
        payloadRefWidget.setReferenceNameExpression(SysmlPackage.eINSTANCE.getFeatureTyping_Type().getName());
        payloadRefWidget.setReferenceOwnerExpression(ServiceMethod.of0(DetailsViewService::getAcceptActionUsagePayloadFeatureTyping).aqlSelf());
        payloadRefWidget.setIsEnabledExpression(AQL_NOT_SELF_IS_READ_ONLY);
        ChangeContext setPayloadRefWidget = ViewFactory.eINSTANCE.createChangeContext();
        setPayloadRefWidget.setExpression(ServiceMethod.of1(DetailsViewService::setAcceptActionUsagePayloadParameter).aqlSelf(ViewFormDescriptionConverter.NEW_VALUE));
        payloadRefWidget.getBody().add(setPayloadRefWidget);

        ReferenceWidgetDescription receiverRefWidget = ReferenceFactory.eINSTANCE.createReferenceWidgetDescription();
        receiverRefWidget.setName("ExtraReceiverWidget");
        receiverRefWidget.setLabelExpression("接收方");
        receiverRefWidget.setReferenceNameExpression(SysmlPackage.eINSTANCE.getMembership_MemberElement().getName());
        receiverRefWidget.setReferenceOwnerExpression(ServiceMethod.of0(DetailsViewService::getAcceptActionUsageReceiverMembership).aqlSelf());
        receiverRefWidget.setIsEnabledExpression(AQL_NOT_SELF_IS_READ_ONLY);
        ChangeContext setReceiverRefWidget = ViewFactory.eINSTANCE.createChangeContext();
        setReceiverRefWidget.setExpression(ServiceMethod.of1(DetailsViewService::setAcceptActionUsageReceiverArgument).aqlSelf(ViewFormDescriptionConverter.NEW_VALUE));
        receiverRefWidget.getBody().add(setReceiverRefWidget);

        group.getChildren().add(payloadRefWidget);
        group.getChildren().add(receiverRefWidget);

        return group;
    }

    private GroupDescription createExtraTransitionSourceTargetPropertiesGroup() {
        GroupDescription group = FormFactory.eINSTANCE.createGroupDescription();
        group.setDisplayMode(GroupDisplayMode.LIST);
        group.setName(TRANSITION_SOURCETARGET_PROPERTIES);
        group.setLabelExpression("");
        group.setSemanticCandidatesExpression(ServiceMethod.of0(DetailsViewService::getTransitionUsage).aqlSelf());

        ReferenceWidgetDescription sourceRefWidget = ReferenceFactory.eINSTANCE.createReferenceWidgetDescription();
        sourceRefWidget.setName("ExtraSourceWidget");
        sourceRefWidget.setLabelExpression("源");
        sourceRefWidget.setReferenceNameExpression(SysmlPackage.eINSTANCE.getTransitionUsage_Source().getName());
        sourceRefWidget.setReferenceOwnerExpression(AQLConstants.AQL_SELF);
        sourceRefWidget.setIsEnabledExpression(AQL_NOT_SELF_IS_READ_ONLY);
        ChangeContext setSourceRefWidget = ViewFactory.eINSTANCE.createChangeContext();
        setSourceRefWidget.setExpression(ServiceMethod.of1(DetailsViewService::setTransitionSourceParameter).aqlSelf(ViewFormDescriptionConverter.NEW_VALUE));
        sourceRefWidget.getBody().add(setSourceRefWidget);

        ReferenceWidgetDescription targetRefWidget = ReferenceFactory.eINSTANCE.createReferenceWidgetDescription();
        targetRefWidget.setName("ExtraTargetWidget");
        targetRefWidget.setLabelExpression("Target");
        targetRefWidget.setReferenceNameExpression(SysmlPackage.eINSTANCE.getTransitionUsage_Target().getName());
        targetRefWidget.setReferenceOwnerExpression(AQLConstants.AQL_SELF);
        targetRefWidget.setIsEnabledExpression(AQL_NOT_SELF_IS_READ_ONLY);
        ChangeContext setTargetRefWidget = ViewFactory.eINSTANCE.createChangeContext();
        setTargetRefWidget.setExpression(ServiceMethod.of1(DetailsViewService::setTransitionTargetParameter).aqlSelf(ViewFormDescriptionConverter.NEW_VALUE));
        targetRefWidget.getBody().add(setTargetRefWidget);

        group.getChildren().add(sourceRefWidget);
        group.getChildren().add(targetRefWidget);

        return group;
    }

    private FormElementFor createCoreWidgets() {
        FormElementFor forElt = FormFactory.eINSTANCE.createFormElementFor();
        forElt.setName("Widgets for Core Group");
        forElt.setIterator(E_STRUCTURAL_FEATURE);
        forElt.setIterableExpression(ServiceMethod.of0(DetailsViewService::getCoreFeatures).aqlSelf());
        forElt.getChildren().addAll(this.createWidgets());
        return forElt;
    }

    private FormElementFor createAdvancedWidgets() {
        FormElementFor forElt = FormFactory.eINSTANCE.createFormElementFor();
        forElt.setName("Widgets for Advanced Group");
        forElt.setIterator(E_STRUCTURAL_FEATURE);
        forElt.setIterableExpression(ServiceMethod.of0(DetailsViewService::getAdvancedFeatures).aqlSelf());
        forElt.getChildren().addAll(this.createWidgets());
        return forElt;
    }

    private List<FormElementIf> createWidgets() {
        List<FormElementIf> widgets = new ArrayList<>();

        FormElementIf label = FormFactory.eINSTANCE.createFormElementIf();
        label.setName("Read-only String Attributes");
        label.setPredicateExpression(ServiceMethod.of1(DetailsViewService::isReadOnlyStringAttribute).aqlSelf(E_STRUCTURAL_FEATURE));
        label.getChildren().add(this.createLabelWidget());
        widgets.add(label);

        FormElementIf textfield = FormFactory.eINSTANCE.createFormElementIf();
        textfield.setName("String Attributes");
        textfield.setPredicateExpression(ServiceMethod.of1(DetailsViewService::isStringAttribute).aqlSelf(E_STRUCTURAL_FEATURE));
        textfield.getChildren().add(this.createTextfieldWidget());
        widgets.add(textfield);

        FormElementIf textArea = FormFactory.eINSTANCE.createFormElementIf();
        textArea.setName("Multiline String Attributes");
        textArea.setPredicateExpression(ServiceMethod.of1(DetailsViewService::isMultilineStringAttribute).aqlSelf(E_STRUCTURAL_FEATURE));
        textArea.getChildren().add(this.createTextAreaFieldWidget());
        widgets.add(textArea);

        FormElementIf checkbox = FormFactory.eINSTANCE.createFormElementIf();
        checkbox.setName("Boolean Attributes");
        checkbox.setPredicateExpression(ServiceMethod.of0(DetailsViewService::isBooleanAttribute).aql(E_STRUCTURAL_FEATURE));
        checkbox.getChildren().add(this.createCheckboxWidget());
        widgets.add(checkbox);

        FormElementIf radio = FormFactory.eINSTANCE.createFormElementIf();
        radio.setName("Radio Attributes");
        radio.setPredicateExpression(ServiceMethod.of0(DetailsViewService::isEnumAttribute).aql(E_STRUCTURAL_FEATURE));
        radio.getChildren().add(this.createRadioWidget());
        widgets.add(radio);

        FormElementIf refWidget = FormFactory.eINSTANCE.createFormElementIf();
        refWidget.setName("ReferenceWidget References");
        refWidget.setPredicateExpression(ServiceMethod.of0(DetailsViewService::isReference).aql(E_STRUCTURAL_FEATURE));
        refWidget.getChildren().add(this.createReferenceWidget());
        widgets.add(refWidget);

        FormElementIf number = FormFactory.eINSTANCE.createFormElementIf();
        number.setName("Number Attributes");
        number.setPredicateExpression(ServiceMethod.of0(DetailsViewService::isNumberAttribute).aql(E_STRUCTURAL_FEATURE));
        number.getChildren().add(this.createTextfieldWidget());
        widgets.add(number);

        return widgets;
    }

    private WidgetDescription createLabelWidget() {
        LabelDescription label = FormFactory.eINSTANCE.createLabelDescription();
        label.setName("LabelWidget");
        label.setLabelExpression(ServiceMethod.of1(DetailsViewService::getDetailsViewLabel).aqlSelf(E_STRUCTURAL_FEATURE));
        label.setHelpExpression(ServiceMethod.of1(DetailsViewService::getDetailsViewHelpText).aqlSelf(E_STRUCTURAL_FEATURE));
        label.setValueExpression(ServiceMethod.of1(EObjectServices.class, EObjectServices::eGet, EObject.class, EStructuralFeature.class).aqlSelf(E_STRUCTURAL_FEATURE));
        return label;
    }

    private WidgetDescription createTextAreaFieldWidget() {
        TextAreaDescription textArea = FormFactory.eINSTANCE.createTextAreaDescription();
        textArea.setName("TextAreaWidget");
        textArea.setLabelExpression(ServiceMethod.of1(DetailsViewService::getDetailsViewLabel).aqlSelf(E_STRUCTURAL_FEATURE));
        textArea.setHelpExpression(ServiceMethod.of1(DetailsViewService::getDetailsViewHelpText).aqlSelf(E_STRUCTURAL_FEATURE));
        textArea.setValueExpression(ServiceMethod.of1(EObjectServices.class, EObjectServices::eGet, EObject.class, EStructuralFeature.class).aqlSelf(E_STRUCTURAL_FEATURE));
        textArea.setIsEnabledExpression(AQL_NOT_SELF_IS_READ_ONLY_E_STRUCTURAL_FEATURE);
        ChangeContext setNewValueOperation = ViewFactory.eINSTANCE.createChangeContext();
        setNewValueOperation.setExpression(ServiceMethod.of2(DetailsViewService.class, DetailsViewService::setNewValue, Element.class, EStructuralFeature.class, Object.class)
                .aqlSelf(E_STRUCTURAL_FEATURE, ViewFormDescriptionConverter.NEW_VALUE));
        textArea.getBody().add(setNewValueOperation);
        return textArea;
    }

    private WidgetDescription createTextfieldWidget() {
        TextfieldDescription textfield = FormFactory.eINSTANCE.createTextfieldDescription();
        textfield.setName("TextfieldWidget");
        textfield.setLabelExpression(ServiceMethod.of1(DetailsViewService::getDetailsViewLabel).aqlSelf(E_STRUCTURAL_FEATURE));
        textfield.setHelpExpression(ServiceMethod.of1(DetailsViewService::getDetailsViewHelpText).aqlSelf(E_STRUCTURAL_FEATURE));
        textfield.setValueExpression(ServiceMethod.of1(EObjectServices.class, EObjectServices::eGet, EObject.class, EStructuralFeature.class).aqlSelf(E_STRUCTURAL_FEATURE));
        textfield.setIsEnabledExpression(AQL_NOT_SELF_IS_READ_ONLY_E_STRUCTURAL_FEATURE);
        ChangeContext setNewValueOperation = ViewFactory.eINSTANCE.createChangeContext();
        setNewValueOperation.setExpression(ServiceMethod.of2(DetailsViewService.class, DetailsViewService::setNewValue, Element.class, EStructuralFeature.class, Object.class)
                .aqlSelf(E_STRUCTURAL_FEATURE, ViewFormDescriptionConverter.NEW_VALUE));
        textfield.getBody().add(setNewValueOperation);
        return textfield;
    }

    private WidgetDescription createCheckboxWidget() {
        CheckboxDescription checkbox = FormFactory.eINSTANCE.createCheckboxDescription();
        checkbox.setName("CheckboxWidget");
        checkbox.setLabelExpression(ServiceMethod.of1(DetailsViewService::getDetailsViewLabel).aqlSelf(E_STRUCTURAL_FEATURE));
        checkbox.setValueExpression(ServiceMethod.of1(EObjectServices.class, EObjectServices::eGet, EObject.class, EStructuralFeature.class).aqlSelf(E_STRUCTURAL_FEATURE));
        checkbox.setHelpExpression(ServiceMethod.of1(DetailsViewService::getDetailsViewHelpText).aqlSelf(E_STRUCTURAL_FEATURE));
        checkbox.setIsEnabledExpression(AQL_NOT_SELF_IS_READ_ONLY_E_STRUCTURAL_FEATURE);
        ChangeContext setNewValueOperation = ViewFactory.eINSTANCE.createChangeContext();
        setNewValueOperation.setExpression(ServiceMethod.of2(DetailsViewService.class, DetailsViewService::setNewValue, Element.class, EStructuralFeature.class, Object.class)
                .aqlSelf(E_STRUCTURAL_FEATURE, ViewFormDescriptionConverter.NEW_VALUE));
        checkbox.getBody().add(setNewValueOperation);
        return checkbox;
    }

    private WidgetDescription createRadioWidget() {
        RadioDescription radio = FormFactory.eINSTANCE.createRadioDescription();
        radio.setName("RadioWidget");
        radio.setLabelExpression(ServiceMethod.of1(DetailsViewService::getDetailsViewLabel).aqlSelf(E_STRUCTURAL_FEATURE));
        radio.setHelpExpression(ServiceMethod.of1(DetailsViewService::getDetailsViewHelpText).aqlSelf(E_STRUCTURAL_FEATURE));
        radio.setCandidatesExpression(ServiceMethod.of1(DetailsViewService.class, DetailsViewService::getEnumCandidates, Element.class, EAttribute.class).aqlSelf(E_STRUCTURAL_FEATURE));
        radio.setCandidateLabelExpression("aql:candidate.name");
        radio.setValueExpression(ServiceMethod.of1(DetailsViewService.class, DetailsViewService::getEnumValue, Element.class, EAttribute.class).aqlSelf(E_STRUCTURAL_FEATURE));
        radio.setIsEnabledExpression(AQL_NOT_SELF_IS_READ_ONLY_E_STRUCTURAL_FEATURE);
        ChangeContext setNewValueOperation = ViewFactory.eINSTANCE.createChangeContext();
        setNewValueOperation.setExpression(
                ServiceMethod.of2(DetailsViewService.class, DetailsViewService::setNewValue, Element.class, EStructuralFeature.class, Object.class).aqlSelf(E_STRUCTURAL_FEATURE, "newValue.instance"));
        radio.getBody().add(setNewValueOperation);
        return radio;
    }

    private WidgetDescription createReferenceWidget() {
        ReferenceWidgetDescription refWidget = ReferenceFactory.eINSTANCE.createReferenceWidgetDescription();
        refWidget.setName("ReferenceWidget");
        refWidget.setLabelExpression(ServiceMethod.of1(DetailsViewService::getDetailsViewLabel).aqlSelf(E_STRUCTURAL_FEATURE));
        refWidget.setHelpExpression(ServiceMethod.of1(DetailsViewService::getDetailsViewHelpText).aqlSelf(E_STRUCTURAL_FEATURE));
        refWidget.setReferenceNameExpression(AQLConstants.AQL + E_STRUCTURAL_FEATURE + ".name");
        refWidget.setReferenceOwnerExpression(AQLConstants.AQL_SELF);
        refWidget.setIsEnabledExpression(AQL_NOT_SELF_IS_READ_ONLY_E_STRUCTURAL_FEATURE);
        ChangeContext setRefWidget = ViewFactory.eINSTANCE.createChangeContext();
        setRefWidget.setExpression(ServiceMethod.of2(DetailsViewService::handleReferenceWidgetNewValue).aqlSelf(E_STRUCTURAL_FEATURE + ".name", ViewFormDescriptionConverter.NEW_VALUE));
        refWidget.getBody().add(setRefWidget);
        return refWidget;
    }

    private FormElementDescription createDocumentationWidget() {
        TextAreaDescription textarea = FormFactory.eINSTANCE.createTextAreaDescription();
        textarea.setName("DocumentationWidget");
        textarea.setLabelExpression("文档");
        textarea.setValueExpression(ServiceMethod.of0(DetailsViewService::getDocumentation).aqlSelf());
        textarea.setHelpExpression("使用 Shift+Enter 添加新行");
        textarea.setIsEnabledExpression("aql:not(self.isReadOnly())");
        ChangeContext setNewValueOperation = ViewFactory.eINSTANCE.createChangeContext();
        setNewValueOperation.setExpression(ServiceMethod.of1(DetailsViewService::setNewDocumentationValue).aqlSelf(ViewFormDescriptionConverter.NEW_VALUE));
        textarea.getBody().add(setNewValueOperation);
        FormElementIf precondition = FormFactory.eINSTANCE.createFormElementIf();
        precondition.getChildren().add(textarea);
        precondition.setName("DocumentationWidget_Precondition");
        precondition.setPredicateExpression("aql:not(self.oclIsKindOf(sysml::AnnotatingElement))");
        return precondition;
    }

    private FormElementDescription createCommentWidget() {
        TextAreaDescription textarea = FormFactory.eINSTANCE.createTextAreaDescription();
        textarea.setName("CommentWidget");
        textarea.setLabelExpression("注释");
        textarea.setValueExpression(ServiceMethod.of0(DetailsViewService::getCommentBody).aqlSelf());
        textarea.setHelpExpression("Use 'shift + enter' to add new lines");
        textarea.setIsEnabledExpression("aql:not(self.isReadOnly())");
        ChangeContext setNewValueOperation = ViewFactory.eINSTANCE.createChangeContext();
        setNewValueOperation.setExpression(ServiceMethod.of1(DetailsViewService::setNewCommentValue).aqlSelf(ViewFormDescriptionConverter.NEW_VALUE));
        textarea.getBody().add(setNewValueOperation);
        FormElementIf precondition = FormFactory.eINSTANCE.createFormElementIf();
        precondition.getChildren().add(textarea);
        precondition.setName("CommentWidget_Precondition");
        precondition.setPredicateExpression("aql:not(self.oclIsKindOf(sysml::AnnotatingElement))");
        return precondition;
    }
}
