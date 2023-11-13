package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsTestCase;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ListUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@Log4j2
public class TestRailsTestCaseDatabaseService  extends DatabaseService<DbTestRailsTestCase>{

    public static final String TESTRAILS_TEST_CASES = "testrails_test_cases";
    public static final String TESTRAILS_TEST_CASE_TEMP_TABLE = "testrails_testcase_temp_table_";
    private final NamedParameterJdbcTemplate template;
    private final ObjectMapper mapper;
    private static final Integer BATCH_SIZE = 100;

    @Autowired
    public TestRailsTestCaseDatabaseService(final ObjectMapper mapper, DataSource dataSource) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        this.mapper = mapper;
    }

    @Override
    public String insert(String company, DbTestRailsTestCase testCase) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void insertBatch(String company, List<DbTestRailsTestCase> testCases) {
        String insertTestCase = "INSERT INTO " + company + "." + TESTRAILS_TEST_CASES +
            " (case_id, integration_id, project_id, suite_id, milestone_id, title, type, priority, refs, created_by, created_on, updated_by, updated_on," +
            " estimate, estimate_forecast, custom_case_fields, created_at, updated_at)" +
            " VALUES (:case_id, :integration_id, :project_id, :suite_id, :milestone_id, :title, :type, :priority, :refs, :created_by, :created_on, :updated_by, :updated_on," +
            " :estimate, :estimate_forecast, :custom_case_fields::jsonb, now(), now())" +
            " ON CONFLICT (case_id, integration_id) DO UPDATE SET" +
            " project_id=EXCLUDED.project_id, suite_id=EXCLUDED.suite_id, milestone_id=EXCLUDED.milestone_id, title=EXCLUDED.title, type=EXCLUDED.type, priority=EXCLUDED.priority," +
            " refs=EXCLUDED.refs, updated_by=EXCLUDED.updated_by, updated_on=EXCLUDED.updated_on," +
            " estimate=EXCLUDED.estimate, estimate_forecast=EXCLUDED.estimate_forecast, custom_case_fields=EXCLUDED.custom_case_fields," +
            " updated_at=now() RETURNING id";

        List<MapSqlParameterSource> batchParams = new ArrayList<>();
        for (DbTestRailsTestCase testCase : testCases) {
            final int integrationId = NumberUtils.toInt(testCase.getIntegrationId());
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("case_id",testCase.getCaseId());
            params.addValue("integration_id",integrationId);
            params.addValue("project_id",testCase.getProjectId());
            params.addValue("suite_id",testCase.getSuiteId());
            params.addValue("milestone_id",testCase.getMilestoneId());
            params.addValue("title",testCase.getTitle());
            params.addValue("type",testCase.getType());
            params.addValue("priority",testCase.getPriority());
            params.addValue("refs",testCase.getRefs());
            params.addValue("created_by",testCase.getCreatedBy());
            params.addValue("created_on",Timestamp.from(testCase.getCreatedOn()));
            params.addValue("updated_by",testCase.getUpdatedBy());
            params.addValue("updated_on",Timestamp.from(testCase.getUpdatedOn()));
            params.addValue("estimate",testCase.getEstimate());
            params.addValue("estimate_forecast",testCase.getEstimateForecast());
            params.addValue("custom_case_fields", ParsingUtils.serialize(mapper, "custom case fields", testCase.getCustomCaseFields(), "{}"));
            batchParams.add(params);
        }
        template.batchUpdate(insertTestCase, batchParams.toArray(new MapSqlParameterSource[0]));
    }

    @Override
    public Boolean update(String company, DbTestRailsTestCase t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<DbTestRailsTestCase> get(String company, String param) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class);
    }

    @Override
    public DbListResponse<DbTestRailsTestCase> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return listByFilter(company, pageNumber, pageSize, null);
    }

    public DbListResponse<DbTestRailsTestCase> listByFilter(String company, Integer pageNumber, Integer pageSize, List<Integer> caseIds) throws SQLException {
        String selectSqlBase = "SELECT * FROM " + company + "." + TESTRAILS_TEST_CASES;
        String orderBy = " ORDER BY created_on DESC ";
        String whereClause = "";

        Map<String, Object> params = new HashMap<>();
        List<String> conditions = createWhereClauseAndUpdateParamsForValues(params, caseIds);
        if (CollectionUtils.isNotEmpty(conditions)) {
            whereClause = " WHERE " + String.join(" AND ", conditions);
        }
        String selectSql = selectSqlBase + whereClause + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSql = "SELECT COUNT(*) FROM (" +  selectSqlBase + ") AS counted";

        List<DbTestRailsTestCase> dbTestRailsTestCases = template.query(selectSql, params, (rs, rowNumber) -> {
            return DbTestRailsTestCase.builder()
                    .caseId(rs.getInt("case_id"))
                    .integrationId(rs.getString("integration_id"))
                    .projectId(rs.getInt("project_id"))
                    .suiteId(rs.getInt("suite_id"))
                    .milestoneId(rs.getInt("milestone_id"))
                    .title(rs.getString("title"))
                    .type(rs.getString("type"))
                    .priority(rs.getString("priority"))
                    .refs(rs.getString("refs"))
                    .createdBy(rs.getString("created_by"))
                    .createdOn(DateUtils.toInstant(rs.getTimestamp("created_on")))
                    .updatedBy(rs.getString("updated_by"))
                    .updatedOn(DateUtils.toInstant(rs.getTimestamp("updated_on")))
                    .estimate(rs.getLong("estimate"))
                    .estimateForecast(rs.getLong("estimate_forecast"))
                    .customCaseFields(MapUtils.emptyIfNull(ParsingUtils.parseJsonObject(mapper, "custom_case_fields", rs.getString("custom_case_fields"))))
                    .build();
        });
        Integer totCount = 0;
        if (dbTestRailsTestCases.size() > 0) {
            totCount = dbTestRailsTestCases.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (dbTestRailsTestCases.size() == pageSize) {
                totCount = template.queryForObject(countSql, Map.of(), Integer.class);
            }
        }
        return DbListResponse.of(dbTestRailsTestCases, totCount);
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    public void createTempTable(String company, String integrationId, String currentTime) {
        String createSql = "CREATE TABLE IF NOT EXISTS " + company + "." + (TESTRAILS_TEST_CASE_TEMP_TABLE + integrationId + "_" + currentTime) +
                " (id INTEGER NOT NULL)";
        template.getJdbcTemplate().execute(createSql);
        log.info("Successfully created temp table of testcase: " + company + "." + (TESTRAILS_TEST_CASE_TEMP_TABLE + integrationId + "_" + currentTime));
    }

    public void insertIntoTempTable(String company, String integrationId, String currentTime, List<Integer> ids) {
        String insertSql = "INSERT INTO " + company + "." + (TESTRAILS_TEST_CASE_TEMP_TABLE + integrationId + "_" + currentTime) + " VALUES(?)";
        template.getJdbcTemplate().execute(TransactionCallback.of(conn -> {
            try (PreparedStatement testCaseStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)){
                int count = 0;
                for (Integer id : ListUtils.emptyIfNull(ids)){
                    testCaseStmt.setObject(1, id);
                    testCaseStmt.addBatch();
                    count++;
                    if(count % BATCH_SIZE == 0){
                        testCaseStmt.executeBatch();
                        count = 0;
                    }
                }
                if(count % BATCH_SIZE != 0){
                    testCaseStmt.executeBatch();
                }
            }
            return true;
        }));
    }
    public void dropTempTable(String company, String integrationId, String currentTime) {
        String dropSql = "DROP TABLE IF EXISTS " + company + "." + (TESTRAILS_TEST_CASE_TEMP_TABLE + integrationId + "_" + currentTime);
        template.getJdbcTemplate().execute(dropSql);
        log.debug("Successfully dropped temp table of testcase: " + company + "." + (TESTRAILS_TEST_CASE_TEMP_TABLE + integrationId + "_" + currentTime));
    }
    public int deleteTestCaseRecords(String company, String integrationId, String currentTime) {
        String deleteSql = "DELETE FROM " + company + "." + TESTRAILS_TEST_CASES +
                " using " + company + "." + TESTRAILS_TEST_CASES + " as tr" +
                " left join " + company + "." + (TESTRAILS_TEST_CASE_TEMP_TABLE + integrationId + "_" + currentTime) +
                " as temp on temp.id = tr.case_id" +
                " WHERE tr.id = " + company + "." + TESTRAILS_TEST_CASES + "." + "id" +
                " AND tr.integration_id=:integration_id AND temp.id is null";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("integration_id", Integer.parseInt(integrationId));
        return template.update(deleteSql, params);
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        final List<String> ddlStmts = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + "." + TESTRAILS_TEST_CASES +
                        " (" +
                        " id UUID PRIMARY KEY DEFAULT uuid_generate_v4()," +
                        " integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE," +
                        " case_id INTEGER NOT NULL," +
                        " project_id INTEGER NOT NULL, " +
                        " suite_id INTEGER NOT NULL," +
                        " milestone_id INTEGER," +
                        " title VARCHAR NOT NULL," +
                        " type VARCHAR," +
                        " priority VARCHAR," +
                        " refs VARCHAR," +
                        " created_by VARCHAR," +
                        " created_on TIMESTAMP WITH TIME ZONE NOT NULL," +
                        " updated_by VARCHAR," +
                        " updated_on TIMESTAMP WITH TIME ZONE NOT NULL," +
                        " estimate INTEGER," +
                        " estimate_forecast INTEGER," +
                        " custom_case_fields JSONB," +
                        " created_at TIMESTAMP WITH TIME ZONE NOT NULL," +
                        " updated_at TIMESTAMP WITH TIME ZONE NOT NULL," +
                        " UNIQUE(case_id, integration_id)" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + TESTRAILS_TEST_CASES + "_integration_id_idx ON " + company + "." + TESTRAILS_TEST_CASES + "(integration_id)",
                "CREATE INDEX IF NOT EXISTS " + TESTRAILS_TEST_CASES + "_created_on_idx ON " + company + "." + TESTRAILS_TEST_CASES + "(created_on)",
                "CREATE INDEX IF NOT EXISTS " + TESTRAILS_TEST_CASES + "_custom_case_fields_idx ON " + company + "." + TESTRAILS_TEST_CASES + " USING GIN(custom_case_fields)",
                "CREATE INDEX IF NOT EXISTS " + TESTRAILS_TEST_CASES + "_case_id_idx on " + company + "." + TESTRAILS_TEST_CASES + "(case_id)",
                "CREATE INDEX IF NOT EXISTS " + TESTRAILS_TEST_CASES + "_priority_idx on " + company + "." + TESTRAILS_TEST_CASES + "(priority)",
                "CREATE INDEX IF NOT EXISTS " + TESTRAILS_TEST_CASES + "_type_idx on " + company + "." + TESTRAILS_TEST_CASES + "(type)"
        );
        ddlStmts.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

    protected List<String> createWhereClauseAndUpdateParamsForValues(Map<String, Object> params,
                                                                     List<Integer> caseIds) {
        List<String> conditions = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(caseIds)) {
            conditions.add("case_id IN (:caseIds)");
            params.put("caseIds", caseIds);
        }
        return conditions;
    }
}
