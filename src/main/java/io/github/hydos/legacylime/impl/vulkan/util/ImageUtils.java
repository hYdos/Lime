package io.github.hydos.legacylime.impl.vulkan.util;

import io.github.hydos.legacylime.impl.vulkan.Variables;
import io.github.hydos.lime.other.VulkanError;
import io.github.hydos.legacylime.impl.vulkan.lowlevel.VKMemoryUtils;
import io.github.hydos.legacylime.impl.vulkan.model.CommandBufferManager;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

import static org.lwjgl.system.MemoryStack.stackGet;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.*;

public class ImageUtils {

    public static void transitionImageLayout(long image, int format, int oldLayout, int newLayout, int mipLevels) {
        try (MemoryStack stack = stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.callocStack(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.oldLayout(oldLayout);
            barrier.newLayout(newLayout);
            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.image(image);

            barrier.subresourceRange().baseMipLevel(0);
            barrier.subresourceRange().levelCount(mipLevels);
            barrier.subresourceRange().baseArrayLayer(0);
            barrier.subresourceRange().layerCount(1);

            if (newLayout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {

                barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_DEPTH_BIT);

                if (Variables.hasStencilComponent(format)) {
                    barrier.subresourceRange().aspectMask(
                            barrier.subresourceRange().aspectMask() | VK_IMAGE_ASPECT_STENCIL_BIT);
                }

            } else {
                barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            }

            int sourceStage;
            int destinationStage;

            if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL) {

                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);

                sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                destinationStage = VK_PIPELINE_STAGE_TRANSFER_BIT;

            } else if (oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL) {

                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

                sourceStage = VK_PIPELINE_STAGE_TRANSFER_BIT;
                destinationStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;

            } else if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL) {

                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT);

                sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                destinationStage = VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT;

            } else if (oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL) {

                barrier.srcAccessMask(0);
                barrier.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

                sourceStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
                destinationStage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;

            } else {
                throw new IllegalArgumentException("Unsupported layout transition");
            }

            VkCommandBuffer commandBuffer = CommandBufferManager.beginSingleTimeCommands();

            vkCmdPipelineBarrier(commandBuffer,
                    sourceStage, destinationStage,
                    0,
                    null,
                    null,
                    barrier);

            CommandBufferManager.endSingleTimeCommands(commandBuffer);
        }
    }

    public static void copyBufferToImage(long buffer, long image, int width, int height) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBuffer commandBuffer = CommandBufferManager.beginSingleTimeCommands();

            VkBufferImageCopy.Buffer region = VkBufferImageCopy.callocStack(1, stack);
            region.bufferOffset(0);
            region.bufferRowLength(0);   // Tightly packed
            region.bufferImageHeight(0);  // Tightly packed
            region.imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            region.imageSubresource().mipLevel(0);
            region.imageSubresource().baseArrayLayer(0);
            region.imageSubresource().layerCount(1);
            region.imageOffset().set(0, 0, 0);
            region.imageExtent(VkExtent3D.callocStack(stack).set(width, height, 1));

            vkCmdCopyBufferToImage(commandBuffer, buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);

            CommandBufferManager.endSingleTimeCommands(commandBuffer);
        }
    }

    public static void createImageViews() {
        Variables.swapChainImageViews = new ArrayList<>(Variables.swapChainImages.size());
        for (long swapChainImage : Variables.swapChainImages) {
            Variables.swapChainImageViews.add(createImageView(swapChainImage, Variables.swapChainImageFormat, VK_IMAGE_ASPECT_COLOR_BIT, 1));
        }
    }

    public static void generateMipmaps(long image, int imageFormat, int width, int height, int mipLevels) {
        try (MemoryStack stack = stackPush()) {
            // Check if image format supports linear blitting
            VkFormatProperties formatProperties = VkFormatProperties.mallocStack(stack);
            vkGetPhysicalDeviceFormatProperties(Variables.physicalDevice, imageFormat, formatProperties);

            if ((formatProperties.optimalTilingFeatures() & VK_FORMAT_FEATURE_SAMPLED_IMAGE_FILTER_LINEAR_BIT) == 0) {
                throw new RuntimeException("Texture image format does not support linear blitting");
            }

            VkCommandBuffer commandBuffer = CommandBufferManager.beginSingleTimeCommands();

            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.callocStack(1, stack);
            barrier.sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER);
            barrier.image(image);
            barrier.srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);
            barrier.dstAccessMask(VK_QUEUE_FAMILY_IGNORED);
            barrier.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
            barrier.subresourceRange().baseArrayLayer(0);
            barrier.subresourceRange().layerCount(1);
            barrier.subresourceRange().levelCount(1);

            int mipWidth = width;
            int mipHeight = height;

            for (int i = 1; i < mipLevels; i++) {
                barrier.subresourceRange().baseMipLevel(i - 1);
                barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
                barrier.newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
                barrier.dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT);

                vkCmdPipelineBarrier(commandBuffer,
                        VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0,
                        null,
                        null,
                        barrier);

                VkImageBlit.Buffer blit = VkImageBlit.callocStack(1, stack);
                blit.srcOffsets(0).set(0, 0, 0);
                blit.srcOffsets(1).set(mipWidth, mipHeight, 1);
                blit.srcSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                blit.srcSubresource().mipLevel(i - 1);
                blit.srcSubresource().baseArrayLayer(0);
                blit.srcSubresource().layerCount(1);
                blit.dstOffsets(0).set(0, 0, 0);
                blit.dstOffsets(1).set(mipWidth > 1 ? mipWidth / 2 : 1, mipHeight > 1 ? mipHeight / 2 : 1, 1);
                blit.dstSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                blit.dstSubresource().mipLevel(i);
                blit.dstSubresource().baseArrayLayer(0);
                blit.dstSubresource().layerCount(1);

                vkCmdBlitImage(commandBuffer,
                        image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
                        image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                        blit,
                        VK_FILTER_LINEAR);

                barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
                barrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
                barrier.srcAccessMask(VK_ACCESS_TRANSFER_READ_BIT);
                barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

                vkCmdPipelineBarrier(commandBuffer,
                        VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0,
                        null,
                        null,
                        barrier);

                if (mipWidth > 1) {
                    mipWidth /= 2;
                }

                if (mipHeight > 1) {
                    mipHeight /= 2;
                }
            }

            barrier.subresourceRange().baseMipLevel(mipLevels - 1);
            barrier.oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);
            barrier.newLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);
            barrier.srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT);
            barrier.dstAccessMask(VK_ACCESS_SHADER_READ_BIT);

            vkCmdPipelineBarrier(commandBuffer,
                    VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0,
                    null,
                    null,
                    barrier);

            CommandBufferManager.endSingleTimeCommands(commandBuffer);
        }
    }


    public static long createImageView(long image, int format, int aspectFlags, int mipLevels) {
        try (MemoryStack stack = stackPush()) {
            VkImageViewCreateInfo viewInfo = VkImageViewCreateInfo.callocStack(stack);
            viewInfo.sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO);
            viewInfo.image(image);
            viewInfo.viewType(VK_IMAGE_VIEW_TYPE_2D);
            viewInfo.format(format);
            viewInfo.subresourceRange().aspectMask(aspectFlags);
            viewInfo.subresourceRange().baseMipLevel(0);
            viewInfo.subresourceRange().levelCount(mipLevels);
            viewInfo.subresourceRange().baseArrayLayer(0);
            viewInfo.subresourceRange().layerCount(1);

            LongBuffer pImageView = stack.mallocLong(1);
            VulkanError.failIfError(vkCreateImageView(Variables.device, viewInfo, null, pImageView));
            return pImageView.get(0);
        }
    }

    public static void createImage(int width, int height, int mipLevels, int numSamples, int format, int tiling, int usage, int memProperties, LongBuffer pTextureImage, LongBuffer pTextureImageMemory) {
        try (MemoryStack stack = stackPush()) {
            VkImageCreateInfo imageInfo = VkImageCreateInfo.callocStack(stack);
            imageInfo.sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO);
            imageInfo.imageType(VK_IMAGE_TYPE_2D);
            imageInfo.extent().width(width);
            imageInfo.extent().height(height);
            imageInfo.extent().depth(1);
            imageInfo.mipLevels(mipLevels);
            imageInfo.arrayLayers(1);
            imageInfo.format(format);
            imageInfo.tiling(tiling);
            imageInfo.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            imageInfo.usage(usage);
            imageInfo.samples(numSamples);
            imageInfo.sharingMode(VK_SHARING_MODE_EXCLUSIVE);
            VulkanError.failIfError(vkCreateImage(Variables.device, imageInfo, null, pTextureImage));

            VkMemoryRequirements memRequirements = VkMemoryRequirements.mallocStack(stack);
            vkGetImageMemoryRequirements(Variables.device, pTextureImage.get(0), memRequirements);

            VkMemoryAllocateInfo allocInfo = VkMemoryAllocateInfo.callocStack(stack);
            allocInfo.sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO);
            allocInfo.allocationSize(memRequirements.size());
            allocInfo.memoryTypeIndex(VKMemoryUtils.findMemoryType(memRequirements.memoryTypeBits(), memProperties));

            VulkanError.failIfError(vkAllocateMemory(Variables.device, allocInfo, null, pTextureImageMemory));
            vkBindImageMemory(Variables.device, pTextureImage.get(0), pTextureImageMemory.get(0), 0);
        }
    }


    public static void createColorResources() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pColorImage = stack.mallocLong(1);
            LongBuffer pColorImageMemory = stack.mallocLong(1);

            ImageUtils.createImage(Variables.swapChainExtent.width(), Variables.swapChainExtent.height(),
                    1,
                    Variables.msaaSamples,
                    Variables.swapChainImageFormat,
                    VK_IMAGE_TILING_OPTIMAL,
                    VK_IMAGE_USAGE_TRANSIENT_ATTACHMENT_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                    pColorImage,
                    pColorImageMemory);

            Variables.colorImage = pColorImage.get(0);
            Variables.colorImageMemory = pColorImageMemory.get(0);

            Variables.colorImageView = ImageUtils.createImageView(Variables.colorImage, Variables.swapChainImageFormat, VK_IMAGE_ASPECT_COLOR_BIT, 1);

            transitionImageLayout(Variables.colorImage, Variables.swapChainImageFormat, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, 1);
        }
    }

    public static void createDepthResources() {
        try (MemoryStack stack = stackPush()) {
            int depthFormat = findDepthFormat();

            LongBuffer pDepthImage = stack.mallocLong(1);
            LongBuffer pDepthImageMemory = stack.mallocLong(1);

            ImageUtils.createImage(
                    Variables.swapChainExtent.width(), Variables.swapChainExtent.height(),
                    1,
                    Variables.msaaSamples,
                    depthFormat,
                    VK_IMAGE_TILING_OPTIMAL,
                    VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
                    VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                    pDepthImage,
                    pDepthImageMemory);

            Variables.depthImage = pDepthImage.get(0);
            Variables.depthImageMemory = pDepthImageMemory.get(0);

            Variables.depthImageView = ImageUtils.createImageView(Variables.depthImage, depthFormat, VK_IMAGE_ASPECT_DEPTH_BIT, 1);

            // Explicitly transitioning the depth image
            transitionImageLayout(Variables.depthImage, depthFormat,
                    VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
                    1);

        }
    }

    public static int findSupportedFormat(IntBuffer formatCandidates, int tiling, int features) {
        try (MemoryStack stack = stackPush()) {
            VkFormatProperties props = VkFormatProperties.callocStack(stack);

            for (int i = 0; i < formatCandidates.capacity(); ++i) {

                int format = formatCandidates.get(i);

                vkGetPhysicalDeviceFormatProperties(Variables.physicalDevice, format, props);

                if (tiling == VK_IMAGE_TILING_LINEAR && (props.linearTilingFeatures() & features) == features) {
                    return format;
                } else if (tiling == VK_IMAGE_TILING_OPTIMAL && (props.optimalTilingFeatures() & features) == features) {
                    return format;
                }

            }
        }

        throw new RuntimeException("Failed to find supported format");
    }

    public static int findDepthFormat() {
        return findSupportedFormat(
                stackGet().ints(VK_FORMAT_D32_SFLOAT, VK_FORMAT_D32_SFLOAT_S8_UINT, VK_FORMAT_D24_UNORM_S8_UINT),
                VK_IMAGE_TILING_OPTIMAL,
                VK_FORMAT_FEATURE_DEPTH_STENCIL_ATTACHMENT_BIT);
    }
}
