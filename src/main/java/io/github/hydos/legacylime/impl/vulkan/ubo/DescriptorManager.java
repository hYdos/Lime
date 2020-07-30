package io.github.hydos.legacylime.impl.vulkan.ubo;

import io.github.hydos.legacylime.impl.vulkan.Variables;
import io.github.hydos.legacylime.impl.vulkan.VulkanError;
import io.github.hydos.legacylime.impl.vulkan.texture.CompiledTexture;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class DescriptorManager {

    public static long descriptorPool;
    public static long descriptorSetLayout;

    public static Map<CompiledTexture, List<Long>> descriptorSetsMap = new HashMap<>();

    public static void createDescriptorSets(List<CompiledTexture> textures) {
        try (MemoryStack stack = stackPush()) {
            for (CompiledTexture compiledTexture : textures) {
                LongBuffer layouts = stack.mallocLong(Variables.swapChainImages.size());
                for (int i = 0; i < layouts.capacity(); i++) {
                    layouts.put(i, descriptorSetLayout);
                }

                VkDescriptorSetAllocateInfo allocInfo = VkDescriptorSetAllocateInfo.callocStack(stack);
                allocInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO);
                allocInfo.descriptorPool(descriptorPool);
                allocInfo.pSetLayouts(layouts);

                LongBuffer pDescriptorSets = stack.mallocLong(Variables.swapChainImages.size());

                VulkanError.failIfError(vkAllocateDescriptorSets(Variables.device, allocInfo, pDescriptorSets));
                List<Long> descriptorSets = new ArrayList<>(pDescriptorSets.capacity());

                VkDescriptorBufferInfo.Buffer bufferInfo = VkDescriptorBufferInfo.callocStack(1, stack);
                bufferInfo.offset(0);
                bufferInfo.range(UboManager.GenericUbo.SIZEOF);

                VkDescriptorImageInfo.Buffer imageInfo = VkDescriptorImageInfo.callocStack(1, stack);
                imageInfo.imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                imageInfo.imageView(compiledTexture.textureImageView);
                imageInfo.sampler(compiledTexture.textureSampler);

                VkWriteDescriptorSet.Buffer descriptorWrites = VkWriteDescriptorSet.callocStack(2, stack);
                VkWriteDescriptorSet uboDescriptorWrite = descriptorWrites.get(0);
                uboDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                uboDescriptorWrite.dstBinding(0);
                uboDescriptorWrite.dstArrayElement(0);
                uboDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
                uboDescriptorWrite.descriptorCount(1);
                uboDescriptorWrite.pBufferInfo(bufferInfo);

                VkWriteDescriptorSet samplerDescriptorWrite = descriptorWrites.get(1);
                samplerDescriptorWrite.sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET);
                samplerDescriptorWrite.dstBinding(1);
                samplerDescriptorWrite.dstArrayElement(0);
                samplerDescriptorWrite.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
                samplerDescriptorWrite.descriptorCount(1);
                samplerDescriptorWrite.pImageInfo(imageInfo);

                for (int i = 0; i < pDescriptorSets.capacity(); i++) {

                    long descriptorSet = pDescriptorSets.get(i);

                    bufferInfo.buffer(Variables.uniformBuffers.get(i));

                    uboDescriptorWrite.dstSet(descriptorSet);
                    samplerDescriptorWrite.dstSet(descriptorSet);

                    vkUpdateDescriptorSets(Variables.device, descriptorWrites, null);

                    descriptorSets.add(descriptorSet);
                }

                descriptorSetsMap.put(compiledTexture, descriptorSets);
            }
        }
    }


    public static void createUBODescriptorSetLayout() {

        try (MemoryStack stack = stackPush()) {

            VkDescriptorSetLayoutBinding.Buffer bindings = VkDescriptorSetLayoutBinding.callocStack(2, stack); //create binding buffer on stack

            VkDescriptorSetLayoutBinding uboLayoutBinding = bindings.get(0);
            uboLayoutBinding.binding(0); //set the binding number
            uboLayoutBinding.descriptorCount(1);
            uboLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            uboLayoutBinding.pImmutableSamplers(null);
            uboLayoutBinding.stageFlags(VK_SHADER_STAGE_VERTEX_BIT);

            VkDescriptorSetLayoutBinding samplerLayoutBinding = bindings.get(1);
            samplerLayoutBinding.binding(1);
            samplerLayoutBinding.descriptorCount(1);
            samplerLayoutBinding.descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            samplerLayoutBinding.pImmutableSamplers(null);
            samplerLayoutBinding.stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

            VkDescriptorSetLayoutCreateInfo layoutInfo = VkDescriptorSetLayoutCreateInfo.callocStack(stack);
            layoutInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO);
            layoutInfo.pBindings(bindings);

            LongBuffer pDescriptorSetLayout = stack.mallocLong(1);

            VulkanError.failIfError(vkCreateDescriptorSetLayout(Variables.device, layoutInfo, null, pDescriptorSetLayout));
            descriptorSetLayout = pDescriptorSetLayout.get(0);
        }
    }

    public static void createDescriptorPool() {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorPoolSize.Buffer poolSizes = VkDescriptorPoolSize.callocStack(2, stack);

            VkDescriptorPoolSize uniformBufferPoolSize = poolSizes.get(0);
            uniformBufferPoolSize.type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER);
            uniformBufferPoolSize.descriptorCount(Variables.swapChainImages.size());

            VkDescriptorPoolSize textureSamplerPoolSize = poolSizes.get(1);
            textureSamplerPoolSize.type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER);
            textureSamplerPoolSize.descriptorCount(Variables.swapChainImages.size());

            VkDescriptorPoolCreateInfo poolInfo = VkDescriptorPoolCreateInfo.callocStack(stack);
            poolInfo.sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO);
            poolInfo.pPoolSizes(poolSizes);
            poolInfo.maxSets(Variables.MAX_DESCRIPTOR_COUNT);
            LongBuffer pDescriptorPool = stack.mallocLong(1);

            VulkanError.failIfError(vkCreateDescriptorPool(Variables.device, poolInfo, null, pDescriptorPool));
            descriptorPool = pDescriptorPool.get(0);
        }
    }
}
