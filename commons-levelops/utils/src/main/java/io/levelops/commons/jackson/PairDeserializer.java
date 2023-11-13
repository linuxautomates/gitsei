package io.levelops.commons.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.IOException;

public class PairDeserializer extends JsonDeserializer<ImmutablePair> {
    @Override
    public ImmutablePair deserialize(
            JsonParser jsonParser,
            DeserializationContext deserializationContext) throws IOException {
        final Object[] array = jsonParser.readValueAs(Object[].class);
        return ImmutablePair.of(array[0], array[1]);
    }
}
