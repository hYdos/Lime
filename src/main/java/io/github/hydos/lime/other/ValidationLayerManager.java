package io.github.hydos.lime.other;

import io.github.hydos.lime.LimeManager;

import java.util.ArrayList;
import java.util.List;

/**
 * used for managing Vulkan validation layers
 */
public class ValidationLayerManager {

    private LimeManager lime;

    private boolean validationlayersEnabled;

    private static List<String> validationLayers;

    static {
        validationLayers = new ArrayList<>();
        validationLayers.add("VK_LAYER_KHRONOS_validation");
    }

    public ValidationLayerManager(LimeManager lime, boolean validationLayersEnabled){
        this.lime = lime;
        this.validationlayersEnabled = validationLayersEnabled;
    }

    /**
     * if validation layers are active it will init them here
     */
    public void setup(){

    }

}
