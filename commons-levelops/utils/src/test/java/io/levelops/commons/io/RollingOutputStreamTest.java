package io.levelops.commons.io;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class RollingOutputStreamTest {

    String largeString;

    @Before
    public void setUp() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; ++i) {
            sb.append(i).append(" ");
            if (i % 100 == 0) {
                sb.append("\n");
            }
        }
        sb.append(".");
        largeString = sb.toString();
    }

    @Test
    public void test() throws IOException {
        RollingOutputStream outputStream = new RollingOutputStream(false,200, 16);

        outputStream.write(largeString.getBytes());

        String out = outputStream.toString();
        outputStream.close();
        System.out.println(out);
        assertThat(out).hasSize(200);
        assertThat(out).isEqualTo(largeString.substring(largeString.length() - 200));
    }

    @Test
    public void test2() throws IOException {
        RollingOutputStream outputStream = new RollingOutputStream(false, 200, largeString.length() * 2);

        outputStream.write(largeString.getBytes());

        String out = outputStream.toString();
        outputStream.close();
        System.out.println(out);
        assertThat(out).hasSize(200);
        assertThat(out).isEqualTo(largeString.substring(largeString.length() - 200));
    }

    @Test
    public void test3() throws IOException {
        RollingOutputStream outputStream = new RollingOutputStream(false, largeString.length() * 2, 512);

        outputStream.write(largeString.getBytes());

        String out = outputStream.toString();
        outputStream.close();
        assertThat(out).isEqualTo(largeString);
    }
}