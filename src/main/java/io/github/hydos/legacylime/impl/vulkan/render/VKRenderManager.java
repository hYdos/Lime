package io.github.hydos.legacylime.impl.vulkan.render;

import io.github.hydos.legacylime.core.render.Renderer;
import io.github.hydos.legacylime.impl.vulkan.Variables;
import io.github.hydos.legacylime.impl.vulkan.VulkanError;
import io.github.hydos.legacylime.impl.vulkan.util.ImageUtils;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

/**
 * used to manage all the renderers and shaders to go with them
 *
 * @author hydos
 */
public class VKRenderManager {

    private static VKRenderManager instance;
    public List<Renderer> renderers;

    public VKRenderManager() {
        instance = this;
        renderers = new ArrayList<>();
    }

    public static VKRenderManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("The Vulkan render manager is not setup");
        }
        return instance;
    }

    public static void createRenderPass() {

        try (MemoryStack stack = stackPush()) {

            VkAttachmentDescription.Buffer attachments = VkAttachmentDescription.callocStack(3, stack);
            VkAttachmentReference.Buffer attachmentRefs = VkAttachmentReference.callocStack(3, stack);

            // Color attachments

            // MSAA Image
            VkAttachmentDescription colorAttachment = attachments.get(0);
            colorAttachment.format(Variables.swapChainImageFormat);
            colorAttachment.samples(Variables.msaaSamples);
            colorAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            colorAttachment.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            colorAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            colorAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachment.finalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkAttachmentReference colorAttachmentRef = attachmentRefs.get(0);
            colorAttachmentRef.attachment(0);
            colorAttachmentRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            // Present Image
            VkAttachmentDescription colorAttachmentResolve = attachments.get(2);
            colorAttachmentResolve.format(Variables.swapChainImageFormat);
            colorAttachmentResolve.samples(VK_SAMPLE_COUNT_1_BIT);
            colorAttachmentResolve.loadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            colorAttachmentResolve.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
            colorAttachmentResolve.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            colorAttachmentResolve.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            colorAttachmentResolve.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            colorAttachmentResolve.finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            VkAttachmentReference colorAttachmentResolveRef = attachmentRefs.get(2);
            colorAttachmentResolveRef.attachment(2);
            colorAttachmentResolveRef.layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);


            // Depth-Stencil attachments

            VkAttachmentDescription depthAttachment = attachments.get(1);
            depthAttachment.format(ImageUtils.findDepthFormat());
            depthAttachment.samples(Variables.msaaSamples);
            depthAttachment.loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR);
            depthAttachment.storeOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            depthAttachment.stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE);
            depthAttachment.stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE);
            depthAttachment.initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            depthAttachment.finalLayout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            VkAttachmentReference depthAttachmentRef = attachmentRefs.get(1);
            depthAttachmentRef.attachment(1);
            depthAttachmentRef.layout(VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer subpass = VkSubpassDescription.callocStack(1, stack);
            subpass.pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS);
            subpass.colorAttachmentCount(1);
            subpass.pColorAttachments(VkAttachmentReference.callocStack(1, stack).put(0, colorAttachmentRef));
            subpass.pDepthStencilAttachment(depthAttachmentRef);
            subpass.pResolveAttachments(VkAttachmentReference.callocStack(1, stack).put(0, colorAttachmentResolveRef));

            VkSubpassDependency.Buffer dependency = VkSubpassDependency.callocStack(1, stack);
            dependency.srcSubpass(VK_SUBPASS_EXTERNAL);
            dependency.dstSubpass(0);
            dependency.srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            dependency.srcAccessMask(0);
            dependency.dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
            dependency.dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

            VkRenderPassCreateInfo renderPassInfo = VkRenderPassCreateInfo.callocStack(stack);
            renderPassInfo.sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO);
            renderPassInfo.pAttachments(attachments);
            renderPassInfo.pSubpasses(subpass);
            renderPassInfo.pDependencies(dependency);

            LongBuffer pRenderPass = stack.mallocLong(1);

            VulkanError.failIfError(vkCreateRenderPass(Variables.device, renderPassInfo, null, pRenderPass));

            Variables.renderPass = pRenderPass.get(0);
        }
    }

    public void addRenderer(Renderer renderer) {
        renderers.add(renderer);
    }

    public void render(MemoryStack stack, VkCommandBuffer commandBuffer, int index) {
        for (Renderer renderer : renderers) {
            renderer.VKRender(stack, commandBuffer, index);
        }
    }
}
