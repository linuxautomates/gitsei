package io.levelops.commons.io;

import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class SynchronizedOutputStreamTest {

    @Test
    public void test() {
        // likely to throw ex without SynchronizedOutputStream
//        OutputStream outputStream = new RollingOutputStream(true, 10000);
        OutputStream outputStream = new SynchronizedOutputStream(new RollingOutputStream(false, 10000));

        IntStream.range(0, 1000).parallel()
                .mapToObj(i -> " " + i)
                .forEach(i -> {
            try {
                outputStream.write(i.getBytes());
                System.out.println(outputStream.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}