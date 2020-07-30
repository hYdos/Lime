package io.github.hydos.legacylime.core.ui;

import io.github.hydos.legacylime.impl.vulkan.model.SimpleVKBufferMesh;
import org.joml.Vector2f;

public abstract class GuiElement {
    public Vector2f position;
    public Vector2f scale;
    public boolean hidden;
    public int id;

    public abstract SimpleVKBufferMesh getQuad();
}
