package io.levelops.commons.client;

import okhttp3.MediaType;

import java.util.Base64;
import java.util.Objects;

public interface ClientConstants {
    String ACCEPT = "Accept";
    String CONTENT_TYPE = "Content-type";
    String AUTHORIZATION = "Authorization";
    String BEARER = "Bearer";
    String BEARER_ = BEARER + " ";
    String BASIC = "Basic";
    String BASIC_ = BASIC + " ";
    String APIKEY = "Apikey";
    String APIKEY_ = APIKEY + " ";
    MediaType APPLICATION_JSON_UTF8 = Objects.requireNonNull(MediaType.parse("application/json; charset=utf-8"));
    MediaType APPLICATION_JSON = Objects.requireNonNull(MediaType.parse("application/json"));
    MediaType GITHUB_APPLICATION_JSON = Objects.requireNonNull(MediaType.parse("application/vnd.github+json"));
    MediaType API_PREVIEW_HEADER = Objects.requireNonNull(MediaType.parse("application/vnd.github.inertia-preview+json"));
    MediaType TEXT_CSV = Objects.requireNonNull(MediaType.parse("text/csv"));
    MediaType APPLICATION_FORM_URL_ENCODED = Objects.requireNonNull(MediaType.parse("application/x-www-form-urlencoded"));

    static String encodeApiKey(String userName, String apiKey) {
        String toEncode = userName + ":" + apiKey;
        return Base64.getEncoder().encodeToString(toEncode.getBytes());
    }
}

