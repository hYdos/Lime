package io.github.hydos.legacylime.impl.vulkan.device;

import io.github.hydos.legacylime.impl.vulkan.Variables;
import io.github.hydos.lime.other.VulkanError;
import io.github.hydos.legacylime.impl.vulkan.swapchain.SwapchainManager;
import io.github.hydos.legacylime.impl.vulkan.util.Utils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.VK10.*;

public class DeviceManager {

    public static final Set<String> DEVICE_EXTENSIONS = Stream.of(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME).collect(Collectors.toSet());

    public static QueueFamilyIndices findQueueFamilies(VkPhysicalDevice device) {
        QueueFamilyIndices indices = new QueueFamilyIndices();

        try (MemoryStack stack = stackPush()) {
            IntBuffer queueFamilyCount = stack.ints(0);
            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, null);
            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.mallocStack(queueFamilyCount.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(device, queueFamilyCount, queueFamilies);
            IntBuffer presentSupport = stack.ints(VK_FALSE);

            for (int i = 0; i < queueFamilies.capacity() || !indices.isComplete(); i++) {
                if ((queueFamilies.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0) {
                    indices.graphicsFamily = i;
                }
                vkGetPhysicalDeviceSurfaceSupportKHR(device, i, Variables.surface, presentSupport);
                if (presentSupport.get(0) == VK_TRUE) {
                    indices.presentFamily = i;
                }
            }
            return indices;
        }
    }

    public static void createLogicalDevice() {
        try (MemoryStack stack = stackPush()) {
            QueueFamilyIndices indices = findQueueFamilies(Variables.physicalDevice);

            int[] uniqueQueueFamilies = indices.unique();

            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.callocStack(uniqueQueueFamilies.length, stack);

            for (int i = 0; i < uniqueQueueFamilies.length; i++) {
                VkDeviceQueueCreateInfo queueCreateInfo = queueCreateInfos.get(i);
                queueCreateInfo.sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
                queueCreateInfo.queueFamilyIndex(uniqueQueueFamilies[i]);
                queueCreateInfo.pQueuePriorities(stack.floats(1.0f));
            }

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.callocStack(stack);
            deviceFeatures.samplerAnisotropy(true);
            deviceFeatures.sampleRateShading(true); // Enable sample shading feature for the device

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.callocStack(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO);
            createInfo.pQueueCreateInfos(queueCreateInfos);
            // queueCreateInfoCount is automatically set

            createInfo.pEnabledFeatures(deviceFeatures);

            createInfo.ppEnabledExtensionNames(Utils.asPointerBuffer(DEVICE_EXTENSIONS));

            PointerBuffer pDevice = stack.pointers(VK_NULL_HANDLE);

            VulkanError.failIfError(vkCreateDevice(Variables.physicalDevice, createInfo, null, pDevice));

            Variables.device = new VkDevice(pDevice.get(0), Variables.physicalDevice, createInfo);

            PointerBuffer pQueue = stack.pointers(VK_NULL_HANDLE);

            vkGetDeviceQueue(Variables.device, indices.graphicsFamily, 0, pQueue);
            Variables.graphicsQueue = new VkQueue(pQueue.get(0), Variables.device);

            vkGetDeviceQueue(Variables.device, indices.presentFamily, 0, pQueue);
            Variables.presentQueue = new VkQueue(pQueue.get(0), Variables.device);
        }
    }

    public static void pickPhysicalDevice() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer deviceCount = stack.ints(0);
            vkEnumeratePhysicalDevices(Variables.instance, deviceCount, null);

            if (deviceCount.get(0) == 0) {
                throw new RuntimeException("Failed to find GPUs with Vulkan support");
            }

            PointerBuffer ppPhysicalDevices = stack.mallocPointer(deviceCount.get(0));
            vkEnumeratePhysicalDevices(Variables.instance, deviceCount, ppPhysicalDevices);
            VkPhysicalDevice device = null;

            for (int i = 0; i < ppPhysicalDevices.capacity(); i++) {
                device = new VkPhysicalDevice(ppPhysicalDevices.get(i), Variables.instance);

                if (isDeviceSuitable(device)) {
                    break;
                }
            }

            if (device == null) {
                throw new RuntimeException("Failed to find a suitable GPU");
            }

            Variables.physicalDevice = device;
            Variables.msaaSamples = Utils.getMaxUsableSampleCount();
        }
    }

    public static boolean checkDeviceExtensionSupport(VkPhysicalDevice device) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer extensionCount = stack.ints(0);

            vkEnumerateDeviceExtensionProperties(device, (String) null, extensionCount, null);

            VkExtensionProperties.Buffer availableExtensions = VkExtensionProperties.mallocStack(extensionCount.get(0), stack);

            return availableExtensions.stream().collect(toSet()).containsAll(DEVICE_EXTENSIONS);
        }
    }

    private static boolean isDeviceSuitable(VkPhysicalDevice device) {
        QueueFamilyIndices indices = findQueueFamilies(device);

        boolean extensionsSupported = checkDeviceExtensionSupport(device);
        boolean swapChainAdequate = false;
        boolean anisotropySupported = false;

        if (extensionsSupported) {
            try (MemoryStack stack = stackPush()) {
                SwapchainManager.SwapChainSupportDetails swapChainSupport = SwapchainManager.querySwapChainSupport(device, stack);
                swapChainAdequate = swapChainSupport.formats.hasRemaining() && swapChainSupport.presentModes.hasRemaining();
                VkPhysicalDeviceFeatures supportedFeatures = VkPhysicalDeviceFeatures.mallocStack(stack);
                vkGetPhysicalDeviceFeatures(device, supportedFeatures);
                anisotropySupported = supportedFeatures.samplerAnisotropy();
            }
        }

        return indices.isComplete() && extensionsSupported && swapChainAdequate && anisotropySupported;
    }

    public static class QueueFamilyIndices {
        public Integer graphicsFamily;
        public Integer presentFamily;

        public boolean isComplete() {
            return graphicsFamily != null && presentFamily != null;
        }

        public int[] unique() {
            return IntStream.of(graphicsFamily, presentFamily).distinct().toArray();
        }
    }
}
