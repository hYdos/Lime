package io.github.hydos.legacylime.core.render;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkCommandBuffer;

public abstract class Renderer {

    public abstract void VKRender(MemoryStack stack, VkCommandBuffer commandBuffer, int index);

    public abstract void createShader();
}