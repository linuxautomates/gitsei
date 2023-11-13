package io.levelops.controlplane.models;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.controlplane.models.jsonb.JsonUtils;
import lombok.extern.log4j.Log4j2;
import org.postgresql.util.PGobject;

import java.util.Map;
import java.util.function.Function;

@Log4j2
public class DbTriggerConverters {

    public static Function<Map<String, Object>, DbTrigger> getDbTriggerParser(ObjectMapper objectMapper) {
        return o -> parseDbTrigger(objectMapper, o);
    }

    public static DbTrigger parseDbTrigger(ObjectMapper objectMapper, Map<String, Object> o) {
        // Since "settings" is a jsonb column it is returned as a PgObject. We need to convert
        // it into a regular map for Jackson to auto deserialize the nested field as part
        // of the DbTrigger class
        if (o.containsKey("settings")) {
            o.put("settings", JsonUtils.parseJsonFromPGObject(objectMapper, (PGobject) o.get("settings")));
        }
        DbTrigger trigger = objectMapper.convertValue(o, DbTrigger.class);
        return trigger.toBuilder()
                .metadata(JsonUtils.parseJson(objectMapper, trigger.getMetadata()))
                .build();
    }
}
