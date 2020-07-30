package io.github.hydos.legacylime.impl.vulkan.lowlevel;

import io.github.hydos.legacylime.impl.vulkan.Variables;
import io.github.hydos.legacylime.impl.vulkan.model.VKVertex;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceMemoryProperties;

public class VKMemoryUtils {

    public static int findMemoryType(int typeFilter, int properties) {
        VkPhysicalDeviceMemoryProperties memProperties = VkPhysicalDeviceMemoryProperties.mallocStack();
        vkGetPhysicalDeviceMemoryProperties(Variables.physicalDevice, memProperties);
        for (int i = 0; i < memProperties.memoryTypeCount(); i++) {
            if ((typeFilter & (1 << i)) != 0 && (memProperties.memoryTypes(i).propertyFlags() & properties) == properties) {
                return i;
            }
        }

        throw new RuntimeException("Failed to find suitable memory type");
    }

    public static void memcpy(ByteBuffer dst, ByteBuffer src, long size) {
        src.limit((int) size);
        dst.put(src);
        src.limit(src.capacity()).rewind();
    }

    public static void memcpy(ByteBuffer buffer, VKVertex[] vertices, boolean is2D) {
        if(is2D){
            for (VKVertex vertex : vertices) {
                buffer.putFloat(vertex.pos2D.x());
                buffer.putFloat(vertex.pos2D.y());

                buffer.putFloat(vertex.colour2D.x());
                buffer.putFloat(vertex.colour2D.y());

                buffer.putFloat(vertex.texCoords.x());
                buffer.putFloat(vertex.texCoords.y());
            }
        }else{
            for (VKVertex vertex : vertices) {
                buffer.putFloat(vertex.pos3D.x());
                buffer.putFloat(vertex.pos3D.y());
                buffer.putFloat(vertex.pos3D.z());

                buffer.putFloat(vertex.colour3D.x());
                buffer.putFloat(vertex.colour3D.y());
                buffer.putFloat(vertex.colour3D.z());

                buffer.putFloat(vertex.texCoords.x());
                buffer.putFloat(vertex.texCoords.y());
            }
        }
    }

    public static void memcpy(ByteBuffer buffer, int[] indices) {
        for (int index : indices) {
            buffer.putInt(index);
        }

        buffer.rewind();
    }
}
