package io.github.hydos.legacylime.impl.vulkan.model;

import io.github.hydos.lime.other.Window;
import io.github.hydos.legacylime.impl.vulkan.Variables;
import io.github.hydos.lime.other.VulkanError;
import io.github.hydos.legacylime.impl.vulkan.device.DeviceManager;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class CommandBufferManager {

    public static void createCommandPool() {
        try (MemoryStack stack = stackPush()) {
            DeviceManager.QueueFamilyIndices queueFamilyIndices = DeviceManager.findQueueFamilies(Variables.physicalDevice);

            VkCommandPoolCreateInfo poolInfo = VkCommandPoolCreateInfo.callocStack(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO);
            poolInfo.queueFamilyIndex(queueFamilyIndices.graphicsFamily);

            LongBuffer pCommandPool = stack.mallocLong(1);

            VulkanError.failIfError(vkCreateCommandPool(Variables.device, poolInfo, null, pCommandPool));

            Variables.commandPool = pCommandPool.get(0);
        }
    }

    public static void createCommandBuffers() {
        final int commandBuffersCount = Variables.swapChainFramebuffers.size();

        Variables.commandBuffers = new ArrayList<>(commandBuffersCount);

        try (MemoryStack stack = stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.commandPool(Variables.commandPool);
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandBufferCount(commandBuffersCount);

            PointerBuffer pCommandBuffers = stack.mallocPointer(commandBuffersCount);

            VulkanError.failIfError(vkAllocateCommandBuffers(Variables.device, allocInfo, pCommandBuffers));

            for (int i = 0; i < commandBuffersCount; i++) {
                Variables.commandBuffers.add(new VkCommandBuffer(pCommandBuffers.get(i), Variables.device));
            }

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);

            VkRenderPassBeginInfo renderPassInfo = VkRenderPassBeginInfo.callocStack(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO);

            renderPassInfo.renderPass(Variables.renderPass);

            VkRect2D renderArea = VkRect2D.callocStack(stack);
            renderArea.offset(VkOffset2D.callocStack(stack).set(0, 0));
            renderArea.extent(Variables.swapChainExtent);
            renderPassInfo.renderArea(renderArea);

            VkClearValue.Buffer clearValues = VkClearValue.callocStack(2, stack);
            clearValues.get(0).color().float32(stack.floats(Window.getClearColour().x / 255, Window.getClearColour().y / 255, Window.getClearColour().z / 255, 1.0f)); //The screens clear colour
            clearValues.get(1).depthStencil().set(1.0f, 0);

            renderPassInfo.pClearValues(clearValues);

            for (int i = 0; i < commandBuffersCount; i++) {
                VkCommandBuffer commandBuffer = Variables.commandBuffers.get(i);

                VulkanError.failIfError(vkBeginCommandBuffer(commandBuffer, beginInfo));
                renderPassInfo.framebuffer(Variables.swapChainFramebuffers.get(i));

                vkCmdBeginRenderPass(commandBuffer, renderPassInfo, VK_SUBPASS_CONTENTS_INLINE);
                {
                    Variables.renderManager.render(stack, commandBuffer, i);
                }
                vkCmdEndRenderPass(commandBuffer);

                VulkanError.failIfError(vkEndCommandBuffer(commandBuffer));
            }
        }
    }

    public static VkCommandBuffer beginSingleTimeCommands() {
        try (MemoryStack stack = stackPush()) {
            VkCommandBufferAllocateInfo allocInfo = VkCommandBufferAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO);
            allocInfo.level(VK_COMMAND_BUFFER_LEVEL_PRIMARY);
            allocInfo.commandPool(Variables.commandPool);
            allocInfo.commandBufferCount(1);

            PointerBuffer pCommandBuffer = stack.mallocPointer(1);
            vkAllocateCommandBuffers(Variables.device, allocInfo, pCommandBuffer);
            VkCommandBuffer commandBuffer = new VkCommandBuffer(pCommandBuffer.get(0), Variables.device);

            VkCommandBufferBeginInfo beginInfo = VkCommandBufferBeginInfo.callocStack(stack);
            beginInfo.sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            beginInfo.flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);

            vkBeginCommandBuffer(commandBuffer, beginInfo);

            return commandBuffer;
        }
    }

    public static void endSingleTimeCommands(VkCommandBuffer commandBuffer) {
        try (MemoryStack stack = stackPush()) {
            vkEndCommandBuffer(commandBuffer);

            VkSubmitInfo.Buffer submitInfo = VkSubmitInfo.callocStack(1, stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);
            submitInfo.pCommandBuffers(stack.pointers(commandBuffer));

            vkQueueSubmit(Variables.graphicsQueue, submitInfo, VK_NULL_HANDLE);
            vkQueueWaitIdle(Variables.graphicsQueue);

            vkFreeCommandBuffers(Variables.device, Variables.commandPool, commandBuffer);
        }
    }
}
