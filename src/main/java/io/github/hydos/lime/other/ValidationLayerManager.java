package io.github.hydos.lime.other;

import io.github.hydos.lime.LimeManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * used for managing Vulkan validation layers
 */
public class ValidationLayerManager {

    private long debugMessenger;
    private final LimeManager lime;

    private final boolean validationlayersEnabled;

    private static final List<String> VALIDATION_LAYERS;

    static {
        VALIDATION_LAYERS = new ArrayList<>();
        VALIDATION_LAYERS.add("VK_LAYER_KHRONOS_validation");
    }

    public ValidationLayerManager(LimeManager lime, boolean validationLayersEnabled) {
        this.lime = lime;
        this.validationlayersEnabled = validationLayersEnabled;
    }

    public void setup() {
        if (this.validationlayersEnabled) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                VkDebugUtilsMessengerCreateInfoEXT createInfoEXT = VkDebugUtilsMessengerCreateInfoEXT.callocStack(stack);
                populateDebugMessengerCreateInfo(createInfoEXT);

                LongBuffer debugMessengerPointer = stack.longs(VK10.VK_NULL_HANDLE);
                VulkanError.failIfError(createDebugUtilsMessengerEXT(createInfoEXT, null, debugMessengerPointer));

                debugMessenger = debugMessengerPointer.get(0);
            }
        } else {
            debugMessenger = 0;
        }
    }

    /**
     * populates the create info struct
     */
    private void populateDebugMessengerCreateInfo(VkDebugUtilsMessengerCreateInfoEXT debugCreateInfoEXT) {
        debugCreateInfoEXT
                .sType(EXTDebugUtils.VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                .messageSeverity(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT)
                .messageType(EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | EXTDebugUtils.VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT)
                .pfnUserCallback(this::debugCallback);
    }

    /**
     * checks if validation layers are supported
     *
     * @return validation layers supported
     */
    public boolean validationLayersSupported() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer layerCount = stack.ints(0);
            VK10.vkEnumerateInstanceLayerProperties(layerCount, null);

            VkLayerProperties.Buffer layers = VkLayerProperties.mallocStack(layerCount.get(0), stack);

            VK10.vkEnumerateInstanceLayerProperties(layerCount, layers);

            Set<String> layerNames = layers
                    .stream()
                    .map(VkLayerProperties::layerNameString)
                    .collect(Collectors.toSet());

            return layerNames.containsAll(VALIDATION_LAYERS);
        }
    }

    /**
     * calls a message when a validation error is triggered
     */
    private int debugCallback(int messageSeverity, int messageType, long callbackDataPointer, long userDataPointer) {
        VkDebugUtilsMessengerCallbackDataEXT callbackDataEXT = VkDebugUtilsMessengerCallbackDataEXT.create(callbackDataPointer);
        System.err.println("Validation layer " + callbackDataEXT.pMessageString());
        return VK10.VK_FALSE;
    }

    /**
     * i dont know the full purpose of this
     */
    private int createDebugUtilsMessengerEXT(VkDebugUtilsMessengerCreateInfoEXT createInfoEXT, VkAllocationCallbacks allocationCallbacks, LongBuffer debugMessengerPointer) {
        if (VK10.vkGetInstanceProcAddr(lime.getVulkanInstance(), "vkCreateDebugUtilsMessengerEXT") != MemoryUtil.NULL) {
            return EXTDebugUtils.vkCreateDebugUtilsMessengerEXT(lime.getVulkanInstance(), createInfoEXT, allocationCallbacks, debugMessengerPointer);
        }
        return VK10.VK_ERROR_EXTENSION_NOT_PRESENT;
    }

}
