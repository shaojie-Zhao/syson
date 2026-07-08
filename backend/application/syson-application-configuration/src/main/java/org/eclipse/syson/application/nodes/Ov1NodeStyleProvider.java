package org.eclipse.syson.application.nodes;

import java.util.Optional;

import org.eclipse.sirius.components.diagrams.ILayoutStrategy;
import org.eclipse.sirius.components.diagrams.INodeStyle;
import org.eclipse.sirius.components.diagrams.LineStyle;
import org.eclipse.sirius.components.interpreter.AQLInterpreter;
import org.eclipse.sirius.components.representations.VariableManager;
import org.eclipse.sirius.components.view.FixedColor;
import org.eclipse.sirius.components.view.diagram.NodeStyleDescription;
import org.eclipse.sirius.components.view.emf.diagram.INodeStyleProvider;
import org.eclipse.syson.sysmlcustomnodes.Ov1NodeStyleDescription;
import org.springframework.stereotype.Service;

@Service
public class Ov1NodeStyleProvider implements INodeStyleProvider {

    public static final String NODE_TYPE = "customnode:dodafov1node";

    @Override
    public Optional<String> getNodeType(NodeStyleDescription nodeStyle) {
        if (nodeStyle instanceof Ov1NodeStyleDescription) {
            return Optional.of(NODE_TYPE);
        }
        return Optional.empty();
    }

    @Override
    public Optional<INodeStyle> createNodeStyle(NodeStyleDescription nodeStyle, ILayoutStrategy childrenLayoutStrategy, AQLInterpreter interpreter, VariableManager variableManager) {
        if (nodeStyle instanceof Ov1NodeStyleDescription ov1Style) {
            var builder = Ov1NodeStyle.newOv1NodeStyle()
                    .background(Optional.ofNullable(ov1Style.getBackground())
                            .filter(FixedColor.class::isInstance)
                            .map(FixedColor.class::cast)
                            .map(FixedColor::getValue)
                            .orElse("transparent"))
                    .borderColor(Optional.ofNullable(ov1Style.getBorderColor())
                            .filter(FixedColor.class::isInstance)
                            .map(FixedColor.class::cast)
                            .map(FixedColor::getValue)
                            .orElse("#3B82F6"))
                    .borderSize(ov1Style.getBorderSize())
                    .borderStyle(LineStyle.valueOf(ov1Style.getBorderLineStyle().getLiteral()))
                    .childrenLayoutStrategy(childrenLayoutStrategy);
            if (ov1Style.getIconId() != null && !ov1Style.getIconId().isBlank()) {
                builder.iconId(ov1Style.getIconId());
            }
            return Optional.of(builder.build());
        }
        return Optional.empty();
    }
}
