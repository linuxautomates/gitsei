package io.levelops.internal_api.models.troubleshooting;

import java.util.regex.Pattern;

public enum IngestionError {
    ERROR_BAD_REQUEST(Pattern.compile("code=400", Pattern.CASE_INSENSITIVE), "Error Code 400 Malformed request - check with engineering"),
    ERROR_UNAUTHORIZED(Pattern.compile("code=401", Pattern.CASE_INSENSITIVE), "Error Code 401 - check the app credentials"),
    ERROR_FORBIDDEN(Pattern.compile("code=403", Pattern.CASE_INSENSITIVE), "Error code 403 - check the app credentials"),
    ERROR_NOT_FOUND(Pattern.compile("code=404", Pattern.CASE_INSENSITIVE), "Error code 404 - check with engineering"),
    ERROR_TOO_MANY_REQUESTS(Pattern.compile("code=429", Pattern.CASE_INSENSITIVE), "Error 429 - running in to rate limits"),
    ERROR_UNKNOWN_HOST(Pattern.compile("java.net.UnknownHostException", Pattern.CASE_INSENSITIVE), "Unknown host - check the configured app url"),
    ERROR_TIMED_OUT(Pattern.compile("Connection timed out", Pattern.CASE_INSENSITIVE), "Connection timed out - check with engineering"),
    ERROR_NO_TOKEN_FOUND(Pattern.compile("No token found for IntegrationKey", Pattern.CASE_INSENSITIVE), "No token found - check app credentials"),
    ERROR_OUT_OF_MEMORY(Pattern.compile("java.lang.OutOfMemoryError", Pattern.CASE_INSENSITIVE), "Ingestion pod running out of memory - contact engineering"),
    UNKNOWN(Pattern.compile(".*"), "Unknown error - check with engineering");
    private final Pattern pattern;
    private final String message;

    IngestionError(Pattern pattern, String message) {
        this.pattern = pattern;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public static IngestionError detectError(String errorString) {
        for (IngestionError error : IngestionError.values()) {
            if (error.pattern.matcher(errorString).find()) {
                return error;
            }
        }
        return UNKNOWN;
    }
}
