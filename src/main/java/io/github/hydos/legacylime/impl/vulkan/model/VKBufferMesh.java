package io.github.hydos.legacylime.impl.vulkan.model;

import io.github.hydos.legacylime.impl.vulkan.Variables;

import static org.lwjgl.vulkan.VK10.vkDestroyBuffer;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;

public class VKBufferMesh extends SimpleVKBufferMesh {

    public long vertexBuffer;
    public long indexBuffer;
    public VKModelLoader.VKMesh vkMesh;
    public int[] indices;
    public VKVertex[] vertices;
    public long vertexBufferMemory;
    public long indexBufferMemory;

    public void cleanup() {
        vkDestroyBuffer(Variables.device, indexBuffer, null);
        vkFreeMemory(Variables.device, indexBufferMemory, null);

        vkDestroyBuffer(Variables.device, vertexBuffer, null);
        vkFreeMemory(Variables.device, vertexBufferMemory, null);
    }
}


