package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.converters.DbWorkItemConverters;
import io.levelops.commons.databases.converters.DbWorkItemHistoryConverters;
import io.levelops.commons.databases.issue_management.DbWorkItem;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.issue_management.DbWorkItemHistory;
import io.levelops.commons.databases.issue_management.DbWorkItemPrioritySLA;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.filters.WorkItemsSprintMappingFilter;
import io.levelops.commons.databases.models.filters.WorkItemsTimelineFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.query_criteria.WorkItemMilestoneQueryCriteria;
import io.levelops.commons.databases.query_criteria.WorkItemQueryCriteria;
import io.levelops.commons.databases.query_criteria.WorkItemTimelineQueryCriteria;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.Query;
import io.levelops.web.exceptions.BadRequestException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.converters.DbWorkItemConverters.mapToDbWorkItem;

@Service
@Log4j2
public class WorkItemsService extends DatabaseService<DbWorkItem> {

    public static final String TABLE_NAME = "issue_mgmt_workitems";
    public static final String MILESTONES_TABLE_NAME = "issue_mgmt_milestones";
    public static final String TIMELINES_TABLE_NAME = "issue_mgmt_workitems_timeline";
    private static final String PRIORITIES_SLA_TABLE = "issue_mgmt_priorities_sla";
    private static final List<String> TIME_BASED_SORTABLE_COLUMNS = List.of("workitem_created_at", "workitem_updated_at", "workitem_resolved_at", "workitem_due_at");
    private static final String WORKITEM_TIMELINES_TBL_QUALIFIER = "timelines.";
    private static final String WORKITEM_TIMELINES_TBL_ALIAS = "timelines";
    private static final String INTEGRATION_TRACKER_TABLE_NAME = "integration_tracker";

