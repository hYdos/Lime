package io.github.hydos.example;

import io.github.hydos.legacylime.core.core.ModelManager;
import io.github.hydos.lime.other.Window;
import io.github.hydos.legacylime.core.player.PlayerController;
import io.github.hydos.legacylime.core.render.RenderObject;
import io.github.hydos.legacylime.core.ui.GuiManager;
import io.github.hydos.legacylime.impl.vulkan.Variables;
import io.github.hydos.legacylime.impl.vulkan.VulkanManager;
import io.github.hydos.legacylime.impl.vulkan.VulkanReg;
import io.github.hydos.legacylime.impl.vulkan.device.DeviceManager;
import io.github.hydos.legacylime.impl.vulkan.io.VKWindow;
import io.github.hydos.legacylime.impl.vulkan.model.CommandBufferManager;
import io.github.hydos.legacylime.impl.vulkan.render.Frame;
import io.github.hydos.legacylime.impl.vulkan.swapchain.SwapchainManager;
import io.github.hydos.legacylime.impl.vulkan.ubo.DescriptorManager;
import io.github.hydos.legacylime.impl.vulkan.util.Utils;
import io.github.hydos.lime.resource.Identifier;
import io.github.hydos.lime.resource.Resource;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.io.IOException;

import static io.github.hydos.lime.resource.ResourceHandler.GLOBAL_RESOURCE_MANAGER;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.vulkan.VK10.vkDeviceWaitIdle;

public class LegacyVulkanExample {

    RenderObject chalet;
    RenderObject dragon;
    RenderObject obamaPrism;
    RenderObject world;

    public LegacyVulkanExample() throws IOException {
        initWindow();
        initVulkan();
        runEngine();
        VulkanManager.getInstance().cleanup();
    }

    public static void main(String[] args) throws IOException {
        new LegacyVulkanExample();
    }

    private void loadModels() throws IOException {
        Resource charletModel = GLOBAL_RESOURCE_MANAGER.getResource(new Identifier("example", "models/chalet.obj")).get();
        Resource dragonModel = GLOBAL_RESOURCE_MANAGER.getResource(new Identifier("example", "models/dragon.obj")).get();
        Resource obamaPrismModel = GLOBAL_RESOURCE_MANAGER.getResource(new Identifier("example", "models/obamium.obj")).get();
        Resource worldModel = GLOBAL_RESOURCE_MANAGER.getResource(new Identifier("example", "models/world.obj")).get();

        Resource charletTexture = GLOBAL_RESOURCE_MANAGER.getResource(new Identifier("example", "textures/chalet.jpg")).get();
        Resource dragonTexture = GLOBAL_RESOURCE_MANAGER.getResource(new Identifier("example", "textures/skybox/back.png")).get();
        Resource obamaPrismTexture = GLOBAL_RESOURCE_MANAGER.getResource(new Identifier("example", "textures/obamium.jpg")).get();
        Resource worldTexture = GLOBAL_RESOURCE_MANAGER.getResource(new Identifier("example", "textures/world-RGBA.png")).get();

        chalet = ModelManager.createObject(charletTexture, charletModel, new Vector3f(-2, -0.4f, 0.5f), new Vector3f(-90, 0, 0), new Vector3f(1f, 1f, 1f));
        dragon = ModelManager.createObject(dragonTexture, dragonModel, new Vector3f(-2, -1, 0), new Vector3f(), new Vector3f(0.3f, 0.3f, 0.3f));
        obamaPrism = ModelManager.createObject(obamaPrismTexture, obamaPrismModel, new Vector3f(-2, -1, 5), new Vector3f(-90, 0, 0), new Vector3f(1f, 1f, 1f));
        world = ModelManager.createObject(worldTexture, worldModel, new Vector3f(-2, -1, 5), new Vector3f(-90, 0, 0), new Vector3f(1f, 1f, 1f));

        VulkanManager.getInstance().entityRenderer.processEntity(chalet);
        VulkanManager.getInstance().entityRenderer.processEntity(dragon);
        VulkanManager.getInstance().entityRenderer.processEntity(obamaPrism);
        VulkanManager.getInstance().entityRenderer.processEntity(world);
        GuiManager.addElement(GuiManager.createUiElement(new Vector2f(0.5f, 0.5f), new Vector2f(1, 1), obamaPrismTexture));
    }

    private void initWindow() {
        Window.create(1200, 800, "Lime Render Engine", 60);
        glfwSetFramebufferSizeCallback(Window.getWindow(), this::framebufferResizeCallback);
    }

    private void framebufferResizeCallback(long window, int width, int height) {
        Variables.framebufferResize = true;
    }

    private void initVulkan() throws IOException {
        VulkanReg.createInstance();
        VKWindow.createSurface();
        VulkanManager.init();
        VulkanManager.getInstance().createRenderers();
        DeviceManager.pickPhysicalDevice();
        DeviceManager.createLogicalDevice();
        CommandBufferManager.createCommandPool();
        DescriptorManager.createUBODescriptorSetLayout();
        loadModels();
        SwapchainManager.createSwapChainObjects();
        Utils.createSyncObjects();
    }

    private void runEngine() {
        Window.lockMouse();
        while (!Window.closed()) {
            loop();
            PlayerController.onInput();
        }
        Window.stop();
        vkDeviceWaitIdle(Variables.device);
    }

    private void loop() {
        if (Window.shouldRender()) {
            chalet.increaseRotation(0, 0, 5);
            obamaPrism.increaseRotation(0, 0, 1);
            Frame.drawFrame();
        }
        glfwPollEvents();
    }
}
