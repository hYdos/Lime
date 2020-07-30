package io.github.hydos.legacylime.impl.vulkan;

import io.github.hydos.lime.resource.ResourceHandler;
import io.github.hydos.legacylime.core.io.Window;
import io.github.hydos.legacylime.impl.vulkan.elements.VulkanRenderObject;
import io.github.hydos.legacylime.impl.vulkan.model.VKBufferMesh;
import io.github.hydos.legacylime.impl.vulkan.render.VKRenderManager;
import io.github.hydos.legacylime.impl.vulkan.render.renderers.EntityRenderer;
import io.github.hydos.legacylime.impl.vulkan.render.renderers.GuiRenderer;
import io.github.hydos.legacylime.impl.vulkan.swapchain.SwapchainManager;
import io.github.hydos.legacylime.impl.vulkan.ubo.DescriptorManager;

import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.VK10.*;

public class VulkanManager {

    private static VulkanManager INSTANCE;

    public EntityRenderer entityRenderer;
    public GuiRenderer guiRenderer;

    public static void init() {
        INSTANCE = new VulkanManager();
        Variables.renderManager = new VKRenderManager();
    }

    public static VulkanManager getInstance() {
        return INSTANCE;
    }

    public static void cleanupVulkan() {
        SwapchainManager.cleanupSwapChain();
// TODO: grab all entities from the vulkan instance and
//        vkDestroySampler(Variables.device, Variables.textureSampler, null);
//        vkDestroyImageView(Variables.device, Variables.textureImageView, null);
//        vkDestroyImage(Variables.device, Variables.textureImage, null);
//        vkFreeMemory(Variables.device, Variables.textureImageMemory, null);

        vkDestroyDescriptorSetLayout(Variables.device, DescriptorManager.descriptorSetLayout, null);

        Variables.inFlightFrames.forEach(frame -> {
            vkDestroySemaphore(Variables.device, frame.renderFinishedSemaphore(), null);
            vkDestroySemaphore(Variables.device, frame.imageAvailableSemaphore(), null);
            vkDestroyFence(Variables.device, frame.fence(), null);
        });
        Variables.inFlightFrames.clear();

        vkDestroyCommandPool(Variables.device, Variables.commandPool, null);

        vkDestroyDevice(Variables.device, null);

        vkDestroySurfaceKHR(Variables.instance, Variables.surface, null);

        vkDestroyInstance(Variables.instance, null);

        Window.destroy();
    }

    public void createRenderers() {
        entityRenderer = new EntityRenderer();
        guiRenderer = new GuiRenderer();
        Variables.renderManager.addRenderer(entityRenderer);
        Variables.renderManager.addRenderer(guiRenderer);
    }

    public void cleanup() {
        for (VulkanRenderObject entity : entityRenderer.entities) {
            VKBufferMesh bufferMesh = entity.getModel();
            bufferMesh.cleanup();
        }

        cleanupVulkan();
        ResourceHandler.GLOBAL_RESOURCE_MANAGER.invalidate();
    }
}
