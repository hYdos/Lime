package io.github.hydos.example;

import io.github.hydos.lime.LimeManager;

public class VulkanExample {

    private final LimeManager limeManager;

    public static void main(String[] args) {
        new VulkanExample();
    }

    public VulkanExample(){
        this.limeManager = new LimeManager(1200, 800, 60, "Lime Rewrite", "Example", true);
        limeManager.setupVulkan();
    }

}
