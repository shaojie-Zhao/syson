package org.eclipse.syson.application.nodes;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import org.eclipse.sirius.components.diagrams.INodeStyle;
import org.eclipse.sirius.components.diagrams.LineStyle;

public class Ov1NodeStyleDeserializer extends StdDeserializer<Ov1NodeStyle> {
    public Ov1NodeStyleDeserializer() { super(Ov1NodeStyle.class); }

    @Override
    public Ov1NodeStyle deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        var builder = Ov1NodeStyle.newOv1NodeStyle();
        if (node.has("background")) builder.background(node.get("background").asText());
        if (node.has("borderColor")) builder.borderColor(node.get("borderColor").asText());
        if (node.has("borderSize")) builder.borderSize(node.get("borderSize").asInt());
        if (node.has("borderStyle")) builder.borderStyle(LineStyle.valueOf(node.get("borderStyle").asText()));
        if (node.has("iconId") && !node.get("iconId").isNull()) builder.iconId(node.get("iconId").asText());
        return builder.build();
    }
}
