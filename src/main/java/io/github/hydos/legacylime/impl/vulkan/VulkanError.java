package io.github.hydos.legacylime.impl.vulkan;

import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VK11;

public class VulkanError {

    public static void failIfError(int code) {
        if (code != VK10.VK_SUCCESS) {
            throw new RuntimeException("Vulkan error: " + getErrorName(code));
        }
    }

    public static String getErrorName(int error) {
        switch (error) {
            case VK10.VK_SUCCESS:
                return "VK_SUCCESS";
            case VK10.VK_NOT_READY:
                return "VK_NOT_READY";
            case VK10.VK_TIMEOUT:
                return "VK_TIMEOUT";
            case VK10.VK_EVENT_SET:
                return "VK_EVENT_SET";
            case VK10.VK_EVENT_RESET:
                return "VK_EVENT_RESET";
            case VK10.VK_INCOMPLETE:
                return "VK_INCOMPLETE";
            case VK10.VK_ERROR_OUT_OF_HOST_MEMORY:
                return "VK_ERROR_OUT_OF_HOST_MEMORY";
            case VK10.VK_ERROR_OUT_OF_DEVICE_MEMORY:
                return "VK_ERROR_OUT_OF_DEVICE_MEMORY";
            case VK10.VK_ERROR_INITIALIZATION_FAILED:
                return "VK_ERROR_INITIALIZATION_FAILED";
            case VK10.VK_ERROR_DEVICE_LOST:
                return "VK_ERROR_DEVICE_LOST";
            case VK10.VK_ERROR_MEMORY_MAP_FAILED:
                return "VK_ERROR_MEMORY_MAP_FAILED";
            case VK10.VK_ERROR_LAYER_NOT_PRESENT:
                return "VK_ERROR_LAYER_NOT_PRESENT";
            case VK10.VK_ERROR_EXTENSION_NOT_PRESENT:
                return "VK_ERROR_EXTENSION_NOT_PRESENT";
            case VK10.VK_ERROR_FEATURE_NOT_PRESENT:
                return "VK_ERROR_FEATURE_NOT_PRESENT";
            case VK10.VK_ERROR_INCOMPATIBLE_DRIVER:
                return "VK_ERROR_INCOMPATIBLE_DRIVER";
            case VK10.VK_ERROR_TOO_MANY_OBJECTS:
                return "VK_ERROR_TOO_MANY_OBJECTS";
            case VK10.VK_ERROR_FORMAT_NOT_SUPPORTED:
                return "VK_ERROR_FORMAT_NOT_SUPPORTED";
            case VK10.VK_ERROR_FRAGMENTED_POOL:
                return "VK_ERROR_FRAGMENTED_POOL";
            case VK11.VK_ERROR_OUT_OF_POOL_MEMORY:
                return "VK_ERROR_OUT_OF_POOL_MEMORY";
        }

        return Integer.toString(error);
    }
}
