package io.levelops.commons.databases.services;

import io.levelops.commons.database.ArrayWrapper;
import io.levelops.commons.database.DBUtils;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.MessageTemplate;
import io.levelops.commons.databases.models.database.MessageTemplate.TemplateType;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
@Service
public class MsgTemplateService extends DatabaseService<MessageTemplate> {
    protected final NamedParameterJdbcTemplate template;

    @Autowired
    public MsgTemplateService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public String insert(String company, MessageTemplate template) throws SQLException {

        String SQL = "INSERT INTO " + company + ".messagetemplates(name,type,botname,"
                + "emailsubject,message,default_template,event_type,system) VALUES(?,?,?,?,?,?,?,?)";

        return this.template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, template.getName());
                pstmt.setString(2, String.valueOf(template.getType()));
                pstmt.setString(3, (template.getBotName() != null) ? template.getBotName() : "");
                pstmt.setString(4, (template.getEmailSubject() != null) ? template.getEmailSubject() : "");
                pstmt.setString(5, template.getMessage());
                pstmt.setBoolean(6, template.isDefaultTemplate());
                pstmt.setString(7, template.getEventType().toString());
                pstmt.setBoolean(8, template.isSystem());

                // Check if there a default already set for the event type
                if (template.isDefaultTemplate()) {
                    removeDefault(company, template.getType(), template.getEventType(), conn);
                }

