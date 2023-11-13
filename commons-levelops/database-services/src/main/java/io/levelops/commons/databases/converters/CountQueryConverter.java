package io.levelops.commons.databases.converters;

import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

@Log4j2
public class CountQueryConverter {
    public static RowMapper<Integer> countMapper() {
        return (rs, rowNumber) -> {
            return rs.getInt("count");
        };
    }
}
