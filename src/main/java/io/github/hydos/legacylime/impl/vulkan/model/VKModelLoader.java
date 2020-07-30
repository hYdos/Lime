package io.github.hydos.legacylime.impl.vulkan.model;

import io.github.hydos.lime.resource.Resource;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class VKModelLoader {

    private static AIFileIO createResourceIO(ByteBuffer data) {
        AIFileIO fileIo = AIFileIO.create();

        fileIo.set((pFileIO, fileName, openMode) -> {
            AIFile file = AIFile.create();

            file.ReadProc((pFile, pBuffer, size, count) -> {
                long max = Math.min(data.remaining(), size * count);
                MemoryUtil.memCopy(MemoryUtil.memAddress(data), pBuffer, max);
                return max;
            });

            file.SeekProc((pFile, offset, origin) -> {
                if (origin == Assimp.aiOrigin_CUR) {
                    data.position(data.position() + (int) offset);
                } else if (origin == Assimp.aiOrigin_SET) {
                    data.position((int) offset);
                } else if (origin == Assimp.aiOrigin_END) {
                    data.position(data.limit() + (int) offset);
                }

                return 0;
            });

            file.FileSizeProc(pFile -> data.limit());

            return file.address();
        }, (pFileIO, pFile) -> {
        }, MemoryUtil.NULL);

        return fileIo;
    }

    public static VKMesh loadModel(Resource file, int flags) throws IOException {
        try (AIScene scene = Assimp.aiImportFileEx(file.getIdentifier().toString(), flags, createResourceIO(file.readIntoBuffer(true)))) {
            if (scene == null || scene.mRootNode() == null) {
                throw new RuntimeException("Could not load model: " + Assimp.aiGetErrorString());
            }

            VKMesh model = new VKMesh();
            processNode(requireNonNull(scene.mRootNode()), scene, model);
            return model;
        }
    }

    private static void processNode(AINode node, AIScene scene, VKMesh model) {
        if (node.mMeshes() != null) {
            processNodeMeshes(scene, node, model);
        }

        if (node.mChildren() != null) {
            PointerBuffer children = node.mChildren();
            for (int i = 0; i < node.mNumChildren(); i++) {
                processNode(AINode.create(children.get(i)), scene, model);
            }
        }
    }

    private static void processNodeMeshes(AIScene scene, AINode node, VKMesh model) {
        PointerBuffer pMeshes = scene.mMeshes();
        IntBuffer meshIndices = node.mMeshes();

        for (int i = 0; i < requireNonNull(meshIndices).capacity(); i++) {
            AIMesh mesh = AIMesh.create(pMeshes.get(meshIndices.get(i)));
            processMesh(mesh, model);
        }
    }

    private static void processMesh(AIMesh mesh, VKMesh model) {
        processPositions(mesh, model.positions);
        processTexCoords(mesh, model.texCoords);
        processIndices(mesh, model.indices);
    }

    private static void processPositions(AIMesh mesh, List<Vector3fc> positions) {
        AIVector3D.Buffer vertices = requireNonNull(mesh.mVertices());

        for (int i = 0; i < vertices.capacity(); i++) {
            AIVector3D position = vertices.get(i);
            positions.add(new Vector3f(position.x(), position.y(), position.z()));
        }
    }

    private static void processTexCoords(AIMesh mesh, List<Vector2fc> texCoords) {
        AIVector3D.Buffer aiTexCoords = requireNonNull(mesh.mTextureCoords(0));

        for (int i = 0; i < aiTexCoords.capacity(); i++) {
            final AIVector3D coords = aiTexCoords.get(i);
            texCoords.add(new Vector2f(coords.x(), coords.y()));
        }
    }

    private static void processIndices(AIMesh mesh, List<Integer> indices) {
        AIFace.Buffer aiFaces = mesh.mFaces();

        for (int i = 0; i < mesh.mNumFaces(); i++) {
            AIFace face = aiFaces.get(i);
            IntBuffer pIndices = face.mIndices();
            for (int j = 0; j < face.mNumIndices(); j++) {
                indices.add(pIndices.get(j));
            }
        }
    }

    public static class VKMesh extends SimpleVKMesh{
        public final List<Vector3fc> positions;
        public final List<Vector2fc> texCoords;
        public final List<Integer> indices;

        public VKMesh() {
            this.positions = new ArrayList<>();
            this.texCoords = new ArrayList<>();
            this.indices = new ArrayList<>();
        }
    }

    public static class SimpleVKMesh {
        public final List<Vector2fc> positions;
        public final List<Vector2fc> texCoords;

        public SimpleVKMesh() {
            this.positions = new ArrayList<>();
            this.texCoords = new ArrayList<>();
        }
    }
}
