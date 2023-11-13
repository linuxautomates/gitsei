package io.levelops.commons.databases.converters.scm;

import io.levelops.commons.databases.models.database.scm.ScmCommitPartial;
import org.springframework.jdbc.core.RowMapper;

import java.util.UUID;

public class ScmCommitPartialConverters {
    public static RowMapper<ScmCommitPartial> rowMapper() {
        return (rs, rowNumber) -> {
            return ScmCommitPartial.builder()
                    .id((UUID) rs.getObject("id"))
                    .commitSha(rs.getString("commit_sha"))
                    .build();
        };
    }
}
