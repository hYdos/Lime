package io.github.hydos.lime.resource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public interface Resource {

    Identifier getIdentifier();

    InputStream openStream() throws IOException;

    default ByteBuffer readIntoBuffer(boolean direct) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        InputStream inputStream = openStream();

        byte[] buffer = new byte[7200000];
        int length;

        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        ByteBuffer byteBuffer = direct ? ByteBuffer.allocateDirect(outputStream.size()) : ByteBuffer.allocate(outputStream.size());
        byteBuffer.put(outputStream.toByteArray());
        byteBuffer.rewind();
        return byteBuffer;
    }
}
