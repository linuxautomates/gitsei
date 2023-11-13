package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import io.levelops.commons.database.ArrayWrapper;
import io.levelops.commons.database.DBUtils;
import io.levelops.commons.databases.converters.WorkItemConverters;
import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.Severity;
import io.levelops.commons.databases.models.database.State;
import io.levelops.commons.databases.models.database.TicketData;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.databases.models.filters.WorkItemFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.dates.DurationUtils;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.UUIDUtils;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Duration;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.database.WorkItem.ITEM_TYPE;

@Log4j2
@Service
public class WorkItemDBService extends DatabaseService<WorkItem> {
    public static final String UNASSIGNED_ID = "unassigned";
    public static final String UNKNOWN_REPORTER = "unknown";

    private static final String TICKET_DATA_DELETE_QUERY = "DELETE FROM %s.ticket_data WHERE work_item_id = ?";
    private static final String TICKET_DATA_INSERT_SQL = "INSERT INTO %s.ticket_data(work_item_id,values,ticket_field_id) VALUES(?,to_json(?::json),?) ON CONFLICT(work_item_id,ticket_field_id) DO NOTHING";

    private static final String TICKET_USER_MAPPINGS_DELETE_SQL = "DELETE FROM %s.ticket_user_mappings WHERE work_item_id = ?";
    private static final String TICKET_USER_MAPPINGS_SQL = "INSERT INTO %s.ticket_user_mappings(work_item_id,user_id) VALUES(?,?) ON CONFLICT(work_item_id,user_id) DO NOTHING";

    private static final String ATTACHMENTS_DELETE_SQL = "DELETE FROM %s.ticket_attachments WHERE work_item_id = ?";
    private static final String ATTACHMENTS_SQL = "INSERT INTO %s.ticket_attachments(work_item_id,upload_id,file_name,comment) VALUES(?,?,?,?) ON CONFLICT(work_item_id,upload_id) DO NOTHING";

    private static final String VANITY_ID_QUERY = "INSERT INTO %s.work_item_vanity_ids(work_item_id,product_id,product_sequence_number) VALUES(?,?,nextval(?)) ON CONFLICT(work_item_id,product_id) DO NOTHING RETURNING product_sequence_number";
    private static final String VANITY_ID_GET_QUERY = "SELECT product_sequence_number from %s.work_item_vanity_ids WHERE work_item_id = ? AND product_id = ?";

    private static final String CICD_MAPPINGS_INSERT_SQL = "INSERT INTO %s.workitem_cicd_mappings(work_item_id,cicd_job_run_id,cicd_job_run_stage_id) VALUES(?,?,?)";

    private static final String TICKET_UPDATE_STATE_FORMAT = "UPDATE %s.workitems SET state_id = ? WHERE id = ?";

    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper mapper;
    private final ProductService productService;
    private final StateDBService stateDBService;
    private final boolean enablePerformanceLog;
    private final int perfLogWarnThresholdMs;

