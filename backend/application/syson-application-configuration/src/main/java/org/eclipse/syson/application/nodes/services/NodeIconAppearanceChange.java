package org.eclipse.syson.application.nodes.services;

import java.util.Objects;

import org.eclipse.sirius.components.diagrams.events.appearance.IAppearanceChange;

public record NodeIconAppearanceChange(String nodeId, String iconId) implements IAppearanceChange {
    public NodeIconAppearanceChange {
        Objects.requireNonNull(nodeId);
        Objects.requireNonNull(iconId);
    }
}
