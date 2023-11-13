package io.levelops.commons.databases.converters.scm;

import io.levelops.commons.databases.models.database.scm.DbScmCommitPRMapping;
import io.levelops.commons.dates.DateUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

import java.util.UUID;

@Log4j2
public class DbScmCommitPRMappingConverters {
    public static RowMapper<DbScmCommitPRMapping> rowMapper() {
        return (rs, rowNumber) -> {
            return DbScmCommitPRMapping.builder()
                    .id((UUID) rs.getObject("id"))
                    .scmCommitId((UUID) rs.getObject("scm_commit_id"))
                    .scmPullrequestId((UUID) rs.getObject("scm_pullrequest_id"))
                    .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                    .build();
        };
    }
}
