package io.github.hydos.legacylime.impl.vulkan.shaders;

import io.github.hydos.legacylime.impl.vulkan.Variables;
import io.github.hydos.lime.VulkanError;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;

/**
 * will be used in the future to manage multiple shaders
 *
 * @author hydos
 */
public class VKShaderManager {

    public static long createShaderModule(ByteBuffer spirvCode) {
        try (MemoryStack stack = stackPush()) {
            VkShaderModuleCreateInfo createInfo = VkShaderModuleCreateInfo.callocStack(stack);

            createInfo.sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO);
            createInfo.pCode(spirvCode);

            LongBuffer pShaderModule = stack.mallocLong(1);

            VulkanError.failIfError(VK10.vkCreateShaderModule(Variables.device, createInfo, null, pShaderModule));

            return pShaderModule.get(0);
        }
    }
}
