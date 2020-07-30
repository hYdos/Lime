package io.github.hydos.legacylime.impl.vulkan.model;

import org.joml.Vector2fc;
import org.joml.Vector3fc;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;

import static org.lwjgl.vulkan.VK10.*;

public class VKVertex {

    private static final int SIZEOF = (3 + 3 + 2) * Float.BYTES;
    private static final int OFFSETOF_POS = 0;
    private static final int OFFSETOF_COLOR = 3 * Float.BYTES;
    private static final int OFFSETOF_TEXTCOORDS = (3 + 3) * Float.BYTES;

    private static final int SIZEOF2D = (2 + 2 + 2) * Float.BYTES;
    private static final int OFFSETOF_POS2D = 0;
    private static final int OFFSETOF_COLOR2D = 2 * Float.BYTES;
    private static final int OFFSETOF_TEXTCOORDS2D = (2 + 2) * Float.BYTES;

    public Vector3fc pos3D;
    public Vector3fc colour3D;

    public Vector2fc pos2D;
    public Vector2fc colour2D;

    public Vector2fc texCoords;

    public boolean is2D;

    public VKVertex(Vector3fc pos, Vector3fc colour, Vector2fc texCoords) {
        this.pos3D = pos;
        this.colour3D = colour;
        this.texCoords = texCoords;
        this.is2D = false;
    }

    public VKVertex(Vector2fc pos, Vector2fc colour, Vector2fc texCoords) {
        this.pos2D = pos;
        this.colour2D = colour;
        this.texCoords = texCoords;
        this.is2D = true;
    }

    public static int getSIZEOF(boolean is2D) {
        return is2D ? SIZEOF2D : SIZEOF;
    }

    public static int getOffsetofPos(boolean is2D) {
        return is2D ? OFFSETOF_POS2D : OFFSETOF_POS;
    }

    public static int getOffsetofColour(boolean is2D) {
        return is2D ? OFFSETOF_COLOR2D : OFFSETOF_COLOR;
    }

    public static int getOffsetofTextcoords(boolean is2D) {
        return is2D ? OFFSETOF_TEXTCOORDS2D : OFFSETOF_TEXTCOORDS;
    }

    public static VkVertexInputBindingDescription.Buffer getBindingDescription(boolean is2D) {
        VkVertexInputBindingDescription.Buffer bindingDescription =
                VkVertexInputBindingDescription.callocStack(1);
        bindingDescription.binding(0);
        bindingDescription.stride(getSIZEOF(is2D));
        bindingDescription.inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

        return bindingDescription;
    }

    public static VkVertexInputAttributeDescription.Buffer getAttributeDescriptions(boolean is2D) {
        VkVertexInputAttributeDescription.Buffer attributeDescriptions =
                VkVertexInputAttributeDescription.callocStack(3);

        // Position
        VkVertexInputAttributeDescription posDescription = attributeDescriptions.get(0);
        posDescription.binding(0);
        posDescription.location(0);
        posDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
        posDescription.offset(getOffsetofPos(is2D));

        // Color
        VkVertexInputAttributeDescription colorDescription = attributeDescriptions.get(1);
        colorDescription.binding(0);
        colorDescription.location(1);
        colorDescription.format(VK_FORMAT_R32G32B32_SFLOAT);
        colorDescription.offset(getOffsetofColour(is2D));

        // Texture coordinates
        VkVertexInputAttributeDescription texCoordsDescription = attributeDescriptions.get(2);
        texCoordsDescription.binding(0);
        texCoordsDescription.location(2);
        texCoordsDescription.format(VK_FORMAT_R32G32_SFLOAT);
        texCoordsDescription.offset(getOffsetofTextcoords(is2D));

        return attributeDescriptions.rewind();
    }
}