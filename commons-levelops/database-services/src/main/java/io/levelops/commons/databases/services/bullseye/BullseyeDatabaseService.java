package io.levelops.commons.databases.services.bullseye;

import io.levelops.commons.databases.converters.DbBullseyeProjectConverters;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.bullseye.BullseyeBuild;
import io.levelops.commons.databases.models.database.bullseye.BullseyeFolder;
import io.levelops.commons.databases.models.database.bullseye.BullseyeSourceFile;
import io.levelops.commons.databases.models.database.bullseye.DbBullseyeBuild;
import io.levelops.commons.databases.models.filters.BullseyeBuildFilter;
import io.levelops.commons.databases.models.filters.BullseyeFileFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.CiCdJobRunsDatabaseService;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.databases.utils.CriteriaUtils;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Log4j2
@Service
public class BullseyeDatabaseService extends DatabaseService<BullseyeBuild> {

    private static final String BULLSEYE_PROJECTS = "bullseye_projects";
    private static final String BULLSEYE_SOURCE_FILES = "bullseye_source_files";
    private static final String CICD_JOB_RUNS = "cicd_job_runs";
    private static final String CICD_JOBS = "cicd_jobs";
    private static final Integer BATCH_LIMIT = 100;
    private static final Set<String> PARTIAL_MATCH_COLUMNS = Set.of("name");
    private static final Set<String> SORTABLE_COLUMNS = Set.of("functions_covered", "total_functions", "decisions_covered",
            "total_decisions", "conditions_covered", "total_conditions");

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public BullseyeDatabaseService(DataSource dataSource) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(CiCdJobRunsDatabaseService.class);
    }

    @Override
    public String insert(String company, BullseyeBuild project) throws SQLException {
        return template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            String insertProjectSql = "INSERT INTO " + company + "." + BULLSEYE_PROJECTS + "(" +
                    "  cicd_job_run_id, project_id, project," +
                    "  built_at, name, directory, functions_covered, total_functions, decisions_covered, total_decisions, " +
                    "  conditions_covered, total_conditions, file_hash" +
                    " ) " +
                    " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                    " ON CONFLICT (cicd_job_run_id, project_id) " +
                    " DO UPDATE SET " +
                    " built_at=EXCLUDED.built_at, name=EXCLUDED.name, directory=EXCLUDED.directory, " +
                    " functions_covered=EXCLUDED.functions_covered, total_functions=EXCLUDED.total_functions, " +
                    " decisions_covered=EXCLUDED.decisions_covered, total_decisions=EXCLUDED.total_decisions, " +
                    " conditions_covered=EXCLUDED.conditions_covered, total_conditions=EXCLUDED.total_conditions, " +
                    " project=EXCLUDED.project, file_hash=EXCLUDED.file_hash " +
                    " RETURNING id";
            String insertSourceFileSql = "INSERT INTO " + company + "." + BULLSEYE_SOURCE_FILES + "(name, project_id, " +
                    "modification_time, functions_covered, total_functions, decisions_covered, total_decisions, " +
                    "conditions_covered, total_conditions) VALUES(?,?,?,?,?,?,?,?,?) ON CONFLICT (project_id, name) " +
                    "DO UPDATE SET modification_time=EXCLUDED.modification_time, functions_covered=EXCLUDED.functions_covered, " +
                    "total_functions=EXCLUDED.total_functions, decisions_covered=EXCLUDED.decisions_covered, " +
                    "total_decisions=EXCLUDED.total_decisions, conditions_covered=EXCLUDED.conditions_covered, " +
                    "total_conditions=EXCLUDED.total_conditions RETURNING id";
            try (PreparedStatement insertProjectStmt = conn.prepareStatement(insertProjectSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement insertSourceFileStmt = conn.prepareStatement(insertSourceFileSql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                insertProjectStmt.setObject(i++, UUID.fromString(project.getCicdJobRunId()));
                insertProjectStmt.setString(i++, project.getBuildId());
                insertProjectStmt.setString(i++, project.getJobName());
                insertProjectStmt.setObject(i++, getTimestamp(project.getBuiltAt()));
                insertProjectStmt.setString(i++, project.getName());
                insertProjectStmt.setString(i++, project.getDirectory());
                insertProjectStmt.setInt(i++, project.getFunctionsCovered());
                insertProjectStmt.setInt(i++, project.getTotalFunctions());
                insertProjectStmt.setInt(i++, project.getDecisionsCovered());
                insertProjectStmt.setInt(i++, project.getTotalDecisions());
                insertProjectStmt.setInt(i++, project.getConditionsCovered());
                insertProjectStmt.setInt(i++, project.getTotalConditions());
                insertProjectStmt.setString(i, project.getFileHash());
                insertProjectStmt.executeUpdate();
                String projectId = null;
                try (ResultSet rs = insertProjectStmt.getGeneratedKeys()) {
                    if (rs.next())
                        projectId = rs.getString(1);
                }
                if (projectId == null)
                    throw new SQLException("Failed to get inserted rowid.");
                UUID projectUuid = UUID.fromString(projectId);
                int batchCount = 0;
                for (BullseyeSourceFile file : CollectionUtils.emptyIfNull(project.getSourceFiles())) {
                    fillInsertSourceFileStmt(insertSourceFileStmt, file, file.getName(), projectUuid);
                    insertSourceFileStmt.addBatch();
                    insertSourceFileStmt.clearParameters();
                    ++batchCount;
                    if (batchCount == BATCH_LIMIT) {
                        insertSourceFileStmt.executeBatch();
                        batchCount = 0;
                    }
                }
                for (BullseyeFolder folder: CollectionUtils.emptyIfNull(project.getFolders()))
                    batchCount = insertSourceFilesOfFolder(insertSourceFileStmt, folder, projectUuid, "",
                            batchCount);
                if (batchCount != 0)
                    insertSourceFileStmt.executeBatch();
                return projectId;
            }
        }));
    }


    private int insertSourceFilesOfFolder(PreparedStatement statement, BullseyeFolder folder, UUID projectUuid,
                                          String currentPath, int batchCount) throws SQLException {
        for (BullseyeSourceFile file : CollectionUtils.emptyIfNull(folder.getSourceFiles())) {
            fillInsertSourceFileStmt(statement, file, currentPath + file.getName(), projectUuid);
            statement.addBatch();
            statement.clearParameters();
            ++batchCount;
            if (batchCount == BATCH_LIMIT) {
                statement.executeBatch();
                batchCount = 0;
            }
        }
        for (BullseyeFolder nestedFolder: CollectionUtils.emptyIfNull(folder.getFolders()))
            batchCount = insertSourceFilesOfFolder(statement, nestedFolder, projectUuid,
                    currentPath + folder.getName() + "/", batchCount);
        return batchCount;
    }

    private void fillInsertSourceFileStmt(PreparedStatement statement, BullseyeSourceFile file, String filename,
                                          UUID projectUuid) throws SQLException {
        int index = 1;
        statement.setObject(index++, filename);
        statement.setObject(index++, projectUuid);
        statement.setObject(index++, getTimestamp(file.getModificationTime()));
        statement.setObject(index++, file.getFunctionsCovered());
        statement.setObject(index++, file.getTotalFunctions());
        statement.setObject(index++, file.getDecisionsCovered());
        statement.setObject(index++, file.getTotalDecisions());
        statement.setObject(index++, file.getConditionsCovered());
        statement.setObject(index, file.getTotalConditions());
    }

    public DbListResponse<DbBullseyeBuild> listProjects(String company,
                                                      BullseyeBuildFilter filter,
                                                      Map<String, SortingOrder> sortBy,
                                                      Integer pageNumber,
                                                      Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        boolean cicdJobTableNeeded = isCicdJobTableNeeded(filter);
        List<String> conditions = createWhereClauseAndUpdateParams(params, filter, "", "");
        setPagingParams(params, pageNumber, pageSize);
        String limitClause = " OFFSET :offset LIMIT :limit";
        String whereClause = getWhereClause(conditions);
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (SORTABLE_COLUMNS.contains(entry.getKey()))
                        return entry.getKey();
                    return "functions_covered";
                })
                .orElse("functions_covered");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        String orderByClause = " ORDER BY " + sortByKey + " " + sortOrder.name();
        String selectQuery = "SELECT * FROM " + company + "." + BULLSEYE_PROJECTS + " ";
        if (cicdJobTableNeeded) {
            selectQuery = "SELECT * FROM (" +
                    "   SELECT t.*, j.job_name, j.job_full_name, j.job_normalized_full_name " +
                    "   FROM (" +
                    "     SELECT p.*, r.cicd_job_id FROM " + company + "." + BULLSEYE_PROJECTS + " AS p " +
                    "     JOIN " + company + "." + CICD_JOB_RUNS + " AS r " +
                    "     ON p.cicd_job_run_id = r.id" +
                    "   ) AS t" +
                    "   JOIN " + company + "." + CICD_JOBS + " AS j " +
                    "   ON t.cicd_job_id = j.id" +
                    " ) AS b ";
        }
        String sqlQuery = selectQuery + whereClause + orderByClause + limitClause;
        List<DbBullseyeBuild> projects = template.query(sqlQuery, params, DbBullseyeProjectConverters.projectListRowMapper(cicdJobTableNeeded));
        String countSql = "SELECT COUNT(*) FROM (" + selectQuery + ") AS x";
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(projects, count);
    }

    private boolean isCicdJobTableNeeded(BullseyeBuildFilter filter) {
        return (CollectionUtils.isNotEmpty(filter.getJobNames()) || CollectionUtils.isNotEmpty(filter.getJobFullNames()) ||
                CollectionUtils.isNotEmpty(filter.getJobNormalizedFullNames()));
    }

    public DbListResponse<BullseyeSourceFile> listFiles(String company,
                                                        BullseyeBuildFilter projectFilter,
                                                        BullseyeFileFilter fileFilter,
                                                        Map<String, SortingOrder> sortBy,
                                                        Integer pageNumber,
                                                        Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        List<String> conditions = createWhereClauseAndUpdateParams(params, projectFilter, "project", "be_build_");
        setPagingParams(params, pageNumber, pageSize);
        String limitClause = " OFFSET :offset LIMIT :limit";
        String whereClause = getWhereClause(conditions);
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (SORTABLE_COLUMNS.contains(entry.getKey()))
                        return entry.getKey();
                    return "functions_covered";
                })
                .orElse("functions_covered");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        String orderByClause = " ORDER BY " + sortByKey + " " + sortOrder.name();
        String projectTable = " (SELECT * FROM (SELECT * FROM " + company + "." + BULLSEYE_PROJECTS + ") AS project " +
                whereClause + ") AS project ";
        conditions = createWhereClauseAndUpdateParams(params, fileFilter, "file", "be_file_");
        whereClause = getWhereClause(conditions);
        String fileTable = " (SELECT * FROM (SELECT * FROM " + company + "." + BULLSEYE_SOURCE_FILES + ") AS file " +
                whereClause + ") AS file ";
        String joinQuery = "SELECT file.* FROM " + projectTable + " INNER JOIN " + fileTable +
                " ON project.id = file.project_id ";
        String sqlQuery = joinQuery + orderByClause + limitClause;
        List<BullseyeSourceFile> files = template.query(sqlQuery, params, DbBullseyeProjectConverters.fileListRowMapper());
        String countSql = "SELECT COUNT(*) FROM (" + joinQuery + ") AS x";
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(files, count);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateProjects(String company, BullseyeBuildFilter filter,
                                                                           String configTableKey) {
        BullseyeBuildFilter.Distinct distinct = filter.getAcross();
        Map<String, Object> params = new HashMap<>();
        boolean cicdJobTableNeeded = isCicdJobTableNeeded(filter);
        List<String> conditions = createWhereClauseAndUpdateParams(params, filter, "", "be_build_");
        String attributes = " SUM(functions_covered) AS functions_covered," +
                " SUM(total_functions) AS total_functions, " +
                getUncovered("SUM(total_functions)", "SUM(functions_covered)") + " AS functions_uncovered," +
                getPercentage("SUM(total_functions)", "SUM(functions_covered)") + " AS function_percentage_coverage," +
                " SUM(decisions_covered) AS decisions_covered, " +
                " SUM(total_decisions) AS total_decisions, " +
                getUncovered("SUM(total_decisions)", "SUM(decisions_covered)") + " AS decisions_uncovered," +
                getPercentage("SUM(total_decisions)", "SUM(decisions_covered)") + " AS decision_percentage_coverage," +
                " SUM(conditions_covered) AS conditions_covered, " +
                " SUM(total_conditions) AS total_conditions, " +
                getUncovered("SUM(total_conditions)", "SUM(conditions_covered)") + " AS conditions_uncovered," +
                getPercentage("SUM(total_conditions)", "SUM(conditions_covered)") + " AS condition_percentage_coverage ";
        String groupByKey, aggName = "";
        switch (distinct) {
            case trend:
                groupByKey = "date";
                aggName = " , EXTRACT(EPOCH FROM built_at) AS date";
                break;
            case job_run_id:
                groupByKey = "cicd_job_run_id,project";
                break;
            case project:
                groupByKey = "project";
                break;
            case directory:
                groupByKey = "directory";
                break;
            case name:
                groupByKey = "name";
                break;
            case job_name:
                groupByKey = "job_name";
                cicdJobTableNeeded = true;
                break;
            case job_full_name:
                groupByKey = "job_full_name";
                cicdJobTableNeeded = true;
                break;
            case job_normalized_full_name:
                groupByKey = "job_normalized_full_name";
                cicdJobTableNeeded = true;
                break;
            default:
                groupByKey = distinct.name();
                break;
        }
        if (StringUtils.isEmpty(aggName))
            aggName = "," + groupByKey;
        String whereClause = getWhereClause(conditions);
        String selectQuery = " FROM " + company + "." + BULLSEYE_PROJECTS + " ";
        if (cicdJobTableNeeded) {
            String jobColumns = " %sjob_name, %sjob_full_name, %sjob_normalized_full_name ";
            selectQuery = " FROM (SELECT t.*," + String.format(jobColumns, "j.", "j.", "j.") + "FROM " +
                    "(SELECT p.*, r.cicd_job_id FROM " + company + "." + BULLSEYE_PROJECTS + " AS p JOIN " +
                    company + "." + CICD_JOB_RUNS + " AS r ON p.cicd_job_run_id = r.id) AS t JOIN " +
                    company + "." + CICD_JOBS + " AS j ON t.cicd_job_id = j.id) AS b ";
        }
        String query = "SELECT " + attributes + aggName + selectQuery + whereClause + " GROUP BY " + groupByKey;
        List<DbAggregationResult> aggregationResults;
        String[] keys = groupByKey.split(",");
        aggregationResults = template.query(query, params, DbBullseyeProjectConverters.aggRowMapper(keys[0],
                keys.length > 1 ? keys[1] : null));
        return DbListResponse.of(aggregationResults, aggregationResults.size());
    }

    private String getUncovered(String total, String covered) {
        return total + " - " + covered;
    }

    private String getPercentage(String total, String covered) {
        return "CASE WHEN " + total + " > 0 THEN (" + covered + "::decimal/" + total + ")*100 ELSE 0 END";
    }

    private List<String> createWhereClauseAndUpdateParams(Map<String, Object> params, BullseyeBuildFilter filter,
                                                          String tableAlias, String prefix) {
        List<String> buildConditions = new LinkedList<>();
        tableAlias = StringUtils.isEmpty(tableAlias) ? "" : tableAlias + ".";
        insertLongRange(buildConditions, params, tableAlias + "functions_covered", prefix + "functions_covered",
                filter.getFunctionsCovered());
        insertLongRange(buildConditions, params, tableAlias + "total_functions", prefix + "total_functions",
                filter.getTotalFunctions());
        insertLongRange(buildConditions, params, tableAlias + "decisions_covered", prefix + "decisions_covered",
                filter.getDecisionsCovered());
        insertLongRange(buildConditions, params, tableAlias + "total_decisions", prefix + "total_decisions",
                filter.getTotalDecisions());
        insertLongRange(buildConditions, params, tableAlias + "conditions_covered", prefix + "conditions_covered",
                filter.getConditionsCovered());
        insertLongRange(buildConditions, params, tableAlias + "total_conditions", prefix + "total_conditions",
                filter.getTotalConditions());
        insertDateRange(buildConditions, params, tableAlias + "built_at", prefix + "built_at_range",
                filter.getBuiltAtRange());
        insertList(buildConditions, params, tableAlias + "name", prefix + "names", filter.getNames());
        insertList(buildConditions, params, tableAlias + "directory", prefix + "directories",
                filter.getDirectories());
        insertList(buildConditions, params, tableAlias + "cicd_job_run_id", prefix + "cicd_job_run_ids",
                ListUtils.emptyIfNull(filter.getCicdJobRunIds()).stream()
                        .map(UUID::fromString)
                        .collect(Collectors.toList()));
        insertList(buildConditions, params, tableAlias + "project", prefix + "projects",
                filter.getProjects());
        insertList(buildConditions, params, tableAlias + "project_id", prefix + "build_ids",
                filter.getBuildIds());
        insertList(buildConditions, params, tableAlias + "job_name", prefix + "job_names", filter.getJobNames());
        insertList(buildConditions, params, tableAlias + "job_full_name", prefix + "job_full_names",
                filter.getJobFullNames());
        insertList(buildConditions, params, tableAlias + "job_normalized_full_name", prefix + "job_normalized_full_names",
                filter.getJobNormalizedFullNames());
        insertList(buildConditions, params, tableAlias + "file_hash", prefix + "file_hashes",
                filter.getFileHashes());
        Map<String, Map<String, String>> partialMatchMap = filter.getPartialMatch();
        if (MapUtils.isNotEmpty(partialMatchMap)) {
            CriteriaUtils.addPartialMatchClause(partialMatchMap, buildConditions, params, null, PARTIAL_MATCH_COLUMNS, Collections.emptySet(), EMPTY);
        }
        return buildConditions;
    }

    private List<String> createWhereClauseAndUpdateParams(Map<String, Object> params, BullseyeFileFilter filter,
                                                          String tableAlias, String prefix) {
        List<String> fileConditions = new LinkedList<>();
        tableAlias = StringUtils.isEmpty(tableAlias) ? "" : tableAlias + ".";
        insertList(fileConditions, params, tableAlias + "name", prefix + "names", filter.getNames());
        insertLongRange(fileConditions, params, tableAlias + "functions_covered", prefix + "functions_covered",
                filter.getFunctionsCovered());
        insertLongRange(fileConditions, params, tableAlias + "total_functions", prefix + "total_functions",
                filter.getTotalFunctions());
        insertLongRange(fileConditions, params, tableAlias + "decisions_covered", prefix + "decisions_covered",
                filter.getDecisionsCovered());
        insertLongRange(fileConditions, params, tableAlias + "total_decisions", prefix + "total_decisions",
                filter.getTotalDecisions());
        insertLongRange(fileConditions, params, tableAlias + "conditions_covered", prefix + "conditions_covered",
                filter.getConditionsCovered());
        insertLongRange(fileConditions, params, tableAlias + "total_conditions", prefix + "total_conditions",
                filter.getTotalConditions());
        insertDateRange(fileConditions, params, tableAlias + "modification_time", prefix + "modification_time_range",
                filter.getModificationTimeRange());
        return fileConditions;
    }

    private <T> void insertList(List<String> conditions, Map<String, Object> params, String columnName, String key,
                                List<T> value) {
        if (CollectionUtils.isNotEmpty(value)) {
            conditions.add(columnName + " IN (:" + key + ")");
            params.put(key, value);
        }
    }

    // value in the Map "value" is of type String and contains a Long. It is used to covert to Long and then put
    // inside params for where clause
    private void insertLongRange(List<String> conditions, Map<String, Object> params, String columnName,
                                 String key, Map<String, String> value) {
        if (MapUtils.isNotEmpty(value)) {
            if (value.containsKey("$gt") && value.get("$gt") != null) {
                conditions.add(columnName + " > :gt_" + key);
                params.put("gt_" + key, NumberUtils.toLong(value.get("$gt")));
            }
            if (value.containsKey("$lt") && value.get("$lt") != null) {
                conditions.add(columnName + " < :lt_" + key);
                params.put("lt_" + key, NumberUtils.toLong(value.get("$lt")));
            }
        }
    }

    // value in the Map "value" is of type String and contains a Long. It is used to covert to timestamp and then put
    // inside params for where clause
    private void insertDateRange(List<String> conditions, Map<String, Object> params, String columnName,
                                 String key, Map<String, String> value) {
        if (MapUtils.isNotEmpty(value)) {
            if (value.containsKey("$gt") && value.get("$gt") != null) {
                conditions.add(columnName + " > :gt_" + key);
                params.put("gt_" + key, getTimestamp(NumberUtils.toLong(value.get("$gt"))));
            }
            if (value.containsKey("$lt") && value.get("$lt") != null) {
                conditions.add(columnName + " < :lt_" + key);
                params.put("lt_" + key, getTimestamp(NumberUtils.toLong(value.get("$lt"))));
            }
        }
    }

    private void setPagingParams(Map<String, Object> params, Integer pageNumber, Integer pageSize) {
        params.put("offset", pageNumber * pageSize);
        params.put("limit", pageSize);
    }

    private String getWhereClause(List<String> conditions) {
        if (!conditions.isEmpty())
            return " WHERE " + String.join(" AND ", conditions);
        return EMPTY;
    }

    @Override
    public Boolean update(String company, BullseyeBuild t) throws SQLException {
        return null;
    }

    @Override
    public Optional<BullseyeBuild> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    @Override
    public DbListResponse<BullseyeBuild> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return null;
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        return null;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> ddlStmts = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + "." + BULLSEYE_PROJECTS +
                        " (id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " cicd_job_run_id UUID NOT NULL REFERENCES " + company + "." + CICD_JOB_RUNS + "(id) ON DELETE CASCADE," +
                        " project_id VARCHAR NOT NULL, " +
                        " project VARCHAR NOT NULL, " +
                        " built_at TIMESTAMP WITH TIME ZONE, " +
                        " name VARCHAR, " +
                        " directory VARCHAR, " +
                        " functions_covered INTEGER, " +
                        " total_functions INTEGER, " +
                        " decisions_covered INTEGER, " +
                        " total_decisions INTEGER, " +
                        " conditions_covered INTEGER, " +
                        " total_conditions INTEGER, " +
                        " file_hash VARCHAR(64), " +
                        " UNIQUE(cicd_job_run_id, project_id))",
                "CREATE INDEX IF NOT EXISTS bullseye_projects__file_hash_idx ON " + company + ".bullseye_projects (file_hash)",

                "CREATE TABLE IF NOT EXISTS " + company + "." + BULLSEYE_SOURCE_FILES +
                        " (id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " name VARCHAR, " +
                        " project_id UUID NOT NULL REFERENCES " + company + "." + BULLSEYE_PROJECTS + "(id) ON DELETE CASCADE, " +
                        " modification_time TIMESTAMP WITH TIME ZONE, " +
                        " functions_covered INTEGER, " +
                        " total_functions INTEGER, " +
                        " decisions_covered INTEGER, " +
                        " total_decisions INTEGER, " +
                        " conditions_covered INTEGER, " +
                        " total_conditions INTEGER, " +
                        " UNIQUE(project_id, name))"
        );
        ddlStmts.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

    private Timestamp getTimestamp(Date date) {
        return date != null ? Timestamp.from(date.toInstant()) : null;
    }

    private Timestamp getTimestamp(Long date) {
        return new Timestamp(date);
    }
}
