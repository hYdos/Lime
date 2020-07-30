package io.github.hydos.legacylime.impl.vulkan.render;

import io.github.hydos.legacylime.impl.vulkan.Variables;
import io.github.hydos.legacylime.impl.vulkan.VulkanError;
import io.github.hydos.legacylime.impl.vulkan.swapchain.SwapchainManager;
import io.github.hydos.legacylime.impl.vulkan.ubo.UboManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkSubmitInfo;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

/**
 * Wraps the needed sync objects for an in flight frame
 * <p>
 * This frame's sync objects must be deleted manually
 */
public class Frame {

    public static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;
    public static final int MAX_FRAMES_IN_FLIGHT = 2;
    private final long imageAvailableSemaphore;
    private final long renderFinishedSemaphore;
    private final long fence;

    public Frame(long imageAvailableSemaphore, long renderFinishedSemaphore, long fence) {
        this.imageAvailableSemaphore = imageAvailableSemaphore;
        this.renderFinishedSemaphore = renderFinishedSemaphore;
        this.fence = fence;
    }

    public static void drawFrame() {
        try (MemoryStack stack = stackPush()) {
            Frame thisFrame = Variables.inFlightFrames.get(Variables.currentFrame);

            vkWaitForFences(Variables.device, thisFrame.pFence(), true, UINT64_MAX);

            IntBuffer pImageIndex = stack.mallocInt(1);

            int vkResult = vkAcquireNextImageKHR(Variables.device, Variables.swapChain, UINT64_MAX,
                    thisFrame.imageAvailableSemaphore(), VK_NULL_HANDLE, pImageIndex);

            if (vkResult == VK_ERROR_OUT_OF_DATE_KHR) {
                SwapchainManager.recreateSwapChain();
                return;
            }

            final int imageIndex = pImageIndex.get(0);
            Variables.currentImageIndex = imageIndex;

            if (Variables.imagesInFlight.containsKey(imageIndex)) {
                vkWaitForFences(Variables.device, Variables.imagesInFlight.get(imageIndex).fence(), true, UINT64_MAX);
                UboManager.updateUniformBuffers(Variables.currentImageIndex);
            }

            Variables.imagesInFlight.put(imageIndex, thisFrame);

            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(thisFrame.pImageAvailableSemaphore());
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));

            submitInfo.pSignalSemaphores(thisFrame.pRenderFinishedSemaphore());

            submitInfo.pCommandBuffers(stack.pointers(Variables.commandBuffers.get(imageIndex)));

            vkResetFences(Variables.device, thisFrame.pFence());

            //Draw objects because yes
            if ((vkResult = vkQueueSubmit(Variables.graphicsQueue, submitInfo, thisFrame.fence())) != VK_SUCCESS) {
                vkResetFences(Variables.device, thisFrame.pFence());
                throw new RuntimeException("Failed to submit draw command buffer: " + vkResult);
            }

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.callocStack(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);

            presentInfo.pWaitSemaphores(thisFrame.pRenderFinishedSemaphore());

            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(Variables.swapChain));

            presentInfo.pImageIndices(pImageIndex);

            vkResult = vkQueuePresentKHR(Variables.presentQueue, presentInfo);

            if (vkResult == VK_ERROR_OUT_OF_DATE_KHR || vkResult == VK_SUBOPTIMAL_KHR || Variables.framebufferResize) {
                Variables.framebufferResize = false;
                SwapchainManager.recreateSwapChain();
            } else if (vkResult != VK_SUCCESS) {
                VulkanError.failIfError(vkResult);
            }

            Variables.currentFrame = (Variables.currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;
        }
    }

    public long imageAvailableSemaphore() {
        return imageAvailableSemaphore;
    }

    public LongBuffer pImageAvailableSemaphore() {
        return stackGet().longs(imageAvailableSemaphore);
    }

    public long renderFinishedSemaphore() {
        return renderFinishedSemaphore;
    }

    public LongBuffer pRenderFinishedSemaphore() {
        return stackGet().longs(renderFinishedSemaphore);
    }

    public long fence() {
        return fence;
    }

    public LongBuffer pFence() {
        return stackGet().longs(fence);
    }
}
