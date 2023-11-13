package io.levelops.bullseye_converter_clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.bullseye_converter_commons.models.ConversionRequest;
import io.levelops.bullseye_converter_commons.models.Result;
import io.levelops.commons.jackson.DefaultObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.UUID;

public class BullseyeConverterClientIntegrationTest {
    private final ObjectMapper objectMapper = DefaultObjectMapper.get();
    private BullseyeConverterClient client;

    @Before
    public void setUp() throws Exception {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.level(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(logging).build();
        client = new BullseyeConverterClient(okHttpClient, objectMapper, "http://localhost:8080");
    }

    @Test
    public void testValid() throws BullseyeConverterClient.BullseyeConverterClientException, IOException, URISyntaxException {
        ConversionRequest conversionRequest = ConversionRequest.builder()
                .customer("test")
                .referenceId(UUID.randomUUID().toString()).jobRunId(UUID.randomUUID())
                .fileName("abc.cov")
                .build();
        File testFile =  new File(this.getClass().getClassLoader().getResource("Valid.cov").toURI());
        Result r = client.convertCovToXml(conversionRequest, testFile);
        System.out.println(r);
    }

    @Test
    public void testInValid() throws BullseyeConverterClient.BullseyeConverterClientException, IOException, URISyntaxException {
        ConversionRequest conversionRequest = ConversionRequest.builder()
                .customer("test")
                .referenceId(UUID.randomUUID().toString()).jobRunId(UUID.randomUUID())
                .fileName("abc.cov")
                .build();
        File testFile =  new File(this.getClass().getClassLoader().getResource("Invalid.cov").toURI());
        Result r = client.convertCovToXml(conversionRequest, testFile);
        System.out.println(r);
    }

}