package io.github.hydos.example;

import io.github.hydos.lime.LimeManager;
import io.github.hydos.lime.other.Window;
import org.lwjgl.glfw.GLFW;

public class VulkanExample {

    private final LimeManager lime;

    public static void main(String[] args) {
        new VulkanExample();
    }

    public VulkanExample(){
        this.lime = new LimeManager(1200, 800, 60, "Lime Rewrite", "Example", false);
        lime.setupVulkan();


        //Window loop?
        while (!Window.closed()){
            if(Window.shouldRender()){
                System.out.println("Render");
            }
            GLFW.glfwPollEvents();
        }


        lime.tidy();
    }

}
      