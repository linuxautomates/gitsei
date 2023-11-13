package io.levelops.controlplane.models.jsonb;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.util.Optional;

import static io.levelops.controlplane.models.jsonb.JsonUtils.getJsonValue;

public class JsonBSerializer extends JsonSerializer<Object> {

    @Override
    public void serialize(Object o, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        Optional<Object> jsonBValue = getJsonValue(o);
        if (jsonBValue.isPresent()) {
            gen.writeObject(jsonBValue.get());
        } else {
            gen.writeObject(o);
        }
    }


}