use vulkano::buffer::cpu_pool::CpuBufferPool;
use vulkano::buffer::{BufferUsage, CpuAccessibleBuffer};
use vulkano::command_buffer::{AutoCommandBufferBuilder, DynamicState};
use vulkano::descriptor::descriptor_set::PersistentDescriptorSet;
use vulkano::device::{Device, DeviceExtensions};
use vulkano::format::Format;
use vulkano::framebuffer::{Framebuffer, FramebufferAbstract, RenderPassAbstract, Subpass};
use vulkano::image::attachment::AttachmentImage;
use vulkano::image::{ImageUsage, SwapchainImage};
use vulkano::instance::{Instance, InstanceExtensions};
use vulkano::instance::PhysicalDevice;
use vulkano::pipeline::vertex::{TwoBuffersDefinition};
use vulkano::pipeline::viewport::Viewport;
use vulkano::pipeline::{GraphicsPipeline, GraphicsPipelineAbstract};
use vulkano::swapchain;
use vulkano::swapchain::{
    AcquireError, ColorSpace, FullscreenExclusive, PresentMode, SurfaceTransform, Swapchain,
    SwapchainCreationError,
};
use vulkano::sync;
use vulkano::sync::{FlushError, GpuFuture};

use vulkano_win::VkSurfaceBuild;
use winit::event::{Event, WindowEvent};
use winit::event_loop::{ControlFlow, EventLoop};
use winit::window::{Window, WindowBuilder};

use cgmath::{Matrix3, Matrix4, Point3, Rad, Vector3};

use std::iter;
use std::sync::Arc;
use std::time::Instant;

use crate::core::util::{VERTICES, NORMALS, INDICES};
use crate::core::constants::{DEBUG};
use crate::core::shaders::{vertex_shader, fragment_shader};

struct EngineInfo {
    vulkano_instance: Instance
}

fn create_devices<'a>(instance: Arc<Instance>) -> PhysicalDevice<'a> {
    let physical = PhysicalDevice::enumerate(&instance).next().unwrap();
    if DEBUG {
        println!(
            "Using device: {} (type: {:?})",
            physical.name(),
            physical.ty()
        );
    }
    return physical;
}

