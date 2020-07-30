package io.github.hydos.legacylime.impl.vulkan.ui;

import io.github.hydos.legacylime.core.ui.GuiElement;
import io.github.hydos.legacylime.impl.vulkan.lowlevel.VKBufferUtils;
import io.github.hydos.legacylime.impl.vulkan.model.SimpleVKBufferMesh;
import io.github.hydos.legacylime.impl.vulkan.model.VKBufferMesh;
import io.github.hydos.legacylime.impl.vulkan.model.VKModelLoader;
import io.github.hydos.legacylime.impl.vulkan.model.VKVertex;
import io.github.hydos.legacylime.impl.vulkan.texture.CompiledTexture;
import org.joml.Vector2f;

public class VulkanGuiElement extends GuiElement {

    private static int lastRegistedId = 0;
    public CompiledTexture texture;
    SimpleVKBufferMesh processedMesh;

    public VulkanGuiElement(Vector2f positon, Vector2f scale, CompiledTexture texture) {
        this.position = positon;
        this.scale = scale;
        this.texture = texture;
        this.id = lastRegistedId;
        VulkanGuiElement.lastRegistedId++;
        setupMesh();
    }

    private void setupMesh() {
        VKModelLoader.SimpleVKMesh mesh = getSimpleMesh();
        processedMesh = new VKBufferMesh();
        int vertexCount = mesh.positions.size();

        processedMesh.vertices = new VKVertex[vertexCount];

        Vector2f color = new Vector2f(1.0f, 1.0f);

        for (int i = 0; i < vertexCount; i++) {
            processedMesh.vertices[i] = new VKVertex(
                    mesh.positions.get(i),
                    color,
                    mesh.texCoords.get(i)
            );
        }

        VKBufferUtils.createVertexBuffer2D(processedMesh);

    }

    private VKModelLoader.SimpleVKMesh getSimpleMesh() {
        VKModelLoader.SimpleVKMesh simpleVKMesh = new VKModelLoader.SimpleVKMesh();
        simpleVKMesh.positions.add(new Vector2f(-1, 1));
        simpleVKMesh.positions.add(new Vector2f(-1, -1));
        simpleVKMesh.positions.add(new Vector2f(1, 1));
        simpleVKMesh.positions.add(new Vector2f(1, -1));

        simpleVKMesh.texCoords.add(new Vector2f(-1, 1));
        simpleVKMesh.texCoords.add(new Vector2f(-1, -1));
        simpleVKMesh.texCoords.add(new Vector2f(1, 1));
        simpleVKMesh.texCoords.add(new Vector2f(1, -1));
        return simpleVKMesh;
    }

    @Override
    public SimpleVKBufferMesh getQuad() {
        return processedMesh;
    }
}
