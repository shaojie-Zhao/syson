package org.eclipse.syson.application.nodes.services;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.eclipse.sirius.components.diagrams.INodeStyle;
import org.eclipse.sirius.components.diagrams.LineStyle;
import org.eclipse.sirius.components.diagrams.components.NodeAppearance;
import org.eclipse.sirius.components.diagrams.events.appearance.INodeAppearanceChange;
import org.eclipse.sirius.components.diagrams.events.appearance.NodeBackgroundAppearanceChange;
import org.eclipse.sirius.components.diagrams.events.appearance.NodeBorderColorAppearanceChange;
import org.eclipse.sirius.components.diagrams.events.appearance.NodeBorderSizeAppearanceChange;
import org.eclipse.sirius.components.diagrams.events.appearance.NodeBorderStyleAppearanceChange;
import org.eclipse.sirius.components.diagrams.events.appearance.ResetNodeAppearanceChange;
import org.eclipse.sirius.components.diagrams.renderer.INodeAppearanceHandler;
import org.eclipse.syson.application.nodes.Ov1NodeStyle;
import org.springframework.stereotype.Service;

@Service
public class Ov1NodeAppearanceHandler implements INodeAppearanceHandler {

    public static final String BACKGROUND = "BACKGROUND";
    public static final String BORDER_COLOR = "BORDER_COLOR";
    public static final String BORDER_SIZE = "BORDER_SIZE";
    public static final String BORDER_STYLE = "BORDER_STYLE";
    public static final String ICON_ID = "ICON_ID";

    @Override
    public boolean canHandle(INodeStyle nodeStyle) {
        return nodeStyle instanceof Ov1NodeStyle;
    }

    @Override
    public NodeAppearance handle(INodeStyle providedStyle, List<INodeAppearanceChange> changes, Optional<NodeAppearance> previousNodeAppearance) {
        Set<String> customizedStyleProperties = previousNodeAppearance.map(NodeAppearance::customizedStyleProperties).orElse(new LinkedHashSet<>());
        Optional<INodeStyle> optionalPreviousNodeStyle = previousNodeAppearance.map(NodeAppearance::style);
        if (providedStyle instanceof Ov1NodeStyle ov1Style) {
            Ov1NodeStyle.Builder builder = Ov1NodeStyle.newOv1NodeStyle()
                    .background(ov1Style.getBackground()).borderColor(ov1Style.getBorderColor())
                    .borderSize(ov1Style.getBorderSize()).borderStyle(ov1Style.getBorderStyle())
                    .iconId(ov1Style.getIconId()).childrenLayoutStrategy(ov1Style.getChildrenLayoutStrategy());
            this.handleBackground(builder, changes, optionalPreviousNodeStyle, customizedStyleProperties);
            this.handleBorderColor(builder, changes, optionalPreviousNodeStyle, customizedStyleProperties);
            this.handleBorderSize(builder, changes, optionalPreviousNodeStyle, customizedStyleProperties);
            this.handleBorderStyle(builder, changes, optionalPreviousNodeStyle, customizedStyleProperties);
            this.handleIconId(builder, changes, optionalPreviousNodeStyle, customizedStyleProperties);
            return new NodeAppearance(builder.build(), customizedStyleProperties);
        }
        return new NodeAppearance(providedStyle, customizedStyleProperties);
    }

    private void handleBackground(Ov1NodeStyle.Builder builder, List<INodeAppearanceChange> changes, Optional<INodeStyle> prevStyle, Set<String> props) {
        if (handleReset(BACKGROUND, changes, props)) return;
        changes.stream().filter(NodeBackgroundAppearanceChange.class::isInstance).map(NodeBackgroundAppearanceChange.class::cast).findFirst().ifPresentOrElse(c -> { builder.background(c.background()); props.add(BACKGROUND); },
            () -> { if (props.contains(BACKGROUND) && prevStyle.isPresent() && prevStyle.get() instanceof Ov1NodeStyle s) builder.background(s.getBackground()); else props.remove(BACKGROUND); });
    }
    private void handleBorderColor(Ov1NodeStyle.Builder builder, List<INodeAppearanceChange> changes, Optional<INodeStyle> prevStyle, Set<String> props) {
        if (handleReset(BORDER_COLOR, changes, props)) return;
        changes.stream().filter(NodeBorderColorAppearanceChange.class::isInstance).map(NodeBorderColorAppearanceChange.class::cast).findFirst().ifPresentOrElse(c -> { builder.borderColor(c.borderColor()); props.add(BORDER_COLOR); },
            () -> { if (props.contains(BORDER_COLOR) && prevStyle.isPresent() && prevStyle.get() instanceof Ov1NodeStyle s) builder.borderColor(s.getBorderColor()); else props.remove(BORDER_COLOR); });
    }
    private void handleBorderSize(Ov1NodeStyle.Builder builder, List<INodeAppearanceChange> changes, Optional<INodeStyle> prevStyle, Set<String> props) {
        if (handleReset(BORDER_SIZE, changes, props)) return;
        changes.stream().filter(NodeBorderSizeAppearanceChange.class::isInstance).map(NodeBorderSizeAppearanceChange.class::cast).findFirst().ifPresentOrElse(c -> { builder.borderSize(c.borderSize()); props.add(BORDER_SIZE); },
            () -> { if (props.contains(BORDER_SIZE) && prevStyle.isPresent() && prevStyle.get() instanceof Ov1NodeStyle s) builder.borderSize(s.getBorderSize()); else props.remove(BORDER_SIZE); });
    }
    private void handleBorderStyle(Ov1NodeStyle.Builder builder, List<INodeAppearanceChange> changes, Optional<INodeStyle> prevStyle, Set<String> props) {
        if (handleReset(BORDER_STYLE, changes, props)) return;
        changes.stream().filter(NodeBorderStyleAppearanceChange.class::isInstance).map(NodeBorderStyleAppearanceChange.class::cast).findFirst().ifPresentOrElse(c -> { builder.borderStyle(c.borderStyle()); props.add(BORDER_STYLE); },
            () -> { if (props.contains(BORDER_STYLE) && prevStyle.isPresent() && prevStyle.get() instanceof Ov1NodeStyle s) builder.borderStyle(s.getBorderStyle()); else props.remove(BORDER_STYLE); });
    }
    private void handleIconId(Ov1NodeStyle.Builder builder, List<INodeAppearanceChange> changes, Optional<INodeStyle> prevStyle, Set<String> props) {
        if (handleReset(ICON_ID, changes, props)) return;
        changes.stream().filter(NodeIconAppearanceChange.class::isInstance).map(NodeIconAppearanceChange.class::cast).findFirst().ifPresentOrElse(c -> { builder.iconId(c.iconId()); props.add(ICON_ID); },
            () -> { if (props.contains(ICON_ID) && prevStyle.isPresent() && prevStyle.get() instanceof Ov1NodeStyle s) builder.iconId(s.getIconId()); else props.remove(ICON_ID); });
    }

    private boolean handleReset(String name, List<INodeAppearanceChange> changes, Set<String> props) {
        boolean reset = changes.stream().filter(ResetNodeAppearanceChange.class::isInstance).map(ResetNodeAppearanceChange.class::cast).anyMatch(r -> Objects.equals(r.propertyName(), name));
        if (reset) props.remove(name);
        return reset;
    }
}
