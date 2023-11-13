package io.levelops.commons.faceted_search.db.converters;

import io.levelops.commons.faceted_search.db.models.ScmPROrCommitJiraWIMapping;
import org.springframework.jdbc.core.RowMapper;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ScmPRConverter {
    public static RowMapper<ScmPROrCommitJiraWIMapping> prJiraWIMapper() {
        return (rs, rowNumber) -> {
            return ScmPROrCommitJiraWIMapping.builder()
                    .prOrCommitId((UUID) rs.getObject("id"))
                    .workItemIds((rs.getArray("workitem_ids") != null &&
                            rs.getArray("workitem_ids").getArray() != null) ?
                            Arrays.asList((String[]) rs.getArray("workitem_ids").getArray()) : List.of())
                    .build();
        };
    }
}
