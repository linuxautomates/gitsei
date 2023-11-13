package io.levelops.commons.databases.services;

import io.levelops.commons.database.ArrayWrapper;
import io.levelops.commons.database.DBUtils;
import io.levelops.commons.databases.models.database.NotificationMode;
import io.levelops.commons.databases.models.database.QuestionnaireNotification;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Log4j2
@Service
public class QuestionnaireNotificationsDatabaseService extends DatabaseService<QuestionnaireNotification> {
    private static final String INSERT_SQL_FORMAT = "INSERT INTO %s.questionnaire_notifications (questionnaire_id, mode, reference_id,channel_id,recipient,url) VALUES(?,?,?,?,?,?) RETURNING id";
    private static final String UPDATE_SQL_FORMAT = "UPDATE %s.questionnaire_notifications SET questionnaire_id = ?, mode = ?, reference_id = ?, channel_id = ?, recipient = ?, url = ? WHERE id = ?";
    private static final String DELETE_SQL_FORMAT = "DELETE FROM %s.questionnaire_notifications WHERE id = ?";

    private final NamedParameterJdbcTemplate template;

    // region CSTOR
    @Autowired
    public QuestionnaireNotificationsDatabaseService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }
    // endregion

    // region get references
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(QuestionnaireDBService.class);
    }
    // endregion

    // region insert
    @Override
    public String insert(String company, QuestionnaireNotification t) throws SQLException {
        String insertSql = String.format(INSERT_SQL_FORMAT, company);
        UUID cicdJobRunStageStepId;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setObject(1, t.getQuestionnaireId());
            pstmt.setString(2, String.valueOf(t.getMode()));
            pstmt.setString(3, t.getReferenceId());
            pstmt.setString(4, t.getChannelId());
            pstmt.setString(5, t.getRecipient());
            pstmt.setString(6, t.getUrl());

            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            if (affectedRows <= 0) {
                throw new SQLException("Failed to create cicd job!");
            }
            // get the ID back
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (!rs.next()) {
                    throw new SQLException("Failed to create cicd job!");
                }
                cicdJobRunStageStepId = (UUID) rs.getObject(1);
                return cicdJobRunStageStepId.toString();
            }
        }
    }
    // endregion

    // region update
    @Override
    public Boolean update(String company, QuestionnaireNotification t) throws SQLException {
        String updateSql = String.format(UPDATE_SQL_FORMAT, company);
        boolean success = true;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            pstmt.setObject(1, t.getQuestionnaireId());
            pstmt.setString(2, String.valueOf(t.getMode()));
            pstmt.setString(3, t.getReferenceId());
            pstmt.setString(4, t.getChannelId());
            pstmt.setString(5, t.getRecipient());
            pstmt.setString(6, t.getUrl());
            pstmt.setObject(7, t.getId());
            int affectedRows = pstmt.executeUpdate();
            success = (affectedRows > 0);
            return success;
        }
    }
    // endregion

    // region Get List Common
    private String formatCriterea(String criterea, List<Object> values, String newCriterea){
        String result = criterea + ((values.size() ==0) ? "" : "AND ");
        result += newCriterea;
        return result;
    }
    public DbListResponse<QuestionnaireNotification> getBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> questionnaireIds, List<String> referenceIds, List<String> channelIds, List<String> modes) throws SQLException {
        String selectSqlBase = "SELECT * FROM " + company + ".questionnaire_notifications";

        String orderBy = " ORDER BY created_at DESC ";
        String criteria = " WHERE ";
        List<Object> values = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(ids)) {
            criteria = formatCriterea(criteria, values, "id = ANY(?::uuid[]) ");
            values.add(new ArrayWrapper<>("uuid", ids));
        }
        if (CollectionUtils.isNotEmpty(questionnaireIds)) {
            criteria = formatCriterea(criteria, values, "questionnaire_id = ANY(?::uuid[]) ");
            values.add(new ArrayWrapper<>("uuid", questionnaireIds));
        }
        if (CollectionUtils.isNotEmpty(referenceIds)) {
            criteria = formatCriterea(criteria, values, "reference_id = ANY(?::varchar[]) ");
            values.add(new ArrayWrapper<>("varchar", referenceIds));
        }
        if (CollectionUtils.isNotEmpty(channelIds)) {
            criteria = formatCriterea(criteria, values, "channel_id = ANY(?::varchar[]) ");
            values.add(new ArrayWrapper<>("varchar", channelIds));
        }
        if (CollectionUtils.isNotEmpty(modes)) {
            criteria = formatCriterea(criteria, values, "mode = ANY(?::varchar[]) ");
            values.add(new ArrayWrapper<>("varchar", modes));
        }
        criteria = CollectionUtils.isEmpty(values) ? "" : criteria;

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" +  selectSqlBase + criteria + ") AS counted";

        List<QuestionnaireNotification> retval = new ArrayList<>();
        Integer totCount = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(selectSql);
             PreparedStatement pstmt2 = conn.prepareStatement(countSql)) {
            for (int i = 0; i < values.size(); i++) {
                Object obj = DBUtils.processArrayValues(conn, values.get(i));
                pstmt.setObject(i + 1, obj);
                pstmt2.setObject(i + 1, obj);
            }
            log.debug("Get or List Query = {}", pstmt);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                UUID id = (UUID) rs.getObject("id");
                UUID questionnaireId = (UUID) rs.getObject("questionnaire_id");
                String mode = rs.getString("mode");
                String referenceId = rs.getString("reference_id");
                String channelId = rs.getString("channel_id");
                String recipient = rs.getString("recipient");
                String url = rs.getString("url");

                Instant createdAt = DateUtils.toInstant(rs.getTimestamp("created_at"));

                QuestionnaireNotification questionnaireNotification = QuestionnaireNotification.builder()
                        .id(id)
                        .questionnaireId(questionnaireId)
                        .referenceId(referenceId)
                        .channelId(channelId)
                        .mode(NotificationMode.fromString(mode))
                        .recipient(recipient)
                        .url(url)
                        .createdAt(createdAt)
                        .build();

                retval.add(questionnaireNotification);
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
    // endregion

    @Override
    public Optional<QuestionnaireNotification> get(String company, String id) throws SQLException {
        var results = getBatch(company, 0, 10, Collections.singletonList(UUID.fromString(id)),null,null,null, null).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }
    // endregion

    @Override
    public DbListResponse<QuestionnaireNotification> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return getBatch(company, pageNumber, pageSize, null, null, null, null, null);
    }
    public DbListResponse<QuestionnaireNotification> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids, List<UUID> questionnaireIds, List<String> referenceIds, List<String> channelIds, List<String> modes) throws SQLException {
        return getBatch(company, pageNumber, pageSize, ids, questionnaireIds, referenceIds, channelIds, modes);
    }
    // endregion

    // region delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String deleteSql = String.format(DELETE_SQL_FORMAT, company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(deleteSql)) {
            pstmt.setObject(1, UUID.fromString(id));
            if (pstmt.executeUpdate() > 0) {
                return true;
            }
        }
        return false;
    }
    // endregion

    // region ensureTableExistence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + ".questionnaire_notifications( \n" +
                        "        id UUID PRIMARY KEY DEFAULT uuid_generate_v4(), \n" +
                        "        questionnaire_id UUID NOT NULL REFERENCES " + company + ".questionnaires(id) ON DELETE CASCADE, \n" +
                        "        mode VARCHAR NOT NULL, \n" +
                        "        reference_id VARCHAR NOT NULL, \n" +
                        "        channel_id VARCHAR, \n" +
                        "        recipient VARCHAR NOT NULL, \n" +
                        "        url VARCHAR, \n" +
                        "        created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now() \n" +
                        "    )",

                "CREATE INDEX IF NOT EXISTS questionnaire_notifications_reference_id_mode_idx on " + company + ".questionnaire_notifications (reference_id,mode)",

                "CREATE INDEX IF NOT EXISTS questionnaire_notifications_questionnaire_id_idx ON " + company + ".questionnaire_notifications(questionnaire_id)");

        ddl.forEach(template.getJdbcTemplate()::execute);
        return true;
    }
    // endregion
}
