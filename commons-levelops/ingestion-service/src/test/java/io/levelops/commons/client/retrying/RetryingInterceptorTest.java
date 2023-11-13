package io.levelops.commons.client.retrying;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

public class RetryingInterceptorTest {


    @Test
    public void test() {
        RetryingInterceptor interceptor = RetryingInterceptor.builder()
                .maxAttempts(12)
                .multiplierMs(100)
                .maxWait(1)
                .maxWaitTimeUnit(TimeUnit.SECONDS)
                .build();
        assertThat(interceptor.calculateExponentialBackoff(0)).isEqualTo(100);
        assertThat(interceptor.calculateExponentialBackoff(1)).isEqualTo(200);
        assertThat(interceptor.calculateExponentialBackoff(2)).isEqualTo(400);
        assertThat(interceptor.calculateExponentialBackoff(3)).isEqualTo(800);
        assertThat(interceptor.calculateExponentialBackoff(4)).isEqualTo(1000);
        assertThat(interceptor.calculateExponentialBackoff(5)).isEqualTo(1000);
    }

    private void testFailedRequest(OkHttpClient okHttpClient, int responseCode, boolean expectingRetry) throws IOException {
        MockWebServer server = new MockWebServer();
        for (int i = 0; i < 10; i++) {
            server.enqueue(new MockResponse().setResponseCode(responseCode));
        }
        server.start();

        HttpUrl baseUrl = server.url("/");
        Request request = new Request.Builder()
                .url(baseUrl)
                .build();
        try {
            var response = okHttpClient.newCall(request).execute();
            assertThat(response.code()).isEqualTo(responseCode);
        } catch (IOException e) {
            // ignore
            assertThat(expectingRetry).isTrue();
        }
        assertThat(server.getRequestCount()).isEqualTo(expectingRetry ? 2: 1);
    }

    @Test
    public void testDefaultRetryer() throws IOException {
        RetryingInterceptor interceptor = RetryingInterceptor.defaultRetyerBuilder()
                .maxAttempts(2)
                .maxWait(1)
                .build();
        OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
                .addInterceptor(interceptor)
                .build();
        List<Integer> excludedErrorCodes = List.of(400, 403, 404, 405);
        excludedErrorCodes.forEach(code -> {
            try {
                testFailedRequest(okHttpClient, code, false);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        testFailedRequest(okHttpClient, 500, true);
        testFailedRequest(okHttpClient, 429, true);
        testFailedRequest(okHttpClient, 501, true);
    }

//    @Test
//    public void name() throws IOException {
//        OkHttpClient client = new OkHttpClient().newBuilder()
//                .addInterceptor(RetryingInterceptor.buildDefaultRetryer())
//                .build();
//        Call call = client.newCall(new Request.Builder()
//                .url("http://127.0.0.1:8080")
//                .get()
//                .build());
//        Response response = call.execute();
//        System.out.println(response.body().string());
//    }
}