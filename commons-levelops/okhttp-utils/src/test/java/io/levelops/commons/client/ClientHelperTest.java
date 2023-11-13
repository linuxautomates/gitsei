package io.levelops.commons.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.exceptions.NoContentException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.function.Predicate;

@SuppressWarnings({"deprecation"})
public class ClientHelperTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final Predicate<Response> AZURE_FAILED_RESPONSE_PREDICATE = response -> !response.isSuccessful() || (response.code() == 203);
    private static OkHttpClient client = Mockito.mock(OkHttpClient.class);
    private static final Request REQUEST_1 = new Request.Builder().url("https://viraj.com/url1").get().build();
    private static final Request REQUEST_2 = new Request.Builder().url("https://viraj.com/url2").get().build();
    private static final Request REQUEST_3 = new Request.Builder().url("https://viraj.com/url3").get().build();
    private static final Request REQUEST_4 = new Request.Builder().url("https://viraj.com/url4").get().build();
    private static final Request REQUEST_5 = new Request.Builder().url("https://viraj.com/url5").get().build();

    private static Call CALL_1 = Mockito.mock(Call.class);
    private static Call CALL_2 = Mockito.mock(Call.class);
    private static Call CALL_3 = Mockito.mock(Call.class);
    private static Call CALL_4 = Mockito.mock(Call.class);
    private static Call CALL_5 = Mockito.mock(Call.class);

    private static Response RESPONSE_1 = new Response.Builder().request(REQUEST_1).protocol(Protocol.HTTP_2).code(200).message("").body(ResponseBody.create(MediaType.parse("application/json"),"{\"id\": \"1\"}")).build();
    private static Response RESPONSE_2 = new Response.Builder().request(REQUEST_2).protocol(Protocol.HTTP_2).code(200).message("").body(null).build();
    private static Response RESPONSE_3 = new Response.Builder().request(REQUEST_3).protocol(Protocol.HTTP_2).code(401).message("").body(ResponseBody.create(MediaType.parse("application/json"),"{\"id\": \"1\"}")).build();
    private static Response RESPONSE_4 = new Response.Builder().request(REQUEST_4).protocol(Protocol.HTTP_2).code(203).message("").body(ResponseBody.create(MediaType.parse("application/json"),"{\"id\": \"1\"}")).build();

    private static Response RESPONSE_5 = new Response.Builder().request(REQUEST_1).protocol(Protocol.HTTP_2).code(200).message("").body(ResponseBody.create(MediaType.parse("application/json"),"{\"id\": \"1\"}")).build();
    private static Response RESPONSE_6 = new Response.Builder().request(REQUEST_2).protocol(Protocol.HTTP_2).code(200).message("").body(null).build();
    private static Response RESPONSE_7 = new Response.Builder().request(REQUEST_3).protocol(Protocol.HTTP_2).code(401).message("").body(ResponseBody.create(MediaType.parse("application/json"),"{\"id\": \"1\"}")).build();
    private static Response RESPONSE_8 = new Response.Builder().request(REQUEST_4).protocol(Protocol.HTTP_2).code(203).message("").body(ResponseBody.create(MediaType.parse("application/json"),"{\"id\": \"1\"}")).build();
    private static Response RESPONSE_9 = new Response.Builder().request(REQUEST_5).protocol(Protocol.HTTP_2).code(204).message("").body(ResponseBody.create(MediaType.parse("application/json"),"")).build();

    @Before
    public void setup() throws IOException {
        Mockito.when(client.newCall(REQUEST_1)).thenReturn(CALL_1);
        Mockito.when(CALL_1.execute()).thenReturn(RESPONSE_1, RESPONSE_5);

        Mockito.when(client.newCall(REQUEST_2)).thenReturn(CALL_2);
        Mockito.when(CALL_2.execute()).thenReturn(RESPONSE_2, RESPONSE_6);

        Mockito.when(client.newCall(REQUEST_3)).thenReturn(CALL_3);
        Mockito.when(CALL_3.execute()).thenReturn(RESPONSE_3, RESPONSE_7);

        Mockito.when(client.newCall(REQUEST_4)).thenReturn(CALL_4);
        Mockito.when(CALL_4.execute()).thenReturn(RESPONSE_4, RESPONSE_8);

        Mockito.when(client.newCall(REQUEST_5)).thenReturn(CALL_5);
        Mockito.when(CALL_5.execute()).thenReturn(RESPONSE_5, RESPONSE_9);
    }

    @Test
    public void test() throws TestException {
        ClientHelper<TestException> clientHelper = ClientHelper.<TestException>builder()
                .client(client)
                .objectMapper(MAPPER)
                .exception(TestException.class)
                .build();

        ClientHelper.BodyAndHeaders<TestObj> page = clientHelper.executeAndParseWithHeaders(REQUEST_1, TestObj.class);
        Assert.assertNotNull(page);
        Assert.assertNotNull(page.getBody());
        Assert.assertEquals("1", page.getBody().getId());

        try{
            page = clientHelper.executeAndParseWithHeaders(REQUEST_2, TestObj.class);
            Assert.fail("TestException expected");
        } catch (TestException e) {
            Assert.assertNotNull(e);
            Assert.assertTrue(e.getMessage().contains("url2"));
        }

        try{
            page = clientHelper.executeAndParseWithHeaders(REQUEST_3, TestObj.class);
            Assert.fail("TestException expected");
        } catch (TestException e) {
            Assert.assertNotNull(e);
            Assert.assertTrue(e.getMessage().contains("url3"));
        }

        page = clientHelper.executeAndParseWithHeaders(REQUEST_4, TestObj.class);
        Assert.assertNotNull(page);
        Assert.assertNotNull(page.getBody());
        Assert.assertEquals("1", page.getBody().getId());

        page = clientHelper.executeAndParseWithHeaders(REQUEST_1, AZURE_FAILED_RESPONSE_PREDICATE, TestObj.class);
        Assert.assertNotNull(page);
        Assert.assertNotNull(page.getBody());
        Assert.assertEquals("1", page.getBody().getId());

        try{
            page = clientHelper.executeAndParseWithHeaders(REQUEST_2, AZURE_FAILED_RESPONSE_PREDICATE, TestObj.class);
            Assert.fail("TestException expected");
        } catch (TestException e) {
            Assert.assertNotNull(e);
            Assert.assertTrue(e.getMessage().contains("url2"));
        }

        try{
            page = clientHelper.executeAndParseWithHeaders(REQUEST_3, AZURE_FAILED_RESPONSE_PREDICATE, TestObj.class);
            Assert.fail("TestException expected");
        } catch (TestException e) {
            Assert.assertNotNull(e);
            Assert.assertTrue(e.getMessage().contains("url3"));
        }

        try{
            page = clientHelper.executeAndParseWithHeaders(REQUEST_4, AZURE_FAILED_RESPONSE_PREDICATE, TestObj.class);
            Assert.fail("TestException expected");
        } catch (TestException e) {
            Assert.assertNotNull(e);
            Assert.assertTrue(e.getMessage().contains("url4"));
        }

        try{
            page = clientHelper.executeAndParseWithHeaders(REQUEST_5, AZURE_FAILED_RESPONSE_PREDICATE, TestObj.class);
            Assert.fail("TestException expected");
        } catch (TestException e) {
            Assert.assertNotNull(e);
            Assert.assertTrue(ExceptionUtils.getRootCause(e) instanceof NoContentException);
            Assert.assertTrue(e.getMessage().contains("No content present."));
        }
    }


    public static class TestException extends Exception {

        public TestException() {
        }

        public TestException(String message) {
            super(message);
        }

        public TestException(String message, Throwable cause) {
            super(message, cause);
        }

        public TestException(Throwable cause) {
            super(cause);
        }
    }

    public static class TestObj {
        public String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}
