/**
 * Copyright (c) 2023, 2025 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 */
package org.eclipse.syson.sysmlcustomnodes.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EFactoryImpl;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import org.eclipse.syson.sysmlcustomnodes.SysMLCustomnodesFactory;
import org.eclipse.syson.sysmlcustomnodes.SysMLCustomnodesPackage;
import org.eclipse.syson.sysmlcustomnodes.SysMLImportedPackageNodeStyleDescription;
import org.eclipse.syson.sysmlcustomnodes.SysMLNoteNodeStyleDescription;
import org.eclipse.syson.sysmlcustomnodes.SysMLPackageNodeStyleDescription;
import org.eclipse.syson.sysmlcustomnodes.SysMLViewFrameNodeStyleDescription;
import org.eclipse.syson.sysmlcustomnodes.DodafOperationalNodeStyleDescription;
import org.eclipse.syson.sysmlcustomnodes.DodafSystemNodeStyleDescription;
import org.eclipse.syson.sysmlcustomnodes.DodafCapabilityStyleDescription;
import org.eclipse.syson.sysmlcustomnodes.DodafOrganizationStyleDescription;
import org.eclipse.syson.sysmlcustomnodes.DodafInformationExchangeStyleDescription;
import org.eclipse.syson.sysmlcustomnodes.Ov1NodeStyleDescription;

/**
 * <!-- begin-user-doc --> An implementation of the model <b>Factory</b>. <!-- end-user-doc -->
 *
 * @generated
 */
public class SysMLCustomnodesFactoryImpl extends EFactoryImpl implements SysMLCustomnodesFactory {
    /**
     * Creates the default factory implementation. <!-- begin-user-doc --> <!-- end-user-doc -->
     *
     * @generated
     */
    public static SysMLCustomnodesFactory init() {
        try {
            SysMLCustomnodesFactory theSysMLCustomnodesFactory = (SysMLCustomnodesFactory) EPackage.Registry.INSTANCE.getEFactory(SysMLCustomnodesPackage.eNS_URI);
            if (theSysMLCustomnodesFactory != null) {
                return theSysMLCustomnodesFactory;
            }
        } catch (Exception exception) {
            EcorePlugin.INSTANCE.log(exception);
        }
        return new SysMLCustomnodesFactoryImpl();
    }

    /**
     * Creates an instance of the factory. <!-- begin-user-doc --> <!-- end-user-doc -->
     *
     * @generated
     */
    public SysMLCustomnodesFactoryImpl() {
        super();
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     *
     * @generated
     */
    @Override
    public EObject create(EClass eClass) {
        switch (eClass.getClassifierID()) {
            case SysMLCustomnodesPackage.SYS_ML_PACKAGE_NODE_STYLE_DESCRIPTION:
                return this.createSysMLPackageNodeStyleDescription();
            case SysMLCustomnodesPackage.SYS_ML_NOTE_NODE_STYLE_DESCRIPTION:
                return this.createSysMLNoteNodeStyleDescription();
            case SysMLCustomnodesPackage.SYS_ML_IMPORTED_PACKAGE_NODE_STYLE_DESCRIPTION:
                return this.createSysMLImportedPackageNodeStyleDescription();
            case SysMLCustomnodesPackage.SYS_ML_VIEW_FRAME_NODE_STYLE_DESCRIPTION:
                return this.createSysMLViewFrameNodeStyleDescription();
            case 4: return this.createDodafOperationalNodeStyleDescription();
            case 5: return this.createDodafSystemNodeStyleDescription();
            case 6: return this.createDodafCapabilityStyleDescription();
            case 7: return this.createDodafOrganizationStyleDescription();
            case 8: return this.createDodafInformationExchangeStyleDescription();
            case 9: return this.createOv1NodeStyleDescription();
            default:
                throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
        }
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     *
     * @generated
     */
    @Override
    public SysMLPackageNodeStyleDescription createSysMLPackageNodeStyleDescription() {
        SysMLPackageNodeStyleDescriptionImpl sysMLPackageNodeStyleDescription = new SysMLPackageNodeStyleDescriptionImpl();
        return sysMLPackageNodeStyleDescription;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     *
     * @generated
     */
    @Override
    public SysMLNoteNodeStyleDescription createSysMLNoteNodeStyleDescription() {
        SysMLNoteNodeStyleDescriptionImpl sysMLNoteNodeStyleDescription = new SysMLNoteNodeStyleDescriptionImpl();
        return sysMLNoteNodeStyleDescription;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     *
     * @generated
     */
    @Override
    public SysMLImportedPackageNodeStyleDescription createSysMLImportedPackageNodeStyleDescription() {
        SysMLImportedPackageNodeStyleDescriptionImpl sysMLImportedPackageNodeStyleDescription = new SysMLImportedPackageNodeStyleDescriptionImpl();
        return sysMLImportedPackageNodeStyleDescription;
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     *
     * @generated
     */
    @Override
    public SysMLViewFrameNodeStyleDescription createSysMLViewFrameNodeStyleDescription() {
        SysMLViewFrameNodeStyleDescriptionImpl sysMLViewFrameNodeStyleDescription = new SysMLViewFrameNodeStyleDescriptionImpl();
        return sysMLViewFrameNodeStyleDescription;
    }
    @Override public DodafOperationalNodeStyleDescription createDodafOperationalNodeStyleDescription() { return new DodafOperationalNodeStyleDescriptionImpl(); }
    @Override public DodafSystemNodeStyleDescription createDodafSystemNodeStyleDescription() { return new DodafSystemNodeStyleDescriptionImpl(); }
    @Override public DodafCapabilityStyleDescription createDodafCapabilityStyleDescription() { return new DodafCapabilityStyleDescriptionImpl(); }
    @Override public DodafOrganizationStyleDescription createDodafOrganizationStyleDescription() { return new DodafOrganizationStyleDescriptionImpl(); }
    @Override public DodafInformationExchangeStyleDescription createDodafInformationExchangeStyleDescription() { return new DodafInformationExchangeStyleDescriptionImpl(); }
    @Override public Ov1NodeStyleDescription createOv1NodeStyleDescription() { return new Ov1NodeStyleDescriptionImpl(); }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     *
     * @generated
     */
    @Override
    public SysMLCustomnodesPackage getSysMLCustomnodesPackage() {
        return (SysMLCustomnodesPackage) this.getEPackage();
    }

    /**
     * <!-- begin-user-doc --> <!-- end-user-doc -->
     *
     * @deprecated
     * @generated
     */
    @Deprecated
    public static SysMLCustomnodesPackage getPackage() {
        return SysMLCustomnodesPackage.eINSTANCE;
    }

} // SysMLCustomnodesFactoryImpl
