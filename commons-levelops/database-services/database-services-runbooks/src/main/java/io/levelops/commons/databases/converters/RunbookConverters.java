package io.levelops.commons.databases.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.KvField;
import io.levelops.commons.databases.models.database.TriggerType;
import io.levelops.commons.databases.models.database.runbooks.Runbook;
import io.levelops.commons.databases.models.database.runbooks.RunbookNode;
import io.levelops.commons.databases.models.database.runbooks.RunbookVariable;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.ParsingUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.RowMapper;

@Log4j2
public class RunbookConverters {

    public static RowMapper<Runbook> rowMapper(ObjectMapper objectMapper) {
        return (rs, rowNumber) -> Runbook.builder()
                .id(rs.getString("id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .enabled(rs.getBoolean("enabled"))
                .previousId(rs.getString("previous_id"))
                .permanentId(rs.getString("permanent_id"))
                .triggerType(TriggerType.fromString(rs.getString("trigger_type")))
                .triggerTemplateType(rs.getString("trigger_template_type"))
                .triggerData(ParsingUtils.parseMap(objectMapper, "trigger data", String.class, RunbookVariable.class,
                        rs.getString("trigger_data")))
                .lastRunAt(DateUtils.toInstant(rs.getTimestamp("last_run_at")))
                .input(ParsingUtils.parseMap(objectMapper, "input", String.class, KvField.class,
                        rs.getString("input")))
                .nodes(ParsingUtils.parseMap(objectMapper, "nodes", String.class, RunbookNode.class,
                        rs.getString("nodes")))
                .settings(ParsingUtils.parseObject(objectMapper, "settings", Runbook.Setting.class,
                        rs.getString("settings")))
                .uiData(ParsingUtils.parseJsonObject(objectMapper, "ui_data", rs.getString("ui_data")))
                .updatedAt(DateUtils.toInstant(rs.getTimestamp("updated_at")))
                .createdAt(DateUtils.toInstant(rs.getTimestamp("created_at")))
                .build();
    }

}
