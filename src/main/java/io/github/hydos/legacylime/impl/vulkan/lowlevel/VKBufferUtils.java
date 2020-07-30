package io.github.hydos.legacylime.impl.vulkan.lowlevel;

import io.github.hydos.legacylime.impl.vulkan.Variables;
import io.github.hydos.lime.VulkanError;
import io.github.hydos.legacylime.impl.vulkan.model.CommandBufferManager;
import io.github.hydos.legacylime.impl.vulkan.model.SimpleVKBufferMesh;
import io.github.hydos.legacylime.impl.vulkan.model.VKBufferMesh;
import io.github.hydos.legacylime.impl.vulkan.model.VKVertex;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class VKBufferUtils {

    public static void createBuffer(long size, int usage, int properties, LongBuffer pBuffer, LongBuffer pBufferMemory) {
        try (MemoryStack stack = stackPush()) {
            VkBufferCreateInfo bufferInfo = VkBufferCreateInfo.callocStack(stack);
            bufferInfo.sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO);
            bufferInfo.size(size);
            bufferInfo.usage(usage);
            bufferInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            VulkanError.failIfError(VK10.vkCreateBuffer(Variables.device, bufferInfo, null, pBuffer));

            VkMemoryRequirements memRequirements = VkMemoryRequirements.mallocStack(stack);
            vkGetBufferMemoryRequirements(Variables.device, pBuffer.get(0), memRequirements);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocInfo.allocationSize(memRequirements.size());
            allocInfo.memoryTypeIndex(VKMemoryUtils.findMemoryType(memRequirements.memoryTypeBits(), properties));

            VulkanError.failIfError(vkAllocateMemory(Variables.device, allocInfo, null, pBufferMemory));

            vkBindBufferMemory(Variables.device, pBuffer.get(0), pBufferMemory.get(0), 0);
        }
    }

    public static void copyBuffer(long srcBuffer, long dstBuffer, long size) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBuffer commandBuffer = CommandBufferManager.beginSingleTimeCommands();

            VkBufferCopy.Buffer copyRegion = VkBufferCopy.callocStack(1, stack);
            copyRegion.size(size);

            vkCmdCopyBuffer(commandBuffer, srcBuffer, dstBuffer, copyRegion);

            CommandBufferManager.endSingleTimeCommands(commandBuffer);
        }
    }

    public static void createVertexBuffer(VKBufferMesh processedMesh) {
        try (MemoryStack stack = stackPush()) {
            long bufferSize = VKVertex.getSIZEOF(false) * processedMesh.vertices.length;

            LongBuffer pBuffer = stack.mallocLong(1);
            LongBuffer pBufferMemory = stack.mallocLong(1);
            VKBufferUtils.createBuffer(bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pBuffer,
                    pBufferMemory);

            long stagingBuffer = pBuffer.get(0);
            long stagingBufferMemory = pBufferMemory.get(0);

            PointerBuffer data = stack.mallocPointer(1);

            vkMapMemory(Variables.device, stagingBufferMemory, 0, bufferSize, 0, data);
            {
                VKMemoryUtils.memcpy(data.getByteBuffer(0, (int) bufferSize), processedMesh.vertices, false);
            }
            vkUnmapMemory(Variables.device, stagingBufferMemory);

            VKBufferUtils.createBuffer(bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    VK_MEMORY_HEAP_DEVICE_LOCAL_BIT,
                    pBuffer,
                    pBufferMemory);

            processedMesh.vertexBuffer = pBuffer.get(0);
            processedMesh.vertexBufferMemory = pBufferMemory.get(0);

            VKBufferUtils.copyBuffer(stagingBuffer, processedMesh.vertexBuffer, bufferSize);

            vkDestroyBuffer(Variables.device, stagingBuffer, null);
            vkFreeMemory(Variables.device, stagingBufferMemory, null);

        }
    }

    public static void createVertexBuffer2D(SimpleVKBufferMesh processedMesh) {
        try (MemoryStack stack = stackPush()) {
            long bufferSize = VKVertex.getSIZEOF(true) * processedMesh.vertices.length;

            LongBuffer pBuffer = stack.mallocLong(1);
            LongBuffer pBufferMemory = stack.mallocLong(1);
            VKBufferUtils.createBuffer(bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pBuffer,
                    pBufferMemory);

            long stagingBuffer = pBuffer.get(0);
            long stagingBufferMemory = pBufferMemory.get(0);

            PointerBuffer data = stack.mallocPointer(1);

            vkMapMemory(Variables.device, stagingBufferMemory, 0, bufferSize, 0, data);
            {
                VKMemoryUtils.memcpy(data.getByteBuffer(0, (int) bufferSize), processedMesh.vertices, true);
            }
            vkUnmapMemory(Variables.device, stagingBufferMemory);

            VKBufferUtils.createBuffer(bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                    VK_MEMORY_HEAP_DEVICE_LOCAL_BIT,
                    pBuffer,
                    pBufferMemory);

            processedMesh.vertexBuffer = pBuffer.get(0);
            processedMesh.vertexBufferMemory = pBufferMemory.get(0);

            VKBufferUtils.copyBuffer(stagingBuffer, processedMesh.vertexBuffer, bufferSize);

            vkDestroyBuffer(Variables.device, stagingBuffer, null);
            vkFreeMemory(Variables.device, stagingBufferMemory, null);

        }
    }

    public static void createIndexBuffer(VKBufferMesh processedMesh) {
        try (MemoryStack stack = stackPush()) {
            long bufferSize = Integer.BYTES * processedMesh.indices.length;

            LongBuffer pBuffer = stack.mallocLong(1);
            LongBuffer pBufferMemory = stack.mallocLong(1);
            VKBufferUtils.createBuffer(bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                    pBuffer,
                    pBufferMemory);

            long stagingBuffer = pBuffer.get(0);
            long stagingBufferMemory = pBufferMemory.get(0);

            PointerBuffer data = stack.mallocPointer(1);

            vkMapMemory(Variables.device, stagingBufferMemory, 0, bufferSize, 0, data);
            {
                VKMemoryUtils.memcpy(data.getByteBuffer(0, (int) bufferSize), processedMesh.indices);
            }
            vkUnmapMemory(Variables.device, stagingBufferMemory);

            VKBufferUtils.createBuffer(bufferSize,
                    VK_BUFFER_USAGE_TRANSFER_DST_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT,
                    VK_MEMORY_HEAP_DEVICE_LOCAL_BIT,
                    pBuffer,
                    pBufferMemory);

            processedMesh.indexBuffer = pBuffer.get(0);
            processedMesh.indexBufferMemory = pBufferMemory.get(0);

            VKBufferUtils.copyBuffer(stagingBuffer, processedMesh.indexBuffer, bufferSize);

            vkDestroyBuffer(Variables.device, stagingBuffer, null);
            vkFreeMemory(Variables.device, stagingBufferMemory, null);
        }
    }
}
