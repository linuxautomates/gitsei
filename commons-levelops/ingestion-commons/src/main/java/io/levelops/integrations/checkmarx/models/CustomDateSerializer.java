package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CustomDateSerializer extends StdSerializer<Date> {

    public CustomDateSerializer() {
        super(Date.class);
    }

    protected CustomDateSerializer(Class<Date> t) {
        super(t);
    }

    protected CustomDateSerializer(JavaType type) {
        super(type);
    }

    protected CustomDateSerializer(Class<?> t, boolean dummy) {
        super(t, dummy);
    }

    protected CustomDateSerializer(StdSerializer<?> src) {
        super(src);
    }

    @Override
    public void serialize(Date value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (value == null) {
            gen.writeNull();
        }
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
        gen.writeString(sdf.format(value));
    }
}
