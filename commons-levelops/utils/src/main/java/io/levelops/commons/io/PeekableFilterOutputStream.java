package io.levelops.commons.io;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class PeekableFilterOutputStream extends FilterOutputStream {

    RollingOutputStream peekableStream;

    public PeekableFilterOutputStream(OutputStream delegate, int maxPeekableSizeBytes) {
        this(delegate, new RollingOutputStream(true, maxPeekableSizeBytes));
    }

    public PeekableFilterOutputStream(OutputStream delegate, RollingOutputStream peekableStream) {
        super(delegate);
        this.peekableStream = peekableStream;
    }

    public void write(int b) throws IOException {
        super.write(b);
        peekableStream.write((char) b);
    }

    public String peek() {
        return peekableStream.toString();
    }

    public byte[] peekBytes() {
        return peekableStream.toByteArray();
    }

}