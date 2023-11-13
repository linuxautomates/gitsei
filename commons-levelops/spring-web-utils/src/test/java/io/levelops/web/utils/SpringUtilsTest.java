package io.levelops.web.utils;

import io.levelops.commons.client.exceptions.HttpException;
import io.levelops.web.util.SpringUtils;

import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SpringUtilsTest {

    @Test
    public void test(){
        DeferredResult<ResponseEntity<Map<String, String>>> responseEntity = SpringUtils.deferResponse(() -> ResponseEntity.ok(Map.of("id", "test")));

        assertThat(responseEntity).isNotNull();
    }

    @Test
    public void testExMessage() throws InterruptedException {
        var r = SpringUtils.deferResponse(() -> {
            throw new HttpException(400, "url", "{\"timestamp\":1594156819015,\"status\":400,\"error\":\"Bad Request\",\"message\":\"\",\"path\":\"/internal/v1/tenants/foo/qtemplates/1e1208ed-62ff-458e-a54c-8c671d00e85d\"}");
        });
        var r2 = SpringUtils.deferResponse(() -> {
            throw new HttpException(400, "url", "{\"timestamp\":1594156819015,\"status\":400,\"error\":\"Bad Request\",\"message\":\"some nice message\",\"path\":\"/internal/v1/tenants/foo/qtemplates/1e1208ed-62ff-458e-a54c-8c671d00e85d\"}");
        });
        var r3 = SpringUtils.deferResponse(() -> {
            throw new HttpException(400, "url", "{ jlksdjfkldsjf ;KLJlks : ;ljsdfldsf: lkjsdlf}");
        });
        r.onCompletion(() -> assertThat(((ResponseStatusException) r.getResult()).getReason()).isEqualTo(""));
        r2.onCompletion(() -> assertThat(((ResponseStatusException) r2.getResult()).getReason()).isEqualTo("some nice message"));
        r3.onCompletion(() -> assertThat(((ResponseStatusException) r3.getResult()).getReason()).startsWith("Request failed with error code"));
    }
}