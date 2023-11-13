package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Log4j2
public class CustomDateDeserializer extends JsonDeserializer<Date> {

    public CustomDateDeserializer() {
        super();
    }

    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String date = p.getValueAsString("DetectionDate");
        if (date.equals("Not available"))
            return null;
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
        try {
            return sdf.parse(date);
        } catch (ParseException e) {
            log.error("Encountered error while parsing date: " + e.getMessage(), e);
        }
        return null;
    }
}
