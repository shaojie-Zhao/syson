package org.eclipse.syson.sysmlcustomnodes.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;
import org.eclipse.sirius.components.view.UserColor;
import org.eclipse.sirius.components.view.diagram.LayoutStrategyDescription;
import org.eclipse.sirius.components.view.diagram.LineStyle;
import org.eclipse.syson.sysmlcustomnodes.Ov1NodeStyleDescription;
import org.eclipse.syson.sysmlcustomnodes.SysMLCustomnodesPackage;

/**
 * Minimal EMF implementation for OV-1 Node Style Description.
 * IconId is the key new property for icon assignment.
 */
public class Ov1NodeStyleDescriptionImpl extends MinimalEObjectImpl.Container implements Ov1NodeStyleDescription {

    protected static final int BORDER_RADIUS_EDEFAULT = 3;
    protected static final int BORDER_SIZE_EDEFAULT = 1;
    protected static final LineStyle BORDER_LINE_STYLE_EDEFAULT = LineStyle.SOLID;
    protected static final String ICON_ID_EDEFAULT = null;

    protected UserColor borderColor;
    protected int borderRadius = BORDER_RADIUS_EDEFAULT;
    protected int borderSize = BORDER_SIZE_EDEFAULT;
    protected LineStyle borderLineStyle = BORDER_LINE_STYLE_EDEFAULT;
    protected LayoutStrategyDescription childrenLayoutStrategy;
    protected String description;
    protected UserColor background;
    protected String iconId = ICON_ID_EDEFAULT;

    @Override public UserColor getBorderColor() { return borderColor; }
    @Override public void setBorderColor(UserColor c) { this.borderColor = c; }
    @Override public int getBorderRadius() { return borderRadius; }
    @Override public void setBorderRadius(int r) { borderRadius = r; }
    @Override public int getBorderSize() { return borderSize; }
    @Override public void setBorderSize(int s) { borderSize = s; }
    @Override public LineStyle getBorderLineStyle() { return borderLineStyle; }
    @Override public void setBorderLineStyle(LineStyle s) { borderLineStyle = s; }
    @Override public LayoutStrategyDescription getChildrenLayoutStrategy() { return childrenLayoutStrategy; }
    @Override public void setChildrenLayoutStrategy(LayoutStrategyDescription l) { childrenLayoutStrategy = l; }
    @Override public String getDescription() { return description; }
    @Override public void setDescription(String d) { description = d; }
    @Override public UserColor getBackground() { return background; }
    @Override public void setBackground(UserColor b) { background = b; }
    @Override public String getIconId() { return iconId; }
    @Override public void setIconId(String id) { iconId = id; }

    @Override protected EClass eStaticClass() { return SysMLCustomnodesPackage.Literals.SYS_ML_PACKAGE_NODE_STYLE_DESCRIPTION; }

    @Override public Object eGet(int featureID, boolean resolve, boolean coreType) {
        switch (featureID) {
            case 0: return getBorderColor();
            case 1: return getBorderRadius();
            case 2: return getBorderSize();
            case 3: return getBorderLineStyle();
            case 4: return getChildrenLayoutStrategy();
            case 5: return getDescription();
            case 6: return getBackground();
            case 7: return getIconId();
        }
        return super.eGet(featureID, resolve, coreType);
    }

    @Override public void eSet(int featureID, Object newValue) {
        switch (featureID) {
            case 0: setBorderColor((UserColor) newValue); return;
            case 1: setBorderRadius((Integer) newValue); return;
            case 2: setBorderSize((Integer) newValue); return;
            case 3: setBorderLineStyle((LineStyle) newValue); return;
            case 4: setChildrenLayoutStrategy((LayoutStrategyDescription) newValue); return;
            case 5: setDescription((String) newValue); return;
            case 6: setBackground((UserColor) newValue); return;
            case 7: setIconId((String) newValue); return;
        }
        super.eSet(featureID, newValue);
    }
}
