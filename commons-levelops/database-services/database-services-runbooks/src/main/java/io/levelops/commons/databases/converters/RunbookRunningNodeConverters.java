package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.runbooks.RunbookRunningNode;
import io.levelops.commons.databases.models.database.runbooks.RunbookRunningNode.RunbookRunningNodeResult;
import io.levelops.commons.databases.models.database.runbooks.RunbookRunningNodeState;
import io.levelops.commons.databases.models.database.runbooks.RunbookVariable;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.ParsingUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

@Log4j2
public class RunbookRunningNodeConverters {

    public static RowMapper<RunbookRunningNode> rowMapper(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> RunbookRunningNode.builder()
                .id(rs.getString("id"))
                .runId(rs.getString("run_id"))
                .nodeId(rs.getString("node_id"))
                .triggeredBy(ParsingUtils.parseMap(objectMapper, "triggered_by", String.class, String.class, rs.getString("triggered_by")))
                .output(ParsingUtils.parseMap(objectMapper, "output", String.class, RunbookVariable.class, rs.getString("output")))
                .data(ParsingUtils.parseJsonObject(objectMapper, "data", rs.getString("data")))
                .hasWarnings(rs.getBoolean("has_warnings"))
                .result(ParsingUtils.parseObject(objectMapper, "result", RunbookRunningNodeResult.class, rs.getString("result")))
                .state(RunbookRunningNodeState.fromString(rs.getString("state")))
                .stateChangedAt(DateUtils.toInstant(rs.getTimestamp("state_changed_at")))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .build();
    }

}
