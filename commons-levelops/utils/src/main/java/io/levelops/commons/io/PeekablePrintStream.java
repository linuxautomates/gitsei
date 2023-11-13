package io.levelops.commons.io;

import java.io.PrintStream;

public class PeekablePrintStream extends PrintStream {

    public PeekablePrintStream(PeekableFilterOutputStream outputStream, boolean autoFlush) {
        super(outputStream, autoFlush);
    }

    private PeekableFilterOutputStream getPeekingFilterOutputStream() {
        return ((PeekableFilterOutputStream) this.out);
    }

    public String peek() {
        return getPeekingFilterOutputStream().peek();
    }

    public byte[] peekBytes() {
        return getPeekingFilterOutputStream().peekBytes();
    }

    public static PeekablePrintStream build(PrintStream toPeek, boolean autoFlush, int maxPeekableSizeBytes) {
        return new PeekablePrintStream(new PeekableFilterOutputStream(toPeek, maxPeekableSizeBytes), autoFlush);
    }

}
