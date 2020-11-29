pub mod vertex_shader {
    vulkano_shaders::shader! {
        ty: "vertex",
        path: "src/res/vert.glsl"
    }
}

pub mod fragment_shader {
    vulkano_shaders::shader! {
        ty: "fragment",
        path: "src/res/frag.glsl"
    }
}