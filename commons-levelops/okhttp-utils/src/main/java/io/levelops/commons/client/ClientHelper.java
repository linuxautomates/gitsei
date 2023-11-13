package io.levelops.commons.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.exceptions.HttpException;
import io.levelops.commons.client.exceptions.NoContentException;
import io.levelops.commons.exceptions.CallableWithException;
import io.levelops.commons.exceptions.ExceptionSuppliers.ExceptionWithCauseAndMessageSupplier;
import io.levelops.commons.exceptions.ExceptionSuppliers.ExceptionWithCauseSupplier;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.functional.UncheckedCloseable;
import io.levelops.commons.io.DeleteOnCloseFileInputStream;
import io.levelops.commons.models.FileUpload;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.MultipartReader;
import okhttp3.MultipartReader.Part;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.util.Strings;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

@AllArgsConstructor
@Getter
@Log4j2
public class ClientHelper<E extends Exception> {
    private static final Predicate<Response> FAILED_RESPONSE_DEFAULT_PREDICATE = response -> !response.isSuccessful();

    private static final int ERROR_SNIPPET_MAX_SIZE = 1000;
    private final OkHttpClient client;
    private final ObjectMapper objectMapper;
    private final ExceptionWithCauseSupplier<E> exceptionWithCauseSupplier;
    private final ExceptionWithCauseAndMessageSupplier<E> exceptionWithCauseAndMessageSupplier;

    @Builder
    public ClientHelper(OkHttpClient client, ObjectMapper objectMapper, Class<E> exception) {
        this.client = client;
        this.objectMapper = objectMapper;
        this.exceptionWithCauseSupplier = ExceptionWithCauseSupplier.forClass(exception);
        this.exceptionWithCauseAndMessageSupplier = ExceptionWithCauseAndMessageSupplier.forClass(exception);
    }

    public <T> T executeAndParse(Request request, JavaType javaType) throws E {
        String rawResponse = executeRequest(request);
        return parseResponse(rawResponse, javaType);
    }

    public <T> BodyAndHeaders<T> executeAndParseWithHeaders(Request request, JavaType javaType) throws E {
        BodyAndHeaders<String> response = executeRequestWithHeadersInternal(request, null);
        return BodyAndHeaders.<T>builder()
                .body(parseResponse(response.getBody(), javaType))
                .headers(response.getHeaders())
                .build();
    }

    public <T> T executeAndParse(Request request, Class<T> clazz) throws E {
        String rawResponse = executeRequest(request);
        return parseResponse(rawResponse, clazz);
    }

    public <T> BodyAndHeaders<T> executeAndParseWithHeaders(Request request, Class<T> clazz) throws E {
        return executeAndParseWithHeaders(request, null, clazz);
    }

    public <T> BodyAndHeaders<T> executeAndParseWithHeaders(Request request, Predicate<Response> failedResponsePredicate, Class<T> clazz) throws E {
        BodyAndHeaders<String> response = executeRequestWithHeadersInternal(request, failedResponsePredicate);
        return BodyAndHeaders.<T>builder()
                .body(parseResponse(response.getBody(), clazz))
                .headers(response.getHeaders())
                .build();
    }

    /**
     * Convinience method that will read a multiform data response and will create an object with the corresponding part.
     *
     * @param <T>     The type expected
     * @param request
     * @param clazz   Class of the expected type
     * @return The object obtained from interpreting each part of the multipar form data as a property in the object.
     * @throws E
     */
    public <T> T executeRequestForObjectFromMultiPart(Request request, Class<T> clazz) throws E {
        Map<String, Object> data = executeRequestForMultiPart(request);
        return objectMapper.convertValue(data, clazz);
    }

    /**
     * Convinience method to get a FileUpload object from a MultiPart response.
     *
     * @param request
     * @return
     * @throws E
     */
    public FileUpload executeRequestForFileUpload(Request request) throws E {
        return executeRequestForObjectFromMultiPart(request, FileUpload.class);
    }

