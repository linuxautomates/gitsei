package io.levelops.commons.databases.converters.scm;

import io.levelops.commons.databases.models.database.scm.ScmPRPartial;
import org.springframework.jdbc.core.RowMapper;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ScmPRPartialConverters {
    public static RowMapper<ScmPRPartial> rowMapper() {
        return (rs, rowNumber) -> {
            return ScmPRPartial.builder()
                    .id((UUID) rs.getObject("id"))
                    .mergeSha(rs.getString("merge_sha"))
                    .commitShas((rs.getArray("commit_shas") != null &&
                            rs.getArray("commit_shas").getArray() != null) ?
                            Arrays.asList((String[]) rs.getArray("commit_shas").getArray()) : List.of())
                    .build();
        };
    }
}
