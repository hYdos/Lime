package io.github.hydos.legacylime.core.ui;

import io.github.hydos.legacylime.impl.vulkan.texture.VKTextureManager;
import io.github.hydos.legacylime.impl.vulkan.ui.VulkanGuiElement;
import io.github.hydos.lime.resource.Resource;
import org.joml.Vector2f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiManager {

    public static final List<GuiElement> elements = new ArrayList<>();

    public static void addElement(GuiElement element) {
        elements.add(element);
    }

    public static void removeElement(GuiElement element) {
        elements.remove(element);
    }

    public static GuiElement createUiElement(Vector2f position, Vector2f scale, Resource imagePath) throws IOException {
        return new VulkanGuiElement(position, scale, VKTextureManager.getCompiledTexture(imagePath));
    }

}
