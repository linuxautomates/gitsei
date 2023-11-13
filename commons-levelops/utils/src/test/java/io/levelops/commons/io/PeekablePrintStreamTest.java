package io.levelops.commons.io;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

public class PeekablePrintStreamTest {

    @Test
    public void test() throws IOException {

        ByteArrayOutputStream underlying = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(underlying);
        PeekablePrintStream stream = PeekablePrintStream.build(printStream, true, 3);

        stream.write("abcdefghijklmnopqrstuvwxyz".getBytes());

        assertThat(underlying.toString()).isEqualTo("abcdefghijklmnopqrstuvwxyz");
        assertThat(stream.peek()).isEqualTo("xyz");
    }
}