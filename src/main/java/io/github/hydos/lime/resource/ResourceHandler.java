package io.github.hydos.lime.resource;

public class ResourceHandler {

    public static final ResourceManager GLOBAL_RESOURCE_MANAGER = new ClassLoaderResourceManager(ClassLoader.getSystemClassLoader(), "assets/");
}
