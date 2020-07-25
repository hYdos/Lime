package io.github.hydos.lime.impl.vulkan.render;

import io.github.hydos.lime.impl.vulkan.VKVariables;
import io.github.hydos.lime.impl.vulkan.swapchain.VKSwapchainManager;
import io.github.hydos.lime.impl.vulkan.utils.VKUtils;
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

    private final long imageAvailableSemaphore;
    private final long renderFinishedSemaphore;
    private final long fence;

    public static final long UINT64_MAX = 0xFFFFFFFFFFFFFFFFL;
    public static final int MAX_FRAMES_IN_FLIGHT = 2;

    public Frame(long imageAvailableSemaphore, long renderFinishedSemaphore, long fence) {
        this.imageAvailableSemaphore = imageAvailableSemaphore;
        this.renderFinishedSemaphore = renderFinishedSemaphore;
        this.fence = fence;
    }

    public static void drawFrame() {
        try (MemoryStack stack = stackPush()) {

            Frame thisFrame = VKVariables.inFlightFrames.get(VKVariables.currentFrame);

            vkWaitForFences(VKVariables.device, thisFrame.pFence(), true, UINT64_MAX);

            IntBuffer pImageIndex = stack.mallocInt(1);

            int vkResult = vkAcquireNextImageKHR(VKVariables.device, VKVariables.swapChain, UINT64_MAX,
                    thisFrame.imageAvailableSemaphore(), VK_NULL_HANDLE, pImageIndex);

            if (vkResult == VK_ERROR_OUT_OF_DATE_KHR) {
                VKSwapchainManager.recreateSwapChain();
                return;
            }

            final int imageIndex = pImageIndex.get(0);
            VKVariables.currentImageIndex = imageIndex;
            if (VKVariables.imagesInFlight.containsKey(imageIndex)) {
                vkWaitForFences(VKVariables.device, VKVariables.imagesInFlight.get(imageIndex).fence(), true, UINT64_MAX);
                VKUtils.updateUniformBuffer(VKVariables.currentImageIndex, null);
            }

            VKVariables.imagesInFlight.put(imageIndex, thisFrame);

            VkSubmitInfo submitInfo = VkSubmitInfo.callocStack(stack);
            submitInfo.sType(VK_STRUCTURE_TYPE_SUBMIT_INFO);

            submitInfo.waitSemaphoreCount(1);
            submitInfo.pWaitSemaphores(thisFrame.pImageAvailableSemaphore());
            submitInfo.pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT));

            submitInfo.pSignalSemaphores(thisFrame.pRenderFinishedSemaphore());

            submitInfo.pCommandBuffers(stack.pointers(VKVariables.commandBuffers.get(imageIndex)));

            vkResetFences(VKVariables.device, thisFrame.pFence());

            //Draw objects because yes
            if ((vkResult = vkQueueSubmit(VKVariables.graphicsQueue, submitInfo, thisFrame.fence())) != VK_SUCCESS) {
                vkResetFences(VKVariables.device, thisFrame.pFence());
                throw new RuntimeException("Failed to submit draw command buffer: " + vkResult);
            }

            VkPresentInfoKHR presentInfo = VkPresentInfoKHR.callocStack(stack);
            presentInfo.sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR);

            presentInfo.pWaitSemaphores(thisFrame.pRenderFinishedSemaphore());

            presentInfo.swapchainCount(1);
            presentInfo.pSwapchains(stack.longs(VKVariables.swapChain));

            presentInfo.pImageIndices(pImageIndex);

            vkResult = vkQueuePresentKHR(VKVariables.presentQueue, presentInfo);

            if (vkResult == VK_ERROR_OUT_OF_DATE_KHR || vkResult == VK_SUBOPTIMAL_KHR || VKVariables.framebufferResize) {
                VKVariables.framebufferResize = false;
                VKSwapchainManager.recreateSwapChain();
            } else if (vkResult != VK_SUCCESS) {
                throw new RuntimeException("Failed to present swap chain image");
            }

            VKVariables.currentFrame = (VKVariables.currentFrame + 1) % MAX_FRAMES_IN_FLIGHT;
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