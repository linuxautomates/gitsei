package io.levelops.commons.client.exceptions;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpExceptionTest {

    @Test
    public void test() {
        HttpException exception = new HttpException(404, "POST", "http://url", "body", Map.of("H", List.of("a", "b")), null);
        assertThat(exception.getMessage()).isEqualTo("Request failed with error code [404] from url=http://url (POST) with body='body' and headers={\"H\":[\"a\",\"b\"]}");

        exception = new HttpException(0);
        assertThat(exception.getMessage()).isEqualTo("Request failed with error code [0]");
    }
}