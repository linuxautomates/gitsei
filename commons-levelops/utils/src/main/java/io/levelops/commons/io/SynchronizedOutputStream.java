package io.levelops.commons.io;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.IOException;
import java.io.OutputStream;

public class SynchronizedOutputStream extends OutputStream {

    private final OutputStream delegate;

    public SynchronizedOutputStream(OutputStream delegate) {
        this.delegate = delegate;
    }

    @Override
    public void write(int b) throws IOException {
        synchronized (this) {
            delegate.write(b);
        }
    }

    @Override
    public String toString() {
        synchronized (this) {
            return delegate.toString();
        }
    }
}