pub fn init_engine(){
    let required_extensions = vulkano_win::required_extensions();
    let vulkano_instance = Instance::new(None, &required_extensions, None).unwrap();
    let physical_device = create_devices(vulkano_instance);

    let event_loop = EventLoop::new();
    let surface = WindowBuilder::new()
        .build_vk_surface(&event_loop, vulkano_instance.clone())
        .unwrap();
    let dimensions: [u32; 2] = surface.window().inner_size().into();

    let queue_family = physical_device
        .queue_families()
        .find(|&q| q.supports_graphics() && surface.is_supported(q).unwrap_or(false))
        .unwrap();

    let device_ext = DeviceExtensions {
        khr_swapchain: true,
        ..DeviceExtensions::none()
    };

    let (device, mut queues) = Device::new(
        physical_device,
        physical_device.supported_features(),
        &device_ext,
        [(queue_family, 0.5)].iter().cloned(),
    )
        .unwrap();

    let queue = queues.next().unwrap();

    let (mut swapchain, images) = {
        let capabilities = surface.capabilities(physical_device).unwrap();
        let format = capabilities.supported_formats[0].0;
        let alpha = capabilities.supported_composite_alpha.iter().next().unwrap();

        Swapchain::new(
            device.clone(),
            surface.clone(),
            capabilities.min_image_count,
            format,
            dimensions,
            1,
            ImageUsage::color_attachment(),
            &queue,
            SurfaceTransform::Identity,
            alpha,
            PresentMode::Fifo,
            FullscreenExclusive::Default,
            true,
            ColorSpace::SrgbNonLinear,
        )
            .unwrap()
    };

    let vertices = VERTICES.iter().cloned();
    let vertex_buffer =
        CpuAccessibleBuffer::from_iter(device.clone(), BufferUsage::all(), false, vertices)
            .unwrap();

    let normals = NORMALS.iter().cloned();
    let normals_buffer =
        CpuAccessibleBuffer::from_iter(device.clone(), BufferUsage::all(), false, normals).unwrap();

    let indices = INDICES.iter().cloned();
    let index_buffer =
        CpuAccessibleBuffer::from_iter(device.clone(), BufferUsage::all(), false, indices).unwrap();

    let uniform_buffer = CpuBufferPool::<vertex_shader::ty::Data>::new(device.clone(), BufferUsage::all());

    let vertex_shader = vertex_shader::Shader::load(device.clone()).unwrap();
    let fragment_shader = fragment_shader::Shader::load(device.clone()).unwrap();

    let render_pass = Arc::new(
        vulkano::single_pass_renderpass!(device.clone(),
            attachments: {
                color: {
                    load: Clear,
                    store: Store,
                    format: swapchain.format(),
                    samples: 1,
                },
                depth: {
                    load: Clear,
                    store: DontCare,
                    format: Format::D16Unorm,
                    samples: 1,
                }
            },
            pass: {
                color: [color],
                depth_stencil: {depth}
            }
        )
            .unwrap(),
    );

    let (mut pipeline, mut framebuffers) = resize_window(device.clone(), &vertex_shader, &fragment_shader, &images, render_pass.clone());
    let mut recreate_swapchain = false;

    let mut previous_frame_end = Some(sync::now(device.clone()).boxed());
    let rotation_start = Instant::now();

    event_loop.run(move |event, _, control_flow| {
        match event {
            Event::WindowEvent {
                event: WindowEvent::CloseRequested,
                ..
            } => {
                *control_flow = ControlFlow::Exit;
            }
            Event::WindowEvent {
                event: WindowEvent::Resized(_),
                ..
            } => {
                recreate_swapchain = true;
            }
            Event::RedrawEventsCleared => {
                previous_frame_end.as_mut().unwrap().cleanup_finished();

                if recreate_swapchain {
                    let dimensions: [u32; 2] = surface.window().inner_size().into();
                    let (new_swapchain, new_images) =
                        match swapchain.recreate_with_dimensions(dimensions) {
                            Ok(r) => r,
                            Err(SwapchainCreationError::UnsupportedDimensions) => return,
                            Err(e) => panic!("Failed to recreate swapchain: {:?}", e),
                        };

                    swapchain = new_swapchain;
                    let (new_pipeline, new_framebuffers) = resize_window(
                        device.clone(),
                        &vertex_shader,
                        &fragment_shader,
                        &new_images,
                        render_pass.clone(),
                    );
                    pipeline = new_pipeline;
                    framebuffers = new_framebuffers;
                    recreate_swapchain = false;
                }

                let uniform_buffer_subbuffer = {
                    let elapsed = rotation_start.elapsed();
                    let rotation =
                        elapsed.as_secs() as f64 + elapsed.subsec_nanos() as f64 / 1_000_000_000.0;
                    let rotation = Matrix3::from_angle_y(Rad(rotation as f32));

                    // note: this teapot was meant for OpenGL where the origin is at the lower left
                    //       instead the origin is at the upper left in Vulkan, so we reverse the Y axis
                    let aspect_ratio = dimensions[0] as f32 / dimensions[1] as f32;
                    let proj = cgmath::perspective(
                        Rad(std::f32::consts::FRAC_PI_2),
                        aspect_ratio,
                        0.01,
                        100.0,
                    );
                    let view = Matrix4::look_at(
                        Point3::new(0.3, 0.3, 1.0),
                        Point3::new(0.0, 0.0, 0.0),
                        Vector3::new(0.0, -1.0, 0.0),
                    );
                    let scale = Matrix4::from_scale(0.01);

                    let uniform_data = vertex_shader::ty::Data {
                        world: Matrix4::from(rotation).into(),
                        view: (view * scale).into(),
                        proj: proj.into(),
                    };

                    uniform_buffer.next(uniform_data).unwrap()
                };

                let layout = pipeline.descriptor_set_layout(0).unwrap();
                let set = Arc::new(
                    PersistentDescriptorSet::start(layout.clone())
                        .add_buffer(uniform_buffer_subbuffer)
                        .unwrap()
                        .build()
                        .unwrap(),
                );

                let (image_num, suboptimal, acquire_future) =
                    match swapchain::acquire_next_image(swapchain.clone(), None) {
                        Ok(r) => r,
                        Err(AcquireError::OutOfDate) => {
                            recreate_swapchain = true;
                            return;
                        }
                        Err(e) => panic!("Failed to acquire next image: {:?}", e),
                    };

                if suboptimal {
                    recreate_swapchain = true;
                }

                let mut builder = AutoCommandBufferBuilder::primary_one_time_submit(
                    device.clone(),
                    queue.family(),
                )
                    .unwrap();
                builder
                    .begin_render_pass(
                        framebuffers[image_num].clone(),
                        false,
                        vec![[0.0, 0.0, 1.0, 1.0].into(), 1f32.into()],
                    )
                    .unwrap()
                    .draw_indexed(
                        pipeline.clone(),
                        &DynamicState::none(),
                        vec![vertex_buffer.clone(), normals_buffer.clone()],
                        index_buffer.clone(),
                        set.clone(),
                        (),
                    )
                    .unwrap()
                    .end_render_pass()
                    .unwrap();
                let command_buffer = builder.build().unwrap();

                let future = previous_frame_end
                    .take()
                    .unwrap()
                    .join(acquire_future)
                    .then_execute(queue.clone(), command_buffer)
                    .unwrap()
                    .then_swapchain_present(queue.clone(), swapchain.clone(), image_num)
                    .then_signal_fence_and_flush();

                match future {
                    Ok(future) => {
                        previous_frame_end = Some(future.boxed());
                    }
                    Err(FlushError::OutOfDate) => {
                        recreate_swapchain = true;
                        previous_frame_end = Some(sync::now(device.clone()).boxed());
                    }
                    Err(e) => {
                        println!("Failed to flush future: {:?}", e);
                        previous_frame_end = Some(sync::now(device.clone()).boxed());
                    }
                }
            }
            _ => (),
        }
    });
}