    /**
     * Coverts a binary response into an InputStream. <br />
     *
     * @param request
     * @return An InputStream that can be used to read the bytes from the binary response
     */
    public InputStream executeRequestForBinary(Request request) throws E {
        File tmpFile = null;
        OutputStream out = null;
        boolean deleteFile = false;
        try {
            tmpFile = Files.createTempFile("client_helper_", ".out").toFile();
            log.info("Temp file: {}", tmpFile);
            out = new FileOutputStream(tmpFile);
            executeRequestForBinary(request, out);
            return new DeleteOnCloseFileInputStream(tmpFile);
        } catch (IOException e) {
            deleteFile = true;
            throw exceptionWithCauseSupplier.build(e);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.error("Unable to close the output stream used to write the temp file: {}", tmpFile, e);
                }
            }
            if (tmpFile != null && deleteFile) {
                tmpFile.delete();
            }
        }
    }

    /**
     * Executes a request that expects a binary response and writes the bytes into the provided OutputStream. <br />
     *
     * @param request the request to be executed
     * @param out     the OutputStream to write the bytes to
     */
    public void executeRequestForBinary(Request request, OutputStream out) throws E {
        Call call = client.newCall(request);
        ResponseBody body = null;
        MultipartReader reader = null;
        Response response = null;
        try {
            response = call.execute();
            body = processResponse(request.url().toString(), null, response);
            IOUtils.copy(body.byteStream(), out);
        } catch (IOException e) {
            throw exceptionWithCauseSupplier.build(e);
        } finally {
            if (body != null) {
                body.close();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.error("Failed to close the MultiPartReader. url={}", request.url(), e);
                }
            }
            if (response != null) {
                response.close();
            }

        }
    }

    /**
     * Coverts a multipart response into a map of the parts. <br />
     * Each key in the map corresponds to the name of each part in the response.
     *
     * @param request
     * @return A map where the keys are the names of a multipart response and the value are the contents of the part.
     */
    public Map<String, Object> executeRequestForMultiPart(Request request) throws E {
        Call call = client.newCall(request);
        ResponseBody body = null;
        MultipartReader reader = null;
        Response response = null;
        FileOutputStream out = null;
        Path tmpFile = null;
        boolean deleteFile = false;
        try {
            response = call.execute();
            body = processResponse(request.url().toString(), null, response);
            reader = new MultipartReader(body);

            Map<String, Object> multipart = new HashMap<>();
            for (Part part = reader.nextPart(); part != null; ) {
                if (Strings.isNotBlank(part.headers().get("filename"))) {
                    // write contents to a tmp file on disk for memory safety
                    tmpFile = Files.createTempFile("multipart_file_" + part.headers().get("filename"), ".tmp");
                    out = new FileOutputStream(tmpFile.toFile());

                    part.body().buffer().copyTo(out);
                    multipart.put(part.headers().get("name"), tmpFile);
                } else {
                    multipart.put(part.headers().get("name"), part.body().readUtf8());
                }
            }
            return multipart;
        } catch (IOException e) {
            deleteFile = true;
            throw exceptionWithCauseSupplier.build(e);
        } finally {
            if (body != null) {
                body.close();
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    log.error("Unable to close the output stream used to write the temp file: {}", tmpFile, e);
                }
            }
            if (tmpFile != null && deleteFile) {
                tmpFile.toFile().delete();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    log.error("Failed to close the MultiPartReader. url={}", request.url(), e);
                }
            }
            if (response != null) {
                response.close();
            }

        }
    }

    public String executeRequest(Request request) throws E {
        return executeRequestWithHeadersInternal(request, null).getBody();
    }

    public Stream<String> executeStreamingRequest(Request request) throws E {
        Call call = client.newCall(request);
        ResponseBody body = null;
        UncheckedCloseable close = null;
        try {
            Response response = call.execute();
            close = UncheckedCloseable.wrap(response);
            body = processResponse(request.url().toString(), null, response);
            close.nest(body);

            BufferedReader bufferedReader = new BufferedReader(body.charStream());
            return bufferedReader.lines().onClose(close);
        } catch (IOException e) {
            if (close != null) {
                try {
                    close.close();
                } catch (Exception ex) {
                    e.addSuppressed(ex);
                }
            }
            throw exceptionWithCauseSupplier.build(e);
        }
    }

    private ResponseBody processResponse(final String requestUrl, final Predicate<Response> failedResponsePredicate, final Response response) throws E {
        final Predicate<Response> effectiveFailedResponsePredicate = ObjectUtils.defaultIfNull(failedResponsePredicate, FAILED_RESPONSE_DEFAULT_PREDICATE);
        ResponseBody body = response.body();
        if (effectiveFailedResponsePredicate.test(response) || body == null) {
            String errorBody = null;
            if (body != null) {
                try {
                    errorBody = body.string();
                } catch (Exception e) {
                    errorBody = e.toString();
                }
            }
            throw exceptionWithCauseAndMessageSupplier.build("Response not successful: " + response.toString(),
                    new HttpException(response.code(), response.request().method(), requestUrl, errorBody));
        }
        return body;
    }

    private BodyAndHeaders<String> executeRequestWithHeadersInternal(Request request, final Predicate<Response> failedResponsePredicate) throws E {
        Call call = client.newCall(request);
        Response response = null;
        ResponseBody body = null;
        try {
            log.debug("Executing {} request to url={}", request.method(), request.url());
            response = call.execute();
            body = processResponse(request.url().toString(), failedResponsePredicate, response);
            String rawResponse = body.string();
            log.debug("Returned {}", response);
            log.debug("Got body:\n{}", rawResponse);
            log.debug("Response headers: {}", response.headers());
            return BodyAndHeaders.<String>builder()
                    .body(rawResponse)
                    .headers(response.headers().toMultimap())
                    .code(response.code())
                    .build();
        } catch (IOException e) {
            throw exceptionWithCauseSupplier.build(e);
        } finally {
            CloseableUtils.closeQuietly(body);
            CloseableUtils.closeQuietly(response);
        }
    }

    public BodyAndHeaders<String> executeRequestWithHeaders(Request request) throws E {
        return executeRequestWithHeadersInternal(request, null);
    }

    public RequestBody createJsonRequestBody(Object obj) throws E {
        return createRequestBody(ClientConstants.APPLICATION_JSON_UTF8, obj);
    }

    public RequestBody createRequestBody(MediaType mediaType, Object obj) throws E {
        return RequestBody.create(handleJsonWriteException(obj, () -> objectMapper.writeValueAsString(obj)), mediaType);
    }

    public RequestBody createFileUploadRequestBody(final FileUpload fileUpload) throws IOException {
        return getFileUploadRequestBodyBuilder(fileUpload).build();
    }

    public MultipartBody.Builder getFileUploadRequestBodyBuilder(final FileUpload fileUpload) throws IOException {
        ByteArrayOutputStream byos = new ByteArrayOutputStream();
        IOUtils.copy(fileUpload.getFile(), byos);
        return getMultiPartFormRequestBodyBuilder(
                Map.of(
                        "upload_name", fileUpload.getUploadName(),
                        "file_name", fileUpload.getFileName(),
                        "content_type", fileUpload.getContentType()),
                Map.of("file", Triple.of(fileUpload.getFileName(), fileUpload.getContentType(), byos.toByteArray())));
    }

    public RequestBody createMultiPartFormRequestBody(final Map<String, String> formData, final Map<String, Triple<String, String, byte[]>> binaries) throws IOException {
        return getMultiPartFormRequestBodyBuilder(formData, binaries).build();
    }

    public MultipartBody.Builder getMultiPartFormRequestBodyBuilder(final Map<String, String> formData, final Map<String, Triple<String, String, byte[]>> binaries) throws IOException {
        var builder = new MultipartBody.Builder();
        for (Map.Entry<String, String> entry : formData.entrySet()) {
            builder.addFormDataPart(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, Triple<String, String, byte[]>> file : binaries.entrySet()) {
            builder.addFormDataPart(file.getKey(), file.getValue().getLeft(), RequestBody.Companion.create(file.getValue().getRight(), MediaType.get(file.getValue().getMiddle())));
        }
        return builder;
    }

    public <T> T parseResponse(String rawResponse, JavaType javaType) throws E {
        if (StringUtils.isEmpty(rawResponse)) {
            throw exceptionWithCauseSupplier.build(new NoContentException("No content present."));
        }
        return handleJsonReadException(rawResponse, () -> objectMapper.readValue(rawResponse, javaType));
    }

    public <T> T parseResponse(final String rawResponse, Class<T> clazz) throws E {
        if (StringUtils.isEmpty(rawResponse)) {
            throw exceptionWithCauseSupplier.build(new NoContentException("No content present."));
        }
        return handleJsonReadException(rawResponse, () -> objectMapper.readValue(rawResponse, clazz));
    }

    public <T> T handleJsonReadException(String rawResponse, CallableWithException<T, IOException> callable) throws E {
        try {
            return callable.call();
        } catch (Exception e) {
            throw exceptionWithCauseAndMessageSupplier.build("Failed to parse response: " + StringUtils.truncate(rawResponse, ERROR_SNIPPET_MAX_SIZE), e);
        }
    }

    public <T> T handleJsonWriteException(Object obj, CallableWithException<T, JsonProcessingException> callable) throws E {
        try {
            return callable.call();
        } catch (JsonProcessingException e) {
            throw exceptionWithCauseAndMessageSupplier.build("Failed to convert to String: " + obj, e);
        }
    }

    @Value
    @Builder(toBuilder = true)
    public static class BodyAndHeaders<T> {
        Map<String, List<String>> headers;
        T body;
        int code;

        public String getHeader(String name) {
            return IterableUtils.getLast(headers.get(name)).orElse(null);
        }
    }

    //Setting testMode configuration. If set as testMode, the connection will skip certification check
    public static OkHttpClient.Builder configureToIgnoreCertificate(OkHttpClient.Builder builder) throws NoSuchAlgorithmException, KeyManagementException {
        // Create a trust manager that does not validate certificate chains
        final TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType)
                            throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                            throws CertificateException {
                    }

                    @Override
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return new java.security.cert.X509Certificate[]{};
                    }
                }
        };

        // Install the all-trusting trust manager
        final SSLContext sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
        // Create an ssl socket factory with our all-trusting manager
        final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

        builder.sslSocketFactory(sslSocketFactory, (X509TrustManager) trustAllCerts[0]);
        builder.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        });
        return builder;
    }
}
