package io.levelops.commons.client.exceptions;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.apache.logging.log4j.util.Strings;

import java.util.List;
import java.util.Map;

@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@Log4j2
public class HttpException extends Exception {

    private final Integer code;
    private final String method;
    private final String url;
    private final String body;

    public HttpException(int code) {
        this(code, null, null, null, null, null);
    }

    public HttpException(int code,
                         String url) {
        this(code, url, null, null, null, null);
    }

    public HttpException(int code,
                         String url,
                         String body) {
        this(code, url, body, null, null, null);
    }

    public HttpException(int code,
                         String method,
                         String url,
                         String body) {
        this(code, method, url, body, null, null);
    }

    public HttpException(int code,
                         String url,
                         String body,
                         Throwable cause) {
        this(code, null, url, body, null, cause);
    }

    @Builder
    public HttpException(int code,
                         String method,
                         String url,
                         String body,
                         Map<String, List<String>> headers,
                         Throwable cause) {
        super(generateMessage(code, method, url, body, headers), cause);
        this.code = code;
        this.method = method;
        this.url = url;
        this.body = body;
    }

    public static String generateMessage(int code, String method, String url, String body, Map<String, List<String>> headers) {
        String headersString = null;
        try {
            if (MapUtils.isNotEmpty(headers)) {
                headersString = DefaultObjectMapper.get().writeValueAsString(headers);
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize headers", e);
        }
        return "Request failed with error code [" + code + "]"
                + (Strings.isNotEmpty(url) ? " from url=" + url : "")
                + (Strings.isNotEmpty(method) ? " (" + method + ")" : "")
                + (Strings.isNotEmpty(body) ? " with body='" + body + "'": "")
                + (Strings.isNotEmpty(headersString) ? " and headers=" + headersString : "");
    }
}
