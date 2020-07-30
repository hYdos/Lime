package io.github.hydos.legacylime.impl.vulkan.model;

import io.github.hydos.legacylime.impl.vulkan.Variables;

import static org.lwjgl.vulkan.VK10.vkDestroyBuffer;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;

public class SimpleVKBufferMesh {

    public long vertexBuffer;
    public VKModelLoader.VKMesh vkMesh;
    public VKVertex[] vertices;
    public long vertexBufferMemory;

    public void cleanup() {
        vkDestroyBuffer(Variables.device, vertexBuffer, null);
        vkFreeMemory(Variables.device, vertexBufferMemory, null);
    }
}