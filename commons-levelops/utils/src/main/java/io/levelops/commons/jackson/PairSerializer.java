package io.levelops.commons.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.IOException;

public class PairSerializer extends JsonSerializer<ImmutablePair> {
    @Override
    public void serialize(
            ImmutablePair pair,
            JsonGenerator jsonGenerator,
            SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeStartArray(2);
        jsonGenerator.writeObject(pair.getLeft());
        jsonGenerator.writeObject(pair.getRight());
        jsonGenerator.writeEndArray();
    }
}
