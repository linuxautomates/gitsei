package io.levelops.notification.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

public class SlackHelper {

    private final ObjectMapper objectMapper;

    public SlackHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String getPlainTextSlackBlock(String plainText) throws JsonProcessingException {
        return objectMapper.writeValueAsString(List.of(getPlainTextSection(plainText)));
    }

    private Map<String, Object> getPlainTextSection(String plainText) throws JsonProcessingException {
        return Map.of("type", "section", "text", getPlainTextBlob(plainText));
    }

    private Map<String, String> getPlainTextBlob(String plainText) throws JsonProcessingException {
        return Map.of("type", "plain_text", "text", plainText);
    }
}
