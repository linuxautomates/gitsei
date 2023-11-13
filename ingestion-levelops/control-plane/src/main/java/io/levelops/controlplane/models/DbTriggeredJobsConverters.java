package io.levelops.controlplane.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.function.Function;

@Log4j2
public class DbTriggeredJobsConverters {

    public static Function<Map<String, Object>, DbTriggeredJob> getDbTriggeredJobParser(ObjectMapper objectMapper) {
        return o -> parseDbTriggeredJob(objectMapper, o);
    }

    public static DbTriggeredJob parseDbTriggeredJob(ObjectMapper objectMapper, Map<String, Object> o) {
        return objectMapper.convertValue(o, DbTriggeredJob.class);
    }
}
