package io.github.hydos.legacylime.impl.vulkan.elements;

import io.github.hydos.legacylime.impl.vulkan.texture.CompiledTexture;

public class TexturedVulkanRenderObject extends VulkanRenderObject {

    public CompiledTexture texture;

    public TexturedVulkanRenderObject(VulkanRenderObject object) {
        this.position = object.position;
        this.rotX = object.rotX;
        this.rotY = object.rotY;
        this.rotZ = object.rotZ;
        this.scale = object.scale;
        this.model = object.model;
        this.rawModel = object.rawModel;
        this.id = object.id;
    }

    public TexturedVulkanRenderObject(VulkanRenderObject object, CompiledTexture compiledTexture) {
        this(object);
        this.texture = compiledTexture;
    }
}
