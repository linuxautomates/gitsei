package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.database.ArrayWrapper;
import io.levelops.commons.database.DBUtils;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.KvField;
import io.levelops.commons.databases.models.database.TicketField;
import io.levelops.commons.databases.models.database.TicketTemplate;
import io.levelops.commons.databases.models.database.TicketTemplateQuestionnaireTemplateMapping;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class TicketTemplateDBService extends DatabaseService<TicketTemplate> {
    private static final String TICKET_FIELD_INSERT = "INSERT INTO %s.ticket_fields(key,type,options,ticket_template_id,required,dynamic_resource_name,validation,search_field,display_name,description) VALUES(?,?,?,?,?,?,?,?,?,?)";
    private static final String TICKET_FIELD_UPDATE = "UPDATE %s.ticket_fields SET key = ?, type = ?, options = ?, ticket_template_id = ?, required = ?, dynamic_resource_name = ?, validation = ?, search_field = ?, display_name = ?, description = ? WHERE ID = ?";
    private static final String TICKET_FIELD_DELETE = "DELETE FROM %s.ticket_fields WHERE id = ANY(?::BIGINT[])";

    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper mapper;

    // region CSTOR
    @Autowired
    public TicketTemplateDBService(DataSource dataSource, ObjectMapper mapper) {
        super(dataSource);
        this.mapper = mapper;
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }
    // endregion

    // region get references
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(QuestionnaireTemplateDBService.class);
    }
    // endregion

    // region Ticket Fields
    private void deleteTicketFields(Connection conn, PreparedStatement pstmt, List<TicketField> ticketFieldsList) throws SQLException {
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(ticketFieldsList)) {
            return;
        }
        List<Long> deleteIds = ticketFieldsList.stream().map(TicketField::getId).collect(Collectors.toList());
        pstmt.setObject(1, DBUtils.processArrayValues(conn, new ArrayWrapper<>("bigint", deleteIds)));
        pstmt.executeUpdate();
    }

    private void insertTicketFields(Connection conn, PreparedStatement pstmt, UUID ticketTemplateId, List<TicketField> ticketFieldsList) throws SQLException {
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(ticketFieldsList)) {
            return;
        }
        for (TicketField tf : ticketFieldsList) {
            pstmt.setString(1, tf.getField().getKey());
            pstmt.setString(2, tf.getField().getType());
            if (!CollectionUtils.isEmpty(tf.getField().getOptions())) {
                pstmt.setArray(3, conn.createArrayOf("varchar", tf.getField().getOptions().toArray()));
            } else {
                pstmt.setArray(3, null);
            }
            pstmt.setObject(4, ticketTemplateId);
            pstmt.setBoolean(5, tf.getField().getRequired());
            pstmt.setString(6, tf.getField().getDynamicResourceName());
            pstmt.setString(7, tf.getField().getValidation());
            pstmt.setString(8, tf.getField().getSearchField());
            pstmt.setString(9, tf.getField().getDisplayName());
            pstmt.setString(10, tf.getField().getDescription());
            pstmt.addBatch();
            pstmt.clearParameters();
        }
        pstmt.executeBatch();
    }

    private void updateTicketFields(Connection conn, PreparedStatement pstmt, UUID ticketTemplateId, List<TicketField> ticketFieldsList) throws JsonProcessingException, SQLException {
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(ticketFieldsList)) {
            return;
        }
        for (TicketField tf : ticketFieldsList) {
            pstmt.setString(1, tf.getField().getKey());
            pstmt.setString(2, tf.getField().getType());
            if (!CollectionUtils.isEmpty(tf.getField().getOptions())) {
                pstmt.setArray(3, conn.createArrayOf("varchar", tf.getField().getOptions().toArray()));
            } else {
                pstmt.setArray(3, null);
            }
            pstmt.setObject(4, ticketTemplateId);
            pstmt.setBoolean(5, tf.getField().getRequired());
            pstmt.setString(6, tf.getField().getDynamicResourceName());
            pstmt.setString(7, tf.getField().getValidation());
            pstmt.setString(8, tf.getField().getSearchField());
            pstmt.setString(9, tf.getField().getDisplayName());
            pstmt.setString(10, tf.getField().getDescription());
            pstmt.setObject(11, tf.getId());
            pstmt.addBatch();
            pstmt.clearParameters();
        }
        pstmt.executeBatch();
    }

    private void syncTicketFields(Connection conn, PreparedStatement deletePstmt, PreparedStatement insertPstmt, PreparedStatement updatePstmt, UUID ticketTemplateId, TicketTemplate ticketTemplate) throws JsonProcessingException, SQLException {
        if (org.apache.commons.collections4.CollectionUtils.isEmpty(ticketTemplate.getTicketFields())) {
            return;
        }
        List<TicketField> deleteTicketField = new ArrayList<>();
        List<TicketField> insertTicketField = new ArrayList<>();
        List<TicketField> updateTicketField = new ArrayList<>();
        for (TicketField tf : ticketTemplate.getTicketFields()) {
            if (BooleanUtils.isTrue(tf.getDeleted())) {
                deleteTicketField.add(tf);
            } else {
                if (tf.getId() != null) {
                    updateTicketField.add(tf);
                } else {
                    insertTicketField.add(tf);
                }
            }
        }
        deleteTicketFields(conn, deletePstmt, deleteTicketField);
        insertTicketFields(conn, insertPstmt, ticketTemplateId, insertTicketField);
        updateTicketFields(conn, updatePstmt, ticketTemplateId, updateTicketField);
    }
    // endregion

    // region insert
    @Override
    public String insert(String company, TicketTemplate t) throws SQLException {
        String ticketTemplateSQL = "INSERT INTO " + company + ".ticket_templates(name,description,enabled,default_fields,notify_by,message_template_ids) VALUES(?,?,?,to_json(?::json),?::json,?)";
        String templatesMappingsSQL = "INSERT INTO " + company + ".ticket_template_questionnaire_template_mappings(name,ticket_template_id,questionnaire_template_id) VALUES(?,?,?)";
        String ticketFieldsInsertQuery = String.format(TICKET_FIELD_INSERT, company);
        String ticketFieldsUpdateQuery = String.format(TICKET_FIELD_UPDATE, company);
        String ticketFieldsDeleteQuery = String.format(TICKET_FIELD_DELETE, company);

        UUID ticketTemplateId;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ticketTemplatePstmt = conn.prepareStatement(ticketTemplateSQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement templatesMappingsPstmt = conn.prepareStatement(templatesMappingsSQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement fieldsInsertPstmt = conn.prepareStatement(ticketFieldsInsertQuery, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement fieldsUpdatePstmt = conn.prepareStatement(ticketFieldsUpdateQuery, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement fieldsDeletePstmt = conn.prepareStatement(ticketFieldsDeleteQuery, Statement.RETURN_GENERATED_KEYS)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                ticketTemplatePstmt.setString(1, t.getName());
                ticketTemplatePstmt.setString(2, t.getDescription());
                ticketTemplatePstmt.setBoolean(3, t.getEnabled());
                ticketTemplatePstmt.setString(4, mapper.writeValueAsString(t.getDefaultFields()));
                ticketTemplatePstmt.setString(5, mapper.writeValueAsString(t.getNotifyBy()));
                ticketTemplatePstmt.setArray(6, CollectionUtils.isNotEmpty(t.getMessageTemplateIds()) ?
                        conn.createArrayOf("varchar", t.getMessageTemplateIds().toArray()) : null);
                int affectedRows = ticketTemplatePstmt.executeUpdate();
                // check the affected rows
                if (affectedRows <= 0) {
                    throw new SQLException("Failed to create ticket template!");
                }
                // get the ID back
                try (ResultSet rs = ticketTemplatePstmt.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("Failed to create ticket template!");
                    }
                    ticketTemplateId = (UUID) rs.getObject(1);
                }

                if (!CollectionUtils.isEmpty(t.getMappings())) {
                    for (TicketTemplateQuestionnaireTemplateMapping mapping : t.getMappings()) {
                        templatesMappingsPstmt.setString(1, mapping.getName());
                        templatesMappingsPstmt.setObject(2, ticketTemplateId);
                        templatesMappingsPstmt.setObject(3, UUID.fromString(mapping.getQuestionnaireTemplateId()));
                        templatesMappingsPstmt.addBatch();
                        templatesMappingsPstmt.clearParameters();
                    }
                    templatesMappingsPstmt.executeBatch();
                }

                syncTicketFields(conn, fieldsDeletePstmt, fieldsInsertPstmt, fieldsUpdatePstmt, ticketTemplateId, t);
                conn.commit();
            } catch (JsonProcessingException e) {
                conn.rollback();
                throw new SQLException("Error serializing field options!", e);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
        return ticketTemplateId.toString();
    }
    // endregion

    // region update
    @Override
    public Boolean update(String company, TicketTemplate t) throws SQLException {
        String deleteTicketTemplateQuestionnaireTemplateMappingsQuery = "DELETE FROM " + company + ".ticket_template_questionnaire_template_mappings WHERE ticket_template_id = ?";
        String ttSQL = "UPDATE " + company + ".ticket_templates SET name = ?,description = ?,enabled = ?,default_fields = to_json(?::json),updated_at = ?, notify_by = ?::json, message_template_ids = ? where id = ?";
        String mappingSQL = "INSERT INTO " + company + ".ticket_template_questionnaire_template_mappings(name,ticket_template_id,questionnaire_template_id,id) VALUES(?,?,?,?)";
        String ticketFieldsInsertQuery = String.format(TICKET_FIELD_INSERT, company);
        String ticketFieldsUpdateQuery = String.format(TICKET_FIELD_UPDATE, company);
        String ticketFieldsDeleteQuery = String.format(TICKET_FIELD_DELETE, company);

        UUID ticketTemplateId = UUID.fromString(t.getId());
        try (Connection conn = dataSource.getConnection();
             PreparedStatement deleteTicketTemplateQuestionnaireTemplateMappingsPstmt = conn.prepareStatement(deleteTicketTemplateQuestionnaireTemplateMappingsQuery);
             PreparedStatement ttPSTMT = conn.prepareStatement(ttSQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement mappingPSTMT = conn.prepareStatement(mappingSQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement fieldsInsertPstmt = conn.prepareStatement(ticketFieldsInsertQuery, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement fieldsUpdatePstmt = conn.prepareStatement(ticketFieldsUpdateQuery, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement fieldsDeletePstmt = conn.prepareStatement(ticketFieldsDeleteQuery, Statement.RETURN_GENERATED_KEYS)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                deleteTicketTemplateQuestionnaireTemplateMappingsPstmt.setObject(1, ticketTemplateId);
                deleteTicketTemplateQuestionnaireTemplateMappingsPstmt.execute();

                ttPSTMT.setString(1, t.getName());
                ttPSTMT.setString(2, t.getDescription());
                ttPSTMT.setBoolean(3, t.getEnabled());
                ttPSTMT.setString(4, mapper.writeValueAsString(t.getDefaultFields()));
                ttPSTMT.setLong(5, Instant.now().getEpochSecond());
                ttPSTMT.setString(6, mapper.writeValueAsString(t.getNotifyBy()));
                ttPSTMT.setArray(7, CollectionUtils.isNotEmpty(t.getMessageTemplateIds()) ?
                        conn.createArrayOf("varchar", t.getMessageTemplateIds().toArray()) : null);
                ttPSTMT.setObject(8, ticketTemplateId);
                ttPSTMT.executeUpdate();

                if (!CollectionUtils.isEmpty(t.getMappings())) {
                    for (TicketTemplateQuestionnaireTemplateMapping m : t.getMappings()) {
                        mappingPSTMT.setString(1, m.getName());
                        mappingPSTMT.setObject(2, ticketTemplateId);
                        mappingPSTMT.setObject(3, UUID.fromString(m.getQuestionnaireTemplateId()));
                        mappingPSTMT.setObject(4, StringUtils.isEmpty(m.getId()) ? UUID.randomUUID() : UUID.fromString(m.getId()));
                        mappingPSTMT.addBatch();
                        mappingPSTMT.clearParameters();
                    }
                    mappingPSTMT.executeBatch();
                }

                syncTicketFields(conn, fieldsDeletePstmt, fieldsInsertPstmt, fieldsUpdatePstmt, ticketTemplateId, t);
                conn.commit();
            } catch (JsonProcessingException e) {
                conn.rollback();
                throw new SQLException("Error serializing field options!", e);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
        return true;
    }
    // endregion

    // region get
    @Override
    public Optional<TicketTemplate> get(String company, String ticketTemplateId) throws SQLException {
        var results = getBatch(company, Collections.singletonList(UUID.fromString(ticketTemplateId)), 0, 1);
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }

    private Map<UUID, List<TicketField>> getTicketFieldsBatch(String company, List<UUID> ticketTemplateIds) throws SQLException {
        String SQL = "SELECT * FROM " + company + ".ticket_fields WHERE ticket_template_id = ANY(?::uuid[])";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setArray(1, conn.createArrayOf("uuid", ticketTemplateIds.toArray(new UUID[0])));
            ResultSet rs = pstmt.executeQuery();

            Map<UUID, List<TicketField>> result = new HashMap<>();
            while (rs.next()) {
                Long id = rs.getLong("id");
                String key = rs.getString("key");
                String type = rs.getString("type");
                String[] options = rs.getArray("options") != null
                        ? (String[]) rs.getArray("options").getArray()
                        : new String[0];
                UUID ticketTemplateId = (UUID) rs.getObject("ticket_template_id");
                Boolean required = rs.getBoolean("required");
                String dynamicResourceName = rs.getString("dynamic_resource_name");
                String searchField = rs.getString("search_field");
                String validation = rs.getString("validation");
                String displayName = rs.getString("display_name");
                String description = rs.getString("description");

                if (!result.containsKey(ticketTemplateId)) {
                    result.put(ticketTemplateId, new ArrayList<>());
                }
                result.get(ticketTemplateId).add(TicketField.builder()
                        .id(id)
                        .ticketTemplateId(String.valueOf(ticketTemplateId))
                        .field(KvField.builder()
                                .key(key)
                                .type(type)
                                .options(Arrays.asList(options))
                                .required(required)
                                .dynamicResourceName(dynamicResourceName)
                                .searchField(searchField)
                                .validation(validation)
                                .displayName(displayName)
                                .description(description)
                                .build())
                        .build()
                );
            }
            return result;
        }
    }

    private Map<UUID, List<TicketTemplateQuestionnaireTemplateMapping>> getMappingsBatch(String company, List<UUID> ticketTemplateIds) throws SQLException {
        String SQL = "SELECT * FROM " + company + ".ticket_template_questionnaire_template_mappings WHERE ticket_template_id = ANY(?::uuid[])";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setArray(1, conn.createArrayOf("uuid", ticketTemplateIds.toArray(new UUID[0])));
            ResultSet rs = pstmt.executeQuery();
            Map<UUID, List<TicketTemplateQuestionnaireTemplateMapping>> result = new HashMap<>();
            while (rs.next()) {
                var id = (UUID) rs.getObject("id");
                String name = rs.getString("name");
                UUID ticketTemplateId = (UUID) rs.getObject("ticket_template_id");
                UUID questionnaireTemplateId = (UUID) rs.getObject("questionnaire_template_id");
                if (!result.containsKey(ticketTemplateId)) {
                    result.put(ticketTemplateId, new ArrayList<>());
                }
                result.get(ticketTemplateId).add(TicketTemplateQuestionnaireTemplateMapping.builder()
                        .id(String.valueOf(id))
                        .name(name)
                        .questionnaireTemplateId(String.valueOf(questionnaireTemplateId))
                        .build()
                );
            }
            return result;
        }
    }

    private Integer getBatchCount(String company, String name) throws SQLException {
        String countSQL = "SELECT COUNT(*) FROM " + company + ".ticket_templates t";
        List<Object> values = new ArrayList<>();
        if (!StringUtils.isEmpty(name)) {
            countSQL += " WHERE name ILIKE ?";
            values.add(name + "%");
        }
        Integer totalCount = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(countSQL)) {
            int i = 1;
            for (Object val : values) {
                pstmt.setObject(i++, val);
            }
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                totalCount = rs.getInt("count");
            }
        }
        return totalCount;
    }

    private List<TicketTemplate> getTicketTemplatesBatch(String company, List<UUID> ids, String name,
                                                         Integer pageNumber, Integer pageSize) throws SQLException {
        return getTicketTemplatesBatch(company, ids, name, null, null, pageNumber, pageSize);
    }

    private List<TicketTemplate> getTicketTemplatesBatch(String company, List<UUID> ids, String name, Long updatedAtStart, Long updatedAtEnd,
                                                         Integer pageNumber, Integer pageSize) throws SQLException {
        String SQL = "SELECT t.id AS tid, t.name, t.description, t.enabled, t.default_fields, t.notify_by, t.message_template_ids, t.created_at, t.updated_at " +
                "FROM " + company + ".ticket_templates t";
        List<Object> values = new ArrayList<>();
        if (!CollectionUtils.isEmpty(ids)) {
            SQL += " WHERE t.id = ANY(?::uuid[])";
            values.add(ids);
        }
        if (!StringUtils.isEmpty(name)) {
            SQL += (values.size() == 0) ? " WHERE t.name LIKE ?" : " AND t.name LIKE ?";
            values.add(name + "%");
        }
        if (updatedAtStart != null) {
            SQL += ((values.size() == 0) ? " WHERE " : " AND ") + "updated_at > ? ";
            values.add(updatedAtStart);
        }
        if (updatedAtEnd != null) {
            SQL += ((values.size() == 0) ? " WHERE " : " AND ") + "updated_at < ? ";
            values.add(updatedAtEnd);
        }
        int pageNumberSanitized = (pageNumber != null) ? pageNumber : 0;
        int pageSizeSanitized = (pageSize != null) ? pageSize : 5000;
        SQL += " ORDER BY t.updated_at DESC";
        SQL += " LIMIT " + pageSizeSanitized;
        SQL += " OFFSET " + (pageNumberSanitized * pageSizeSanitized);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            int i = 1;
            for (Object obj : values) {
                if (obj instanceof List)
                    pstmt.setArray(i++, conn.createArrayOf("uuid", ((List<UUID>) obj).toArray(new UUID[0])));
                else
                    pstmt.setObject(i++, obj);
            }

            ResultSet rs = pstmt.executeQuery();
            List<TicketTemplate> ticketTemplates = new ArrayList<>();
            while (rs.next()) {
                TicketTemplate ticketTemplate = TicketTemplate.builder()
                        .id(rs.getString("tid"))
                        .name(rs.getString("name"))
                        .description(rs.getString("description"))
                        .enabled(rs.getBoolean("enabled"))
                        .defaultFields(mapper.readValue(rs.getString("default_fields"),
                                mapper.getTypeFactory().constructParametricType(
                                        HashMap.class, String.class, Boolean.class)))
                        .notifyBy(mapper.readValue(rs.getString("notify_by"),
                                mapper.getTypeFactory().constructMapLikeType(
                                        HashMap.class, EventType.class, List.class)))
                        .messageTemplateIds((rs.getArray("message_template_ids") != null &&
                                rs.getArray("message_template_ids").getArray() != null) ?
                                Arrays.asList((String[]) rs.getArray("message_template_ids").getArray()) : List.of())
                        .updatedAt(rs.getLong("updated_at"))
                        .createdAt(rs.getLong("created_at"))
                        .build();
                ticketTemplates.add(ticketTemplate);
            }
            return ticketTemplates;
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to read default fields.", e);
        }
    }

    private List<TicketTemplate> mergeMappingsAndFields(final List<TicketTemplate> ticketTemplates, final Map<UUID,
            List<TicketTemplateQuestionnaireTemplateMapping>> mappings, final Map<UUID, List<TicketField>> fields) {
        return ticketTemplates.stream()
                .map(t -> {
                    UUID ttId = UUID.fromString(t.getId());
                    if (!mappings.containsKey(ttId)) {
                        return t;
                    }
                    return t.toBuilder().mappings(mappings.get(ttId)).build();
                })
                .map(t -> {
                    UUID ttId = UUID.fromString(t.getId());
                    if (!fields.containsKey(ttId)) {
                        return t;
                    }
                    return t.toBuilder().ticketFields(fields.get(ttId)).build();
                }).collect(Collectors.toList());
    }

    private List<TicketTemplate> getBatch(String company, List<UUID> ids, Integer pageNumber, Integer pageSize) throws SQLException {
        List<TicketTemplate> ticketTemplates = getTicketTemplatesBatch(company, ids, null, pageNumber, pageSize);
        Map<UUID, List<TicketTemplateQuestionnaireTemplateMapping>> mappings = getMappingsBatch(company, ids);
        Map<UUID, List<TicketField>> fields = getTicketFieldsBatch(company, ids);
        return mergeMappingsAndFields(ticketTemplates, mappings, fields);
    }
    // endregion

    public DbListResponse<TicketTemplate> listByFilters(String company, String templateName, Integer pageNumber,
                                                        Integer pageSize) throws SQLException {
        return listByFilters(company, templateName, null, null, pageNumber, pageSize);
    }

    public DbListResponse<TicketTemplate> listByFilters(String company, String templateName, Long updatedAtStart, Long updatedAtEnd, Integer pageNumber,
                                                        Integer pageSize) throws SQLException {
        Integer totalCount = getBatchCount(company, templateName);
        List<TicketTemplate> ticketTemplates = getTicketTemplatesBatch(company, null, templateName, updatedAtStart, updatedAtEnd, pageNumber, pageSize);
        List<UUID> ticketTemplateIds = ticketTemplates.stream()
                .map(TicketTemplate::getId)
                .map(UUID::fromString)
                .collect(Collectors.toList());
        Map<UUID, List<TicketTemplateQuestionnaireTemplateMapping>> mappings = getMappingsBatch(company, ticketTemplateIds);
        Map<UUID, List<TicketField>> fields = getTicketFieldsBatch(company, ticketTemplateIds);
        List<TicketTemplate> fullMappings = mergeMappingsAndFields(ticketTemplates, mappings, fields);
        return DbListResponse.of(fullMappings, totalCount);
    }

    // region get list
    @Override
    public DbListResponse<TicketTemplate> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        Integer totalCount = getBatchCount(company, null);
        List<TicketTemplate> ticketTemplates = getTicketTemplatesBatch(company, null, null, pageNumber, pageSize);
        List<UUID> ticketTemplateIds = ticketTemplates.stream()
                .map(TicketTemplate::getId)
                .map(UUID::fromString)
                .collect(Collectors.toList());
        Map<UUID, List<TicketTemplateQuestionnaireTemplateMapping>> mappings = getMappingsBatch(company, ticketTemplateIds);
        Map<UUID, List<TicketField>> fields = getTicketFieldsBatch(company, ticketTemplateIds);
        List<TicketTemplate> fullMappings = mergeMappingsAndFields(ticketTemplates, mappings, fields);
        return DbListResponse.of(fullMappings, totalCount);
    }
    // endregion

    // region delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".ticket_templates WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setObject(1, UUID.fromString(id));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }
    // endregion

    public int bulkDelete(String company, List<String> ids) throws SQLException {
        String SQL = "DELETE FROM " + company + ".ticket_templates WHERE id IN (:ids)";
        int rowsDeleted = 0;
        if (CollectionUtils.isNotEmpty(ids)) {
            Map<String, Object> params = Map.of("ids", ids.stream().map(UUID::fromString)
                    .collect(Collectors.toList()));
            rowsDeleted = template.update(SQL, params);
        }
        return rowsDeleted;
    }

    // region ensureTableExistence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".ticket_templates(\n" +
                        "    id                     UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    name                   VARCHAR NOT NULL,\n" +
                        "    description            VARCHAR,\n" +
                        "    default_fields         JSONB NOT NULL DEFAULT '{}'::jsonb,\n" +
                        "    notify_by              JSONB NOT NULL DEFAULT '{}'::jsonb,\n" +
                        "    message_template_ids   VARCHAR[],\n" +
                        "    enabled                BOOLEAN NOT NULL,\n" +
                        "    updated_at             BIGINT DEFAULT extract(epoch from now()),\n" +
                        "    created_at             BIGINT DEFAULT extract(epoch from now())\n" +
                        ")",

                "CREATE INDEX IF NOT EXISTS ticket_templates_updated_at_idx ON " + company + ".ticket_templates(updated_at)",

                "CREATE TABLE IF NOT EXISTS " + company + ".ticket_template_questionnaire_template_mappings(\n" +
                        "    id                          UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    name                        VARCHAR NOT NULL,\n" +
                        "    ticket_template_id          UUID REFERENCES " + company + ".ticket_templates(id) ON DELETE CASCADE,\n" +
                        "    questionnaire_template_id   UUID REFERENCES " + company + ".questionnaire_templates(id) ON DELETE RESTRICT\n" +
                        ")",

                "CREATE INDEX IF NOT EXISTS ticket_template_questionnaire_template_mappings_ticket_template_id_idx on "
                        + company + ".ticket_template_questionnaire_template_mappings (ticket_template_id)",

                "CREATE INDEX IF NOT EXISTS ticket_template_questionnaire_template_mappings_questionnaire_template_id_idx on "
                        + company + ".ticket_template_questionnaire_template_mappings (questionnaire_template_id)",

                "CREATE TABLE IF NOT EXISTS " + company + ".ticket_fields(\n" +
                        "    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY, \n" +
                        "    key                     VARCHAR NOT NULL,\n" +
                        "    type                    VARCHAR NOT NULL,\n" +
                        "    options                 VARCHAR[],\n" +
                        "    required                BOOLEAN NOT NULL,\n" +
                        "    dynamic_resource_name   VARCHAR,\n" +
                        "    search_field            VARCHAR,\n" +
                        "    validation              VARCHAR,\n" +
                        "    display_name            VARCHAR,\n" +
                        "    description             VARCHAR,\n" +
                        "    ticket_template_id      UUID REFERENCES " + company + ".ticket_templates(id) ON DELETE CASCADE\n" +
                        ")",

                "CREATE INDEX IF NOT EXISTS ticket_fields_ticket_template_id_idx on " + company + ".ticket_fields (ticket_template_id)"
        );

        ddl.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
}
