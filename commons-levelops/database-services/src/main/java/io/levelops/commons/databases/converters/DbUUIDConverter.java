package io.levelops.commons.databases.converters;

import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

import java.util.UUID;

@Log4j2
public class DbUUIDConverter {
    public static RowMapper<UUID> uuidMapper() {
        return (rs, rowNumber) -> {
            return (UUID) rs.getObject("id");
        };
    }
}
