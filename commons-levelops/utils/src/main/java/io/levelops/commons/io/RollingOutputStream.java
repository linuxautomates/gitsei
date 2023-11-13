package io.levelops.commons.io;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;

public class RollingOutputStream extends OutputStream {

    private static final int DEFAULT_BLOCK_SIZE = 512;
    private final LinkedList<byte[]> blocks = new LinkedList<>();
    private final int blockSize;
    private final boolean synchronize;
    private final int maxSizeInBytes;
    private byte[] buf;
    private int count = 0;

    public RollingOutputStream(boolean synchronize, int maxSizeInBytes) {
        this(synchronize, maxSizeInBytes, DEFAULT_BLOCK_SIZE);
    }

    public RollingOutputStream(boolean synchronize, int maxSizeInBytes, int blockSize) {
        this.synchronize = synchronize;
        this.maxSizeInBytes = maxSizeInBytes;
        this.blockSize = blockSize;
        this.buf = new byte[blockSize];
    }

    @Override
    public void write(int b) throws IOException {
        if (synchronize) {
            synchronized (this) {
                writeUnsynchronized(b);
            }
        } else {
            writeUnsynchronized(b);
        }
    }

    private void writeUnsynchronized(int b) {
        if (count >= blockSize) {
            if (blocks.size() * blockSize > maxSizeInBytes) {
                blocks.removeFirst();
            }
            blocks.addLast(buf);
            buf = new byte[blockSize];
            count = 0;
        }
        buf[count] = (byte) b;
        count += 1;
    }

    public byte[] toByteArray() {
        if (synchronize) {
            synchronized (this) {
                return toByteArrayUnsynchronized();
            }
        } else {
            return toByteArrayUnsynchronized();
        }
    }

    private byte[] toByteArrayUnsynchronized() {
        int totalAvailableSize = blocks.size() * blockSize + count;
        byte[] bytes = new byte[Math.min(totalAvailableSize, maxSizeInBytes)];
        int startOffset = (maxSizeInBytes >= totalAvailableSize) ? 0 : totalAvailableSize - maxSizeInBytes;
        int index = 0;
        for (byte[] block : blocks) {
            if (index + blockSize < startOffset) {
                index += blockSize;
                continue;
            }
            for (int i = 0; i < blockSize; ++i) {
                if (index >= startOffset) {
                    bytes[index - startOffset] = block[i];
                }
                index++;
            }
        }
        for (int i = 0; i < count; ++i) {
            if (index >= startOffset) {
                bytes[index - startOffset] = buf[i];
            }
            index++;
        }
        return bytes;
    }

    public String toString() {
        return new String(toByteArray());
    }

}
