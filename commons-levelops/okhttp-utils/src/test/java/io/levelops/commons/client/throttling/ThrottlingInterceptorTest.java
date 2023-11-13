package io.levelops.commons.client.throttling;

import okhttp3.Interceptor;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class ThrottlingInterceptorTest {

    @Test
    public void test() throws IOException {
        Interceptor.Chain chain = Mockito.mock(Interceptor.Chain.class);

        ThrottlingInterceptor throttlingInterceptor = new ThrottlingInterceptor(1);
        long t0 = System.currentTimeMillis();
        throttlingInterceptor.intercept(chain);
        throttlingInterceptor.intercept(chain);
        long t1 = System.currentTimeMillis();
        System.out.println(t1-t0);
        assertThat(t1-t0).isGreaterThanOrEqualTo(1000); // takes at least a second
    }
}