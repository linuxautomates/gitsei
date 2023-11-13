package io.levelops.commons.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Date;

@Log4j2
public class EpochSecondsToDateDeserializer extends JsonDeserializer<Date> {

    // WARNING this doesnt seem to work

    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        if (StringUtils.isEmpty(p.getText())) {
            return null;
        }
        String timestamp = p.getText().trim();
        try {
            return new Date(Long.parseLong(timestamp) * 1000);
        } catch (NumberFormatException e) {
            log.debug("Unable to deserialize Date from epoch in seconds: " + timestamp, e);
            return null;
        }
    }

}
