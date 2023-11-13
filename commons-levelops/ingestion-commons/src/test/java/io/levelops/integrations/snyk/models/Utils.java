package io.levelops.integrations.snyk.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;

public class Utils {
    public static ObjectMapper constructObjectMapper(){
        ObjectMapper mapper = DefaultObjectMapper.get()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }
}