fn resize_window(
    device: Arc<Device>,
    vertex_shader: &vertex_shader::Shader,
    fragment_shader: &fragment_shader::Shader,
    images: &[Arc<SwapchainImage<Window>>],
    render_pass: Arc<dyn RenderPassAbstract + Send + Sync>,
) -> (
    Arc<dyn GraphicsPipelineAbstract + Send + Sync>,
    Vec<Arc<dyn FramebufferAbstract + Send + Sync>>,
) {
    let dimensions = images[0].dimensions();

    let depth_buffer =
        AttachmentImage::transient(device.clone(), dimensions, Format::D16Unorm).unwrap();

    let framebuffers = images
        .iter()
        .map(|image| {
            Arc::new(
                Framebuffer::start(render_pass.clone())
                    .add(image.clone())
                    .unwrap()
                    .add(depth_buffer.clone())
                    .unwrap()
                    .build()
                    .unwrap(),
            ) as Arc<dyn FramebufferAbstract + Send + Sync>
        })
        .collect::<Vec<_>>();

    let pipeline = Arc::new(
        GraphicsPipeline::start()
            .vertex_input(TwoBuffersDefinition::<crate::core::util::Vertex, crate::core::util::Normal>::new())
            .vertex_shader(vertex_shader.main_entry_point(), ())
            .triangle_list()
            .viewports_dynamic_scissors_irrelevant(1)
            .viewports(iter::once(Viewport {
                origin: [0.0, 0.0],
                dimensions: [dimensions[0] as f32, dimensions[1] as f32],
                depth_range: 0.0..1.0,
            }))
            .fragment_shader(fragment_shader.main_entry_point(), ())
            .depth_stencil_simple_depth()
            .render_pass(Subpass::from(render_pass.clone(), 0).unwrap())
            .build(device.clone())
            .unwrap(),
    );

    (pipeline, framebuffers)
}