                int affectedRows = pstmt.executeUpdate();
                // check the affected rows
                if (affectedRows > 0) {
                    // get the ID back
                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            return rs.getString(1);
                        }
                    }
                }
            }
            return null;
        }));
    }

    private void removeDefault(String company, TemplateType templateType, EventType eventType, Connection conn)
            throws SQLException {
        var defaultCheckQuery = "SELECT id FROM " + company + ".messagetemplates WHERE default_template = TRUE AND" +
                " event_type = '" + eventType.toString() + "' AND type = '" + templateType.toString() + "'";
        var update = "UPDATE " + company + ".messagetemplates SET default_template = FALSE WHERE id = ?";
        try (ResultSet rs = conn.prepareStatement(defaultCheckQuery).executeQuery();
             PreparedStatement updateStm = conn.prepareStatement(update)) {
            if (rs.next()) {
                // update the previous default
                var id = rs.getInt("id");
                updateStm.setInt(1, id);
                var count = updateStm.executeUpdate();
                if (count < 1) {
                    log.warn("Unabled to unset as default, maybe it was already updated just now... Template id = {}", id);
                }
            }
        }
    }

    @Override
    public Boolean update(String company, MessageTemplate template) throws SQLException {
        String SQL = "UPDATE " + company + ".messagetemplates SET ";
        String updates = "";
        String condition = " WHERE id = ?";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(template.getName())) {
            updates = "name = ?";
            values.add(template.getName());
        }
        if (template.getType() != null) {
            updates = StringUtils.isEmpty(updates) ? "type = ?" : updates + ", type = ?";
            values.add(template.getType().toString());
        }
        if (StringUtils.isNotEmpty(template.getBotName())) {
            updates = StringUtils.isEmpty(updates) ? "botname = ?" : updates + ", botname = ?";
            values.add(template.getBotName());
        }
        if (template.getEmailSubject() != null) {
            updates = StringUtils.isEmpty(updates) ? "emailsubject = ?" : updates + ", emailsubject = ?";
            values.add(template.getEmailSubject());
        }
        if (template.getMessage() != null) {
            updates = StringUtils.isEmpty(updates) ? "message = ?" : updates + ", message = ?";
            values.add(template.getMessage());
        }
        if (template.getEventType() != null) {
            updates = StringUtils.isEmpty(updates) ? "event_type = ?" : updates + ", event_type = ?";
            values.add(template.getEventTypeString());
        }
        updates = StringUtils.isEmpty(updates) ? "default_template = ?" : updates + ", default_template = ?";
        values.add(template.isDefaultTemplate());

        //no updates
        if (values.size() == 0) {
            return false;
        }
        updates += ", updated_at = extract(epoch from now())";
        // add the id at the end
        values.add(Integer.parseInt(template.getId()));

        final String finalSQL = SQL + updates + condition;

        return this.template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            try (PreparedStatement pstmt = conn.prepareStatement(finalSQL,
                    Statement.RETURN_GENERATED_KEYS)) {
                for (int i = 1; i <= values.size(); i++) {
                    pstmt.setObject(i, values.get(i - 1));
                }
                if (template.isDefaultTemplate()) {
                    // remove the previous default
                    removeDefault(company, template.getType(), template.getEventType(), conn);
                }
                int affectedRows = pstmt.executeUpdate();
                // check the affected rows
                return affectedRows > 0;
            }
        }));
    }

    @Override
    public Optional<MessageTemplate> get(String company, String templateId) {
        String SQL = "SELECT id,name,botname,emailsubject,type,message,default_template,system,event_type,createdat FROM "
                + company + ".messagetemplates WHERE id = ? LIMIT 1";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {

            pstmt.setInt(1, Integer.parseInt(templateId));

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return Optional.of(MessageTemplate.builder()
                        .id(rs.getString("id"))
                        .name(rs.getString("name"))
                        .botName(rs.getString("botname"))
                        .type(MessageTemplate.TemplateType.fromString(rs.getString("type")))
                        .emailSubject(rs.getString("emailsubject"))
                        .message(rs.getString("message"))
                        .defaultTemplate(rs.getBoolean("default_template"))
                        .system(rs.getBoolean("system"))
                        .eventType(EventType.fromString(rs.getString("event_type")))
                        .createdAt(rs.getLong("createdat"))
                        .build());
            }
        } catch (SQLException ex) {
            log.error(ex);
        }
        return Optional.empty();
    }

    private String formatCriterea(String criterea, List<Object> values, String newCriterea){
        String result = criterea + ((values.size() ==0) ? "" : "AND ");
        result += newCriterea;
        return result;
    }

    public DbListResponse<MessageTemplate> listByFilter(String company, String name, String type,
                                                        Integer pageNumber, Integer pageSize,
                                                        List<TemplateType> types, Boolean defaultTemplate, List<EventType> eventTypes) throws SQLException {
        String criteria = " WHERE ";
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(name)) {
            criteria += "name ILIKE ? ";
            values.add(name + "%");
        }
        if (MessageTemplate.TemplateType.fromString(type) != null) {
            criteria += (values.size() == 0) ? "type = ? " : "AND type = ? ";
            values.add(type);
        }
        if(CollectionUtils.isNotEmpty(types)) {
            List<String> typeStrings = types.stream().map(TemplateType::toString).collect(Collectors.toList());
            criteria = formatCriterea(criteria, values, "type = ANY(?::varchar[]) ");
            values.add(new ArrayWrapper<>("varchar", typeStrings));
        }
        if(defaultTemplate != null) {
            criteria = formatCriterea(criteria, values, "default_template = ? ");
            values.add(defaultTemplate);
        }
        if(CollectionUtils.isNotEmpty(eventTypes)) {
            List<String> eventTypeStrings = eventTypes.stream().map(EventType::toString).collect(Collectors.toList());
            criteria = formatCriterea(criteria, values, "event_type = ANY(?::varchar[]) ");
            values.add(new ArrayWrapper<>("varchar", eventTypeStrings));
        }
        if (values.size() == 0) {
            criteria = " ";
        }

        String sorting = " ORDER BY updated_at DESC ";
        String SQL = "SELECT * FROM " + company + ".messagetemplates" + criteria + sorting
                + "LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        List<MessageTemplate> retval = new ArrayList<>();
        String countSQL = "SELECT COUNT(*) FROM " + company + ".messagetemplates " + criteria;
        Integer totCount = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {
            for (int i = 0; i < values.size(); i++) {
                Object obj = DBUtils.processArrayValues(conn, values.get(i));
                pstmt.setObject(i + 1, obj);
                pstmt2.setObject(i + 1, obj);
            }
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                retval.add(MessageTemplate.builder()
                        .id(rs.getObject("id").toString())
                        .name(rs.getString("name"))
                        .type(MessageTemplate.TemplateType.fromString(rs.getString("type")))
                        .botName(rs.getString("botname"))
                        .emailSubject(rs.getString("emailsubject"))
                        .message(rs.getString("message"))
                        .defaultTemplate(rs.getBoolean("default_template"))
                        .system(rs.getBoolean("system"))
                        .eventType(EventType.fromString(rs.getString("event_type")))
                        .createdAt(rs.getLong("createdat"))
                        .build());
            }
            if (retval.size() > 0) {
                totCount = retval.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
                if (retval.size() == pageSize) {
                    rs = pstmt2.executeQuery();
                    if (rs.next()) {
                        totCount = rs.getInt("count");
                    }
                }
            }
        }
        return DbListResponse.of(retval, totCount);
    }

    @Override
    public DbListResponse<MessageTemplate> list(String company, Integer pageNumber,
                                                Integer pageSize) throws SQLException {
        return listByFilter(company, null, null, pageNumber, pageSize, null, null, null);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".messagetemplates WHERE id = ? AND system = FALSE";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setLong(1, Integer.parseInt(id));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }

    public int bulkDelete(String company, List<String> ids) throws SQLException {
        int rowsDeleted = 0;
        if (CollectionUtils.isNotEmpty(ids)) {
            Map<String, Object> params = Map.of("ids", ids.stream()
                    .map(NumberUtils::toInt)
                    .collect(Collectors.toList()));
            String SQL = "DELETE FROM " + company + ".messagetemplates WHERE id IN (:ids)";
            rowsDeleted = template.update(SQL, params);
        }
        return rowsDeleted;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of("CREATE TABLE IF NOT EXISTS {0}.messagetemplates(\n" +
                        "        id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, \n" +
                        "        name VARCHAR(50) NOT NULL, \n" +
                        "        type VARCHAR NOT NULL, \n" +
                        "        botname VARCHAR, \n" +
                        "        emailsubject VARCHAR, \n" +
                        "        message VARCHAR NOT NULL, \n" +
                        "        default_template BOOLEAN NOT NULL DEFAULT FALSE, \n" +
                        "        system BOOLEAN NOT NULL DEFAULT FALSE, \n" +
                        "        event_type VARCHAR NOT NULL DEFAULT ''all'', \n" +
                        "        createdat BIGINT DEFAULT extract(epoch from now()),\n" +
                        "        updated_at BIGINT DEFAULT extract(epoch from now())\n" +
                        "    )",
                "CREATE UNIQUE INDEX IF NOT EXISTS messagetemplates_uniqname_idx ON {0}.messagetemplates(lower(name))"
        );

        sqlList.stream().map(sql -> MessageFormat.format(sql, company)).forEach(template.getJdbcTemplate()::execute);
        return true;
    }

}