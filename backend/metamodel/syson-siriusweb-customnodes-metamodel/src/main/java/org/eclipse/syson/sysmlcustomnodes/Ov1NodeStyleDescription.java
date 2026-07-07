package org.eclipse.syson.sysmlcustomnodes;

import org.eclipse.sirius.components.view.UserColor;
import org.eclipse.sirius.components.view.diagram.NodeStyleDescription;

public interface Ov1NodeStyleDescription extends NodeStyleDescription {
    UserColor getBackground();
    void setBackground(UserColor value);
    String getIconId();
    void setIconId(String value);
}
