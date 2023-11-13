package io.levelops.commons.databases.converters;

import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

@Log4j2
public class StringQueryConverter {
    public static RowMapper<String> stringMapper(String key) {
        return (rs, rowNumber) -> {
            return rs.getString(key);
        };
    }
}
