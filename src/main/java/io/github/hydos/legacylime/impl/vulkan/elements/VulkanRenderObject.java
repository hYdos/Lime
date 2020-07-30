package io.github.hydos.legacylime.impl.vulkan.elements;

import io.github.hydos.legacylime.core.render.RenderObject;
import io.github.hydos.legacylime.impl.vulkan.model.VKBufferMesh;
import io.github.hydos.legacylime.impl.vulkan.model.VKModelLoader;

public class VulkanRenderObject extends RenderObject {

    public int id;
    public VKModelLoader.VKMesh rawModel;
    public VKBufferMesh model = null;

    public VKBufferMesh getModel() {
        return model;
    }

    public void setModel(VKBufferMesh model) {
        this.model = model;
    }

    public VKModelLoader.VKMesh getRawModel() {
        return rawModel;
    }
}
