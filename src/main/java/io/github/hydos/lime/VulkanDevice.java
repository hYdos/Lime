package io.github.hydos.lime;

import io.github.hydos.lime.other.LowLevelLimeException;
import io.github.hydos.lime.other.VulkanError;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.Pointer;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class VulkanDevice {

    private static final Set<String> DEVICE_EXTENSIONS = Stream.of(KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME).collect(Collectors.toSet());

    private final LimeManager lime;
    private VkPhysicalDevice physicalDevice;
    private VkDevice logicalDevice;

    public VulkanDevice(LimeManager lime) {
        this.lime = lime;
        findPhysicalDevice();
        createLogicalDevice();
    }

    /**
     * creates a logical device from the physical device
     */
    private void createLogicalDevice() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            QueueFamilyIndices indices = findQueueFamilies(physicalDevice);

            int[] uniqueQueueFamilies = indices.unique();

            VkDeviceQueueCreateInfo.Buffer queueCreateInfos = VkDeviceQueueCreateInfo.callocStack(uniqueQueueFamilies.length, stack);

            for (int i = 0; i < uniqueQueueFamilies.length; i++) {
                queueCreateInfos.get(i)
                        .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                        .queueFamilyIndex(uniqueQueueFamilies[i])
                        .pQueuePriorities(stack.floats(1.0f));
            }

            VkPhysicalDeviceFeatures deviceFeatures = VkPhysicalDeviceFeatures.callocStack(stack)
                    .samplerAnisotropy(true)
                    .sampleRateShading(true);

            VkDeviceCreateInfo createInfo = VkDeviceCreateInfo.callocStack(stack)
                    .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pQueueCreateInfos(queueCreateInfos)
                    .pEnabledFeatures(deviceFeatures)
                    .ppEnabledExtensionNames(asPointerBuffer(DEVICE_EXTENSIONS));

            PointerBuffer devicePointer = stack.pointers(VK10.VK_NULL_HANDLE);

            VulkanError.failIfError(VK10.vkCreateDevice(physicalDevice, createInfo, null, devicePointer));

            this.logicalDevice = new VkDevice(devicePointer.get(0), this.physicalDevice, createInfo);
            PointerBuffer queuePointer = stack.pointers(VK10.VK_NULL_HANDLE);

            VK10.vkGetDeviceQueue(logicalDevice, indices.graphicsFamily, 0, queuePointer);
            lime.setGraphicsQueue(new VkQueue(queuePointer.get(0), logicalDevice));

            VK10.vkGetDeviceQueue(logicalDevice, indices.presentFamily, 0, queuePointer);
            lime.setPresentQueue(new VkQueue(queuePointer.get(0), logicalDevice));

        }
    }

    /**
     * find a physical GPU.
     */
    private void findPhysicalDevice() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer deviceCount = stack.ints(0);
            VK10.vkEnumeratePhysicalDevices(lime.getVulkanInstance(), deviceCount, null);

            if (deviceCount.get(0) == 0) {
                throw new LowLevelLimeException("Cannot find GPU(s) with Vulkan capabilities");
            }

            PointerBuffer physicalDevicesPointer = stack.mallocPointer(deviceCount.get(0));
            VK10.vkEnumeratePhysicalDevices(lime.getVulkanInstance(), deviceCount, physicalDevicesPointer);
            VkPhysicalDevice physicalDevice = null;

            for (int i = 0; i < physicalDevicesPointer.capacity(); i++) {
                physicalDevice = new VkPhysicalDevice(physicalDevicesPointer.get(i), lime.getVulkanInstance());

                if (isUsableDevice(physicalDevice)) {
                    break;
                }
            }

            if (physicalDevice == null) {
                throw new LowLevelLimeException("Cannot find GPU(s)");
            }

            this.physicalDevice = physicalDevice;
        }
    }

    private boolean isUsableDevice(VkPhysicalDevice physicalDevice) {
        QueueFamilyIndices indices = findQueueFamilies(physicalDevice);
        return indices.isComplete();
    }

    private QueueFamilyIndices findQueueFamilies(VkPhysicalDevice physicalDevice) {
        QueueFamilyIndices indices = new QueueFamilyIndices();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer queueFamilyCount = stack.ints(0);
            VK10.vkGetPhysicalDeviceQueueFamilyProperties(physicalDevice, queueFamilyCount, null);
            VkQueueFamilyProperties.Buffer queueFamilies = VkQueueFamilyProperties.mallocStack(queueFamilyCount.get(0), stack);
            IntBuffer presentSupport = stack.ints(VK10.VK_FALSE);

            for (int i = 0; i < queueFamilies.capacity() || !indices.isComplete(); i++) {
                if ((queueFamilies.get(i).queueFlags() & VK10.VK_QUEUE_GRAPHICS_BIT) != 0) {
                    indices.graphicsFamily = i;
                }
                KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(physicalDevice, i, lime.getVulkanSurface(), presentSupport);
                if (presentSupport.get(0) == VK10.VK_TRUE) {
                    indices.presentFamily = i;
                }
            }
            return indices;
        }
    }

    private PointerBuffer asPointerBuffer(Collection<String> collection) {
        MemoryStack stack = MemoryStack.stackGet();
        PointerBuffer buffer = stack.mallocPointer(collection.size());
        collection.stream()
                .map(stack::UTF8)
                .forEach(buffer::put);
        return buffer.rewind();
    }

    private PointerBuffer asPointerBuffer(List<? extends Pointer> list) {
        MemoryStack stack = MemoryStack.stackGet();
        PointerBuffer buffer = stack.mallocPointer(list.size());
        list.forEach(buffer::put);
        return buffer.rewind();
    }

    /**
     * legacy code used to store queue family indices
     */
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
