package io.github.hydos.legacylime.impl.vulkan.texture;

import io.github.hydos.lime.resource.Identifier;

public class CompiledTexture {

    public int mipLevels;
    public long textureImage;
    public long textureImageMemory;
    public long textureImageView;
    public long textureSampler;
    public Identifier path;

    public CompiledTexture(int mipLevels, long textureImage, long textureImageMemory, long textureImageView, long textureSampler) {
        this.mipLevels = mipLevels;
        this.textureImage = textureImage;
        this.textureImageMemory = textureImageMemory;
        this.textureImageView = textureImageView;
        this.textureSampler = textureSampler;
    }

    public CompiledTexture() {
    }
}
