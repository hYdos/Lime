package io.github.hydos.legacylime.core.player;

import io.github.hydos.legacylime.core.io.Window;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

/**
 * this class is only for moving around the scene and testing
 */
public class PlayerController {

    public static Vector3f position = new Vector3f(2, 0, 0);
    public static Vector3f rotation = new Vector3f();
    public static Vector3f offsetRotation = new Vector3f();

    public static void onInput() {
        offsetRotation = new Vector3f();
        if (Window.isKeyDown(GLFW.GLFW_KEY_A)) {
            position.add(0, 0, 0.1f);
            offsetRotation.add(0, 0, 0.1f);
        }
        if (Window.isKeyDown(GLFW.GLFW_KEY_D)) {
            offsetRotation.add(0, 0, -0.1f);
            position.add(0, 0, -0.1f);
        }
        if (Window.isKeyDown(GLFW.GLFW_KEY_W)) {
            offsetRotation.add(-0.1f, 0, 0);
            position.add(-0.1f, 0, 0);
        }
        if (Window.isKeyDown(GLFW.GLFW_KEY_S)) {
            offsetRotation.add(0.1f, 0, 0);
            position.add(0.1f, 0, 0);
        }
        if (Window.isKeyDown(GLFW.GLFW_KEY_SPACE)) {
            offsetRotation.add(0f, -0.1f, 0);
            position.add(0f, 0.1f, 0);
        }

        rotation.y = Window.getNormalizedMouseCoordinates().y * 2;
        rotation.z = -(Window.getNormalizedMouseCoordinates().x * 2);
        rotation.add(offsetRotation);

    }

}
