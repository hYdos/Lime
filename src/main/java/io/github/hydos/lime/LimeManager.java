package io.github.hydos.lime;

import io.github.hydos.lime.other.VulkanError;
import io.github.hydos.lime.other.Window;
import io.github.hydos.lime.other.LowLevelLimeException;
import io.github.hydos.lime.other.ValidationLayerManager;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;

import java.nio.LongBuffer;

/**
 * the core of the engine. this handles instances of most of it.
 */
public class LimeManager {

    private final String title;
    private final String internalName;

    private VkInstance vulkanInstance;
    private ValidationLayerManager validationManager;
    private final boolean useValidation;
    private long windowVkSurface;

    public LimeManager(int windowWidth, int windowHeight, int maxFps, String title, String internalName, boolean debug) {
        Window.create(windowWidth, windowHeight, title, maxFps);
        this.title = title;
        this.internalName = internalName;
        this.useValidation = debug;
    }

    /**
     * create the vulkan surface for the window
     */
    private void createVulkanSurface() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            LongBuffer pSurface = stack.longs(VK10.VK_NULL_HANDLE);

            VulkanError.failIfError(GLFWVulkan.glfwCreateWindowSurface(vulkanInstance, Window.getWindow(), null, pSurface));
            windowVkSurface = pSurface.get(0);
        }
    }

    /**
     * creates the vulkan instance and gives info on the application and engine running
     */
    private void createVulkanInstance() {
        this.validationManager = new ValidationLayerManager(this, useValidation);

        if (!validationManager.validationLayersSupported() && useValidation) {
            throw new LowLevelLimeException("Validation was requested, but isn't supported on this machine.");
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            VkApplicationInfo info = VkApplicationInfo.callocStack()
                    .sType(VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .pApplicationName(stack.UTF8Safe(internalName))
                    .applicationVersion(VK10.VK_MAKE_VERSION(1, 0, 0))
                    .pEngineName(stack.UTF8Safe("Lime"))
                    .engineVersion(VK10.VK_MAKE_VERSION(1, 1, 0))
                    .apiVersion(VK10.VK_API_VERSION_1_0);

            VkInstanceCreateInfo instanceCreateInfo = VkInstanceCreateInfo.callocStack()
                    .sType(VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(info)
                    .ppEnabledExtensionNames(getRequiredExtensions());

            //Retrieve pointer to instance
            PointerBuffer instancePointer = stack.mallocPointer(1);
            VulkanError.failIfError(VK10.vkCreateInstance(instanceCreateInfo, null, instancePointer));
            this.vulkanInstance = new VkInstance(instancePointer.get(0), instanceCreateInfo);
        }

        this.validationManager.setup();
    }

    private PointerBuffer getRequiredExtensions() {
        return GLFWVulkan.glfwGetRequiredInstanceExtensions();
    }

    /**
     * sets up core vulkan parts of the engine
     */
    public void setupVulkan() {
        createVulkanInstance();
        createVulkanSurface();
    }

    /**
     * always tidy up after yourself
     */
    public void tidy() {
        VK10.vkDestroyInstance(vulkanInstance, null);
    }

    public String getTitle() {
        return title;
    }

    public String getApplicationName() {
        return internalName;
    }

    public VkInstance getVulkanInstance() {
        return vulkanInstance;
    }
}
