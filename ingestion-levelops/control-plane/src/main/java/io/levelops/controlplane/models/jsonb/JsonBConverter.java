package io.levelops.controlplane.models.jsonb;

import com.fasterxml.jackson.databind.util.StdConverter;
import org.postgresql.util.PGobject;

public class JsonBConverter extends StdConverter<PGobject, Object> {

    @Override
    public Object convert(PGobject o) {
        // FIXME
        return "hello";
//        return getJsonBValue(o).orElse(o);
    }
}