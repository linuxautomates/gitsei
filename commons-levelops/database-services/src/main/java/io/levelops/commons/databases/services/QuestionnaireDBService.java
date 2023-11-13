package io.levelops.commons.databases.services;

import com.google.common.base.MoreObjects;
import com.google.common.base.Verify;
import io.levelops.commons.database.ArrayWrapper;
import io.levelops.commons.database.DBUtils;
import io.levelops.commons.databases.converters.QuestionnaireConverters;
import io.levelops.commons.databases.models.database.Questionnaire;
import io.levelops.commons.databases.models.database.Questionnaire.State;
import io.levelops.commons.databases.models.database.Severity;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping;
import io.levelops.commons.databases.models.filters.DateFilter;
import io.levelops.commons.databases.models.filters.QuestionnaireAggFilter;
import io.levelops.commons.databases.models.filters.QuestionnaireAggFilter.Calculation;
import io.levelops.commons.databases.models.filters.QuestionnaireAggFilter.Distinct;
import io.levelops.commons.databases.models.filters.QuestionnaireFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.models.response.QuestionnaireDetails;
import io.levelops.commons.databases.utils.StringHelper;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
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
import java.sql.Types;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.WorkItemDBService.UNASSIGNED_ID;

@Log4j2
@Service
public class QuestionnaireDBService extends DatabaseService<Questionnaire> {
    private static final TagItemMapping.TagItemType ITEM_TYPE = TagItemMapping.TagItemType.QUESTIONNAIRE_TEMPLATE;
    private static final String Q_KB_MAPPINGS_INSERT = "INSERT INTO %s.questionnaire_bpracticesitem_mappings(questionnaire_id,bpracticesitem_id) VALUES(?,?)";
    private static final String Q_KB_MAPPINGS_DELETE = "DELETE FROM %s.questionnaire_bpracticesitem_mappings WHERE questionnaire_id = ?";
    public static final int STACK_PAGE_OFFSET = 0;
    public static final int STACK_PAGE_SIZE = 90;

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public QuestionnaireDBService(DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
    }

