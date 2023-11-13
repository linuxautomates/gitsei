package io.levelops.aggregations_shared.utils;

import io.levelops.commons.utils.ResourceUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class CompressionUtilTest {
    @Test
    public void testCompression() throws IOException {
        testCompressionInternal("asdkjflsakjdfl;kjsadfl;kjasf");
        testCompressionInternal("hello how are you");
        testCompressionInternal("laksjdfralksnjdafl;ksadflkjsad;lkfhsakldjhf;alskdjfl;akshdfbkljasnkldfhasdklfjhalskdjfgbhlkjdfghkljsdhfgk;ldfjg;lkjlkfjshadflkjbsakldfj");

        // This goes from 2MB to like 0.07MB!
        String largePayload = ResourceUtils.getResourceAsString("large_payload.txt");
        testCompressionInternal(largePayload);
    }

    private void testCompressionInternal(String str) throws IOException {
        var compressed = CompressionUtils.compress(str);
        var decompressed = CompressionUtils.decompress(compressed);
        var initialSize = str.getBytes(StandardCharsets.UTF_8).length;
        var compressedSize = compressed.length;
        System.out.println("Initial size: " + initialSize + ", compressed size: " + compressedSize);
        assertThat(decompressed).isEqualTo(str);
    }
}
