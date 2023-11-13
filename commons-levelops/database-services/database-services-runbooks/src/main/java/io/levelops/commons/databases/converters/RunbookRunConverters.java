package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.runbooks.RunbookRun;
import io.levelops.commons.databases.models.database.runbooks.RunbookRunState;
import io.levelops.commons.databases.models.database.runbooks.RunbookVariable;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.ParsingUtils;
import lombok.extern.log4j.Log4j2;

import org.springframework.jdbc.core.RowMapper;

@Log4j2
public class RunbookRunConverters {

    public static RowMapper<RunbookRun> rowMapper(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> RunbookRun.builder()
                .id(rs.getString("id"))
                .runbookId(rs.getString("runbook_id"))
                .permanentId(rs.getString("permanent_id"))
                .triggerType(rs.getString("trigger_type"))
                .args(ParsingUtils.parseMap(objectMapper, "args", String.class, RunbookVariable.class,
                        rs.getString("args")))
                .state(RunbookRunState.fromString(rs.getString("state")))
                .stateChangedAt(DateUtils.toInstant(rs.getTimestamp("state_changed_at")))
                .hasWarnings(rs.getBoolean("has_warnings"))
                .result(ParsingUtils.parseObject(objectMapper, "result", RunbookRun.RunbookRunResult.class, rs.getString("result")))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .build();
    }
}
