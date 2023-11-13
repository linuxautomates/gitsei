package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.velocity.VelocityConfig;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.utils.DatabaseUtils;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.ParsingUtils;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.StringUtils;
import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.RowMapper;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
public class VelocityConfigConverters {
    public static RowMapper<VelocityConfig> mapVelocityConfig(ObjectMapper mapper) {
        return (rs, rowNumber) -> {
            UUID id = (UUID) rs.getObject("id");
            String name = rs.getString("name");
            Boolean isNew = rs.getBoolean("is_new");
            Boolean defaultConfig = rs.getBoolean("default_config");
            VelocityConfigDTO config = null;
            try {
                config = mapper.readValue(rs.getString("config"), VelocityConfigDTO.class);
            } catch (JsonProcessingException e) {
                throw new SQLException("Failed to deserialize velocity config with id=" + id.toString());
            }

            /*
            If Velocity Config does not have any cicd jobs the db query returns
            cicd_job_id_name => "{"{\"cicd_job_id\" : null, \"job_name\" : null}"}"
             */
            Map<UUID, String> cicdIdJobNameMapping = DatabaseUtils.fromSqlArray(rs.getArray("cicd_job_id_name"), String.class)
                    //.map(PGobject::getValue)
                    .filter(StringUtils::isNoneEmpty)
                    .map(value -> ParsingUtils.parseObject(mapper, "cicd_job_id_name", CicdIdJobName.class, value))
                    .filter(Objects::nonNull)
                    .filter(x -> (x.getCicdJobId() != null))
                    .filter(x -> (x.getJobName() != null))
                    .collect(Collectors.toMap(x-> x.getCicdJobId(), x-> x.getJobName()));
            log.debug("cicdIdJobNameMapping = {}", cicdIdJobNameMapping);

            Instant createdAt = DateUtils.toInstant(rs.getTimestamp("created_at"));
            Instant updatedAt = DateUtils.toInstant(rs.getTimestamp("updated_at"));
            VelocityConfig velocityConfig = VelocityConfig.builder()
                    .id(id)
                    .name(name).defaultConfig(defaultConfig)
                    .isNew(isNew)
                    .config(config)
                    .cicdJobIdNameMappings(cicdIdJobNameMapping)
                    .createdAt(createdAt).updatedAt(updatedAt)
                    .build();
            return velocityConfig;
        };
    }
    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CicdIdJobName.CicdIdJobNameBuilder.class)
    public static class CicdIdJobName {
        @JsonProperty("cicd_job_id")
        UUID cicdJobId;
        @JsonProperty("job_name")
        String jobName;
    }

}
