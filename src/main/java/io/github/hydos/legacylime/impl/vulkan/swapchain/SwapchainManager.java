package io.github.hydos.legacylime.impl.vulkan.swapchain;

import io.github.hydos.legacylime.core.io.Window;
import io.github.hydos.legacylime.core.math.CitrusMath;
import io.github.hydos.legacylime.core.render.Renderer;
import io.github.hydos.legacylime.impl.vulkan.Variables;
import io.github.hydos.legacylime.impl.vulkan.VulkanError;
import io.github.hydos.legacylime.impl.vulkan.device.DeviceManager;
import io.github.hydos.legacylime.impl.vulkan.model.CommandBufferManager;
import io.github.hydos.legacylime.impl.vulkan.render.VKRenderManager;
import io.github.hydos.legacylime.impl.vulkan.texture.VKTextureManager;
import io.github.hydos.legacylime.impl.vulkan.ubo.DescriptorManager;
import io.github.hydos.legacylime.impl.vulkan.ubo.UboManager;
import io.github.hydos.legacylime.impl.vulkan.util.ImageUtils;
import io.github.hydos.legacylime.impl.vulkan.util.Utils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwWaitEvents;
import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

public class SwapchainManager {

    public static void cleanupSwapChain() {
        vkDestroyImageView(Variables.device, Variables.colorImageView, null);
        vkDestroyImage(Variables.device, Variables.colorImage, null);
        vkFreeMemory(Variables.device, Variables.colorImageMemory, null);
        vkDestroyImageView(Variables.device, Variables.depthImageView, null);
        vkDestroyImage(Variables.device, Variables.depthImage, null);
        vkFreeMemory(Variables.device, Variables.depthImageMemory, null);
        Variables.uniformBuffers.forEach(ubo -> vkDestroyBuffer(Variables.device, ubo, null));
        Variables.uniformBuffersMemory.forEach(uboMemory -> vkFreeMemory(Variables.device, uboMemory, null));
        vkDestroyDescriptorPool(Variables.device, DescriptorManager.descriptorPool, null);
        Variables.swapChainFramebuffers.forEach(framebuffer -> vkDestroyFramebuffer(Variables.device, framebuffer, null));
        vkFreeCommandBuffers(Variables.device, Variables.commandPool, Utils.asPointerBuffer(Variables.commandBuffers));
//        vkDestroyPipeline(Variables.device, Variables.graphicsPipeline, null); TODO: loop through renderers and clean
        vkDestroyPipelineLayout(Variables.device, Variables.pipelineLayout, null);
        vkDestroyRenderPass(Variables.device, Variables.renderPass, null);
        Variables.swapChainImageViews.forEach(imageView -> vkDestroyImageView(Variables.device, imageView, null));
        vkDestroySwapchainKHR(Variables.device, Variables.swapChain, null);
    }