    // region getReferences
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(QuestionnaireTemplateDBService.class, WorkItemDBService.class, ProductService.class, BestPracticesService.class);
    }
    // endregion

    // region QT KB Mappings
    private void deleteQKBMappings(PreparedStatement qKBMappingsDeletePtst, UUID questionnaireId) throws SQLException {
        qKBMappingsDeletePtst.setObject(1, questionnaireId);
        qKBMappingsDeletePtst.execute();
    }

    private List<String> insertQKBMappings(PreparedStatement qKBMappingsInsertPtst, UUID questionnaireId, List<UUID> kbIds) throws SQLException {
        if (CollectionUtils.isEmpty(kbIds)) {
            return Collections.emptyList();
        }
        for (UUID currentKbId : kbIds) {
            qKBMappingsInsertPtst.setObject(1, questionnaireId);
            qKBMappingsInsertPtst.setObject(2, currentKbId);
            qKBMappingsInsertPtst.addBatch();
            qKBMappingsInsertPtst.clearParameters();
        }
        qKBMappingsInsertPtst.executeBatch();
        List<String> ids = new ArrayList<>();
        try (ResultSet rs = qKBMappingsInsertPtst.getGeneratedKeys()) {
            while (rs.next()) {
                ids.add(rs.getString("id"));
            }
        }
        return ids;
    }
    // endregion

    // region create
    // we have to insert questionnaire id if it comes from the questionnaire because it is used in the url-building.
    @Override
    public String insert(String company, Questionnaire questionnaire) throws SQLException {

        String SQL = String.format("INSERT INTO %s.questionnaires("
                + "id,product_id,questionnaire_template_id,workitemid,targetemail,senderemail,bucketpath,bucketname,score,"
                + "answered,priority,messagesent,totalpossiblescore,total_questions,state,main) "
                + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)", company);
        String qKBMappingsInsertSql = String.format(Q_KB_MAPPINGS_INSERT, company);

        UUID id;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement qKBMappingsInsertPtst = conn.prepareStatement(qKBMappingsInsertSql, Statement.RETURN_GENERATED_KEYS)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                pstmt.setObject(1, (StringUtils.isEmpty(questionnaire.getId())) ? UUID.randomUUID() : UUID.fromString(questionnaire.getId()));
                if (StringUtils.isNotEmpty(questionnaire.getProductId()) && NumberUtils.isParsable(questionnaire.getProductId())) {
                    pstmt.setObject(2, Integer.parseInt(questionnaire.getProductId()));
                } else {
                    pstmt.setNull(2, Types.INTEGER);
                }
                pstmt.setObject(3, UUID.fromString(questionnaire.getQuestionnaireTemplateId()));
                pstmt.setObject(4, (StringUtils.isEmpty(questionnaire.getWorkItemId())) ? null : UUID.fromString(questionnaire.getWorkItemId()));
                pstmt.setString(5, questionnaire.getTargetEmail());
                pstmt.setString(6, questionnaire.getSenderEmail());
                pstmt.setString(7, questionnaire.getBucketPath());
                pstmt.setString(8, questionnaire.getBucketName());
                pstmt.setInt(9, questionnaire.getScore());
                pstmt.setInt(10, questionnaire.getAnswered());
                pstmt.setInt(11, questionnaire.getPriority().getValue());
                pstmt.setBoolean(12, Boolean.TRUE.equals(questionnaire.getMessageSent()));
                pstmt.setInt(13, questionnaire.getTotalPossibleScore());
                pstmt.setInt(14, questionnaire.getTotalQuestions());
                pstmt.setString(15, questionnaire.getState().toString());
                pstmt.setBoolean(16, BooleanUtils.isTrue(questionnaire.getMain()));
                int affectedRows = pstmt.executeUpdate();
                // check the affected rows
                if (affectedRows <= 0) {
                    throw new SQLException("Failed to create ticket!");
                }
                // get the ID back
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("Failed to create questionnaire!");
                    }
                    id = (UUID) rs.getObject(1);
                }

                insertQKBMappings(qKBMappingsInsertPtst, id, questionnaire.getKbIds());
                conn.commit();
                return id.toString();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
    }
    // endregion

    // region update
    @Override
    public Boolean update(String company, Questionnaire questionnaire) throws SQLException {
        String qKBMappingsDeleteSql = String.format(Q_KB_MAPPINGS_DELETE, company);
        String qKBMappingsInsertSql = String.format(Q_KB_MAPPINGS_INSERT, company);

        String updates = "";
        String condition = "id = ?";
        List<Object> values = new ArrayList<>();
        if (questionnaire.getAnswered() != null) {
            updates = "answered = ?";
            values.add(questionnaire.getAnswered());
        }
        if (StringUtils.isNotEmpty(questionnaire.getTargetEmail())) {
            updates = StringUtils.isEmpty(updates) ? "targetemail = ?" : updates + ", targetemail = ?";
            values.add(questionnaire.getTargetEmail());
        }
        if (questionnaire.getMessageSent() != null) {
            updates = StringUtils.isEmpty(updates) ? "messagesent = ?" : updates + ", messagesent = ?";
            values.add(questionnaire.getMessageSent());
        }
        if (questionnaire.getTotalPossibleScore() != null) {
            updates = StringUtils.isEmpty(updates) ? "totalpossiblescore = ?" : updates + ", totalpossiblescore = ?";
            values.add(questionnaire.getTotalPossibleScore());
        }
        if (questionnaire.getPriority() != null) {
            updates = StringUtils.isEmpty(updates) ? "priority = ?" : updates + ", priority = ?";
            values.add(questionnaire.getPriority().getValue());
        }
        if (questionnaire.getScore() != null) {
            updates = StringUtils.isEmpty(updates) ? "score = ?" : updates + ", score = ?";
            values.add(questionnaire.getScore());
        }
        // Update state
        if (questionnaire.getState() != null) {
            updates = StringUtils.isEmpty(updates) ? "state = ?" : updates + ", state = ?";
            values.add(questionnaire.getState().toString());
            if (questionnaire.getState() == State.COMPLETED) {
                updates = StringUtils.isEmpty(updates) ? "completedat = ?" : updates + ", completedat = ?";
                values.add(Instant.now().getEpochSecond());
            }
        }
        if (questionnaire.getMain() != null) {
            updates = StringUtils.isEmpty(updates) ? "main = ?" : updates + ", main = ?";
            values.add(questionnaire.getMain());
        }
        // Update time
        updates = StringUtils.isEmpty(updates) ? "updatedat = ?" : updates + ", updatedat = ?";
        values.add(Instant.now().getEpochSecond());
        String SQL = String.format("UPDATE %s.questionnaires "
                + "SET %s "
                + "WHERE %s", company, updates, condition);
        //no updates
        if (values.size() == 0) {
            return false;
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement qKBMappingsDeletePstmt = conn.prepareStatement(qKBMappingsDeleteSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement qKBMappingsInsertPstmt = conn.prepareStatement(qKBMappingsInsertSql, Statement.RETURN_GENERATED_KEYS)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                for (int i = 1; i <= values.size(); i++) {
                    pstmt.setObject(i, values.get(i - 1));
                }
                // id
                pstmt.setObject(values.size() + 1, UUID.fromString(questionnaire.getId()));
                int affectedRows = pstmt.executeUpdate();
                // check the affected rows
                if (affectedRows <= 0) {
                    throw new SQLException("Failed to update questionnaire!");
                }
                deleteQKBMappings(qKBMappingsDeletePstmt, UUID.fromString(questionnaire.getId()));
                insertQKBMappings(qKBMappingsInsertPstmt, UUID.fromString(questionnaire.getId()), questionnaire.getKbIds());
                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
    }
    // endregion

    // region get
    @Override
    public Optional<Questionnaire> get(String company, String qId) throws SQLException {
        var results = listByFilters(company, 0, 1, Collections.singletonMap("id", UUID.fromString(qId)));
        if (results.getRecords().size() == 0) {
            return Optional.empty();
        }
        return Optional.of(results.getRecords().get(0));
    }

    public Optional<QuestionnaireDetails> getDetails(String company, String qId)
            throws SQLException {
        var results = listQuestionnaireDetailsByFilters(company, 0, 1, null,
                null, null, null, Collections.singletonList(UUID.fromString(qId)),
                null, null, null, null, null, null, null, null, null);
        if (results.getRecords().size() == 0) {
            return Optional.empty();
        }
        return Optional.of(results.getRecords().get(0));
    }
    // endregion

    // region list helper
    @Override
    public DbListResponse<Questionnaire> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return listByFilters(company, pageNumber, pageSize, null);
    }

    public DbListResponse<Questionnaire> listByWorkItemId(final String company, final String workItemId,
                                                          final Integer pageNumber, final Integer pageSize)
            throws SQLException {
        Verify.verify(StringUtils.isNotBlank(workItemId), "[" + company
                + "] workItemId cannot be null nor empty when querying by listByWorkItemId.");
        return listByFilters(company, pageNumber, pageSize, Map.of("workitemid", UUID.fromString(workItemId)));
    }

    protected DbListResponse<Questionnaire> listByFilters(final String company, final Integer pageNumber,
                                                          final Integer pageSize, final Map<String, Object> filters)
            throws SQLException {
        String SQL = "SELECT "
                + "qs.id as id,"
                + "qs.questionnaire_template_id as questionnaire_template_id,"
                + "qs.workitemid as workitemid,"
                + "qs.product_id as product_id,"
                + "qs.targetemail as targetemail,"
                + "qs.senderemail as senderemail,"
                + "qs.bucketpath as bucketpath,"
                + "qs.bucketname as bucketname,"
                + "qs.answered as answered,"
                + "qs.score,"
                + "qs.priority,"
                + "qs.updatedat,"
                + "qs.createdat,"
                + "qs.messagesent as messagesent,"
                + "qs.totalpossiblescore,"
                + "qs.total_questions,"
                + "qs.state,"
                + "qs.main,"
                + "qs.completedat,"
                + "kb_ids"
                + " FROM " + company + ".questionnaires qs"
                + " LEFT OUTER JOIN ("
                + " SELECT mp.questionnaire_id::uuid, array_remove(array_agg(mp.bpracticesitem_id), NULL)::uuid[] as kb_ids FROM " + company + ".questionnaire_bpracticesitem_mappings as mp"
                + " GROUP BY mp.questionnaire_id ) as m ON m.questionnaire_id = qs.id";
        List<Object> params = new ArrayList<>();
        StringBuilder criteria = new StringBuilder("");
        if (filters != null && filters.size() > 0) {
            criteria.append(" WHERE");
            for (Entry<String, Object> filter : filters.entrySet()) {
                criteria.append(" qs.").append(filter.getKey()).append(" = ? AND");
                params.add(filter.getValue());
            }
            criteria = new StringBuilder(criteria.substring(0, criteria.length() - 4));
        }
        SQL = SQL + criteria + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSQL = "SELECT COUNT(*) FROM " + company + ".questionnaires qs" + criteria;
        Integer totCount = 0;
        List<Questionnaire> retval = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement recordsPstmt = conn.prepareStatement(SQL);
             PreparedStatement countPstmt2 = conn.prepareStatement(countSQL)) {
            if (params.size() > 0) {
                for (int i = 1; i <= params.size(); i++) {
                    recordsPstmt.setObject(i, params.get(i - 1));
                    countPstmt2.setObject(i, params.get(i - 1));
                }
            }
            ResultSet rs = recordsPstmt.executeQuery();
            while (rs.next()) {
                UUID[] kbIds = rs.getArray("kb_ids") != null
                        ? (UUID[]) rs.getArray("kb_ids").getArray()
                        : new UUID[0];
                Questionnaire questionnaire = Questionnaire.builder()
                        .id(String.valueOf(rs.getObject("id")))
                        .workItemId(StringHelper.valueOf(rs.getObject("workitemid")))
                        .questionnaireTemplateId(String.valueOf(rs.getObject("questionnaire_template_id")))
                        .senderEmail(rs.getString("senderemail"))
                        .targetEmail(rs.getString("targetemail"))
                        .bucketName(rs.getString("bucketname"))
                        .bucketPath(rs.getString("bucketpath"))
                        .answered(rs.getInt("answered"))
                        .score(rs.getInt("score"))
                        .productId(rs.getString("product_id"))
                        .priority(Severity.fromIntValue(rs.getInt("priority")))
                        .totalQuestions(rs.getInt("total_questions"))
                        .totalPossibleScore(rs.getInt("totalpossiblescore"))
                        .messageSent(rs.getBoolean("messagesent"))
                        .state(State.fromString(rs.getString("state")))
                        .main(rs.getBoolean("main"))
                        .completedAt(rs.getObject("completedat") != null ? rs.getLong("completedat") : null)
                        .createdAt(rs.getLong("createdat"))
                        .updatedAt(rs.getLong("updatedat"))
                        .kbIds(kbIds.length > 0 ? Arrays.asList(kbIds) : Collections.emptyList())
                        .build();
                retval.add(questionnaire);
            }
            if (retval.size() > 0) {
                totCount = retval.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
                if (retval.size() == pageSize) {
                    rs = countPstmt2.executeQuery();
                    if (rs.next()) {
                        totCount = rs.getInt("count");
                    }
                }
            }
        }
        return DbListResponse.of(retval, totCount);
    }
    // endregion

    // region list

    /**
     * @deprecated use listQuestionnaireDetailsByFilters with QuestionnaireFilter
     * (not adding new filters to the deprecated method)
     */
    @Deprecated
    public DbListResponse<QuestionnaireDetails> listQuestionnaireDetailsByFilters(final String company,
                                                                                  final Integer pageNumber,
                                                                                  final Integer pageSize,
                                                                                  final String productId,
                                                                                  final String priority,
                                                                                  final List<UUID> workItemIds,
                                                                                  final List<UUID> questionnaireTemplateIds,
                                                                                  final List<UUID> ids,
                                                                                  final List<String> tagIds,
                                                                                  final String targetEmail,
                                                                                  final Boolean main,
                                                                                  final State state,
                                                                                  final Boolean isFullyAnswered, // this looks at num qns answered vs total qns
                                                                                  final Long updatedAtStart,
                                                                                  final Long updatedAtEnd,
                                                                                  Long createdAtStart,
                                                                                  Long createdAtEnd)
            throws SQLException {
        return listQuestionnaireDetailsByFilters(company, pageNumber, pageSize, QuestionnaireFilter.builder()
                .productId(productId)
                .priority(priority)
                .workItemIds(workItemIds)
                .questionnaireTemplateIds(questionnaireTemplateIds)
                .ids(ids)
                .tagIds(tagIds)
                .targetEmail(targetEmail)
                .main(main)
                .state(state)
                .isFullyAnswered(isFullyAnswered)
                .updatedAt(DateFilter.builder()
                        .gt(updatedAtStart)
                        .lt(updatedAtEnd)
                        .build())
                .createdAt(DateFilter.builder()
                        .gt(createdAtStart)
                        .lt(createdAtEnd)
                        .build())
                .build());
    }

    public DbListResponse<QuestionnaireDetails> listQuestionnaireDetailsByFilters(final String company,
                                                                                  Integer pageNumber,
                                                                                  Integer pageSize,
                                                                                  QuestionnaireFilter questionnaireFilter) throws SQLException {
        pageNumber = MoreObjects.firstNonNull(pageNumber, 0);
        pageSize = MoreObjects.firstNonNull(pageSize, 25);

        boolean userJoinNeeded = false;
        List<String> conditions = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(questionnaireFilter.getWorkItemIds())) {
            conditions.add("qs.workitemid = ANY(?::uuid[])");
            values.add(new ArrayWrapper<>("uuid", questionnaireFilter.getWorkItemIds()));
        }
        if (CollectionUtils.isNotEmpty(questionnaireFilter.getQuestionnaireTemplateIds())) {
            conditions.add("qs.questionnaire_template_id = ANY(?::uuid[])");
            values.add(new ArrayWrapper<>("uuid", questionnaireFilter.getQuestionnaireTemplateIds()));
        }
        if (StringUtils.isNotEmpty(questionnaireFilter.getPriority())) {
            Severity severity = Severity.fromString(questionnaireFilter.getPriority());
            conditions.add("qs.priority = ?");
            values.add(severity.getValue());
        }
        if (StringUtils.isNotEmpty(questionnaireFilter.getProductId())) {
            conditions.add("qs.product_id = ?");
            values.add(Integer.parseInt(questionnaireFilter.getProductId()));
        }
        if (CollectionUtils.isNotEmpty(questionnaireFilter.getIds())) {
            conditions.add("qs.id = ANY(?::uuid[])");
            values.add(new ArrayWrapper<>("uuid", questionnaireFilter.getIds()));
        }
        if (CollectionUtils.isNotEmpty(questionnaireFilter.getTagIds())) {
            conditions.add("tag_ids && ?");
            values.add(new ArrayWrapper<>("varchar", questionnaireFilter.getTagIds()));
        }
        if (StringUtils.isNotEmpty(questionnaireFilter.getTargetEmail())) {
            conditions.add("qs.targetemail = ?");
            values.add(questionnaireFilter.getTargetEmail());
        }
        if (questionnaireFilter.getMain() != null) {
            conditions.add("qs.main = ?");
            values.add(questionnaireFilter.getMain());
        }
        if (questionnaireFilter.getState() != null) {
            conditions.add("qs.state = ?");
            values.add(questionnaireFilter.getState().toString());
        }
        if (questionnaireFilter.getUpdatedAt() != null && questionnaireFilter.getUpdatedAt().getGt() != null) {
            conditions.add("qs.updatedat > ?");
            values.add(questionnaireFilter.getUpdatedAt().getGt());
        }
        if (questionnaireFilter.getUpdatedAt() != null && questionnaireFilter.getUpdatedAt().getLt() != null) {
            conditions.add("qs.updatedat < ?");
            values.add(questionnaireFilter.getUpdatedAt().getLt());
        }
        if (questionnaireFilter.getCreatedAt() != null && questionnaireFilter.getCreatedAt().getGt() != null) {
            conditions.add("qs.createdat > ?");
            values.add(questionnaireFilter.getCreatedAt().getGt());
        }
        if (questionnaireFilter.getCreatedAt() != null && questionnaireFilter.getCreatedAt().getLt() != null) {
            conditions.add("qs.createdat < ?");
            values.add(questionnaireFilter.getCreatedAt().getLt());
        }
        if (questionnaireFilter.getIsFullyAnswered() != null) {
            conditions.add(String.format("qs.answered %s qs.total_questions",
                    questionnaireFilter.getIsFullyAnswered() ? "=" : "!="));
        }
        if (CollectionUtils.isNotEmpty(questionnaireFilter.getAssigneeUserIds())) {
            userJoinNeeded = true;
            List<String> assigneeUserIds = questionnaireFilter.getAssigneeUserIds();
            String assigneeCondition = "assignee_user_ids && ?";
            if (assigneeUserIds.contains(UNASSIGNED_ID)) {
                assigneeCondition = String.format("(%s OR assignee_user_ids IS NULL)", assigneeCondition);
                assigneeUserIds = ListUtils.removeAll(assigneeUserIds, List.of(UNASSIGNED_ID));
            }
            conditions.add(assigneeCondition);
            values.add(new ArrayWrapper<>("varchar", assigneeUserIds));
        }
        String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions) + " ";


        String templateJoin = "" +
                " LEFT OUTER JOIN (" +
                "    SELECT id,name " +
                "    FROM {0}.questionnaire_templates " +
                " ) q ON qs.questionnaire_template_id = q.id ";
        String tagJoin = "" +
                " LEFT OUTER JOIN ( " +
                "    SELECT " +
                "      ti.itemid::uuid," +
                "      array_remove(array_agg(ti.tagid), NULL)::varchar[] as tag_ids " +
                "    FROM {0}.tagitems as ti " +
                "    WHERE ti.itemtype IN ('''" + ITEM_TYPE + "''', '''" + WorkItem.ITEM_TYPE + "''') " +
                "    GROUP BY ti.itemid " +
                " ) as t ON t.itemid = q.id ";
        String workItemJoin = "" +
                " LEFT OUTER JOIN (" +
                "    SELECT " +
                "      id, " +
                "      artifact, " +
                "      integrationid," +
                "      state_id,  " +
                "      reason " +
                "    FROM {0}.workitems " +
                " ) wi ON qs.workitemid = wi.id ";
        String integrationJoin = "" +
                " LEFT OUTER JOIN ( " +
                "    SELECT application,url,id FROM {0}.integrations " +
                " ) i ON wi.integrationid = i.id ";
        String kbJoin = "" +
                " LEFT OUTER JOIN ( " +
                "    SELECT " +
                "        mp.questionnaire_id::uuid, " +
                "        array_remove(array_agg(mp.bpracticesitem_id), NULL)::uuid[] as kb_ids " +
                "    FROM {0}.questionnaire_bpracticesitem_mappings as mp " +
                "    GROUP BY mp.questionnaire_id " +
                " ) as m ON m.questionnaire_id = qs.id ";
        String userJoin = "";
        if (userJoinNeeded) {
            userJoin = " LEFT JOIN " +
                    "( " +
                    "  SELECT work_item_id, ARRAY_REMOVE(ARRAY_AGG(user_id), NULL)::VARCHAR[] AS assignee_user_ids " +
                    "  FROM " + company + ".ticket_user_mappings" +
                    "  GROUP BY work_item_id " +
                    ") AS u " +
                    " ON u.work_item_id = qs.workitemid ";
        }
        String sql = "" +
                " SELECT " +
                "   qs.id as id," +
                "   qs.workitemid," +
                "   qs.senderemail as senderemail," +
                "   qs.targetemail as targetemail," +
                "   qs.bucketname as bucketname," +
                "   qs.bucketpath as bucketpath," +
                "   qs.answered as answered," +
                "   qs.total_questions," +
                "   qs.score as score," +
                "   qs.product_id as product_id," +
                "   qs.createdat as createdat," +
                "   qs.updatedat as updatedat," +
                "   qs.totalpossiblescore," +
                "   qs.priority," +
                "   qs.state," +
                "   qs.main," +
                "   qs.completedat," +
                "   qs.questionnaire_template_id," +
                "   qs.messagesent," +
                "   wi.reason as reason," +
                "   wi.integrationid," +
                "   wi.artifact as artifact," +
                "   wi.state_id," +
                "   q.name as questionnaire_template_name," +
                "   i.application as integapp, " +
                "   i.url as integurl," +
                "   tag_ids," +
                "   kb_ids " +
                " FROM {0}.questionnaires qs " +
                templateJoin +
                tagJoin +
                workItemJoin +
                integrationJoin +
                kbJoin +
                userJoin;
        sql = MessageFormat.format(sql, company);

        //count query needs to happen before sql change.
        String countSQL = "SELECT COUNT(*) FROM ( " + sql + where + " ) t";
        sql = sql + where + " ORDER BY updatedat DESC LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        List<QuestionnaireDetails> retval = new ArrayList<>();
        Integer totCount = 0;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement recordsPstmt = conn.prepareStatement(sql);
             PreparedStatement countPstmt2 = conn.prepareStatement(countSQL)) {
            for (int i = 0; i < values.size(); i++) {
                Object obj = DBUtils.processArrayValues(conn, values.get(i));
                recordsPstmt.setObject(i + 1, obj);
                countPstmt2.setObject(i + 1, obj);
            }

            ResultSet rs = recordsPstmt.executeQuery();
            while (rs.next()) {
                String[] rsTagIds = rs.getArray("tag_ids") != null
                        ? (String[]) rs.getArray("tag_ids").getArray()
                        : new String[0];
                UUID[] kbIds = rs.getArray("kb_ids") != null
                        ? (UUID[]) rs.getArray("kb_ids").getArray()
                        : new UUID[0];
                retval.add(QuestionnaireDetails.questionnaireDetailsBuilder()
                        .id(String.valueOf(rs.getObject("id")))
                        .workItemId(StringHelper.valueOf(rs.getObject("workitemid")))
                        .reason(rs.getString("reason"))
                        .questionnaireTemplateId(String.valueOf(rs.getObject("questionnaire_template_id")))
                        .questionnaireTemplateName(rs.getString("questionnaire_template_name"))
                        .integrationApplication(rs.getString("integapp"))
                        .integrationUrl(rs.getString("integurl"))
                        .artifact(rs.getString("artifact"))
                        .productId(rs.getString("product_id"))
                        .senderEmail(rs.getString("senderemail"))
                        .targetEmail(rs.getString("targetemail"))
                        .bucketName(rs.getString("bucketname"))
                        .bucketPath(rs.getString("bucketpath"))
                        .answered(rs.getInt("answered"))
                        .totalQuestions(rs.getInt("total_questions"))
                        .totalPossibleScore(rs.getInt("totalpossiblescore"))
                        .score(rs.getInt("score"))
                        .priority(Severity.fromIntValue(rs.getInt("priority")))
                        .state(State.fromString(rs.getString("state")))
                        .main(rs.getBoolean("main"))
                        .completedAt(rs.getObject("completedat") != null ? rs.getLong("completedat") : null)
                        .createdAt(rs.getLong("createdat"))
                        .updatedAt(rs.getLong("updatedat"))
                        .tagIds(rsTagIds.length > 0 ? Arrays.asList(rsTagIds) : Collections.emptyList())
                        .kbIds(kbIds.length > 0 ? Arrays.asList(kbIds) : Collections.emptyList())
                        .messageSent(rs.getBoolean("messagesent"))
                        .build());
            }
            if (retval.size() > 0) {
                totCount = retval.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
                if (retval.size() == pageSize) {
                    rs = countPstmt2.executeQuery();
                    if (rs.next()) {
                        totCount = rs.getInt("count");
                    }
                }
            }
        }
        return DbListResponse.of(retval, totCount);
    }
    // endregion

    // region aggs


    public DbListResponse<DbAggregationResult> aggregate(String company,
                                                         QuestionnaireAggFilter filter,
                                                         Integer pageNumber, Integer pageSize) throws SQLException {
        pageNumber = MoreObjects.firstNonNull(pageNumber, 0);
        pageSize = MoreObjects.firstNonNull(pageSize, 25);
        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");
        Calculation calculation = MoreObjects.firstNonNull(filter.getCalculation(), Calculation.count);


        // -- CALCULATION

        String calculationComponent, orderByString;
        switch (calculation) {
            case response_time:
                calculationComponent = "" +
                        "MIN(resp_time) AS mn," +
                        "MAX(resp_time) AS mx," +
                        "COUNT(id) AS ct," +
                        "PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY resp_time)";
                orderByString = "mx DESC";
                break;

            default:
            case count:
                orderByString = "ct DESC";
                calculationComponent = "COUNT(id) as ct";
                break;
        }

        // -- GROUP BY

        boolean userJoinNeeded = false;
        boolean tagJoinNeeded = false;
        boolean workItemJoinNeeded = false;
        boolean wiTagJoinNeeded = false;
        String selectDistinctString, groupByString;
        String aggResultKey = filter.getAcross().toString();
        switch (filter.getAcross()) {

            case assignee:
                userJoinNeeded = true;
                groupByString = filter.getAcross().toString();
                // using coalesce to have a bucket for unassigned
                selectDistinctString = "UNNEST(COALESCE(user_ids, ARRAY['" + UNASSIGNED_ID + "'])) AS " + groupByString;
                break;
            case tag:
                tagJoinNeeded = true;
                groupByString = filter.getAcross().toString();
                selectDistinctString = "UNNEST(tag_ids) AS " + groupByString;
                break;
            case work_item_tag:
                wiTagJoinNeeded = true;
                groupByString = filter.getAcross().toString();
                selectDistinctString = "UNNEST(wi_tag_ids) AS " + groupByString;
                break;
            case trend:
                if (calculation != Calculation.count) {
                    throw new UnsupportedOperationException("Trend is only supported across count");
                }
                // To get the accumulated sum grouped by day,
                // we first need to aggregate the data per day.
                // Later, we'll run another query to accumulate the results.
                groupByString = "day";
                orderByString = "day ASC";
                selectDistinctString = "EXTRACT(EPOCH FROM DATE_TRUNC('day', TO_TIMESTAMP(updatedat)))::TEXT AS day";
                break;
            case created:
            case updated:
                groupByString = filter.getAcross().toString();
                orderByString = filter.getAcross().toString() + " ASC";
                selectDistinctString = filter.getAcross() + "at AS " + filter.getAcross();
                break;
            case questionnaire_template_id:
                groupByString = filter.getAcross().toString();
                selectDistinctString = filter.getAcross().toString();
                break;
            case completed:
                groupByString = filter.getAcross().toString();
                selectDistinctString = "completed::TEXT";
                break;
            case submitted:
                groupByString = filter.getAcross().toString();
                selectDistinctString = "submitted::TEXT";
                break;
            case state:
                workItemJoinNeeded = true;
                groupByString = "wi_state_id";
                orderByString = "wi_state_id";
                aggResultKey = "wi_state_id";
                selectDistinctString = "COALESCE(wi_state_id::text, 'unknown') AS wi_state_id";
                break;
            case work_item_product:
                workItemJoinNeeded = true;
                groupByString = "wi_product_id";
                orderByString = "wi_product_id";
                aggResultKey = "wi_product_id";
                selectDistinctString = "COALESCE(wi_product_id::text, 'unknown') AS wi_product_id";
                break;
            default:
                throw new UnsupportedOperationException("Across not supported: " + filter.getAcross());
        }

        // -- WHERE

        List<String> questionnaireConditions = new ArrayList<>();
        List<String> tagConditions = new ArrayList<>();

        Map<String, Object> params = new HashMap<>();
        if (CollectionUtils.isNotEmpty(filter.getQuestionnaireTemplateId())) {
            questionnaireConditions.add("questionnaire_template_id::TEXT IN (:questionnaire_template_ids)");
            params.put("questionnaire_template_ids", filter.getQuestionnaireTemplateId());
        }
        if (CollectionUtils.isNotEmpty(filter.getAssignees())) {
            // use && for OR between users or @> for AND between users
            questionnaireConditions.add("user_ids && ARRAY[ :user_ids ] ");
            params.put("user_ids", filter.getAssignees());
            userJoinNeeded = true;
        }
        if (filter.getUnassigned() != null) {
            questionnaireConditions.add("(:unassigned_id = ANY(user_ids)) = :unassigned");
            params.put("unassigned_id", UNASSIGNED_ID);
            params.put("unassigned", filter.getUnassigned());
            userJoinNeeded = true;
        }
        if (filter.getCompleted() != null) {
            questionnaireConditions.add(String.format("q.answered %s q.total_questions",
                    filter.getCompleted() ? "=" : "!="));
        }
        if (filter.getSubmitted() != null) {
            questionnaireConditions.add(String.format("q.state %s 'COMPLETED'",
                    filter.getSubmitted() ? "=" : "!="));
        }
        if (CollectionUtils.isNotEmpty(filter.getTags())) {
            tagConditions.add("tagid::TEXT IN (:tag_ids)");
            params.put("tag_ids", filter.getTags());
            tagJoinNeeded = true;
        }
        if (CollectionUtils.isNotEmpty(filter.getWorkItemTags())) {
            // using && for OR between tags (use @> for AND)
            questionnaireConditions.add("wi_tag_ids::VARCHAR[] && ARRAY[ :wi_tag_ids ]");
            params.put("wi_tag_ids", filter.getWorkItemTags());
            wiTagJoinNeeded = true;
        }
        if (CollectionUtils.isNotEmpty(filter.getStates())) {
            questionnaireConditions.add("wi_state_id::VARCHAR IN (:wi_state_ids)");
            params.put("wi_state_ids", filter.getStates());
        }
        if (CollectionUtils.isNotEmpty(filter.getWorkItemProductIds())) {
            questionnaireConditions.add("wi_product_id::VARCHAR IN (:wi_product_ids)");
            params.put("wi_product_ids", filter.getWorkItemProductIds());
        }
        if (filter.getUpdatedAt() != null && filter.getUpdatedAt().getGt() != null) {
            questionnaireConditions.add("updatedat > :updatedAtStart");
            params.put("updatedAtStart", filter.getUpdatedAt().getGt());
        }
        if (filter.getUpdatedAt() != null && filter.getUpdatedAt().getLt() != null) {
            questionnaireConditions.add("updatedat < :updatedAtEnd");
            params.put("updatedAtEnd", filter.getUpdatedAt().getLt());
        }
        if (filter.getCreatedAt() != null && filter.getCreatedAt().getGt() != null) {
            questionnaireConditions.add("createdat > :createdAtStart");
            params.put("createdAtStart", filter.getCreatedAt().getGt());
        }
        if (filter.getCreatedAt() != null && filter.getCreatedAt().getLt() != null) {
            questionnaireConditions.add("createdat < :createdAtEnd");
            params.put("createdAtEnd", filter.getCreatedAt().getLt());
        }

        // -- questionnaire where
        String questionnaireWhere = questionnaireConditions.isEmpty() ? "" :
                " WHERE " + String.join(" AND ", questionnaireConditions);

        // -- user join
        String userJoin = "";
        if (userJoinNeeded) {
            userJoin = " LEFT JOIN " +
                    "( " +
                    "  SELECT work_item_id, ARRAY_REMOVE(ARRAY_AGG(user_id), NULL)::VARCHAR[] AS user_ids " +
                    "  FROM " + company + ".ticket_user_mappings" +
                    "  GROUP BY work_item_id " +
                    ") AS u " +
                    " ON u.work_item_id = q.workitemid ";
        }

        // -- tags join
        String tagJoin = "";
        if (tagJoinNeeded) {
            tagConditions.add("itemtype = '" + ITEM_TYPE + "'");
            // we want inner join (and not left join) because we don't want tag-less records
            // and we are filtering on the inner query (we may not be doing that for other tags queries)
            tagJoin = " INNER JOIN " +
                    "( " +
                    "  SELECT itemid::UUID, ARRAY_REMOVE(ARRAY_AGG(tagid), NULL)::VARCHAR[] AS tag_ids " +
                    "  FROM " + company + ".tagitems " +
                    "  WHERE " + String.join(" AND ", tagConditions) +
                    "  GROUP BY itemid " +
                    ") AS tags ON tags.itemid = q.questionnaire_template_id ";
        }

        // -- wiTags join
        String wiTagsJoin = "";
        if (wiTagJoinNeeded) {
            String wiTagsConditions = "itemtype = '" + WorkItem.ITEM_TYPE + "'";
            wiTagsJoin = " LEFT JOIN " +
                    "( " +
                    "  SELECT itemid::UUID, ARRAY_REMOVE(ARRAY_AGG(tagid), NULL)::VARCHAR[] AS wi_tag_ids " +
                    "  FROM " + company + ".tagitems " +
                    "  WHERE " + wiTagsConditions +
                    "  GROUP BY itemid " +
                    ") AS wi_tags ON wi_tags.itemid = q.workitemid ";
        }

        // -- wi join
        String workItemJoin = "";
        if (workItemJoinNeeded) {
            workItemJoin = " LEFT JOIN " +
                    "( " +
                    "  SELECT " +
                    "    id as wi_id," +
                    "    state_id as wi_state_id, " +
                    "    product_id as wi_product_id " +
                    "  FROM " + company + ".workitems " +
                    ") AS wi ON wi.wi_id = q.workitemid ";
        }


        // --- FINAL QUERY ---

        String sql = "SELECT " +
                selectDistinctString + "," +
                calculationComponent +
                " FROM" +
                " ( SELECT " +
                "     *," +
                "     (q.answered = q.total_questions) AS completed, " +
                "     (q.state = 'COMPLETED') AS submitted, " +
                "     (CASE WHEN (q.answered > 0) THEN (updatedat - createdat) " +
                "           ELSE  (" + Instant.now().getEpochSecond() + " - createdat) " +
                "           END) AS resp_time " +
                "   FROM " + company + ".questionnaires AS q " +
                userJoin +
                tagJoin +
                workItemJoin +
                wiTagsJoin +
                questionnaireWhere +
                " ) AS finaltable " +
                " GROUP BY " + groupByString +
                " ORDER BY " + orderByString;

        String countSQL = "SELECT COUNT(*) FROM ( " + sql + " ) t";

        if (filter.getAcross() == Distinct.trend) {
            // To get a trend, we do a cumulative sum of the # of new items per day.
            // - the sum has to sort from oldest to newest (ASC) to get the right sum
            // - the limit has to be taken after sorting from newest to oldest (DESC)
            // - the inner query cannot be limited or the sum will miss items
            sql = "" +
                    " SELECT day AS trend, " +
                    " SUM(ct) OVER (ORDER BY day ASC ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS ct " +
                    " FROM (" + sql + ") AS aggregated_per_day" +
                    " ORDER BY trend DESC" +
                    " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);

            // apply the desired sort (ASC) order *after* the sum has been done
            sql = "SELECT trend, ct FROM (" + sql + ") AS trend_table ORDER BY trend ASC";

        } else {
            sql += " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        }

        Integer count = 0;
        count = template.queryForObject(countSQL, params, Integer.class);
        List<DbAggregationResult> results = template.query(sql, params,
                QuestionnaireConverters.aggResultRowMapper(aggResultKey, calculation));

        return DbListResponse.of(results, count);
    }

    public DbListResponse<DbAggregationResult> stackedAggregate(String company,
                                                                QuestionnaireAggFilter filter,
                                                                List<Distinct> stacks,
                                                                Integer pageNumber,
                                                                Integer pageSize) throws SQLException {
        // first aggregation
        Distinct firstDistinct = filter.getAcross();
        DbListResponse<DbAggregationResult> result = aggregate(company, filter, pageNumber, pageSize);

        // check if there is a second aggregation and if stacking is supported
        Optional<Distinct> secondDistinctOpt = IterableUtils.getFirst(stacks)
                .filter(Distinct.STACKABLE::contains);
        if (secondDistinctOpt.isEmpty() || !Distinct.STACKABLE.contains(firstDistinct)) {
            // no stack - or stack not supported
            return result;
        }

        Distinct secondDistinct = secondDistinctOpt.get();
        try {
            List<DbAggregationResult> finalList = ListUtils.emptyIfNull(result.getRecords()).stream()
                    .map(RuntimeStreamException.wrap(row -> {
                        if (row.getKey() == null) {
                            return row.toBuilder()
                                    .stacks(List.of())
                                    .build();
                        }
                        var newFilter = filter.toBuilder()
                                .across(secondDistinct);
                        switch (firstDistinct) {
                            case questionnaire_template_id:
                                newFilter.questionnaireTemplateId(List.of(row.getKey()));
                                break;
                            case assignee:
                                newFilter.assignees(List.of(row.getKey()));
                                break;
                            case completed:
                                newFilter.completed(Boolean.valueOf(row.getKey()));
                                break;
                            case submitted:
                                newFilter.submitted(Boolean.valueOf(row.getKey()));
                                break;
                            case tag:
                                newFilter.tags(List.of(row.getKey()));
                                break;
                            default:
                                throw new UnsupportedOperationException("Stacked agg not supported: " + firstDistinct);
                        }
                        return row.toBuilder()
                                .stacks(aggregate(company, newFilter.build(), STACK_PAGE_OFFSET, STACK_PAGE_SIZE).getRecords())
                                .build();
                    }))
                    .collect(Collectors.toList());
            return DbListResponse.of(finalList, finalList.size());
        } catch (RuntimeStreamException e) {
            throw new SQLException("Failed to run stacked aggregation", e);
        }
    }
    // endregion


    // region delete
    public Optional<Questionnaire> deleteAndReturn(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".questionnaires WHERE id = ? RETURNING *";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL)) {
            pstmt.setObject(1, UUID.fromString(id));
            pstmt.executeQuery();
            ResultSet rs = pstmt.getResultSet();
            if (rs.next()) {
                return Optional.of(Questionnaire.builder()
                        .id(String.valueOf(rs.getObject("id")))
                        .questionnaireTemplateId(String.valueOf(rs.getObject("questionnaire_template_id")))
                        .workItemId(StringHelper.valueOf(rs.getObject("workitemid")))
                        .senderEmail(rs.getString("senderemail"))
                        .targetEmail(rs.getString("targetemail"))
                        .answered(rs.getInt("answered"))
                        .score(rs.getInt("score"))
                        .productId(rs.getString("product_id"))
                        .bucketName(rs.getString("bucketname"))
                        .bucketPath(rs.getString("bucketpath"))
                        .createdAt(rs.getLong("createdat"))
                        .updatedAt(rs.getLong("updatedat"))
                        .priority(Severity.fromIntValue(rs.getInt("priority")))
                        .messageSent(rs.getBoolean("messagesent"))
                        .totalPossibleScore(rs.getInt("totalpossiblescore"))
                        .state(State.fromString(rs.getString("state")))
                        .completedAt(rs.getObject("completedat") != null ? rs.getLong("completedat") : null)
                        .build());
            }
        }
        return Optional.empty();
    }

    @Override
    public Boolean delete(String company, String id) {
        throw new UnsupportedOperationException();
    }
    // endregion

    // region ensureTableExistence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqls = new ArrayList<>();
        String sql = "CREATE TABLE IF NOT EXISTS " + company + ".questionnaires(\n" +
                "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    questionnaire_template_id UUID REFERENCES " + company + ".questionnaire_templates(id) ON DELETE SET NULL, \n" +
                "    workitemid UUID REFERENCES " + company + ".workitems(id) ON DELETE CASCADE, \n" +
                "    product_id INTEGER REFERENCES " + company + ".products(id) ON DELETE SET NULL,\n" +
                "    targetemail VARCHAR,\n" +
                "    senderemail VARCHAR,\n" +
                "    answered INTEGER,\n" +
                "    score INTEGER,\n" +
                "    total_questions INTEGER NOT NULL,\n" +
                "    totalpossiblescore INTEGER,\n" +
                "    priority INTEGER,\n" +
                "    bucketpath VARCHAR NOT NULL,\n" +
                "    bucketname VARCHAR NOT NULL,\n" +
                "    messagesent BOOLEAN NOT NULL DEFAULT false,\n" +
                "    main BOOLEAN NOT NULL DEFAULT false,\n" +
                "    state VARCHAR NOT NULL DEFAULT 'CREATED',\n" +
                "    updatedat BIGINT DEFAULT extract(epoch from now()),\n" +
                "    completedat BIGINT,\n" + // NOT USED??
                "    createdat BIGINT DEFAULT extract(epoch from now())\n" +
                ")";
        sqls.add(sql);

        sql = "CREATE INDEX IF NOT EXISTS questionnaire_questionnaire_template_id_idx on " + company + ".questionnaires (questionnaire_template_id)";
        sqls.add(sql);

        sql = "CREATE INDEX IF NOT EXISTS questionnaire_workitemid_idx on " + company + ".questionnaires (workitemid)";
        sqls.add(sql);

        sql = "CREATE INDEX IF NOT EXISTS questionnaire_product_id_idx on " + company + ".questionnaires (product_id)";
        sqls.add(sql);

        sql = "CREATE TABLE IF NOT EXISTS " + company + ".questionnaire_bpracticesitem_mappings(\n" +
                "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    questionnaire_id UUID REFERENCES " + company + ".questionnaires(id) ON DELETE CASCADE,\n" +
                "    bpracticesitem_id UUID REFERENCES " + company + ".bpracticesitems(id) ON DELETE CASCADE\n" +
                ")";
        sqls.add(sql);

        sql = "CREATE INDEX IF NOT EXISTS questionnaire_bpracticesitem_mappings_questionnaire_id_idx on "
                + company + ".questionnaire_bpracticesitem_mappings (questionnaire_id)";
        sqls.add(sql);

        sql = "CREATE INDEX IF NOT EXISTS questionnaire_bpracticesitem_mappings_bpracticesitem_id_idx on "
                + company + ".questionnaire_bpracticesitem_mappings (bpracticesitem_id)";
        sqls.add(sql);

        try (Connection conn = dataSource.getConnection()) {
            for (String currentSql : sqls) {
                try (PreparedStatement pstmt = conn.prepareStatement(currentSql)) {
                    pstmt.execute();
                }
            }
            return true;
        }
    }
    // endregion
}
