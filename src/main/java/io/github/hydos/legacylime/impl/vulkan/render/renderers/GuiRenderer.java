package io.github.hydos.legacylime.impl.vulkan.render.renderers;

import io.github.hydos.lime.resource.ResourceHandler;
import io.github.hydos.legacylime.core.render.Renderer;
import io.github.hydos.legacylime.core.ui.GuiElement;
import io.github.hydos.legacylime.core.ui.GuiManager;
import io.github.hydos.legacylime.impl.vulkan.Variables;
import io.github.hydos.legacylime.impl.vulkan.elements.TexturedVulkanRenderObject;
import io.github.hydos.legacylime.impl.vulkan.model.SimpleVKBufferMesh;
import io.github.hydos.legacylime.impl.vulkan.render.pipelines.VKPipelineManager;
import io.github.hydos.legacylime.impl.vulkan.ubo.DescriptorManager;
import io.github.hydos.legacylime.impl.vulkan.ui.VulkanGuiElement;
import io.github.hydos.lime.resource.Identifier;
import io.github.hydos.lime.resource.Resource;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.io.IOException;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.vulkan.VK10.*;

public class GuiRenderer extends Renderer {
    public List<TexturedVulkanRenderObject> elements;//TODO: batch rendering for ui's with the same texture

    public long graphicsPipeline;

    public GuiRenderer() {
        elements = new ArrayList<>();
    }

    @Override
    public void VKRender(MemoryStack stack, VkCommandBuffer commandBuffer, int index) {
        for (GuiElement element : GuiManager.elements) {
            VulkanGuiElement vkElement = (VulkanGuiElement) element;
            SimpleVKBufferMesh mesh = element.getQuad();

            LongBuffer vertexBuffers = stack.longs(mesh.vertexBuffer);
            LongBuffer offsets = stack.longs(0);
            vkCmdBindVertexBuffers(commandBuffer, 0, vertexBuffers, offsets);

            int[] pushConstants = new int[2];
            pushConstants[0] = element.id;

            VK10.vkCmdBindDescriptorSets(commandBuffer, VK_PIPELINE_BIND_POINT_GRAPHICS,
                    Variables.pipelineLayout,
                    0, stack.longs(
                            DescriptorManager.descriptorSetsMap.get(vkElement.texture).get(index)
                    ),
                    null);
            vkCmdPushConstants(commandBuffer, Variables.pipelineLayout, VK_SHADER_STAGE_VERTEX_BIT, 0, pushConstants);
            vkCmdDraw(commandBuffer, mesh.vertices.length, 1, 0, 0);
        }

    }

    @Override
    public void createShader() {
        Resource vertexShader = ResourceHandler.GLOBAL_RESOURCE_MANAGER.getResource(new Identifier("example", "shaders/gui.vert")).get();
        Resource fragmentShader = ResourceHandler.GLOBAL_RESOURCE_MANAGER.getResource(new Identifier("example", "shaders/gui.frag")).get();

        try {
            graphicsPipeline = VKPipelineManager.createGraphicsPipeline(vertexShader, fragmentShader, true);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