    public static VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer availableFormats) {
        return availableFormats.stream()
                .filter(availableFormat -> availableFormat.format() == VK_FORMAT_B8G8R8_SRGB)
                .filter(availableFormat -> availableFormat.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                .findAny()
                .orElse(availableFormats.get(0));
    }

    public static int chooseSwapPresentMode(IntBuffer availablePresentModes) {
        for (int i = 0; i < availablePresentModes.capacity(); i++) {
            if (availablePresentModes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
                return availablePresentModes.get(i);
            }
        }

        return VK_PRESENT_MODE_FIFO_KHR;
    }

    public static VkExtent2D chooseSwapExtent(VkSurfaceCapabilitiesKHR capabilities) {
        if (capabilities.currentExtent().width() != Variables.UINT32_MAX) {
            return capabilities.currentExtent();
        }

        IntBuffer width = stackGet().ints(0);
        IntBuffer height = stackGet().ints(0);

        glfwGetFramebufferSize(Window.getWindow(), width, height);

        VkExtent2D actualExtent = VkExtent2D.mallocStack().set(width.get(0), height.get(0));

        VkExtent2D minExtent = capabilities.minImageExtent();
        VkExtent2D maxExtent = capabilities.maxImageExtent();

        actualExtent.width(CitrusMath.clamp(minExtent.width(), maxExtent.width(), actualExtent.width()));
        actualExtent.height(CitrusMath.clamp(minExtent.height(), maxExtent.height(), actualExtent.height()));

        return actualExtent;
    }

    public static SwapChainSupportDetails querySwapChainSupport(VkPhysicalDevice device, MemoryStack stack) {
        SwapChainSupportDetails details = new SwapChainSupportDetails();

        details.capabilities = VkSurfaceCapabilitiesKHR.mallocStack(stack);
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device, Variables.surface, details.capabilities);

        IntBuffer count = stack.ints(0);

        vkGetPhysicalDeviceSurfaceFormatsKHR(device, Variables.surface, count, null);

        if (count.get(0) != 0) {
            details.formats = VkSurfaceFormatKHR.mallocStack(count.get(0), stack);
            vkGetPhysicalDeviceSurfaceFormatsKHR(device, Variables.surface, count, details.formats);
        }

        vkGetPhysicalDeviceSurfacePresentModesKHR(device, Variables.surface, count, null);

        if (count.get(0) != 0) {
            details.presentModes = stack.mallocInt(count.get(0));
            vkGetPhysicalDeviceSurfacePresentModesKHR(device, Variables.surface, count, details.presentModes);
        }

        return details;
    }

    public static void recreateSwapChain() {
        try (MemoryStack stack = stackPush()) {

            IntBuffer width = stack.ints(0);
            IntBuffer height = stack.ints(0);

            while (width.get(0) == 0 && height.get(0) == 0) {
                GLFW.glfwGetFramebufferSize(Window.getWindow(), width, height);
                glfwWaitEvents();
            }
        }

        vkDeviceWaitIdle(Variables.device);

        SwapchainManager.cleanupSwapChain();

        createSwapChainObjects();
    }

    public static void createSwapChain() {
        try (MemoryStack stack = stackPush()) {

            SwapChainSupportDetails swapChainSupport = querySwapChainSupport(Variables.physicalDevice, stack);

            VkSurfaceFormatKHR surfaceFormat = chooseSwapSurfaceFormat(swapChainSupport.formats);
            int presentMode = chooseSwapPresentMode(swapChainSupport.presentModes);
            VkExtent2D extent = chooseSwapExtent(swapChainSupport.capabilities);

            IntBuffer imageCount = stack.ints(swapChainSupport.capabilities.minImageCount() + 1);

            if (swapChainSupport.capabilities.maxImageCount() > 0 && imageCount.get(0) > swapChainSupport.capabilities.maxImageCount()) {
                imageCount.put(0, swapChainSupport.capabilities.maxImageCount());
            }

            VkSwapchainCreateInfoKHR createInfo = VkSwapchainCreateInfoKHR.callocStack(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR);
            createInfo.surface(Variables.surface);

            // Image settings
            createInfo.minImageCount(imageCount.get(0));
            createInfo.imageFormat(surfaceFormat.format());
            createInfo.imageColorSpace(surfaceFormat.colorSpace());
            createInfo.imageExtent(extent);
            createInfo.imageArrayLayers(1);
            createInfo.imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

            DeviceManager.QueueFamilyIndices indices = DeviceManager.findQueueFamilies(Variables.physicalDevice);

            if (!indices.graphicsFamily.equals(indices.presentFamily)) {
                createInfo.imageSharingMode(VK_SHARING_MODE_CONCURRENT);
                createInfo.pQueueFamilyIndices(stack.ints(indices.graphicsFamily, indices.presentFamily));
            } else {
                createInfo.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }

            createInfo.preTransform(swapChainSupport.capabilities.currentTransform());
            createInfo.compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR);
            createInfo.presentMode(presentMode);
            createInfo.clipped(true);

            createInfo.oldSwapchain(VK_NULL_HANDLE);

            LongBuffer pSwapChain = stack.longs(VK_NULL_HANDLE);

            VulkanError.failIfError(vkCreateSwapchainKHR(Variables.device, createInfo, null, pSwapChain));

            Variables.swapChain = pSwapChain.get(0);

            vkGetSwapchainImagesKHR(Variables.device, Variables.swapChain, imageCount, null);

            LongBuffer pSwapchainImages = stack.mallocLong(imageCount.get(0));

            vkGetSwapchainImagesKHR(Variables.device, Variables.swapChain, imageCount, pSwapchainImages);

            Variables.swapChainImages = new ArrayList<>(imageCount.get(0));

            for (int i = 0; i < pSwapchainImages.capacity(); i++) {
                Variables.swapChainImages.add(pSwapchainImages.get(i));
            }

            Variables.swapChainImageFormat = surfaceFormat.format();
            Variables.swapChainExtent = VkExtent2D.create().set(extent);
        }
    }

    /**
     * creates objects in the swap chain such as the render pass, pipeline, resources, framebuffers, ubos, etc
     */
    public static void createSwapChainObjects() {
        createSwapChain();
        ImageUtils.createImageViews();
        VKRenderManager.createRenderPass();
        for (Renderer renderer : VKRenderManager.getInstance().renderers) {
            renderer.createShader();
        }
        ImageUtils.createColorResources();
        ImageUtils.createDepthResources();
        Utils.createFramebuffers();
        UboManager.createObjectUniformBuffers();
        DescriptorManager.createDescriptorPool();
        DescriptorManager.createDescriptorSets(VKTextureManager.compiledTextures);
        CommandBufferManager.createCommandBuffers();
    }

    public static class SwapChainSupportDetails {
        public VkSurfaceCapabilitiesKHR capabilities;
        public VkSurfaceFormatKHR.Buffer formats;
        public IntBuffer presentModes;
    }
}
