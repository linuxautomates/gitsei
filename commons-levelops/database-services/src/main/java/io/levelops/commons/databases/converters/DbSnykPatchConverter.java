package io.levelops.commons.databases.converters;

import io.levelops.integrations.snyk.models.SnykVulnerability;
import org.springframework.jdbc.core.RowMapper;

public class DbSnykPatchConverter {

    public static RowMapper<SnykVulnerability.Patch> listRowMapper() {
        return (((rs, rowNum) -> SnykVulnerability.Patch.builder()
                .id(rs.getString("patch_id"))
                .version(rs.getString("version"))
                .modificationTime(rs.getDate("modification_time"))
                .build()));
    }
}
