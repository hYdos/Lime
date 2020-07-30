package io.github.hydos.lime.other;

import io.github.hydos.lime.LimeManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkLayerProperties;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * used for managing Vulkan validation layers
 */
public class ValidationLayerManager {

    private LimeManager lime;

    private boolean validationlayersEnabled;

    private static final List<String> VALIDATION_LAYERS;

    static {
        VALIDATION_LAYERS = new ArrayList<>();
        VALIDATION_LAYERS.add("VK_LAYER_KHRONOS_validation");
    }

    public ValidationLayerManager(LimeManager lime, boolean validationLayersEnabled){
        this.lime = lime;
        this.validationlayersEnabled = validationLayersEnabled;
    }

    /**
     * checks if validation layers are supported
     * @return validation layers supported
     */
    public boolean validationLayersSupported(){
        try(MemoryStack stack = MemoryStack.stackPush()){
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

}
