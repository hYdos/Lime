package io.github.hydos.legacylime.impl.vulkan.render.renderers;

import io.github.hydos.lime.resource.ResourceHandler;
import io.github.hydos.legacylime.core.render.RenderObject;
import io.github.hydos.legacylime.core.render.Renderer;
import io.github.hydos.legacylime.impl.vulkan.Variables;
import io.github.hydos.legacylime.impl.vulkan.elements.TexturedVulkanRenderObject;
import io.github.hydos.legacylime.impl.vulkan.lowlevel.VKBufferUtils;
import io.github.hydos.legacylime.impl.vulkan.model.VKBufferMesh;
import io.github.hydos.legacylime.impl.vulkan.model.VKModelLoader;
import io.github.hydos.legacylime.impl.vulkan.model.VKVertex;
import io.github.hydos.legacylime.impl.vulkan.render.pipelines.VKPipelineManager;
import io.github.hydos.legacylime.impl.vulkan.ubo.DescriptorManager;
import io.github.hydos.lime.resource.Identifier;
import io.github.hydos.lime.resource.Resource;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.io.IOException;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class EntityRenderer extends Renderer {

    public List<TexturedVulkanRenderObject> entities;//TODO: batch rendering

    public long graphicsPipeline;

    public EntityRenderer() {
        entities = new ArrayList<>();
    }

    public void processEntity(RenderObject entity) {
        TexturedVulkanRenderObject vulkanEntity = (TexturedVulkanRenderObject) entity;
        VKModelLoader.VKMesh mesh = vulkanEntity.getRawModel();
        VKBufferMesh processedMesh = new VKBufferMesh();
        processedMesh.vkMesh = mesh;
        int vertexCount = mesh.positions.size();

        processedMesh.vertices = new VKVertex[vertexCount];

        Vector3f color = new Vector3f(1.0f, 1.0f, 1.0f);

        for (int i = 0; i < vertexCount; i++) {
            processedMesh.vertices[i] = new VKVertex(
                    mesh.positions.get(i),
                    color,
                    mesh.texCoords.get(i)
            );
        }

        processedMesh.indices = new int[mesh.indices.size()];

        for (int i = 0; i < processedMesh.indices.length; i++) {
            processedMesh.indices[i] = mesh.indices.get(i);
        }

        VKBufferUtils.createVertexBuffer(processedMesh);
        VKBufferUtils.createIndexBuffer(processedMesh);
        vulkanEntity.setModel(processedMesh);
        entities.add(vulkanEntity);
    }

    @Override
    public void VKRender(MemoryStack stack, VkCommandBuffer commandBuffer, int index) {
        vkCmdBindPipeline(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS, graphicsPipeline);

        for (TexturedVulkanRenderObject entity : entities) {
            VKBufferMesh mesh = entity.getModel();

            LongBuffer vertexBuffers = stack.longs(mesh.vertexBuffer);
            LongBuffer offsets = stack.longs(0);
            vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);

            vkCmdBindIndexBuffer(commandBuffer, mesh.indexBuffer, 0, VK_INDEX_TYPE_UINT32);

            int[] pushConstants = new int[2];
            pushConstants[0] = entity.id;

            VK10.vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                    Variables.pipelineLayout,
                    0, stack.longs(
                            DescriptorManager.descriptorSetsMap.get(entity.texture).get(index)
                    ),
                    null);
            vkCmdPushConstants(commandBuffer, Variables.pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstants);
            vkCmdDrawIndexed(commandBuffer, mesh.vkMesh.indices.size(), 1, 0, 0, 0);
        }
    }

    @Override
    public void createShader() {
        Resource vertexShader = ResourceHandler.GLOBAL_RESOURCE_MANAGER.getResource(new Identifier("example", "shaders/entity.vert")).get();
        Resource fragmentShader = ResourceHandler.GLOBAL_RESOURCE_MANAGER.getResource(new Identifier("example", "shaders/entity.frag")).get();

        try {
            graphicsPipeline = VKPipelineManager.createGraphicsPipeline(vertexShader, fragmentShader, false);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
