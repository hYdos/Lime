package io.github.hydos.legacylime.impl.vulkan.util;

import io.github.hydos.legacylime.impl.vulkan.Variables;
import io.github.hydos.lime.other.VulkanError;
import io.github.hydos.legacylime.impl.vulkan.render.Frame;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Pointer;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDeviceProperties;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class Utils {

    public static int getMaxUsableSampleCount() {
        try (MemoryStack stack = stackPush()) {
            VkPhysicalDeviceProperties physicalDeviceProperties = VkPhysicalDeviceProperties.mallocStack(stack);
            vkGetPhysicalDeviceProperties(Variables.physicalDevice, physicalDeviceProperties);
            int sampleCountFlags = physicalDeviceProperties.limits().framebufferColorSampleCounts()
                    & physicalDeviceProperties.limits().framebufferDepthSampleCounts();

            if ((sampleCountFlags & VK_SAMPLE_COUNT_64_BIT) != 0) {
                return VK_SAMPLE_COUNT_64_BIT;
            }
            if ((sampleCountFlags & VK_SAMPLE_COUNT_32_BIT) != 0) {
                return VK_SAMPLE_COUNT_32_BIT;
            }
            if ((sampleCountFlags & VK_SAMPLE_COUNT_16_BIT) != 0) {
                return VK_SAMPLE_COUNT_16_BIT;
            }
            if ((sampleCountFlags & VK_SAMPLE_COUNT_8_BIT) != 0) {
                return VK_SAMPLE_COUNT_8_BIT;
            }
            if ((sampleCountFlags & VK_SAMPLE_COUNT_4_BIT) != 0) {
                return VK_SAMPLE_COUNT_4_BIT;
            }
            if ((sampleCountFlags & VK_SAMPLE_COUNT_2_BIT) != 0) {
                return VK_SAMPLE_COUNT_2_BIT;
            }
            return VK_SAMPLE_COUNT_1_BIT;
        }
    }

    public static void createFramebuffers() {
        Variables.swapChainFramebuffers = new ArrayList<>(Variables.swapChainImageViews.size());

        try (MemoryStack stack = stackPush()) {
            LongBuffer attachments = stack.longs(Variables.colorImageView, Variables.depthImageView, VK_NULL_HANDLE);
            LongBuffer pFramebuffer = stack.mallocLong(1);

            // Lets allocate the create info struct once and just update the pAttachments field each iteration
            VkFramebufferCreateInfo framebufferInfo = VkFramebufferCreateInfo.callocStack(stack);
            framebufferInfo.sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO);
            framebufferInfo.renderPass(Variables.renderPass);
            framebufferInfo.width(Variables.swapChainExtent.width());
            framebufferInfo.height(Variables.swapChainExtent.height());
            framebufferInfo.layers(1);

            for (long imageView : Variables.swapChainImageViews) {

                attachments.put(2, imageView);

                framebufferInfo.pAttachments(attachments);

                VulkanError.failIfError(vkCreateFramebuffer(Variables.device, framebufferInfo, null, pFramebuffer));
                Variables.swapChainFramebuffers.add(pFramebuffer.get(0));
            }
        }
    }

    public static void createSyncObjects() {
        Variables.inFlightFrames = new ArrayList<>(Frame.MAX_FRAMES_IN_FLIGHT);
        Variables.imagesInFlight = new HashMap<>(Variables.swapChainImages.size());

        try (MemoryStack stack = stackPush()) {
            VkSemaphoreCreateInfo semaphoreInfo = VkSemaphoreCreateInfo.callocStack(stack);
            semaphoreInfo.sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);

            VkFenceCreateInfo fenceInfo = VkFenceCreateInfo.callocStack(stack);
            fenceInfo.sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO);
            fenceInfo.flags(VK_FENCE_CREATE_SIGNALED_BIT);

            LongBuffer pImageAvailableSemaphore = stack.mallocLong(1);
            LongBuffer pRenderFinishedSemaphore = stack.mallocLong(1);
            LongBuffer pFence = stack.mallocLong(1);

            for (int i = 0; i < Frame.MAX_FRAMES_IN_FLIGHT; i++) {
                VulkanError.failIfError(vkCreateSemaphore(Variables.device, semaphoreInfo, null, pImageAvailableSemaphore));
                VulkanError.failIfError(vkCreateSemaphore(Variables.device, semaphoreInfo, null, pRenderFinishedSemaphore));
                VulkanError.failIfError(vkCreateFence(Variables.device, fenceInfo, null, pFence));
                Variables.inFlightFrames.add(new Frame(pImageAvailableSemaphore.get(0), pRenderFinishedSemaphore.get(0), pFence.get(0)));
            }
        }
    }

    public static PointerBuffer asPointerBuffer(Collection<String> collection) {
        MemoryStack stack = stackGet();

        PointerBuffer buffer = stack.mallocPointer(collection.size());

        collection.stream()
                .map(stack::UTF8)
                .forEach(buffer::put);

        return buffer.rewind();
    }

    public static PointerBuffer asPointerBuffer(List<? extends Pointer> list) {
        MemoryStack stack = stackGet();

        PointerBuffer buffer = stack.mallocPointer(list.size());

        list.forEach(buffer::put);

        return buffer.rewind();
    }
}
