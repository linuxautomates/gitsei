package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CiCdInstanceConfig;
import io.levelops.commons.databases.models.database.CiCdInstanceDetails;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

@Log4j2
public class CICDInstanceConverters {

    public static RowMapper<CICDInstance> listRowMapper() {
        return ((rs, rowNum) -> buildInstance(rs));
    }

    private static CICDInstance buildInstance(ResultSet rs) throws SQLException {
        return CICDInstance.builder()
                .id((UUID) rs.getObject("id"))
                .name(rs.getString("name"))
                .url(rs.getString("url"))
                .integrationId(rs.getString("integration_id"))
                .type(rs.getString("type"))
                .details(parseInstanceDetails(DefaultObjectMapper.get(), rs.getString("details")))
                .config(parseConfig(DefaultObjectMapper.get(), rs.getString("config")))
                .configUpdatedAt(DateUtils.toInstant(rs.getTimestamp("config_updated_at")))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .updatedAt(DateUtils.toInstant(rs.getTimestamp("updated_at")))
                .lastHeartbeatAt(DateUtils.toInstant(rs.getTimestamp("last_hb_at")))
                .build();
    }

    public static CiCdInstanceConfig parseConfig(ObjectMapper objectMapper, String configString) {
        try {
            if (configString == null) return CiCdInstanceConfig.builder().build();
            return objectMapper.readValue(configString, objectMapper.getTypeFactory().constructType(CiCdInstanceConfig.class));
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize config json. will store empty json.", e);
            return CiCdInstanceConfig.builder().build();
        }
    }

    private static CiCdInstanceDetails parseInstanceDetails(ObjectMapper objectMapper, String details) {
        try {
            if (details == null) return CiCdInstanceDetails.builder().build();
            return objectMapper.readValue(details, CiCdInstanceDetails.class);
        } catch (JsonProcessingException e) {
            log.warn("parseInstanceDetails: Failed to serialize config json. will store empty json.", e);
            return CiCdInstanceDetails.builder().build();
        }
    }

    public static RowMapper<DbAggregationResult> aggRowMapper(String key) {
        return ((rs, rowNum) -> DbAggregationResult.builder()
                .key(key.equals("none") ? null : rs.getString(key))
                .count(rs.getLong("ct"))
                .build());
    }

}
