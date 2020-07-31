package io.github.hydos.lime;

import io.github.hydos.lime.other.MathHelper;
import io.github.hydos.lime.other.VulkanError;
import io.github.hydos.lime.other.Window;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * limes vulkan swapchain object
 */
public class VulkanSwapchain {

    private final LimeManager lime;

    private long swapChain;
    private List<Long> swapChainImages;
    private int swapChainImageFormat;
    private VkExtent2D swapChainExtent;
    private List<Long> swapChainImageViews;
    private List<Long> swapChainFramebuffers;

    public VulkanSwapchain(LimeManager lime) {
        this.lime = lime;
        createSwapChain();

    }

    /**
     * creates the swapchain
     */
    private void createSwapChain() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            SwapChainSupportDetails swapChainSupportDetails = checkSwapChainSupport(stack);

            VkSurfaceFormatKHR surfaceFormatKHR = chooseSwapSurfaceFormat(swapChainSupportDetails.formats);
            int presentMode = chooseSwapPresentMode(swapChainSupportDetails.presentModes);
            VkExtent2D extent = chooseSwapExtent(swapChainSupportDetails.capabilities);

            IntBuffer imageAmount = stack.ints(swapChainSupportDetails.capabilities.minImageCount() + 1);

            if (swapChainSupportDetails.capabilities.maxImageCount() > 0 && imageAmount.get(0) > swapChainSupportDetails.capabilities.maxImageCount()) {
                imageAmount.put(0, swapChainSupportDetails.capabilities.maxImageCount());
            }

            VkSwapchainCreateInfoKHR createInfoKHR = VkSwapchainCreateInfoKHR.callocStack(stack)
                    //Set type and image
                    .sType(KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(lime.getVulkanSurface())
                    //Image settings
                    .minImageCount(imageAmount.get(0))
                    .imageFormat(surfaceFormatKHR.format())
                    .imageColorSpace(surfaceFormatKHR.colorSpace())
                    .imageExtent(extent)
                    .imageArrayLayers(1)
                    .imageUsage(VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)

                    .preTransform(swapChainSupportDetails.capabilities.currentTransform())
                    .compositeAlpha(KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                    .presentMode(presentMode)
                    .clipped(true)
                    .oldSwapchain(VK10.VK_NULL_HANDLE);

            VulkanDevice.QueueFamilyIndices indices = lime.getDevice().findQueueFamilies(lime.getDevice().getPhysicalDevice());
            if(indices.graphicsFamily.equals(indices.presentFamily)){
                createInfoKHR
                        .imageSharingMode(VK10.VK_SHARING_MODE_CONCURRENT)
                        .pQueueFamilyIndices(stack.ints(indices.graphicsFamily, indices.presentFamily));
            }else{
                createInfoKHR.imageSharingMode(VK10.VK_SHARING_MODE_EXCLUSIVE);
            }

            LongBuffer swapChainPointer = stack.longs(VK10.VK_NULL_HANDLE);
            VulkanError.failIfError(KHRSwapchain.vkCreateSwapchainKHR(lime.getDevice().getLogicalDevice(), createInfoKHR, null, swapChainPointer));
            this.swapChain = swapChainPointer.get(0);
            KHRSwapchain.vkGetSwapchainImagesKHR(lime.getDevice().getLogicalDevice(), this.swapChain, imageAmount, null);
            LongBuffer swapchainImagesPointer = stack.mallocLong(imageAmount.get(0));
            KHRSwapchain.vkGetSwapchainImagesKHR(lime.getDevice().getLogicalDevice(), swapChain, imageAmount, swapchainImagesPointer);

            this.swapChainImages = new ArrayList<>(imageAmount.get(0));

            for (int i = 0; i < swapchainImagesPointer.capacity(); i++) {
                swapChainImages.add(swapchainImagesPointer.get(i));
            }

            swapChainImageFormat = surfaceFormatKHR.format();
            swapChainExtent = VkExtent2D.create().set(extent);
        }

    }

    private VkExtent2D chooseSwapExtent(VkSurfaceCapabilitiesKHR capabilities) {
        if (capabilities.currentExtent().width() != LimeManager.UINT32_MAX) {
            return capabilities.currentExtent();
        }

        IntBuffer width = MemoryStack.stackGet().ints(0);
        IntBuffer height = MemoryStack.stackGet().ints(0);

        GLFW.glfwGetFramebufferSize(Window.getWindow(), width, height);

        VkExtent2D actualExtent = VkExtent2D.mallocStack().set(width.get(0), height.get(0));

        VkExtent2D minExtent = capabilities.minImageExtent();
        VkExtent2D maxExtent = capabilities.maxImageExtent();

        actualExtent.width(MathHelper.clamp(minExtent.width(), maxExtent.width(), actualExtent.width()));
        actualExtent.height(MathHelper.clamp(minExtent.height(), maxExtent.height(), actualExtent.height()));

        return actualExtent;
    }

    private int chooseSwapPresentMode(IntBuffer presentModes) {
        for (int i = 0; i < presentModes.capacity(); i++) {
            if (presentModes.get(i) == KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR) {
                return presentModes.get(i);
            }
        }

        return KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
    }

    private VkSurfaceFormatKHR chooseSwapSurfaceFormat(VkSurfaceFormatKHR.Buffer formats) {
        return formats
                .stream()
                .filter(vkSurfaceFormatKHR -> vkSurfaceFormatKHR.format() == VK10.VK_FORMAT_B8G8R8_SRGB)
                .filter(vkSurfaceFormatKHR -> vkSurfaceFormatKHR.colorSpace() == KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR)
                .findAny()
                .orElse(formats.get(0));
    }

    /**
     * checks if the device supports swapchains
     *
     * @param stack the memory stack
     * @return the support details
     */
    private SwapChainSupportDetails checkSwapChainSupport(MemoryStack stack) {
        SwapChainSupportDetails details = new SwapChainSupportDetails();
        details.capabilities = VkSurfaceCapabilitiesKHR.mallocStack(stack);
        KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR(lime.getDevice().getPhysicalDevice(), lime.getVulkanSurface(), details.capabilities);

        IntBuffer count = stack.ints(0);
        KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(lime.getDevice().getPhysicalDevice(), lime.getVulkanSurface(), count, null);

        if (count.get(0) != 0) {
            details.formats = VkSurfaceFormatKHR.mallocStack(count.get(0), stack);
            KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR(lime.getDevice().getPhysicalDevice(), lime.getVulkanSurface(), count, details.formats);
        }

        KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(lime.getDevice().getPhysicalDevice(), lime.getVulkanSurface(), count, null);

        if (count.get(0) != 0) {
            details.presentModes = stack.mallocInt(count.get(0));
            KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(lime.getDevice().getPhysicalDevice(), lime.getVulkanSurface(), count, details.presentModes);
        }
        return details;
    }

    /**
     * contains the swapchain support details
     */
    public static class SwapChainSupportDetails {
        public VkSurfaceCapabilitiesKHR capabilities;
        public VkSurfaceFormatKHR.Buffer formats;
        public IntBuffer presentModes;
    }
}
