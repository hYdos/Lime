package io.github.hydos.legacylime.impl.vulkan.ubo;

import io.github.hydos.legacylime.core.math.CitrusMath;
import io.github.hydos.legacylime.core.player.PlayerController;
import io.github.hydos.legacylime.impl.vulkan.Variables;
import io.github.hydos.legacylime.impl.vulkan.VulkanManager;
import io.github.hydos.legacylime.impl.vulkan.elements.VulkanRenderObject;
import io.github.hydos.legacylime.impl.vulkan.lowlevel.AlignmentUtils;
import io.github.hydos.legacylime.impl.vulkan.lowlevel.VKBufferUtils;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class UboManager {

    static final int mat4Size = 16 * Float.BYTES;
    static final int modelArraySize = mat4Size * 10; //FIXME: also hardcoded

    public static void createObjectUniformBuffers() {
        try (MemoryStack stack = stackPush()) {
            Variables.uniformBuffers = new ArrayList<>(Variables.swapChainImages.size());
            Variables.uniformBuffersMemory = new ArrayList<>(Variables.swapChainImages.size());

            LongBuffer pBuffer = stack.mallocLong(1);
            LongBuffer pBufferMemory = stack.mallocLong(1);

            for (int i = 0; i < Variables.swapChainImages.size(); i++) {
                VKBufferUtils.createBuffer(GenericUbo.SIZEOF, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT, pBuffer, pBufferMemory);

                Variables.uniformBuffers.add(pBuffer.get(0));
                Variables.uniformBuffersMemory.add(pBufferMemory.get(0));
            }
        }
    }

    private static void putUBOInMemory(ByteBuffer buffer, GenericUbo ubo) {
        putArrayInUboMemory(buffer, ubo.model);
        ubo.view.get(AlignmentUtils.alignas(modelArraySize, AlignmentUtils.alignof(ubo.view)), buffer);
        ubo.proj.get(AlignmentUtils.alignas(modelArraySize + mat4Size, AlignmentUtils.alignof(ubo.view)), buffer);
    }

    private static void putArrayInUboMemory(ByteBuffer buffer, Matrix4f[] models) {
        int i = 0;

        for (Matrix4f mat : models) {
            if (mat == null) {
                mat = new Matrix4f();
            }

            mat.get(mat4Size * i, buffer);
            i++;
        }
    }

    public static void updateUniformBuffers(int currentImage) {
        try (MemoryStack stack = stackPush()) {
            GenericUbo ubo = new GenericUbo(10);

            for (VulkanRenderObject object : VulkanManager.getInstance().entityRenderer.entities) {
                ubo.model[object.id] = CitrusMath.createTransformationMatrix(object.getPosition(), object.getRotX(), object.getRotY(), object.getRotZ(), object.getScale());
            }

            //Constant stuff generally goes here. later on view matrix will use the Math class one.
            ubo.view = CitrusMath.calcView(PlayerController.position, PlayerController.rotation);
            ubo.proj.perspective((float) java.lang.Math.toRadians(45), (float) Variables.swapChainExtent.width() / (float) Variables.swapChainExtent.height(), 0.1f, 1000.0f);
            ubo.proj.m11(ubo.proj.m11() * -1);

            PointerBuffer data = stack.mallocPointer(1);
            vkMapMemory(Variables.device, Variables.uniformBuffersMemory.get(currentImage), 0, GenericUbo.SIZEOF * VulkanManager.getInstance().entityRenderer.entities.size(), 0, data);
            {
                putUBOInMemory(data.getByteBuffer(0, GenericUbo.SIZEOF * VulkanManager.getInstance().entityRenderer.entities.size()), ubo);
            }
            vkUnmapMemory(Variables.device, Variables.uniformBuffersMemory.get(currentImage));
        }
    }

    public static class GenericUbo {
        public static final int MATRIX4F_SIZE = 16 * Float.BYTES;
        public static final int SIZEOF = (2 * MATRIX4F_SIZE) + MATRIX4F_SIZE * 10;

        public Matrix4f[] model;
        public Matrix4f view;
        public Matrix4f proj;

        public GenericUbo(int maxModelCount) {
            model = new Matrix4f[maxModelCount]; //TMP: 10 object cap? FIXME
            view = new Matrix4f();
            proj = new Matrix4f();
        }
    }
}
