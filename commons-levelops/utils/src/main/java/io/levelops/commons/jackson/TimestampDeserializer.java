package io.levelops.commons.jackson;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;

public class TimestampDeserializer extends JsonDeserializer<Timestamp> {

    private ObjectMapper mapper;

    public TimestampDeserializer() {
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public Timestamp deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        try {
            long timestampInSeconds = jsonParser.getLongValue();
            return new Timestamp(timestampInSeconds * 1000L);
        } catch (JsonParseException e) {
            try {
                return Timestamp.from(Instant.parse(jsonParser.getValueAsString()));
            } catch (Exception ex) {
                return this.mapper.readValue(jsonParser.getValueAsString(), Timestamp.class);
            }
        }
    }
}
