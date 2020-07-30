package io.github.hydos.legacylime.core.io;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.glfw.GLFWVulkan;

import java.nio.DoubleBuffer;

import static org.lwjgl.glfw.GLFW.GLFW_CLIENT_API;
import static org.lwjgl.glfw.GLFW.GLFW_NO_API;

public class Window {
    private static final Vector3f clearColour = new Vector3f(118f, 215f, 234f);
    private static final boolean[] mouseButtons = new boolean[GLFW.GLFW_MOUSE_BUTTON_LAST];
    private static final GLFWImage.Buffer iconBuffer = null;
    public static int width;
    public static int height;
    public static double dy = 0;
    public static double dx = 0;
    public static int actualWidth, actualHeight;
    static double oldX = 0;
    static double oldY = 0;
    static double newX = 0;
    static double newY = 0;
    private static String title;
    private static long window;
    private static double fpsCap, time, processedTime = 0;
    private static boolean fullscreen = false;

    public static int getWidth() {
        return width;
    }

    public static int getHeight() {
        return height;
    }

    public static boolean isFullscreen() {
        return fullscreen;
    }

    public static boolean closed() {
        return GLFW.glfwWindowShouldClose(getWindow());
    }

    public static void create() {
        if (!GLFW.glfwInit()) {
            System.err.println("Error: Couldn't initialize GLFW");
            System.exit(-1);
        }
        GLFW.glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);
        if (!GLFWVulkan.glfwVulkanSupported()) {
            throw new AssertionError("GLFW failed to find the Vulkan loader");
        }
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 6);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE);
        GLFWVidMode vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
        window = GLFW.glfwCreateWindow(actualWidth, actualHeight, title, (fullscreen) ? GLFW.glfwGetPrimaryMonitor() : 0, getWindow());
        if (getWindow() == 0) {
            System.err.println("Error: Couldnt initilize window");
            System.exit(-1);
        }
        GLFW.glfwMakeContextCurrent(getWindow());
        GLFW.glfwSetWindowPos(getWindow(), (vidmode.width() - actualWidth) / 2, (vidmode.height() - actualHeight) / 2);
        GLFW.glfwShowWindow(getWindow());
        time = getTime();
        getCurrentTime();
    }

    public static void create(int width, int height, String title, float fpsCap) {
        Window.width = width / 2;
        Window.height = height / 2;
        Window.actualHeight = height;
        Window.actualWidth = width;
        Window.title = title;
        Window.fpsCap = fpsCap;
        Window.create();
        Window.setIcon();
    }

    public static Vector3f getClearColour() {
        return Window.clearColour;
    }

    private static long getCurrentTime() {
        return (long) (GLFW.glfwGetTime() * 1000 / GLFW.glfwGetTimerFrequency());
    }

    public static double getMouseX() {
        DoubleBuffer buffer = BufferUtils.createDoubleBuffer(1);
        GLFW.glfwGetCursorPos(getWindow(), buffer, null);
        return buffer.get(0);
    }

    public static double getMouseY() {
        DoubleBuffer buffer = BufferUtils.createDoubleBuffer(1);
        GLFW.glfwGetCursorPos(getWindow(), null, buffer);
        return buffer.get(0);
    }

    public static Vector2f getNormalizedMouseCoordinates() {
        float normalX = -1.0f + 2.0f * (float) getMouseX() / width;
        float normalY = 1.0f - 2.0f * (float) getMouseY() / height;
        return new Vector2f(normalX, normalY);
    }

    public static double getTime() {
        return System.currentTimeMillis() / 1000D;
    }

    public static boolean isKeyDown(int keycode) {
        return GLFW.glfwGetKey(getWindow(), keycode) == 1;
    }

    public static boolean isMouseDown(int mouseButton) {
        return GLFW.glfwGetMouseButton(getWindow(), mouseButton) == 1;
    }

    public static boolean isMousePressed(int keyCode) {
        return isMouseDown(keyCode) && !mouseButtons[keyCode];
    }

    public static boolean isMouseReleased(int keyCode) {
        return !isMouseDown(keyCode) && mouseButtons[keyCode];
    }

    public static boolean shouldRender() {
        double nextTime = getTime();
        double passedTime = nextTime - time;
        processedTime += passedTime;
        time = nextTime;
        if (processedTime > 1.0 / fpsCap) {
            processedTime -= 1.0 / fpsCap;
            return true;
        }
        return false;
    }

    public static void lockMouse() {
        GLFW.glfwSetInputMode(getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
    }

    private static void setIcon() {
//        Image icon = Image.createImage("/icon.png");
//        GLFWImage iconImage = GLFWImage.malloc();
//        iconBuffer = GLFWImage.malloc(1);
//        iconImage.set(icon.getWidth(), icon.getHeight(), icon.getImage());
//        iconBuffer.put(0, iconImage);
//        GLFW.glfwSetWindowIcon(getWindow(), iconBuffer);
    }

    public static void showIcon() {
        if (iconBuffer != null) GLFW.glfwSetWindowIcon(getWindow(), iconBuffer);
    }

    public static void stop() {
        GLFW.glfwTerminate();
    }

    public static void swapBuffers() {
        GLFW.glfwSwapBuffers(getWindow());
    }

    public static void unlockMouse() {
        GLFW.glfwSetInputMode(getWindow(), GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_NORMAL);
    }

    public static void fullscreen() {
        Window.fullscreen = !Window.isFullscreen();
    }

    public static long getWindow() {
        return window;
    }

    public static void destroy() {
        GLFW.glfwDestroyWindow(Window.getWindow());

        GLFW.glfwTerminate();
    }
}