    // region CSTOR
    @Autowired
    public WorkItemDBService(DataSource dataSource,
                             ObjectMapper mapper,
                             ProductService productService,
                             StateDBService stateDBService,
                             @Value("${work_items.perf_log.enabled:true}") boolean enablePerformanceLog,
                             @Value("${work_items.perf_log.warn_threshold_ms:100}") int perfLogWarnThresholdMs) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        this.mapper = mapper;
        this.productService = productService;
        this.stateDBService = stateDBService;
        this.enablePerformanceLog = enablePerformanceLog;
        this.perfLogWarnThresholdMs = perfLogWarnThresholdMs;
    }
    // endregion

    // region get references
    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class, UserService.class, ProductService.class, StateDBService.class, TicketTemplateDBService.class, CiCdJobRunsDatabaseService.class, CiCdJobRunStageDatabaseService.class);
    }
    // endregion

    // region Commons for Create and Update
    private void insertTicketDataValues(Connection conn, PreparedStatement pstmt, UUID workItemId, List<TicketData> ticketDataList) throws JsonProcessingException, SQLException {
        if (CollectionUtils.isEmpty(ticketDataList)) {
            return;
        }
        for (TicketData td : ticketDataList) {
            pstmt.setObject(1, workItemId);
            pstmt.setString(2, mapper.writeValueAsString(td.getData().getValues()));
            pstmt.setObject(3, td.getTicketFieldId());

            pstmt.addBatch();
            pstmt.clearParameters();
        }
        pstmt.executeBatch();
    }

    private void insertTicketAssignees(PreparedStatement pstmt, UUID workItemId, WorkItem workItem) throws SQLException {
        if (CollectionUtils.isEmpty(workItem.getAssignees())) {
            return;
        }
        for (WorkItem.Assignee assignee : workItem.getAssignees()) {
            pstmt.setObject(1, workItemId);
            pstmt.setInt(2, Integer.parseInt(assignee.getUserId()));
            pstmt.addBatch();
            pstmt.clearParameters();
        }
        pstmt.executeBatch();
    }

    private void insertTicketAttachments(PreparedStatement pstmt, UUID workItemId, WorkItem workItem) throws SQLException {
        if (CollectionUtils.isEmpty(workItem.getAttachments())) {
            return;
        }
        for (WorkItem.Attachment attachment : workItem.getAttachments()) {
            pstmt.setObject(1, workItemId);
            pstmt.setObject(2, UUID.fromString(attachment.getUploadId()));
            pstmt.setString(3, attachment.getFileName());
            pstmt.setObject(4, attachment.getComment());
            pstmt.addBatch();
            pstmt.clearParameters();
        }
        pstmt.executeBatch();
    }

    private Long insertVanityId(PreparedStatement getPstmt, PreparedStatement pstmt, final String company, UUID workItemId, Integer productId) throws SQLException {
        getPstmt.setObject(1, workItemId);
        getPstmt.setObject(2, productId);
        try (ResultSet rs = getPstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong(1);
            }
        }
        String sequnceName = ProductService.generateSequenceName(company, productId);
        pstmt.setObject(1, workItemId);
        pstmt.setObject(2, productId);
        pstmt.setString(3, sequnceName);
        int affectedRows = pstmt.executeUpdate();
        // check the affected rows
        if (affectedRows <= 0) {
            throw new SQLException("Failed to create Vanity Id!");
        }
        try (ResultSet rs = pstmt.getGeneratedKeys()) {
            if (!rs.next()) {
                throw new SQLException("Failed to create Vanity Id!");
            }
            //return rs.getLong("product_sequence_number");
            return rs.getLong(1);
        }
    }

    private void insertCiCdMappings(PreparedStatement cicdMappingsInsert, final String company, UUID workItemId, WorkItem workItem) throws SQLException {
        if (CollectionUtils.isEmpty(workItem.getCicdMappings())) {
            return;
        }
        for (WorkItem.CICDMapping cicdMapping : workItem.getCicdMappings()) {
            cicdMappingsInsert.setObject(1, workItemId);
            cicdMappingsInsert.setObject(2, cicdMapping.getCicdJobRunId());
            cicdMappingsInsert.setObject(3, cicdMapping.getCicdJobRunStageId());
            cicdMappingsInsert.addBatch();
            cicdMappingsInsert.clearParameters();
        }
        cicdMappingsInsert.executeBatch();
    }
    // endregion

    // region Create

    /*
    ToDo: sanitizeCreateInput functions should be deleted soon. Once callers/clients have upgraded to send product_id & state_id.
     */
    private WorkItem sanitizeCreateInput(final String company, WorkItem workItem) throws SQLException {
        WorkItem.WorkItemBuilder bldr = workItem.toBuilder();
        if (StringUtils.isBlank(workItem.getProductId())) {
            List<Product> systemProducts = productService.getSystemImmutableProducts(company);
            bldr.productId(systemProducts.get(0).getId());
        }
        if (workItem.getStateId() == null) {
            State state = stateDBService.getStateByName(company, WorkItem.ItemStatus.NEW.toString());
            bldr.stateId(state.getId());
        }
        if (workItem.getTicketType() == null) {
            bldr.ticketType(WorkItem.TicketType.WORK_ITEM);
        }
        return bldr.build();
    }

    /**
     * NOTE: this does not return id unless insert was successful.
     *
     * @param company
     * @param workItem
     * @return
     * @throws SQLException
     */
    @Override
    public String insert(String company, WorkItem workItem) throws SQLException {
        UUID workItemId = null;
        String SQL = "INSERT INTO " + company + ".workitems(type,dueat,reason,integrationid,artifact," +
                "artifacttitle,cloudowner,notify,product_id,ticket_template_id,title,reporter,parent_id,state_id," +
                "ticket_type,description) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id";

        String ticketDataInsertSQL = String.format(TICKET_DATA_INSERT_SQL, company);
        String SQL3 = String.format(TICKET_USER_MAPPINGS_SQL, company);
        String SQL4 = String.format(ATTACHMENTS_SQL, company);
        String SQL5 = String.format(VANITY_ID_QUERY, company);
        String SQL6 = String.format(VANITY_ID_GET_QUERY, company);
        String SQL7 = String.format(CICD_MAPPINGS_INSERT_SQL, company);

        workItem = sanitizeCreateInput(company, workItem);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmt3 = conn.prepareStatement(SQL3, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmt4 = conn.prepareStatement(SQL4, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmt5 = conn.prepareStatement(SQL5, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmt6 = conn.prepareStatement(SQL6, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement pstmt7 = conn.prepareStatement(SQL7, Statement.RETURN_GENERATED_KEYS);

             PreparedStatement ticketDataInsertPstmt = conn.prepareStatement(ticketDataInsertSQL, Statement.RETURN_GENERATED_KEYS)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                int i = 1;
                pstmt.setString(i++, String.valueOf(workItem.getType()));
                if (workItem.getDueAt() != null) {
                    pstmt.setLong(i++, workItem.getDueAt());
                } else {
                    pstmt.setNull(i++, Types.INTEGER);
                }
                pstmt.setString(i++, workItem.getReason());
                if (StringUtils.isNotEmpty(workItem.getIntegrationId())) {
                    pstmt.setInt(i++, Integer.parseInt(workItem.getIntegrationId()));
                } else {
                    pstmt.setNull(i++, Types.INTEGER);
                }
                if (workItem.getArtifact() != null) {
                    pstmt.setString(i++, workItem.getArtifact());
                } else {
                    pstmt.setNull(i++, Types.VARCHAR);
                }
                if (workItem.getArtifactTitle() != null) {
                    pstmt.setString(i++, workItem.getArtifactTitle());
                } else {
                    pstmt.setNull(i++, Types.VARCHAR);
                }
                if (workItem.getCloudOwner() != null) {
                    pstmt.setString(i++, workItem.getCloudOwner());
                } else {
                    pstmt.setNull(i++, Types.VARCHAR);
                }
                pstmt.setBoolean(i++, Boolean.TRUE.equals(workItem.getNotify()));
                if (StringUtils.isNotEmpty(workItem.getProductId())) {
                    pstmt.setInt(i++, Integer.parseInt(workItem.getProductId()));
                } else {
                    pstmt.setNull(i++, Types.INTEGER);
                }
                if (StringUtils.isNotEmpty(workItem.getTicketTemplateId())) {
                    pstmt.setObject(i++, UUID.fromString(workItem.getTicketTemplateId()));
                } else {
                    pstmt.setObject(i++, null);
                }
                pstmt.setObject(i++, workItem.getTitle());
                pstmt.setObject(i++, workItem.getReporter());
                pstmt.setObject(i++, UUIDUtils.fromString(workItem.getParentId()));
                pstmt.setObject(i++, workItem.getStateId());
                pstmt.setObject(i++, workItem.getTicketType().toString());
                pstmt.setObject(i, workItem.getDescription());

                int affectedRows = pstmt.executeUpdate();
                // check the affected rows
                if (affectedRows <= 0) {
                    throw new SQLException("Failed to create ticket!");
                }
                // get the ID back
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (!rs.next()) {
                        throw new SQLException("Failed to create ticket!");
                    }
                    workItemId = (UUID) rs.getObject(1);
                }

                insertTicketDataValues(conn, ticketDataInsertPstmt, workItemId, workItem.getTicketDataValues());
                insertTicketAssignees(pstmt3, workItemId, workItem);
                insertTicketAttachments(pstmt4, workItemId, workItem);
                insertVanityId(pstmt6, pstmt5, company, workItemId, Integer.parseInt(workItem.getProductId()));
                insertCiCdMappings(pstmt7, company, workItemId, workItem);
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to convert jsonobject to string.", e);
        }
        return workItemId.toString();
    }
    // endregion

    // region Update
    @Override
    public Boolean update(String company, WorkItem workItem) throws SQLException {
        String ticketDataDeleteQuery = String.format(TICKET_DATA_DELETE_QUERY, company);
        String ticketDataInsertSQL = String.format(TICKET_DATA_INSERT_SQL, company);

        String ticketUserMappingsDeleteSQL = String.format(TICKET_USER_MAPPINGS_DELETE_SQL, company);
        String ticketUserMappingsSQL = String.format(TICKET_USER_MAPPINGS_SQL, company);
        String attachmentsDeleteSQL = String.format(ATTACHMENTS_DELETE_SQL, company);
        String attachmentsSQL = String.format(ATTACHMENTS_SQL, company);

        String SQL = "UPDATE " + company + ".workitems SET ";
        String updates = "";
        String condition = " WHERE id = ?";
        List<Object> values = new ArrayList<>();
        boolean hasUpdates = false;

        //assignee, dueat, status, reason, artifact
        if (StringUtils.isNotEmpty(workItem.getReason())) {
            updates = "reason = ?";
            hasUpdates = true;
            values.add(workItem.getReason());
        }
        if (workItem.getNotify() != null) {
            updates = !hasUpdates ? "notify = ?" : updates + ", notify = ?";
            hasUpdates = true;
            values.add(workItem.getNotify());
        }
        if (workItem.getDueAt() != null) {
            updates = !hasUpdates ? "dueat = ?" : updates + ", dueat = ?";
            values.add((workItem.getDueAt() == -1) ? null : workItem.getDueAt());
            hasUpdates = true;
        }
        if (StringUtils.isNotEmpty(workItem.getParentId())) {
            updates = !hasUpdates ? "parent_id = ?" : updates + ", parent_id = ?";
            values.add(UUIDUtils.fromString(workItem.getParentId()));
            hasUpdates = true;
        }
        if (workItem.getStateId() != null) {
            updates = !hasUpdates ? "state_id = ?" : updates + ", state_id = ?";
            values.add(workItem.getStateId());
            hasUpdates = true;
        }
        if (StringUtils.isNotEmpty(workItem.getTitle())) {
            updates = !hasUpdates ? "title = ?" : updates + ", title = ?";
            values.add(workItem.getTitle());
            hasUpdates = true;
        }
        if (StringUtils.isNotEmpty(workItem.getDescription())) {
            updates = !hasUpdates ? "description = ?" : updates + ", description = ?";
            values.add(workItem.getDescription());
            hasUpdates = true;
        }
        if (workItem.getTicketType() != null) {
            updates = !hasUpdates ? "ticket_type = ?" : updates + ", ticket_type = ?";
            values.add(workItem.getTicketType().toString());
            hasUpdates = true;
        }
        //no updates
        if (!hasUpdates) {
            return false;
        }
        updates += StringUtils.isEmpty(updates) ? "updatedat = ?" : ", updatedat = ?";
        values.add(Instant.now().getEpochSecond());
        SQL = SQL + updates + condition;

        UUID workItemId = UUID.fromString(workItem.getId());
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS);

             PreparedStatement ticketDataDeletePstmt = conn.prepareStatement(ticketDataDeleteQuery);
             PreparedStatement ticketDataInsertPstmt = conn.prepareStatement(ticketDataInsertSQL, Statement.RETURN_GENERATED_KEYS);

             PreparedStatement ticketUserMappingsDeletePSTMT = conn.prepareStatement(ticketUserMappingsDeleteSQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement ticketUserMappingsPSTMT = conn.prepareStatement(ticketUserMappingsSQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement attachmentsDeletePSTMT = conn.prepareStatement(attachmentsDeleteSQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement attachmentsPSTMT = conn.prepareStatement(attachmentsSQL, Statement.RETURN_GENERATED_KEYS)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                if (values.size() != 0) {
                    for (int i = 1; i <= values.size(); i++) {
                        Object obj = values.get(i - 1);
                        pstmt.setObject(i, obj);
                    }
                    pstmt.setObject(values.size() + 1, workItemId);
                    pstmt.executeUpdate();
                }

                ticketDataDeletePstmt.setObject(1, workItemId);
                ticketDataDeletePstmt.execute();
                insertTicketDataValues(conn, ticketDataInsertPstmt, workItemId, workItem.getTicketDataValues());

                ticketUserMappingsDeletePSTMT.setObject(1, workItemId);
                ticketUserMappingsDeletePSTMT.executeUpdate();
                insertTicketAssignees(ticketUserMappingsPSTMT, workItemId, workItem);

                attachmentsDeletePSTMT.setObject(1, workItemId);
                attachmentsDeletePSTMT.executeUpdate();
                insertTicketAttachments(attachmentsPSTMT, workItemId, workItem);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        } catch (JsonProcessingException e) {
            throw new SQLException("Failed to serialize workitem related json objs.", e);
        }
        return true;
    }

    public Boolean updateParentId(String company, UUID workItemId, final UUID newParentId) throws SQLException {
        String SQL = "UPDATE " + company + ".workitems SET ";
        String updates = "";
        String condition = " WHERE id = ?";
        List<Object> values = new ArrayList<>();
        updates = "parent_id = ?";
        values.add(newParentId);

        updates += StringUtils.isEmpty(updates) ? "updatedat = ?" : ", updatedat = ?";
        values.add(Instant.now().getEpochSecond());
        SQL = SQL + updates + condition;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {
            if (values.size() != 0) {
                for (int i = 1; i <= values.size(); i++) {
                    Object obj = values.get(i - 1);
                    pstmt.setObject(i, obj);
                }
                pstmt.setObject(values.size() + 1, workItemId);
                pstmt.executeUpdate();
            }
        }
        return true;
    }

    public Long updateProductId(String company, final UUID workItemId, final String newProductId) throws SQLException {
        String SQL = "UPDATE " + company + ".workitems SET product_id = ?, updatedat = ? WHERE id = ?";
        String insertVanityIdSql = String.format(VANITY_ID_QUERY, company);
        String getVanityIdSql = String.format(VANITY_ID_GET_QUERY, company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement insertVanityIdPstmt = conn.prepareStatement(insertVanityIdSql, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement getVanityIdPstmt = conn.prepareStatement(getVanityIdSql, Statement.RETURN_GENERATED_KEYS)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                pstmt.setInt(1, Integer.parseInt(newProductId));
                pstmt.setObject(2, Instant.now().getEpochSecond());
                pstmt.setObject(3, workItemId);
                int affectedRows = pstmt.executeUpdate();
                // check the affected rows
                if (affectedRows <= 0) {
                    throw new SQLException("Failed to update product for WorkItem id = " + workItemId + " to new productId = " + newProductId);
                }
                Long vanitySequence = insertVanityId(getVanityIdPstmt, insertVanityIdPstmt, company, workItemId, Integer.parseInt(newProductId));
                conn.commit();
                return vanitySequence;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
    }

    public Long updateProductIdRecursive(String company, UUID workItemId, final String newProductId) throws SQLException {
        String SQL = "UPDATE " + company + ".workitems SET product_id = ?, updatedat = ? WHERE id IN (\n" +
                "WITH RECURSIVE sub_tree AS (\n" +
                "  SELECT id\n" +
                "  FROM " + company + ".workitems\n" +
                "  WHERE id = ?\n" +
                "  UNION ALL\n" +
                "  SELECT cat.id\n" +
                "  FROM " + company + ".workitems cat, sub_tree st\n" +
                "  WHERE cat.parent_id = st.id\n" +
                ")\n" +
                "SELECT id FROM sub_tree\n" +
                ")";
        String insertVanityIdSql = "INSERT INTO " + company + ".work_item_vanity_ids(work_item_id,product_id,product_sequence_number) \n" +
                "SELECT id,?,nextval(?) FROM (\n" +
                "   WITH RECURSIVE sub_tree AS (\n" +
                "       SELECT id\n" +
                "       FROM " + company + ".workitems\n" +
                "       WHERE id = ?\n" +
                "       UNION ALL\n" +
                "       SELECT cat.id\n" +
                "       FROM " + company + ".workitems cat, sub_tree st\n" +
                "       WHERE cat.parent_id = st.id\n" +
                "   )\n" +
                "   SELECT id FROM sub_tree\n" +
                ") wi";
        String latestSequenceNumberSql = "SELECT product_sequence_number FROM " + company + ".work_item_vanity_ids WHERE work_item_id = ? AND product_id =? ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SQL);
             PreparedStatement insertVanityIdPstmt = conn.prepareStatement(insertVanityIdSql);
             PreparedStatement latestSequenceNumberPstmt = conn.prepareStatement(latestSequenceNumberSql)) {
            boolean autoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                Integer productId = Integer.parseInt(newProductId);
                pstmt.setInt(1, productId);
                pstmt.setObject(2, Instant.now().getEpochSecond());
                pstmt.setObject(3, workItemId);
                int affectedRows = pstmt.executeUpdate();
                // check the affected rows
                if (affectedRows <= 0) {
                    throw new SQLException("Failed to update product for WorkItem id = " + workItemId + " to new productId = " + newProductId);
                }

                String sequnceName = ProductService.generateSequenceName(company, productId);
                insertVanityIdPstmt.setObject(1, productId);
                insertVanityIdPstmt.setString(2, sequnceName);
                insertVanityIdPstmt.setObject(3, workItemId);
                affectedRows = insertVanityIdPstmt.executeUpdate();
                // check the affected rows
                if (affectedRows <= 0) {
                    throw new SQLException("Failed to create Vanity Id!");
                }

                conn.commit();

                latestSequenceNumberPstmt.setObject(1, workItemId);
                latestSequenceNumberPstmt.setInt(2, Integer.parseInt(newProductId));
                ResultSet rs = latestSequenceNumberPstmt.executeQuery();
                if (rs.next()) {
                    Long vanitySequence = rs.getLong("product_sequence_number");
                    return vanitySequence;
                }
                throw new SQLException("Failed to retrieve Vanity Id! id : " + workItemId);
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(autoCommit);
            }
        }
    }
    // endregion

    // region Get
    @Override
    public Optional<WorkItem> get(String company, String workItemId) throws SQLException {
        DbListResponse<WorkItem> dbListResponse = listByFilter(company, 0, 100,
                Collections.singletonList(UUID.fromString(workItemId)), null, false, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
        if ((dbListResponse == null) || (dbListResponse.getTotalCount() > 1)) {
            throw new SQLException("Error fetching WorkItem with id = " + workItemId);
        }
        List<WorkItem> results = dbListResponse.getRecords();
        return (CollectionUtils.isNotEmpty(results)) ? Optional.of(results.get(0)) : Optional.empty();
    }

    public Optional<WorkItem> getByVanitySequenceNumber(String company, String productKey, Long productSequenceNumber) throws SQLException {
        UUID workItemId = null;
        String sql = "SELECT v.work_item_id FROM " + company + ".work_item_vanity_ids v\n"
                + "LEFT OUTER JOIN " + company + ".products p on p.id = v.product_id\n"
                + "WHERE p.key = ? AND v.product_sequence_number = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setObject(1, productKey.toUpperCase());
            pstmt.setLong(2, productSequenceNumber);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                workItemId = (UUID) rs.getObject("work_item_id");
                return get(company, workItemId.toString());
            }
        }
        return Optional.empty();
    }
    // endregion

    // region List
    public DbListResponse<WorkItem> getTicketsBatch(String company, Integer pageNumber, Integer pageSize, List<UUID> ids,
                                                    String title, boolean unAssigneed, List<Integer> assigneeUserIds,
                                                    List<UUID> ticketTemplateIds, List<String> tagIds, String reporter, Set<String> reporters,
                                                    List<Integer> productIds, String type, Severity priority, String artifact,
                                                    String artifactTitle, String status, List<UUID> cicdJobRunIds,
                                                    List<UUID> cicdJobRunStageIds, Long createdAtStart, Long createdAtEnd,
                                                    Long updatedAtStart, Long updatedAtEnd, Integer createdAfter,
                                                    Integer updatedOrderBy, Integer priorityOrderBy, Integer dueDateOrderBy,
                                                    Integer createdOrderBy)
            throws SQLException {
        List<String> finalCriteriaList = new ArrayList<>();
        List<String> workItemCriteriaList = new ArrayList<>();
        String orderBy = "";
        List<Object> workItemValues = new ArrayList<>();
        List<Object> values = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(ids)) {
            workItemCriteriaList.add("id = ANY(?::uuid[]) ");
            workItemValues.add(new ArrayWrapper<>("uuid", ids));
        }
        if (StringUtils.isNotEmpty(title)) {
            workItemCriteriaList.add("title ILIKE ? ");
            workItemValues.add("%" + title + "%");
        }
        if (CollectionUtils.isNotEmpty(ticketTemplateIds)) {
            workItemCriteriaList.add("ticket_template_id = ANY(?::uuid[]) ");
            workItemValues.add(new ArrayWrapper<>("uuid", ticketTemplateIds));
        }
        if (CollectionUtils.isNotEmpty(productIds)) {
            workItemCriteriaList.add("product_id = ANY(?::int[]) ");
            workItemValues.add(new ArrayWrapper<>("int", productIds));
        }
        if (StringUtils.isNotEmpty(type)) {
            workItemCriteriaList.add("type = ? ");
            workItemValues.add(type);
        }
        if (StringUtils.isNotEmpty(reporter)) {
            workItemCriteriaList.add("reporter = ? ");
            workItemValues.add(reporter);
        }
        if (CollectionUtils.isNotEmpty(reporters)) {
            workItemCriteriaList.add("reporter = ANY(?::text[]) ");
            workItemValues.add(new ArrayWrapper<>("text", List.copyOf(reporters)));
        }
        if (StringUtils.isNotEmpty(artifact)) {
            workItemCriteriaList.add("artifact ILIKE ? ");
            workItemValues.add(artifact + "%");
        }
        if (StringUtils.isNotEmpty(artifactTitle)) {
            workItemCriteriaList.add("artifacttitle ILIKE ? ");
            workItemValues.add("%" + artifactTitle + "%");
        }
        if (createdAtStart != null) {
            workItemCriteriaList.add("createdat > ? ");
            workItemValues.add(createdAtStart);
        }
        if (createdAtEnd != null) {
            workItemCriteriaList.add("createdat < ? ");
            workItemValues.add(createdAtEnd);
        }
        if (updatedAtStart != null) {
            workItemCriteriaList.add("updatedat > ? ");
            workItemValues.add(updatedAtStart);
        }
        if (updatedAtEnd != null) {
            workItemCriteriaList.add("updatedat < ? ");
            workItemValues.add(updatedAtEnd);
        }
        if (createdAfter != null) {
            workItemCriteriaList.add("createdat > ? ");
            workItemValues.add(createdAfter);
        }
        if (StringUtils.isNotEmpty(status)) {
            workItemCriteriaList.add("state_id = ( SELECT id FROM " + company + ".states WHERE name = ? ) ");
            workItemValues.add(status);
        }
        if (unAssigneed) {
            workItemCriteriaList.add("NOT EXISTS ( SELECT user_id FROM " + company + ".ticket_user_mappings WHERE work_item_id = w.id ) ");
        } else if (CollectionUtils.isNotEmpty(assigneeUserIds)) {
            workItemCriteriaList.add("EXISTS ( SELECT user_id FROM " + company + ".ticket_user_mappings WHERE work_item_id = w.id AND user_id = ANY( ?::int[] ) ) ");
            workItemValues.add(new ArrayWrapper<>("int", assigneeUserIds));
        }
        if (CollectionUtils.isNotEmpty(tagIds)) {
            workItemCriteriaList.add("EXISTS ( SELECT tagid FROM " + company + ".tagitems WHERE itemtype = '" + ITEM_TYPE + "' AND itemid = w.id::text AND tagid = ANY( ?::int[] ) ) ");
            workItemValues.add(new ArrayWrapper<>("int", tagIds.stream().map(Integer::parseInt).collect(Collectors.toList())));
        }
        if (CollectionUtils.isNotEmpty(cicdJobRunIds)) {
            workItemCriteriaList.add("EXISTS ( SELECT cicd_job_run_id FROM " + company + ".workitem_cicd_mappings WHERE work_item_id = w.id AND cicd_job_run_id = ANY( ?::uuid[] ) ) ");
            workItemValues.add(new ArrayWrapper<>("uuid", cicdJobRunIds));
        }
        if (CollectionUtils.isNotEmpty(cicdJobRunStageIds)) {
            workItemCriteriaList.add("EXISTS ( SELECT cicd_job_run_stage_id FROM " + company + ".workitem_cicd_mappings WHERE work_item_id = w.id AND cicd_job_run_stage_id = ANY( ?::uuid[] ) ) ");
            workItemValues.add(new ArrayWrapper<>("uuid", cicdJobRunStageIds));
        }
        //final table criteria next
        if (priority != null) {
            finalCriteriaList.add("priority = ? ");
            values.add(priority.getValue());
        }

        String workItemCriteria = "";
        if (workItemCriteriaList.size() > 0) {
            workItemCriteria = " WHERE " + String.join(" AND ", workItemCriteriaList);
            workItemValues.addAll(values);
            values = workItemValues;
        }
        finalCriteriaList.add("sequence_number is not null");
        String finalCriteria = "";
        if (finalCriteriaList.size() > 0) {
            finalCriteria = " WHERE " + String.join(" AND ", finalCriteriaList);
        }

        if (priorityOrderBy != null) {
            orderBy += " ORDER BY " + ((priorityOrderBy < 1) ? "wi.priority DESC " : "wi.priority ASC ");
        } else if (dueDateOrderBy != null) {
            orderBy += " ORDER BY " + ((dueDateOrderBy < 1) ? "wi.dueat DESC " : "wi.dueat ASC ");
        } else if (createdOrderBy != null) {
            orderBy += " ORDER BY " + ((createdOrderBy < 1) ? "wi.createdat DESC " : "wi.createdat ASC ");
        } else {
            if (updatedOrderBy == null) {
                updatedOrderBy = -1;
            }
            orderBy += " ORDER BY " + ((updatedOrderBy < 1) ? "wi.updatedat DESC " : "wi.updatedat ASC ");
        }

        String sqlBase = "FROM ( SELECT w.id as id, w.type as type, w.dueat as dueat, w.artifacttitle as artifacttitle, "
                + "w.cloudowner as cloudowner, w.notify as notify, w.reason as reason, w.artifact as artifact, "
                + "w.product_id as product_id, w.ticket_type as ticket_type, w.title as title,w.integrationid as integration_id, "
                + "w.description as description, w.reporter as reporter, w.ticket_template_id as ticket_template_id, "
                + "w.state_id as state_id, w.createdat as createdat, w.updatedat as updatedat, w.parent_id as parent_id, "

                + "COALESCE((SELECT MAX(priority) FROM " + company + ".questionnaires WHERE workitemid = w.id), 0) AS priority, "

                + "(SELECT key FROM " + company + ".products WHERE id = w.product_id) AS product_key, "

                + "ARRAY( SELECT to_json(tdata) FROM ( SELECT * FROM " + company + ".ticket_data WHERE work_item_id = w.id ) tdata )::text[] ticket_data,"

                + "ARRAY( SELECT to_json(attach) FROM ( SELECT * FROM " + company + ".ticket_attachments WHERE work_item_id = w.id ) attach )::text[] AS attachments, "

                + "( SELECT product_sequence_number FROM "
                + company + ".work_item_vanity_ids WHERE work_item_id = w.id AND product_id = w.product_id ) AS sequence_number, "

                + "ARRAY( SELECT to_json(a) FROM ( SELECT email AS user_email,user_id FROM " + company + ".ticket_user_mappings tum LEFT JOIN "
                + company + ".users ON users.id = tum.user_id WHERE w.id = tum.work_item_id ) a )::text[] AS assignees, "

                + "ARRAY( SELECT tagid FROM " + company + ".tagitems WHERE itemtype = '" + ITEM_TYPE + "' AND w.id::text = itemid ) AS tag_ids, "

                + "ARRAY( SELECT id::uuid FROM " + company + ".workitems WHERE parent_id = w.id ) AS child_ids, "

                + "( SELECT name FROM " + company + ".states WHERE id = w.state_id ) AS status, "

                + "ARRAY( SELECT to_json(cicds) FROM (SELECT cicd_job_run_id::uuid,cicd_job_run_stage_id::uuid FROM "
                + company + ".workitem_cicd_mappings WHERE work_item_id = w.id ) cicds )::text[] AS cicd_job_runs"

                + " FROM " + company + ".workitems w "

                + workItemCriteria
                + " ) wi "
                + finalCriteria;


        String sql = "SELECT * " + sqlBase + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSQL = "SELECT COUNT(*) " + sqlBase;


        List<WorkItem> retval = new ArrayList<>();
        Integer totCount = 0;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {
            for (int i = 0; i < values.size(); i++) {
                Object obj = DBUtils.processArrayValues(conn, values.get(i));
                pstmt.setObject(i + 1, obj);
                pstmt2.setObject(i + 1, obj);
            }
            log.info("Get or List Query = {}", pstmt);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Long dueAt = (rs.getObject("dueat") == null) ?
                        null : rs.getLong("dueat");
                Integer[] rsTagIds = rs.getArray("tag_ids") != null
                        ? (Integer[]) rs.getArray("tag_ids").getArray()
                        : new Integer[0];
                UUID[] childIds = rs.getArray("child_ids") != null
                        ? (UUID[]) rs.getArray("child_ids").getArray() : new UUID[0];
                List<String> childIdStrings = UUIDUtils.toStringsList(Arrays.asList(childIds));

                String productKey = rs.getString("product_key");
                Long sequenceNumber = rs.getObject("sequence_number") == null ? null : rs.getLong("sequence_number");
                WorkItem.WorkItemBuilder bldr = WorkItem.builder()
                        .id(rs.getObject("id").toString())
                        .type(WorkItem.ItemType.fromString(rs.getString("type")))
                        .ticketType(WorkItem.TicketType.fromString(rs.getString("ticket_type")))
                        .dueAt(dueAt)
                        .productId(rs.getString("product_id"))
                        .cloudOwner(rs.getString("cloudowner"))
                        .artifactTitle(rs.getString("artifacttitle"))
                        .artifact(rs.getString("artifact"))
                        .priority(Severity.fromIntValue(rs.getInt("priority")))
                        .notify(rs.getBoolean("notify"))
                        .updatedAt(rs.getLong("updatedat"))
                        .createdAt(rs.getLong("createdat"))
                        .reason(rs.getString("reason"))
                        .integrationId(rs.getString("integration_id"))
                        .ticketTemplateId(rs.getString("ticket_template_id"))
                        .title(rs.getString("title"))
                        .description(rs.getString("description"))
                        .reporter(rs.getString("reporter"))
                        .stateId(rs.getInt("state_id"))
                        .status(rs.getString("status"))
                        .vanityId(productKey + "-" + sequenceNumber)
                        .parentId(rs.getString("parent_id"))
                        .tagIds(rsTagIds.length > 0 ?
                                Arrays.stream(rsTagIds).map(Object::toString).collect(Collectors.toList())
                                : Collections.emptyList())
                        .childIds(childIdStrings);
                String[] cicdJobs = rs.getArray("cicd_job_runs") != null ? (String[]) rs.getArray("cicd_job_runs").getArray() : new String[0];
                if (cicdJobs != null && cicdJobs.length > 0) {
                    List<WorkItem.CICDMapping> cicds = new ArrayList<>();
                    for (String a : cicdJobs) {
                        try {
                            cicds.add(mapper.readValue(a, WorkItem.CICDMapping.class));
                        } catch (JsonProcessingException e) {
                            log.warn("failed to parse cicd mapping.");
                        }
                    }
                    bldr.cicdMappings(cicds);
                }

                String[] assignees = rs.getArray("assignees") != null ? (String[]) rs.getArray("assignees").getArray() : new String[0];
                if (assignees != null && assignees.length > 0) {
                    List<WorkItem.Assignee> assigneeList = new ArrayList<>();
                    for (String a : assignees) {
                        try {
                            assigneeList.add(mapper.readValue(a, WorkItem.Assignee.class));
                        } catch (JsonProcessingException e) {
                            log.warn("failed to parse assignee mapping.");
                        }
                    }
                    bldr.assignees(assigneeList);
                }

                String[] attachments = rs.getArray("attachments") != null ? (String[]) rs.getArray("attachments").getArray() : new String[0];
                if (attachments != null && attachments.length > 0) {
                    List<WorkItem.Attachment> attachmentsList = new ArrayList<>();
                    for (String a : attachments) {
                        try {
                            attachmentsList.add(mapper.readValue(a, WorkItem.Attachment.class));
                        } catch (JsonProcessingException e) {
                            log.warn("failed to parse attachment mapping.");
                        }
                    }
                    bldr.attachments(attachmentsList);
                }

                String[] ticketDatas = rs.getArray("ticket_data") != null ? (String[]) rs.getArray("ticket_data").getArray() : new String[0];
                if (ticketDatas != null && ticketDatas.length > 0) {
                    List<TicketData> ticketDataList = new ArrayList<>();
                    for (String a : ticketDatas) {
                        try {
                            ticketDataList.add(mapper.readValue(a, TicketData.class));
                        } catch (JsonProcessingException e) {
                            log.warn("failed to parse TicketData values.");
                        }
                    }
                    bldr.ticketDataValues(ticketDataList);
                }
                retval.add(bldr.build());
            }
            if (retval.size() > 0) {
                totCount = retval.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
                if (retval.size() == pageSize) {
                    log.info("Count Query = {}", pstmt2);
                    rs = pstmt2.executeQuery();
                    if (rs.next()) {
                        totCount = rs.getInt("count");
                    }
                }
            }
        }
        log.info("Return counts ret:{}, tot:{}", retval.size(), totCount);
        return DbListResponse.of(retval, totCount);
    }

    public DbListResponse<WorkItem> listByFilter(String company, Integer pageNumber, Integer pageSize, List<UUID> ids,
                                                 String title, boolean unAssigned, List<Integer> assigneeUserIds,
                                                 List<UUID> ticketTemplateIds, List<String> tagIds, String reporter, Set<String> reporters,
                                                 List<Integer> productIds, String type, String status,
                                                 Severity priority, String artifact, String artifactTitle,
                                                 List<UUID> cicdJobRunIds, List<UUID> cicdJobRunStageIds,
                                                 Long createdAtStart, Long createdAtEnd,
                                                 Long updatedBefore, Long updatedAfter, Integer createdAfter,
                                                 Integer updatedOrderBy, Integer priorityOrderBy,
                                                 Integer dueDateOrderBy, Integer createdOrderBy)
            throws SQLException {
        Stopwatch stopwatch = null;
        Duration t1 = null, t2 = null, t3 = null;
        if (enablePerformanceLog) {
            stopwatch = Stopwatch.createStarted();
        }
        DbListResponse<WorkItem> workItemsDbResponse = getTicketsBatch(company, pageNumber, pageSize, ids, title,
                unAssigned, assigneeUserIds, ticketTemplateIds, tagIds, reporter, reporters, productIds, type,
                priority, artifact, artifactTitle, status, cicdJobRunIds, cicdJobRunStageIds, createdAtStart, createdAtEnd,
                updatedBefore, updatedAfter, createdAfter, updatedOrderBy, priorityOrderBy, dueDateOrderBy, createdOrderBy);
        if (enablePerformanceLog) {
            t1 = stopwatch.elapsed();
        }
        List<WorkItem> workItems = workItemsDbResponse.getRecords();
        if (enablePerformanceLog) {
            stopwatch.stop();
            Level level = stopwatch.elapsed(TimeUnit.MILLISECONDS) > perfLogWarnThresholdMs ? Level.WARN : Level.DEBUG;
            log.log(level, "listing work items took {} : get_tickets = {}",
                    stopwatch.toString(), DurationUtils.toPrettyString(t1.toNanos()));
        }
        return DbListResponse.of(workItems, workItemsDbResponse.getTotalCount());

    }

    @Override
    public DbListResponse<WorkItem> list(String company, Integer pageNumber,
                                         Integer pageSize) throws SQLException {
        return listByFilter(company, pageNumber, pageSize, null, null, false, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
    }
    // endregion

    // region Delete
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        String SQL = "DELETE FROM " + company + ".workitems WHERE id = ?";

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

    //region Update - Partial
    public Boolean updateState(String company, String id, Integer stateId) throws SQLException {
        String updateStateSql = String.format(TICKET_UPDATE_STATE_FORMAT, company);
        boolean success = true;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(updateStateSql)) {
            pstmt.setObject(1, stateId);
            pstmt.setObject(2, UUID.fromString(id));
            int affectedRows = pstmt.executeUpdate();
            success = (affectedRows > 0);
            return success;
        }
    }

    // endregion

    // region AGGS

    public DbListResponse<DbAggregationResult> aggregate(String company,
                                                         WorkItemFilter filter) throws SQLException {
        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");
        WorkItemFilter.Calculation calculation = MoreObjects.firstNonNull(filter.getCalculation(), WorkItemFilter.Calculation.count);
        List<String> conditions = new ArrayList<>();

        // -- CALCULATION

        String calculationComponent, orderByString;
        //noinspection SwitchStatementWithTooFewBranches
        switch (calculation) {
            case count:
                orderByString = "ct DESC";
                calculationComponent = "COUNT(id) as ct";
                break;
            default:
                throw new UnsupportedOperationException("Calculation not supported: " + calculation);
        }


        // -- GROUP BY

        boolean userJoinNeeded = false;
        boolean tagJoinNeeded = false;
        String selectDistinctString, groupByString, distinctKey;
        switch (filter.getAcross()) {
            case state:
                groupByString = "state_id";
                selectDistinctString = "state_id";
                distinctKey = "state_id";
                break;
            case assignee:
                groupByString = "assignee_user_id";
                selectDistinctString = "UNNEST(COALESCE(assignee_user_ids::text[], ARRAY['" + UNASSIGNED_ID + "'])) AS " + groupByString;
                distinctKey = "assignee_user_id";
                userJoinNeeded = true;
                if (CollectionUtils.isEmpty(filter.getAssignees())) {
                    //this condition improves perf by filtering out unassigned tickets
                    conditions.add("EXISTS (SELECT user_id FROM " + company + ".ticket_user_mappings WHERE wi.id = work_item_id)");
                }
                break;
            case reporter:
                groupByString = "reporter";
                selectDistinctString = "COALESCE(reporter, '" + UNKNOWN_REPORTER + "') as reporter";
                distinctKey = "reporter";
                break;
            case tag:
                tagJoinNeeded = true;
                groupByString = "tag";
                selectDistinctString = "UNNEST(tag_ids) AS " + groupByString;
                distinctKey = "tag";
                if (CollectionUtils.isEmpty(filter.getTags())) {
                    //this condition improves perf by filtering out untagged tickets
                    conditions.add("EXISTS (SELECT tagid FROM " + company + ".tagitems WHERE wi.id = tagitems.itemid::uuid AND itemtype = 'WORK_ITEM')");
                }
                break;
            case product:
                groupByString = "product_id";
                selectDistinctString = "product_id";
                distinctKey = "product_id";
                break;
            case created:
            case updated:
                groupByString = filter.getAcross().toString();
                orderByString = filter.getAcross().toString() + " ASC";
                selectDistinctString = filter.getAcross() + "at AS " + filter.getAcross();
                distinctKey = filter.getAcross().toString();
                break;
            case trend:
                if (calculation != WorkItemFilter.Calculation.count) {
                    throw new UnsupportedOperationException("Trend is only supported across count");
                }
                // To get the accumulated sum grouped by day,
                // we first need to aggregate the data per day.
                // Later, we'll run another query to accumulate the results.
                groupByString = "day";
                orderByString = "day ASC";
                selectDistinctString = "EXTRACT(EPOCH FROM DATE_TRUNC('day', TO_TIMESTAMP(updatedat)))::TEXT AS day";
                distinctKey = "trend";
                break;
            default:
                throw new UnsupportedOperationException("Across not supported: " + filter.getAcross());
        }

        // -- WHERE

        Map<String, Object> params = new HashMap<>();
        if (CollectionUtils.isNotEmpty(filter.getAssignees())) {
            userJoinNeeded = true;
            conditions.add("EXISTS (SELECT user_id FROM "+company+".ticket_user_mappings WHERE wi.id = work_item_id AND user_id IN (:assignees))");
            params.put("assignees", filter.getAssignees().stream().map(Integer::parseInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getTags())) {
            tagJoinNeeded = true;
            conditions.add("EXISTS (SELECT tagid FROM " + company + ".tagitems WHERE wi.id = tagitems.itemid::uuid AND itemtype = 'WORK_ITEM' AND tagid IN (:tags))");
            params.put("tags", filter.getTags().stream().map(Integer::parseInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getStates())) {
            conditions.add("state_id::TEXT IN (:states)");
            params.put("states", filter.getStates());
        }
        if (CollectionUtils.isNotEmpty(filter.getReporters())) {
            conditions.add("COALESCE(reporter::TEXT, '" + UNKNOWN_REPORTER + "') IN (:reporters)");
            params.put("reporters", filter.getReporters());
        }
        if (CollectionUtils.isNotEmpty(filter.getProducts())) {
            conditions.add("product_id::TEXT IN (:product_ids)");
            params.put("product_ids", filter.getProducts());
        }
        if (filter.getUpdatedAt() != null && filter.getUpdatedAt().getGt() != null) {
            conditions.add("updatedat > :updatedAtStart");
            params.put("updatedAtStart", filter.getUpdatedAt().getGt());
        }
        if (filter.getUpdatedAt() != null && filter.getUpdatedAt().getLt() != null) {
            conditions.add("updatedat < :updatedAtEnd");
            params.put("updatedAtEnd", filter.getUpdatedAt().getLt());
        }

        // -- work item where
        String workItemWhere = conditions.isEmpty() ? "" :
                " WHERE " + String.join(" AND ", conditions);

        // --- FINAL QUERY ---

        String sql = "SELECT " +
                selectDistinctString + "," +
                calculationComponent +
                " FROM" +
                " ( SELECT * FROM ( SELECT id, product_id, reporter, state_id, createdat, updatedat"

                + (userJoinNeeded ? ",ARRAY(SELECT user_id FROM " + company + ".ticket_user_mappings WHERE wi.id = work_item_id) AS assignee_user_ids" : "")

                + (tagJoinNeeded ? ",ARRAY(SELECT tagid FROM " + company + ".tagitems WHERE itemtype = '" + ITEM_TYPE + "' AND wi.id::text = itemid) AS tag_ids" : "")

                + " FROM " + company + ".workitems wi ) wi "
                + workItemWhere +
                " ) AS final_table " +
                " GROUP BY " + groupByString +
                " ORDER BY " + orderByString;

        if (filter.getAcross() == WorkItemFilter.Distinct.trend) {
            // To get a trend, we do a cumulative sum of the # of new items per day.
            // - the sum has to sort from oldest to newest (ASC) to get the right sum
            // - the limit has to be taken after sorting from newest to oldest (DESC)
            // - the inner query cannot be limited or the sum will miss items
            sql = "" +
                    " SELECT day AS trend, " +
                    " SUM(ct) OVER (ORDER BY day ASC ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS ct " +
                    " FROM (" + sql + ") AS aggregated_per_day" +
                    " ORDER BY trend DESC" +
                    " LIMIT 90 ";

            // apply the desired sort (ASC) order *after* the sum has been done
            sql = "SELECT trend, ct FROM (" + sql + ") AS trend_table ORDER BY trend ASC";

        } else {
            sql += " LIMIT 90";
        }

        List<DbAggregationResult> results = template.query(sql, params,
                WorkItemConverters.aggResultRowMapper(distinctKey, calculation));
        return DbListResponse.of(results, results.size());
    }

    public DbListResponse<DbAggregationResult> stackedAggregate(String company,
                                                                WorkItemFilter filter,
                                                                List<WorkItemFilter.Distinct> stacks) throws SQLException {
        // first aggregation
        WorkItemFilter.Distinct firstDistinct = filter.getAcross();
        DbListResponse<DbAggregationResult> result = aggregate(company, filter);

        // check if there is a second aggregation and if stacking is supported
        Optional<WorkItemFilter.Distinct> secondDistinctOpt = IterableUtils.getFirst(stacks)
                .filter(WorkItemFilter.Distinct.STACKABLE::contains);
        if (secondDistinctOpt.isEmpty() || !WorkItemFilter.Distinct.STACKABLE.contains(firstDistinct)) {
            // no stack - or stack not supported
            return result;
        }

        WorkItemFilter.Distinct secondDistinct = secondDistinctOpt.get();
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
                            case state:
                                newFilter.states(List.of(row.getKey()));
                                break;
                            case assignee:
                                newFilter.assignees(List.of(row.getKey()));
                                break;
                            case reporter:
                                newFilter.reporters(List.of(row.getKey()));
                                break;
                            case tag:
                                newFilter.tags(List.of(row.getKey()));
                                break;
                            case product:
                                newFilter.products(List.of(row.getKey()));
                                break;
                            default:
                                throw new UnsupportedOperationException("Stacked agg not supported: " + firstDistinct);
                        }
                        return row.toBuilder()
                                .stacks(aggregate(company, newFilter.build()).getRecords())
                                .build();
                    }))
                    .collect(Collectors.toList());
            return DbListResponse.of(finalList, finalList.size());
        } catch (RuntimeStreamException e) {
            throw new SQLException("Failed to run stacked aggregation", e);
        }
    }
    // endregion


    // region ensureTableExistence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlStatements = new ArrayList<>();
        String sqlStatement = "CREATE TABLE IF NOT EXISTS " + company + ".workitems(\n" +
                "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    type VARCHAR NOT NULL,\n" + // currently auto vs manual
                "    ticket_type VARCHAR NOT NULL,\n" +
                "    notify BOOLEAN NOT NULL DEFAULT FALSE,\n" +

                "    artifacttitle VARCHAR,\n" + //change to title field and becomes editable
                "    cloudowner VARCHAR,\n" + // convert to artifact obj & save in Ticket fields
                "    reason VARCHAR,\n" + // put it as description
                "    artifact VARCHAR,\n" + // convert to artifact obj & save in Ticket fields

                "    dueat BIGINT,\n" +
                "    integrationid INTEGER REFERENCES " + company + ".integrations(id) ON DELETE CASCADE,\n" +
                "    product_id INTEGER NOT NULL REFERENCES " + company + ".products(id) ON DELETE RESTRICT,\n" +
                "    updatedat BIGINT DEFAULT extract(epoch from now()),\n" +
                "    createdat BIGINT DEFAULT extract(epoch from now()),\n" +

                "    title VARCHAR,\n" + // keep
                "    description VARCHAR,\n" +
                "    reporter VARCHAR,\n" + // keep
                "    ticket_template_id UUID REFERENCES " + company + ".ticket_templates(id) ON DELETE RESTRICT,\n" +
                "    state_id INTEGER NOT NULL REFERENCES " + company + ".states(id) ON DELETE RESTRICT,\n" +
                "    parent_id UUID REFERENCES " + company + ".workitems(id) ON DELETE CASCADE" +
                ")";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE INDEX IF NOT EXISTS workitems_integrationid_idx on " + company + ".workitems (integrationid)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE INDEX IF NOT EXISTS workitems_artifact_idx on " + company + ".workitems (artifact)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE INDEX IF NOT EXISTS workitems_product_id_idx on " + company + ".workitems (product_id)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE INDEX IF NOT EXISTS workitems_parent_id_idx on " + company + ".workitems (parent_id)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE TABLE IF NOT EXISTS " + company + ".ticket_data(\n" +
                "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    values JSONB,\n" +
                "    ticket_field_id BIGINT NOT NULL REFERENCES " + company + ".ticket_fields(id) ON DELETE CASCADE,\n" +
                "    work_item_id UUID NOT NULL REFERENCES " + company + ".workitems(id) ON DELETE CASCADE\n" +
                ")";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE INDEX IF NOT EXISTS ticket_data_work_item_id_idx on " + company + ".ticket_data (work_item_id)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE UNIQUE INDEX IF NOT EXISTS uniq_ticket_data_work_item_id_ticket_field_id_idx on " + company + ".ticket_data (work_item_id,ticket_field_id)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE TABLE IF NOT EXISTS " + company + ".ticket_user_mappings(\n" +
                "id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, \n" +
                "work_item_id UUID NOT NULL REFERENCES " + company + ".workitems(id) ON DELETE CASCADE,\n" +
                "user_id INTEGER NOT NULL REFERENCES " + company + ".users(id) ON DELETE CASCADE,\n" +
                "createdat BIGINT DEFAULT extract(epoch from now())\n" +
                "    )";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE UNIQUE INDEX IF NOT EXISTS uniq_ticket_user_mappings_work_item_id_user_id_idx on " + company + ".ticket_user_mappings (work_item_id,user_id)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE INDEX IF NOT EXISTS ticket_user_mappings_work_item_id_idx on " + company + ".ticket_user_mappings (work_item_id)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE INDEX IF NOT EXISTS ticket_user_mappings_user_id_idx on " + company + ".ticket_user_mappings (user_id)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE TABLE IF NOT EXISTS " + company + ".ticket_attachments(\n" +
                "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                "    upload_id UUID NOT NULL,\n" +
                "    work_item_id UUID NOT NULL REFERENCES " + company + ".workitems(id) ON DELETE CASCADE,\n" +
                "    file_name VARCHAR NOT NULL,\n" +
                "    comment VARCHAR,\n" +
                "    uploaded_at BIGINT DEFAULT extract(epoch from now())\n" +
                ")";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE UNIQUE INDEX IF NOT EXISTS uniq_ticket_attachments_work_item_id_upload_id_idx on " + company + ".ticket_attachments (work_item_id,upload_id)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE INDEX IF NOT EXISTS ticket_attachments_work_item_id_idx on " + company + ".ticket_attachments (work_item_id)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE TABLE IF NOT EXISTS " + company + ".work_item_vanity_ids(\n"
                + "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n"
                + "    work_item_id UUID NOT NULL REFERENCES " + company + ".workitems(id) ON DELETE CASCADE,\n"
                + "    product_id INTEGER NOT NULL REFERENCES " + company + ".products(id) ON DELETE RESTRICT,\n"
                + "    product_sequence_number BIGINT NOT NULL,\n"
                + "    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()\n"
                + ")";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE UNIQUE INDEX IF NOT EXISTS uniq_work_item_vanity_ids_work_item_id_product_id_idx on " + company + ".work_item_vanity_ids (work_item_id,product_id)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE UNIQUE INDEX IF NOT EXISTS uniq_work_item_vanity_ids_product_id_sequence_number_idx on " + company + ".work_item_vanity_ids (product_id,product_sequence_number)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE INDEX IF NOT EXISTS work_item_vanity_ids_created_at_idx on " + company + ".work_item_vanity_ids (created_at DESC)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE INDEX IF NOT EXISTS workitems_state_id_idx ON " + company + ".workitems(state_id)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE INDEX IF NOT EXISTS workitems_updated_at_idx ON " + company + ".workitems(updatedat DESC)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE INDEX IF NOT EXISTS workitems_created_at_idx ON " + company + ".workitems(createdat DESC)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE TABLE IF NOT EXISTS " + company + ".workitem_cicd_mappings(\n"
                + "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n"
                + "    work_item_id UUID NOT NULL REFERENCES " + company + ".workitems(id) ON DELETE CASCADE,\n"
                + "    cicd_job_run_id UUID NOT NULL REFERENCES " + company + ".cicd_job_runs(id) ON DELETE CASCADE,\n"
                + "    cicd_job_run_stage_id UUID REFERENCES " + company + ".cicd_job_run_stages(id) ON DELETE CASCADE\n"
                + ")";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE INDEX IF NOT EXISTS workitem_cicd_mappings_work_item_id_idx ON " + company + ".workitem_cicd_mappings(work_item_id)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE INDEX IF NOT EXISTS workitem_cicd_mappings_cicd_job_run_id_idx ON " + company + ".workitem_cicd_mappings(cicd_job_run_id)";
        sqlStatements.add(sqlStatement);

        sqlStatement = "CREATE INDEX IF NOT EXISTS workitem_cicd_mappings_cicd_job_run_stage_id_idx ON " + company + ".workitem_cicd_mappings(cicd_job_run_stage_id)";
        sqlStatements.add(sqlStatement);

        try (Connection conn = dataSource.getConnection()) {
            for (String currentSql : sqlStatements) {
                try (PreparedStatement pstmt = conn.prepareStatement(currentSql)) {
                    pstmt.execute();
                }
            }
            return true;
        }
    }
}