    //upsert
    private static final String INSERT_SQL_FORMAT = "INSERT INTO %s." + TABLE_NAME + " (workitem_id, integration_id, summary," +
            " sprint_ids, priority, assignee, assignee_id, epic, parent_workitem_id, reporter, reporter_id, status, workitem_type, story_points," +
            " ingested_at,custom_fields, project, project_id, components, labels, versions, fix_versions, resolution, status_category," +
            " original_estimate, desc_size, hops, bounces, num_attachments, workitem_created_at, workitem_updated_at," +
            " workitem_resolved_at, workitem_due_at, first_attachment_at, first_comment_at, attributes, is_active)"
            + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,to_json(?::jsonb),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,to_json(?::json),?)\n" +
            "ON CONFLICT(workitem_id, integration_id, ingested_at) " +
            "DO UPDATE SET (summary, sprint_ids, priority, assignee,assignee_id, epic, parent_workitem_id, reporter, reporter_id, status, workitem_type," +
            " story_points, custom_fields, project, project_id, components," +
            " labels, versions, fix_versions, resolution, status_category, original_estimate, desc_size, hops, bounces, num_attachments," +
            " workitem_created_at, workitem_updated_at, workitem_resolved_at, workitem_due_at, first_attachment_at, first_comment_at, attributes, is_active) " +
            "= (EXCLUDED.summary, EXCLUDED.sprint_ids, EXCLUDED.priority, EXCLUDED.assignee, EXCLUDED.assignee_id,EXCLUDED.epic, EXCLUDED.parent_workitem_id," +
            " EXCLUDED.reporter,EXCLUDED.reporter_id, EXCLUDED.status, EXCLUDED.workitem_type, EXCLUDED.story_points," +
            " EXCLUDED.custom_fields, EXCLUDED.project, EXCLUDED.project_id, EXCLUDED.components," +
            "EXCLUDED.labels, EXCLUDED.versions, EXCLUDED.fix_versions, EXCLUDED.resolution, EXCLUDED.status_category," +
            " EXCLUDED.original_estimate, EXCLUDED.desc_size, EXCLUDED.hops, EXCLUDED.bounces, EXCLUDED.num_attachments," +
            " EXCLUDED.workitem_created_at, EXCLUDED.workitem_updated_at, EXCLUDED.workitem_resolved_at, EXCLUDED.workitem_due_at," +
            " EXCLUDED.first_attachment_at, EXCLUDED.first_comment_at, EXCLUDED.attributes, EXCLUDED.is_active)\n" + //extract(epoch from now())
            "RETURNING id";

    private static final String DELETE_SQL_FORMAT = "DELETE FROM %s." + TABLE_NAME + " WHERE id = ?";

    private final NamedParameterJdbcTemplate template;
    private final WorkItemsReportService workItemsReportService;
    private final WorkItemsStageTimesReportService workItemsStageTimesReportService;
    private final WorkItemsAgeReportService workItemsAgeReportService;
    private final WorkItemsResolutionTimeReportService workItemsResolutionTimeReportService;
    private final WorkItemsResponseTimeReportService workItemsResponseTimeReportService;
    private final WorkItemsSprintMetricReportService workItemsSprintMetricReportService;
    private final WorkItemsAssigneeAllocationReportService workItemsAssigneeAllocationReportService;
    private final WorkItemsPrioritySLAService workItemsPrioritySLAService;
    private final WorkItemsBouncesReportService workItemsBouncesReportService;
    private final WorkItemsHopsReportService workItemsHopsReportService;
    private final WorkItemFieldsMetaService workItemFieldsMetaService;
    private final WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService;
    private final WorkItemsStageBounceReportService workItemsStageBounceReportService;

    @Autowired
    public WorkItemsService(DataSource dataSource,
                            WorkItemsReportService workItemsReportService,
                            WorkItemsStageTimesReportService workItemsStageTimesReportService,
                            WorkItemsAgeReportService workItemsAgeReportService,
                            WorkItemsResolutionTimeReportService workItemsResolutionTimeReportService,
                            WorkItemsResponseTimeReportService workItemsResponseTimeReportService,
                            WorkItemsSprintMetricReportService workItemsSprintMetricReportService,
                            WorkItemsAssigneeAllocationReportService workItemsAssigneeAllocationReportService,
                            WorkItemsPrioritySLAService workItemsPrioritySLAService,
                            WorkItemsBouncesReportService workItemsBouncesReportService,
                            WorkItemsHopsReportService workItemsHopsReportService,
                            WorkItemsFirstAssigneeReportService workItemsFirstAssigneeReportService,
                            WorkItemFieldsMetaService workItemFieldsMetaService,
                            WorkItemsStageBounceReportService workItemsStageBounceReportService) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.workItemsReportService = workItemsReportService;
        this.workItemsStageTimesReportService = workItemsStageTimesReportService;
        this.workItemsAgeReportService = workItemsAgeReportService;
        this.workItemsResolutionTimeReportService = workItemsResolutionTimeReportService;
        this.workItemsResponseTimeReportService = workItemsResponseTimeReportService;
        this.workItemsSprintMetricReportService = workItemsSprintMetricReportService;
        this.workItemsAssigneeAllocationReportService = workItemsAssigneeAllocationReportService;
        this.workItemsPrioritySLAService = workItemsPrioritySLAService;
        this.workItemsBouncesReportService = workItemsBouncesReportService;
        this.workItemsHopsReportService = workItemsHopsReportService;
        this.workItemsFirstAssigneeReportService = workItemsFirstAssigneeReportService;
        this.workItemFieldsMetaService = workItemFieldsMetaService;
        this.workItemsStageBounceReportService = workItemsStageBounceReportService;
    }

    @Override
    public String insert(String company, DbWorkItem dbWorkItem) throws SQLException {
        String insertSql = String.format(INSERT_SQL_FORMAT, company);
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            pstmt.setObject(++i, dbWorkItem.getWorkItemId());
            pstmt.setObject(++i, Integer.valueOf(dbWorkItem.getIntegrationId()));
            pstmt.setObject(++i, dbWorkItem.getSummary());
            pstmt.setArray(++i, conn.createArrayOf("uuid",
                    ListUtils.emptyIfNull(dbWorkItem.getSprintIds()).toArray()));
            pstmt.setObject(++i, dbWorkItem.getPriority());
            pstmt.setObject(++i, dbWorkItem.getAssignee());
            pstmt.setObject(++i, StringUtils.isEmpty(dbWorkItem.getAssigneeId()) ? null : UUID.fromString(dbWorkItem.getAssigneeId()));
            pstmt.setObject(++i, dbWorkItem.getEpic());
            pstmt.setObject(++i, dbWorkItem.getParentWorkItemId());
            pstmt.setObject(++i, dbWorkItem.getReporter());
            pstmt.setObject(++i, StringUtils.isEmpty(dbWorkItem.getReporterId()) ? null : UUID.fromString(dbWorkItem.getReporterId()));
            pstmt.setObject(++i, dbWorkItem.getStatus());
            pstmt.setObject(++i, dbWorkItem.getWorkItemType());
            pstmt.setObject(++i, dbWorkItem.getStoryPoint());
            pstmt.setObject(++i, dbWorkItem.getIngestedAt());
            try {
                pstmt.setObject(++i, DefaultObjectMapper.get().writeValueAsString(dbWorkItem.getCustomFields()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize custom_fields json ", e);
                pstmt.setObject(i, "{}");
            }
            pstmt.setObject(++i, dbWorkItem.getProject());
            pstmt.setObject(++i, dbWorkItem.getProjectId());
            pstmt.setArray(++i, conn.createArrayOf("varchar",
                    ListUtils.emptyIfNull(dbWorkItem.getComponents()).toArray()));
            pstmt.setArray(++i, conn.createArrayOf("varchar",
                    ListUtils.emptyIfNull(dbWorkItem.getLabels()).toArray()));
            pstmt.setArray(++i, conn.createArrayOf("varchar",
                    ListUtils.emptyIfNull(dbWorkItem.getVersions()).toArray()));
            pstmt.setArray(++i, conn.createArrayOf("varchar",
                    ListUtils.emptyIfNull(dbWorkItem.getFixVersions()).toArray()));
            pstmt.setObject(++i, dbWorkItem.getResolution());
            pstmt.setObject(++i, dbWorkItem.getStatusCategory());
            pstmt.setObject(++i, dbWorkItem.getOriginalEstimate());
            pstmt.setObject(++i, dbWorkItem.getDescSize());
            pstmt.setObject(++i, dbWorkItem.getHops());
            pstmt.setObject(++i, dbWorkItem.getBounces());
            pstmt.setObject(++i, dbWorkItem.getNumAttachments());
            pstmt.setTimestamp(++i, dbWorkItem.getWorkItemCreatedAt());
            pstmt.setTimestamp(++i, dbWorkItem.getWorkItemUpdatedAt());
            pstmt.setTimestamp(++i, dbWorkItem.getWorkItemResolvedAt());
            pstmt.setTimestamp(++i, dbWorkItem.getWorkItemDueAt());
            pstmt.setTimestamp(++i, dbWorkItem.getFirstAttachmentAt());
            pstmt.setTimestamp(++i, dbWorkItem.getFirstCommentAt());
            try {
                pstmt.setObject(++i, DefaultObjectMapper.get().writeValueAsString(dbWorkItem.getAttributes()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize attributes json ", e);
            }
            pstmt.setBoolean(++i, dbWorkItem.getIsActive());
            int affectedRows = pstmt.executeUpdate();
            // check the affected rows
            if (affectedRows <= 0) {
                throw new SQLException("Failed to create WorkItem job!");
            }
            // get the ID back
            String insertedRowId = null;
            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    insertedRowId = rs.getString(1);
                }
            }
            if (insertedRowId == null) {
                throw new SQLException("Failed to get inserted rowId.");
            }
            // insert priority sla
            if (StringUtils.isNotEmpty(dbWorkItem.getPriority())) {
                workItemsPrioritySLAService.insert(company,
                        DbWorkItemPrioritySLA.builder()
                                .priority(dbWorkItem.getPriority())
                                .workitemType(dbWorkItem.getWorkItemType())
                                .project(dbWorkItem.getProject())
                                .integrationId(dbWorkItem.getIntegrationId())
                                .build());
            }
            return insertedRowId;
        }
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class, UserIdentityService.class);
    }

    //we dont support updates because the insert does all the work
    @Override
    public Boolean update(String company, DbWorkItem t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    //we dont support this get because the filter requires: integration_id + key + ingestedAt??
    @Override
    public Optional<DbWorkItem> get(String company, String param) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public DbWorkItem get(String company, String integrationId, String workItemId, Long ingestedAt) throws SQLException {
        Validate.notNull(workItemId, "Missing workitem_id.");
        Validate.notNull(integrationId, "Missing integrationId.");
        String sql = "SELECT * FROM " + company + "." + TABLE_NAME
                + " WHERE workitem_id = ?"
                + " AND integration_id = ? "
                + (ingestedAt != null ? " AND ingested_at = ? " : " ORDER BY ingested_at DESC LIMIT 1 ");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, workItemId);
            pstmt.setObject(2, Integer.valueOf(integrationId));
            if (ingestedAt != null) {
                pstmt.setLong(3, ingestedAt);
            }
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapToDbWorkItem(rs);
            }
        }
        return null;
    }

    public DbWorkItem get(String company, String integrationId, String workItemId) throws SQLException {
        return get(company, integrationId, workItemId, null);
    }

    @Override
    public DbListResponse<DbWorkItem> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        log.info("list: API Being hit for {}, pageNumber-{} and pageSize-{}", company, pageNumber, pageSize);
        String criteria = " ";
        String sql = "SELECT * " +
                " FROM " + company + "." + TABLE_NAME + " as w" + criteria
                + "LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        List<DbWorkItem> retval = new ArrayList<>();
        String countSQL = "SELECT COUNT(*) FROM ( SELECT w.integration_id,w.workitem_id FROM " + company
                + "." + TABLE_NAME + " as w" + criteria + ") as ct";
        int totCount = 0;
        log.info("list: Parsing DbListResponse");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {
            log.info("list: connection established, processing queries");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                log.info("list: ResultSet processing");
                retval.add(mapToDbWorkItem(rs));
            }
            if (retval.size() > 0) {
                totCount = retval.size() + pageNumber * pageSize;
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

    /**
     * It fetches the workitem data from the DB based on ingestedAt
     *
     * @return List of DbWorkItem
     * @throws SQLException
     */
    public DbListResponse<DbWorkItem> list(String company, Long ingestedAt, Map<String, Long> ingestedAtByIntegrationId, Integer pageNumber, Integer pageSize) throws SQLException {
        Validate.notNull(ingestedAt, "ingestedAt cannot be null");

        log.info("list: API Being hit for {}, pageNumber-{} and pageSize-{}", company, pageNumber, pageSize);

        List<String> critereas = new ArrayList<>();
        if ((ingestedAt != null) && (MapUtils.isNotEmpty(ingestedAtByIntegrationId))) {
            ingestedAtByIntegrationId.forEach((integrationId, latestIngestedAt) -> {
                critereas.add(String.format("(ingested_at = %s AND integration_id = '%s') ", latestIngestedAt, integrationId));
            });
        } else {
            critereas.add("ingested_at = " + ingestedAt);
        }

        String criteria = (CollectionUtils.isEmpty(critereas)) ? ""
                : " WHERE " + String.join(" AND ", critereas);

        String sql = "SELECT * " +
                " FROM " + company + "." + TABLE_NAME + " as w" + criteria
                + "LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        List<DbWorkItem> retval = new ArrayList<>();
        String countSQL = "SELECT COUNT(*) FROM ( SELECT w.integration_id,w.workitem_id FROM " + company
                + "." + TABLE_NAME + " as w" + criteria + ") as ct";
        int totCount = 0;
        log.info("list: Parsing DbListResponse");
        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             PreparedStatement pstmt2 = conn.prepareStatement(countSQL)) {
            log.info("list: connection established, processing queries");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                log.info("list: ResultSet processing");
                retval.add(mapToDbWorkItem(rs));
            }
            if (retval.size() > 0) {
                totCount = retval.size() + pageNumber * pageSize;
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

    public DbListResponse<DbWorkItem> listByFilter(String company, WorkItemsFilter filter,
                                                   WorkItemsMilestoneFilter milestoneFilter,
                                                   final OUConfiguration ouConfig,
                                                   Integer pageNumber,
                                                   Integer pageSize) throws SQLException {
        return listByFilter(company, filter, milestoneFilter, ouConfig, pageNumber, pageSize, false);
    }

    public DbListResponse<DbWorkItem> listByFilter(String company, WorkItemsFilter filter,
                                                   WorkItemsMilestoneFilter milestoneFilter,
                                                   final OUConfiguration ouConfig,
                                                   Integer pageNumber,
                                                   Integer pageSize,
                                                   boolean needSlaColumns) throws SQLException {
        log.info("listByFilter: API Being hit for {}, pageNumber-{} and pageSize-{}", company, pageNumber, pageSize);
        Instant now = Instant.now();
        Map<String, SortingOrder> sortBy = filter.getSort();
        List<DbWorkItemField> workItemCustomFields = null;
        if (MapUtils.isNotEmpty(filter.getCustomFields())) {
            workItemCustomFields = workItemFieldsMetaService.listByFilter(company, filter.getIntegrationIds(), true,
                    null, null, null, null, null, 0,
                    1000).getRecords();
        }
        Query selectionCriteria = WorkItemQueryCriteria.getSelectionCriteria(company, filter, workItemCustomFields, null, null, null, true, ouConfig);
        Query milestoneSelectionCriteria = WorkItemMilestoneQueryCriteria.getSelectionCriteria(milestoneFilter, null);
        Query timelineSelectionCriteria = WorkItemTimelineQueryCriteria.getSelectionCriteria(WORKITEM_TIMELINES_TBL_QUALIFIER,WorkItemsTimelineFilter.builder().build(), null);
        Query.QueryConditions milestoneQueryConditions = milestoneSelectionCriteria.getCriteria();
        Query.QueryConditions queryConditions = selectionCriteria.getCriteria();

        if (filter.getAcross() != null && filter.getAcross().equals(WorkItemsFilter.DISTINCT.trend)
                && filter.getAggInterval() != null && !AGG_INTERVAL.day.name().equalsIgnoreCase(filter.getAggInterval())) {
            Query trendEpochForInterval = AggTimeQueryHelper.getAggTimeQueryForTimestampForList(filter.getAggInterval(), true);
            selectionCriteria.getSelectFields().addAll(trendEpochForInterval.getSelectFields());
            Query trendEpochForIntervalFilter = AggTimeQueryHelper.getAggTimeQueryForTimestampForFilter(filter.getAggInterval(), true);
            selectionCriteria.getCriteria().getConditions().add("trend_epoch = " + trendEpochForIntervalFilter.getSelectFields().get(0).getField());
            queryConditions.getQueryParams().put("wi_ingested_at_for_trend", filter.getIngestedAt());
            selectionCriteria.getCriteria().getConditions().remove("ingested_at = (:ingested_at)");
        }

        String whereClause = (CollectionUtils.isNotEmpty(queryConditions.getConditions()))
                ? " WHERE " + String.join(" AND ", queryConditions.getConditions()) : "";

        String orderByString = "";
        if (MapUtils.isNotEmpty(sortBy)) {
            String orderByField = sortBy.keySet().stream().findFirst().get();
            SortingOrder sortOrder = sortBy.values().stream().findFirst().get();
            if ("milestone_start_date".equals(orderByField) || "milestone_end_date".equals(orderByField) || TIME_BASED_SORTABLE_COLUMNS.contains(orderByField)) {
                orderByString = " ORDER BY " + orderByField + " " + sortOrder + " NULLS LAST, workitem_id ASC ";
            } else {
                orderByString = " ORDER BY LOWER(" + orderByField + ") " + sortOrder + " NULLS LAST, workitem_id ASC ";
            }
        }
        boolean needMilestonesJoin = milestoneFilter.isSpecified();
        List<Query.SelectField> selectFields = new ArrayList<>();
        boolean needSlaTimeStuff = filter.getExtraCriteria() != null &&
                (filter.getExtraCriteria().contains(WorkItemsFilter.EXTRA_CRITERIA.missed_resolution_time)
                        || filter.getExtraCriteria().contains(WorkItemsFilter.EXTRA_CRITERIA.missed_response_time));
        needSlaTimeStuff = needSlaColumns || needSlaTimeStuff;
        boolean needStageTimelinesJoin = CollectionUtils.isNotEmpty(filter.getStages()) ||
                CollectionUtils.isNotEmpty(filter.getExcludeStages());

        if (needMilestonesJoin) {
            selectFields.add(Query.selectField("workitems.*"));
            selectFields.add(Query.selectField("milestone_start_date"));
            selectFields.add(Query.selectField("milestone_end_date"));
            if (needSlaTimeStuff) {
                selectFields.add(Query.selectField("p.*"));
            }
        } else {
            selectFields.add(Query.selectField("*"));
        }
        selectFields.addAll(selectionCriteria.getSelectFields());

        List<Query.SelectField> milestoneSelectFields = new ArrayList<>(milestoneSelectionCriteria.getSelectFields());
        List<Query.SelectField> timelineSelectFields = new ArrayList<>(timelineSelectionCriteria.getSelectFields());

        Query fetchWorkitems;
        if (needMilestonesJoin || needSlaTimeStuff) {
            fetchWorkitems = Query.builder().select(selectFields)
                    .from(Query.fromField(company + "." + TABLE_NAME))
                    .build();
        } else {
            fetchWorkitems = Query.builder().select(selectFields)
                    .from(Query.fromField(company + "." + TABLE_NAME))
                    .build();
        }

        Query.QueryBuilder fetchMilestonesBuilder = Query.builder().select(milestoneSelectFields)
                .from(Query.fromField(company + "." + MILESTONES_TABLE_NAME))
                .where(milestoneSelectionCriteria.getCriteria(), Query.Condition.AND);
        if (milestoneFilter.getSprintCount() > 0) {
            fetchMilestonesBuilder = fetchMilestonesBuilder
                    .orderBy(Query.sortByField("end_date", "DESC", false))
                    .limit(milestoneFilter.getSprintCount());
        }
        Query fetchMilestones = fetchMilestonesBuilder.build();
        if (needMilestonesJoin) {
            timelineSelectFields.add(Query.selectField("milestone_start_date"));
            timelineSelectFields.add(Query.selectField("milestone_end_date"));
            timelineSelectFields.add(Query.selectField("latest_ingested_at"));
        }
        Query fetchTimelines = Query.builder().select(timelineSelectFields)
                .from(Query.fromField(company + "." + TIMELINES_TABLE_NAME, "timelines"))
                .build();

        String sql;
        String countSQL;
        String slaQuery = "";
        String innerTimelineJoin = "";
        if (needSlaTimeStuff || Boolean.TRUE.equals(filter.getIncludeSolveTime())) {
            fetchWorkitems.getSelectFields().addAll(List.of(
                    Query.selectField("extract(epoch from(COALESCE(first_comment_at,now())-workitem_created_at))", "resp_time")
            ));
            boolean needExcludeTime = CollectionUtils.isNotEmpty(filter.getExcludeStages());
            String solveTimeString = "extract(epoch from coalesce(workitem_resolved_at, now())) - extract(epoch from  workitem_created_at)";
            fetchWorkitems.getSelectFields().addAll(List.of(
                    needExcludeTime
                            ? Query.selectField("greatest(" + solveTimeString + " - COALESCE(exclude_time, 0), 0)", "solve_time")
                            : Query.selectField(solveTimeString, "solve_time")
            ));

            if (needExcludeTime) {
                Query timelineQueryCriteria = WorkItemTimelineQueryCriteria.getSelectionCriteria(
                        WorkItemsTimelineFilter.builder()
                                .fieldTypes(List.of("status"))
                                .fieldValues(filter.getExcludeStages())
                                .build(), "timeline_");
                selectionCriteria.getCriteria().getQueryParams().putAll(timelineQueryCriteria.getCriteria().getQueryParams());
                List<Query.SelectField> innerTimelineSelectFields = new ArrayList<>(
                        List.of(
                                Query.selectField("integration_id", "timeline_integration_id"),
                                Query.selectField("workitem_id", "timeline_workitem_id"),
                                Query.selectField("COALESCE( SUM(EXTRACT(EPOCH FROM COALESCE(end_date, now())) - EXTRACT(EPOCH FROM start_date)) , 0)",
                                        "exclude_time")
                        ));
                List<Query.GroupByField> timelineGroupByFields = new ArrayList<>(
                        List.of(
                                Query.groupByField("timeline_integration_id"),
                                Query.groupByField("timeline_workitem_id")
                        ));
                Query timelineQuery = Query.builder().select(innerTimelineSelectFields)
                        .from(Query.fromField(company + "." + TIMELINES_TABLE_NAME))
                        .where(timelineQueryCriteria.getCriteria(), Query.Condition.AND)
                        .groupBy(timelineGroupByFields)
                        .build();
                String wiAlias = needMilestonesJoin ? "workitems" : "wi";
                innerTimelineJoin = " left join ( " + timelineQuery.toSql() + " ) wtm ON "
                        + wiAlias + ".integration_id = wtm.timeline_integration_id AND " + wiAlias + ".workitem_id = wtm.timeline_workitem_id ";
            }
        }
        if (needSlaTimeStuff) {
            Query prioritySlaQuery = Query.builder()
                    .select(List.of(
                            Query.selectField("solve_sla"),
                            Query.selectField("resp_sla"),
                            Query.selectField("project", "proj"),
                            Query.selectField("workitem_type", "ttype"),
                            Query.selectField("priority", "prio"),
                            Query.selectField("integration_id", "integid")
                    ))
                    .from(Query.fromField(
                            company + "." + PRIORITIES_SLA_TABLE, null))
                    .build();
            slaQuery = " inner join ( " + prioritySlaQuery.toSql() + " ) p ON "
                    + " p.proj = workitems.project AND p.prio = workitems.priority AND p.integid = workitems.integration_id AND p.ttype = workitems.workitem_type";
        }
        String stageTimelineQuery = "";
        if (needStageTimelinesJoin) {
            stageTimelineQuery = getStageTimelineQuery(company, filter, selectionCriteria);
        }
        boolean needFirstAssigneeJoin = MapUtils.isNotEmpty(filter.getMissingFields())
                && filter.getMissingFields().containsKey(WorkItemsFilter.MISSING_BUILTIN_FIELD.first_assignee.toString())
                && filter.getMissingFields().get(WorkItemsFilter.MISSING_BUILTIN_FIELD.first_assignee.toString());
        String firstAssigneeQuery = "";
        if (needFirstAssigneeJoin) {
            Query firstAssigneeSelectionQuery = WorkItemTimelineQueryCriteria.getSelectionCriteria(
                    WorkItemsTimelineFilter.builder()
                            .integrationIds(filter.getIntegrationIds())
                            .fieldTypes(List.of("assignee"))
                            .excludeFieldValues(List.of("UNASSIGNED"))
                            .build(), "fa_timeline_");
            selectionCriteria.getCriteria().getQueryParams().putAll(firstAssigneeSelectionQuery.getCriteria().getQueryParams());
            List<Query.SelectField> firstAssigneeTimelineSelectFields = new ArrayList<>(
                    List.of(
                            Query.selectField("DISTINCT ON (integration_id, workitem_id) integration_id", "tl_integration_id"),
                            Query.selectField("workitem_id", "tl_workitem_id")
                    ));
            List<Query.SortByField> firstAssigneeTimelineOrderByFields = new ArrayList<>(
                    List.of(
                            Query.sortByField("tl_integration_id", "ASC", true),
                            Query.sortByField("workitem_id", "ASC", true),
                            Query.sortByField("start_date", "ASC", true)
                    ));
            Query firstAssigneeTimelineQuery = Query.builder().select(firstAssigneeTimelineSelectFields)
                    .from(Query.fromField(company + "." + TIMELINES_TABLE_NAME))
                    .where(firstAssigneeSelectionQuery.getCriteria(), Query.Condition.AND)
                    .orderBy(firstAssigneeTimelineOrderByFields)
                    .build();
            firstAssigneeQuery = " inner join ( " + firstAssigneeTimelineQuery.toSql() + " ) t ON " +
                    "t.tl_workitem_id = workitems.workitem_id AND t.tl_integration_id = workitems.integration_id";
        }
        if (needMilestonesJoin) {
            String integrationTrackerJoinSQL =  " INNER JOIN "+company+"."+INTEGRATION_TRACKER_TABLE_NAME+" it ON timelines.integration_id = it.integration_id";
            String intermediateSQL = fetchTimelines.toSql() + integrationTrackerJoinSQL + " INNER JOIN (" + fetchMilestones.toSql() + ") as milestones ON "
                    + "milestones.milestone_parent_field_value || '\\' || milestones.milestone_name = timelines.field_value AND "
                    + "milestones.milestone_integration_id = timelines.integration_id "
                    + "AND timelines.start_date <= to_timestamp(latest_ingested_at) AND timelines.end_date >= to_timestamp(latest_ingested_at) ";


            String tempSQL = fetchWorkitems.toSql() + " as workitems "
                    + innerTimelineJoin
                    + stageTimelineQuery
                    + slaQuery
                    + firstAssigneeQuery
                    + " INNER JOIN (" + intermediateSQL + ") tmp"
                    + " ON tmp.timeline_workitem_id = workitem_id AND"
                    + " tmp.timeline_integration_id = integration_id ";

            String finalQuerySelectFields = "*";
            if (BooleanUtils.isTrue(filter.getIncludeSprintFullNames())) {
                finalQuerySelectFields += ", " + generateSprintFullNamesArraySql(company, "final");
            }
            sql = "select distinct on (workitem_id, integration_id) " + finalQuerySelectFields
                    + "from ( "
                    + tempSQL
                    + orderByString
                    + " ) final "
                    + whereClause
                    + " order by workitem_id, integration_id";
            countSQL = "SELECT COUNT(*) FROM (" + sql + ") as ct";
            sql += "  LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
            queryConditions.getQueryParams().putAll(milestoneQueryConditions.getQueryParams());
        } else {
            String finalQuerySelectFields = "*";
            if (BooleanUtils.isTrue(filter.getIncludeSprintFullNames())) {
                finalQuerySelectFields += ", " + generateSprintFullNamesArraySql(company, "workitems");
            }
            sql = "select " + finalQuerySelectFields
                    + "from ( "
                    + fetchWorkitems.toSql() + " as wi " + innerTimelineJoin
                    + " ) as workitems "
                    + stageTimelineQuery
                    + slaQuery
                    + firstAssigneeQuery
                    + whereClause
                    + orderByString;
            countSQL = "SELECT COUNT(*) FROM (" + sql + ") as ct";
            sql += " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        }
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", queryConditions.getQueryParams());
        log.info("list: Parsing DbListResponse");
        List<DbWorkItem> aggResults = template.query(sql, queryConditions.getQueryParams(), DbWorkItemConverters.listRowMapper());
        aggResults = aggResults.stream()
                .map(dbWorkItem -> {
                    List<DbWorkItemHistory> workItemHistories = template.query(
                            "SELECT * FROM " + company + "." + TIMELINES_TABLE_NAME
                                    + " WHERE workitem_id = :workitemId AND integration_id = :integrationId"
                                    + " ORDER BY start_date DESC",
                            Map.of("workitemId", dbWorkItem.getWorkItemId(),
                                    "integrationId", Integer.parseInt(dbWorkItem.getIntegrationId())),
                            DbWorkItemHistoryConverters.listRowMapper());

                    List<DbWorkItemHistory> statusList = DbWorkItemHistoryConverters.sanitizeEventList(workItemHistories.stream()
                            .filter(dbWorkItemHistory -> "status".equalsIgnoreCase(dbWorkItemHistory.getFieldType()))
                            .collect(Collectors.toList()), now);
                    List<DbWorkItemHistory> assigneeList = DbWorkItemHistoryConverters.sanitizeEventList(workItemHistories.stream()
                            .filter(dbWorkItemHistory -> "assignee".equalsIgnoreCase(dbWorkItemHistory.getFieldType()))
                            .collect(Collectors.toList()), now);

                    return dbWorkItem.toBuilder()
                            .statusList(statusList)
                            .assigneeList(assigneeList)
                            .build();
                })
                .collect(Collectors.toList());
        log.info("countSQL = {}", countSQL);
        Integer totalCount = template.queryForObject(countSQL, queryConditions.getQueryParams(), Integer.class);
        return DbListResponse.of(aggResults, totalCount);
    }

    private String generateSprintFullNamesArraySql(String company, String workItemTableAlias) {
        return "  array(" +
                " select t.field_value as sprint_full_name " +
                " from " + company + ".issue_mgmt_workitems_timeline as t " +
                " join " + company + ".issue_mgmt_milestones as m " +
                " on " +
                "   m.field_type = t.field_type " +
                "   and m.integration_id = t.integration_id " +
                "   and m.parent_field_value || '\\' || m.name = t.field_value  " +
                " where " +
                "   t.workitem_id = " + workItemTableAlias + ".workitem_id and " +
                "   t.integration_id = " + workItemTableAlias + ".integration_id and " +
                "   t.field_type='sprint' " +
                " order by m.start_date asc " +
                " ) as sprint_full_names ";
    }

    @NotNull
    private String getStageTimelineQuery(String company, WorkItemsFilter filter, Query selectionCriteria) {
        Query timelineQueryCriteria = WorkItemTimelineQueryCriteria.getSelectionCriteria(
                WorkItemsTimelineFilter.builder()
                        .fieldTypes(List.of("status"))
                        .fieldValues(filter.getStages())
                        .excludeFieldValues(filter.getExcludeStages())
                        .build(), "timeline_");
        selectionCriteria.getCriteria().getQueryParams().putAll(timelineQueryCriteria.getCriteria().getQueryParams());
        List<Query.SelectField> stageTimelineSelectFields = new ArrayList<>(
                List.of(
                        Query.selectField("integration_id", "timeline_integration_id"),
                        Query.selectField("workitem_id", "timeline_workitem_id"),
                        Query.selectField("count(*)", "count")
                ));
        List<Query.GroupByField> timelineGroupByFields = new ArrayList<>(
                List.of(
                        Query.groupByField("timeline_integration_id"),
                        Query.groupByField("timeline_workitem_id")
                ));
        Query timelineQuery = Query.builder()
                .select(stageTimelineSelectFields)
                .from(Query.fromField(company + "." + TIMELINES_TABLE_NAME))
                .where(timelineQueryCriteria.getCriteria(), Query.Condition.AND)
                .groupBy(timelineGroupByFields)
                .build();

        return " INNER JOIN ( " + timelineQuery.toSql() + " ) wt ON "
                + " workitems.integration_id = wt.timeline_integration_id AND workitems.workitem_id = wt.timeline_workitem_id ";
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        log.info("Deleted record for id-{} and company-{}", id, company);
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

    public Boolean updateWorkItems(String company, String updateFields, List<String> conditions, Map<String, Object> params) {
        String updateWorkitemSql = "UPDATE %s." + TABLE_NAME + " " + updateFields;
        String whereClause = "";
        if (CollectionUtils.isNotEmpty(conditions)) {
            whereClause = String.join(" AND ", conditions);
        }
        updateWorkitemSql = updateWorkitemSql + " WHERE " + whereClause;
        String updatedSql = String.format(updateWorkitemSql, company);
        log.info("updateWorkItems sql = {}", updatedSql);
        log.info("updateWorkItems params = {}", params);
        if (template.update(updatedSql, params) >= 0) {
            log.info("Updated workitems for company {}", company);
            return true;
        } else {
            log.error("Failed to Update workitems for company {}", company);
            return false;
        }

    }

    public DbListResponse<DbAggregationResult> getWorkItemsReport(String company, WorkItemsFilter filter,
                                                                  WorkItemsMilestoneFilter milestoneFilter,
                                                                  WorkItemsFilter.DISTINCT stack,
                                                                  Boolean valuesOnly,
                                                                  final OUConfiguration ouConfig) throws SQLException {
        return workItemsReportService.generateReport(company, filter, milestoneFilter, stack, valuesOnly, ouConfig);
    }

    public DbListResponse<DbAggregationResult> getAssigneeAllocationReport(String company, WorkItemsFilter filter,
                                                                           WorkItemsFilter.DISTINCT stack,
                                                                           Boolean valuesOnly,
                                                                           final OUConfiguration ouConfig,
                                                                           int page, int pageSize) throws SQLException, BadRequestException {
        return workItemsAssigneeAllocationReportService.generateReport(company, filter, stack, valuesOnly, ouConfig, page, pageSize);
    }

    public DbListResponse<DbAggregationResult> getWorkItemsStageTimesReport(String company, WorkItemsFilter itemsFilter,
                                                                            WorkItemsTimelineFilter historyFilter,
                                                                            WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                                                            WorkItemsFilter.DISTINCT stack,
                                                                            Boolean valuesOnly,
                                                                            final OUConfiguration ouConfig) throws SQLException {
        return workItemsStageTimesReportService.generateReport(company, itemsFilter, workItemsMilestoneFilter, historyFilter, stack, valuesOnly, ouConfig);
    }

    public DbListResponse<DbAggregationResult> getWorkItemsAgeReport(String company, WorkItemsFilter filter,
                                                                     WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                                                     WorkItemsFilter.DISTINCT stack, Boolean valuesOnly,
                                                                     final OUConfiguration ouConfig) throws SQLException {
        return workItemsAgeReportService.generateReport(company, filter, workItemsMilestoneFilter, stack, valuesOnly, ouConfig);
    }

    public DbListResponse<DbAggregationResult> getWorkItemsHopsReport(String company, WorkItemsFilter filter,
                                                                      WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                                                      WorkItemsFilter.DISTINCT stack, Boolean valuesOnly,
                                                                      final OUConfiguration ouConfig) throws SQLException {
        return workItemsHopsReportService.generateReport(company, filter, workItemsMilestoneFilter, stack, valuesOnly, ouConfig);
    }

    public DbListResponse<DbAggregationResult> getWorkItemsBouncesReport(String company, WorkItemsFilter filter,
                                                                         WorkItemsMilestoneFilter workItemsMilestoneFilter,
                                                                         WorkItemsFilter.DISTINCT stack, Boolean valuesOnly,
                                                                         final OUConfiguration ouConfig) throws SQLException {
        return workItemsBouncesReportService.generateReport(company, filter, workItemsMilestoneFilter, stack, valuesOnly, ouConfig);
    }

    public DbListResponse<DbAggregationResult> getWorkItemsResolutionTimeReport(String company, WorkItemsFilter filter,
                                                                                WorkItemsMilestoneFilter milestoneFilter,
                                                                                WorkItemsFilter.DISTINCT stack, Boolean valuesOnly,
                                                                                final OUConfiguration ouConfig) throws SQLException {
        return workItemsResolutionTimeReportService.generateReport(company, filter, milestoneFilter, stack, valuesOnly, ouConfig);
    }

    public DbListResponse<DbAggregationResult> getWorkItemsReponseTimeReport(String company, WorkItemsFilter filter,
                                                                             WorkItemsMilestoneFilter milestoneFilter,
                                                                             WorkItemsFilter.DISTINCT stack, Boolean valuesOnly,
                                                                             final OUConfiguration ouConfig) throws SQLException {
        return workItemsResponseTimeReportService.generateReport(company, filter, milestoneFilter, stack, valuesOnly, ouConfig);
    }

    public DbListResponse<DbAggregationResult> getSprintMetricsReport(String company, WorkItemsFilter itemsFilter,
                                                                      WorkItemsMilestoneFilter milestoneFilter,
                                                                      WorkItemsSprintMappingFilter sprintMappingFilter,
                                                                      WorkItemsFilter.DISTINCT stack, Boolean valuesOnly,
                                                                      final OUConfiguration ouConfig) throws SQLException {
        return workItemsSprintMetricReportService.generateReport(company, itemsFilter, milestoneFilter,
                sprintMappingFilter, stack, valuesOnly, ouConfig);
    }

    public DbListResponse<DbAggregationResult> getFirstAssigneeReport(String company, WorkItemsFilter itemsFilter, WorkItemsTimelineFilter workItemsTimelineFilter,
                                                                      WorkItemsMilestoneFilter workItemsMilestoneFilter, Boolean valuesOnly,
                                                                      final OUConfiguration ouConfig) throws SQLException {
        return workItemsFirstAssigneeReportService.generateReport(company, itemsFilter, workItemsTimelineFilter, workItemsMilestoneFilter, valuesOnly, ouConfig);
    }

    public DbListResponse<DbAggregationResult> getSprintMetricsReportCount(String company, WorkItemsFilter itemsFilter,
                                                                           WorkItemsMilestoneFilter milestoneFilter,
                                                                           WorkItemsSprintMappingFilter sprintMappingFilter,
                                                                           WorkItemsFilter.DISTINCT stack,
                                                                           Boolean valuesOnly,
                                                                           final OUConfiguration ouConfig) throws SQLException {
        return workItemsSprintMetricReportService.generateCountReport(company, itemsFilter, milestoneFilter,
                sprintMappingFilter, stack, valuesOnly, ouConfig);
    }

    public DbListResponse<DbAggregationResult> getWorkItemsStageBounceReport(String company, WorkItemsFilter filter,
                                                                             WorkItemsMilestoneFilter milestoneFilter,
                                                                             WorkItemsFilter.DISTINCT stack,
                                                                             Boolean valuesOnly,
                                                                             final OUConfiguration ouConfig) throws SQLException {
        return workItemsStageBounceReportService.generateReport(company, filter, milestoneFilter, stack, valuesOnly, ouConfig);
    }

    public int cleanUpOldData(String company, Long currentTime, Long olderThanSeconds) {
        return template.update("DELETE FROM " + company + "." + TABLE_NAME + " WHERE ingested_at < :olderThanTime "
                        + "AND EXTRACT(DAY FROM to_timestamp(ingested_at)) NOT IN (1)",
                Map.of("olderThanTime", currentTime - olderThanSeconds));
    }

    private static final String ISSUES_COUNT_SQL = "SELECT count(*) FROM %s." + TABLE_NAME + " WHERE ingested_at=:ingested_at AND integration_id IN (:integration_ids)";
    public Integer getIssuesCount(final String company, List<Integer> integrationIds, Long ingestedAt) {
        String sql = String.format(ISSUES_COUNT_SQL, company);
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ingested_at", ingestedAt);
        params.addValue("integration_ids", integrationIds);
        Integer count = template.query(sql, params, CountQueryConverter.countMapper()).get(0);
        return count;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddl = List.of("CREATE TABLE IF NOT EXISTS {0}.issue_mgmt_workitems(\n" +
                        "    id                         UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "       workitem_id             VARCHAR, integration_id INTEGER NOT NULL REFERENCES " + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "       summary                 VARCHAR,\n" +
                        "       sprint_ids              UUID[],\n" +
                        "       priority                VARCHAR,\n" +
                        "       assignee                VARCHAR,\n" +
                        "    assignee_id UUID REFERENCES " +
                        company + "." + UserIdentityService.USER_IDS_TABLE + "(id) ON DELETE RESTRICT,\n" +
                        "       epic                    VARCHAR,\n" +
                        "       parent_workitem_id      VARCHAR,\n" +
                        "       reporter                VARCHAR,\n" +
                        "    reporter_id UUID REFERENCES " +
                        company + "." + UserIdentityService.USER_IDS_TABLE + "(id) ON DELETE RESTRICT,\n" +
                        "       status                  VARCHAR,\n" +
                        "       workitem_type           VARCHAR,\n" +
                        "       story_points            float(2),\n" +
                        "       ingested_at             BIGINT,\n" +
                        "       custom_fields           jsonb,\n" +
                        "       project                 VARCHAR,\n" +
                        "       project_id              VARCHAR,\n" +
                        "       components              VARCHAR[],\n" +
                        "       labels                  VARCHAR[],\n" +
                        "       versions                VARCHAR[],\n" +
                        "       fix_versions            VARCHAR[],\n" +
                        "       resolution              VARCHAR,\n" +
                        "       status_category          VARCHAR,\n" +
                        "       original_estimate        float(2),\n" +
                        "       desc_size               INT,\n" +
                        "       hops                    INT,\n" +
                        "       bounces                 INT,\n" +
                        "       num_attachments         INT,\n" +
                        "       workitem_created_at     TIMESTAMP WITH TIME ZONE,\n" +
                        "       workitem_updated_at     TIMESTAMP WITH TIME ZONE,\n" +
                        "       workitem_resolved_at    TIMESTAMP WITH TIME ZONE,\n" +
                        "       workitem_due_at         TIMESTAMP WITH TIME ZONE,\n" +//not done
                        "       first_attachment_at     TIMESTAMP WITH TIME ZONE,\n" +//not done
                        "       first_comment_at        TIMESTAMP WITH TIME ZONE,\n" +//not done
                        "       attributes              JSONB NOT NULL DEFAULT '''{}'''::jsonb,\n" +
                        "       is_active               BOOLEAN NOT NULL DEFAULT true" +
                        ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS issue_mgmt_workitems_workitem_id_integration_id_ingested_at_idx " +
                        "on {0}." + TABLE_NAME + " (workitem_id, integration_id, ingested_at)",
                "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_ingested_at_integration_id_compound_idx " +
                        "on " + company + "." + TABLE_NAME + "(ingested_at,integration_id)",
                "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_custom_fields_idx " +
                        "on " + company + "." + TABLE_NAME + " USING GIN(custom_fields)",
                "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_components_idx " +
                        "on " + company + "." + TABLE_NAME + " USING GIN(components)",
                "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_labels_idx " +
                        "on " + company + "." + TABLE_NAME + " USING GIN(labels)",
                "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_ingested_at_idx " +
                        "on " + company + "." + TABLE_NAME + "(ingested_at)",
                "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_status_idx on " + company + "." + TABLE_NAME + "(status)",
                "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_status_category_idx on " + company + "." + TABLE_NAME + "(status_category)",
                "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_project_idx on " + company + "." + TABLE_NAME + "(project)",
                "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_integration_id_idx on " + company + "." + TABLE_NAME + "(integration_id)",
                "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_assignee_idx on " + company + "." + TABLE_NAME + "(assignee)",
                "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_epic_idx on " + company + "." + TABLE_NAME + "(epic)",
                "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_reporter_idx on " + company + "." + TABLE_NAME + "(reporter)",
                "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_workitem_type_idx on " + company + "." + TABLE_NAME + "(workitem_type)",
                "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_versions_idx on " + company + "." + TABLE_NAME + " USING GIN(versions)",
                "CREATE INDEX IF NOT EXISTS " + TABLE_NAME + "_fix_versions_idx on " + company + "." + TABLE_NAME + " USING GIN(fix_versions)"

        );
        ddl.stream().map(statement -> MessageFormat.format(statement, company)).forEach(template.getJdbcTemplate()::execute);
        return true;
    }

    @Value
    @Builder(toBuilder = true)
    public static class WorkItemIssueCriteria {
        String criterias;
        MapSqlParameterSource mapSqlParameterSource;
    }
}
