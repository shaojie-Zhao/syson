package org.eclipse.syson.application.nodes;

import java.util.Objects;
import org.eclipse.sirius.components.diagrams.ILayoutStrategy;
import org.eclipse.sirius.components.diagrams.INodeStyle;
import org.eclipse.sirius.components.diagrams.LineStyle;

public final class Ov1NodeStyle implements INodeStyle {
    private String background;
    private String borderColor;
    private int borderSize;
    private LineStyle borderStyle;
    private String iconId;
    private ILayoutStrategy childrenLayoutStrategy;

    private Ov1NodeStyle() {}

    public String getBackground() { return background; }
    public String getBorderColor() { return borderColor; }
    public int getBorderSize() { return borderSize; }
    public LineStyle getBorderStyle() { return borderStyle; }
    public String getIconId() { return iconId; }
    public ILayoutStrategy getChildrenLayoutStrategy() { return childrenLayoutStrategy; }

    public static Builder newOv1NodeStyle() { return new Builder(); }

    public static class Builder {
        private String background = "#F1F5F9";
        private String borderColor = "#3B82F6";
        private int borderSize = 2;
        private LineStyle borderStyle = LineStyle.Solid;
        private String iconId;
        private ILayoutStrategy childrenLayoutStrategy;

        public Builder background(String v) { this.background = v; return this; }
        public Builder borderColor(String v) { this.borderColor = v; return this; }
        public Builder borderSize(int v) { this.borderSize = v; return this; }
        public Builder borderStyle(LineStyle v) { this.borderStyle = v; return this; }
        public Builder iconId(String v) { this.iconId = v; return this; }
        public Builder childrenLayoutStrategy(ILayoutStrategy v) { this.childrenLayoutStrategy = v; return this; }

        public Ov1NodeStyle build() {
            Ov1NodeStyle style = new Ov1NodeStyle();
            style.background = this.background;
            style.borderColor = this.borderColor;
            style.borderSize = this.borderSize;
            style.borderStyle = this.borderStyle;
            style.iconId = this.iconId;
            style.childrenLayoutStrategy = this.childrenLayoutStrategy;
            return style;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ov1NodeStyle)) return false;
        Ov1NodeStyle that = (Ov1NodeStyle) o;
        return borderSize == that.borderSize && Objects.equals(background, that.background)
                && Objects.equals(borderColor, that.borderColor) && borderStyle == that.borderStyle
                && Objects.equals(iconId, that.iconId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(background, borderColor, borderSize, borderStyle, iconId);
    }
}
