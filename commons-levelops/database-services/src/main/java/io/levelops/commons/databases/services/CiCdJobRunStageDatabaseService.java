package io.levelops.commons.databases.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.PathSegment;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.Strings;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.sql.DataSource;

@Log4j2
@Service
public class CiCdJobRunStageDatabaseService extends DatabaseService<JobRunStage> {
    public final static String TABLE_NAME = "cicd_job_run_stages";

    private final int DEFAULT_PAGE_SIZE = 50;
    private final NamedParameterJdbcTemplate template;
    private final Set<String> allowedFilters = Set.of("id", "cicd_job_run_id", "state", "result", "name", "child_job_runs", "job_ids", "start_time", "end_time", "stage_ids");
    private final Set<String> allowedSorting = Set.of("id", "cicd_job_run_id", "state", "result", "name", "start_time", "duration");

    private final static List<String> ddl = List.of(
        "CREATE TABLE IF NOT EXISTS {0}.{1}("
            + "id                UUID PRIMARY KEY DEFAULT uuid_generate_v4(),"
            + "cicd_job_run_id   UUID NOT NULL REFERENCES {0}.cicd_job_runs(id) ON DELETE CASCADE,"
            + "stage_id          VARCHAR(50) NOT NULL,"
            + "name              VARCHAR(50) NOT NULL,"
            + "description       VARCHAR(200) NOT NULL,"
            + "result            VARCHAR(30) NOT NULL,"
            + "state             VARCHAR(30) NOT NULL,"
            + "duration          BIGINT NOT NULL,"
            + "start_time        BIGINT NOT NULL,"
            + "logs              VARCHAR(300) NOT NULL,"
            + "url               VARCHAR(500) NOT NULL,"
            + "full_path         JSONB NOT NULL,"
            + "child_job_runs    UUID[],"
            + "CONSTRAINT        unq_stage UNIQUE(cicd_job_run_id, stage_id)"
        + ")",
        
        "CREATE INDEX IF NOT EXISTS {1}_cicd_job_run_id_idx ON {0}.{1}(cicd_job_run_id)",
            
        "CREATE INDEX IF NOT EXISTS {1}_state_idx ON {0}.{1}(state)"
    );

    private final String insert = "INSERT INTO {0}.{1}(id, cicd_job_run_id, stage_id, name, description, result, state, duration, start_time, logs, url, full_path, child_job_runs) "
        + "VALUES(:id, :run_id, :stage_id, :name, :description, :result, :state, :duration, :start_time, :logs, :url, :full_path::jsonb, :child_job_runs::uuid[]) "
        + "ON CONFLICT(cicd_job_run_id,stage_id) DO UPDATE SET (name, description, result, state, duration, start_time, logs, url, full_path, child_job_runs) = "
        + " (EXCLUDED.name,EXCLUDED.description,EXCLUDED.result,EXCLUDED.state,EXCLUDED.duration,EXCLUDED.start_time,EXCLUDED.logs, EXCLUDED.url, EXCLUDED.full_path, EXCLUDED.child_job_runs) "
        + " RETURNING id";
    private final String queryForId = "SELECT id FROM {0}.{1} WHERE cicd_job_run_id = :run_id AND stage_id = :stage_id";
    
    private final String delete = "DELETE FROM {0}.{1} WHERE id = :id::uuid";

    private final ObjectMapper mapper;

    public CiCdJobRunStageDatabaseService(DataSource dataSource, ObjectMapper mapper) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.mapper = mapper;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(CiCdJobRunsDatabaseService.class);
    }

    @Override
    public String insert(String company, JobRunStage stage) throws SQLException {
        var keyHolder = new GeneratedKeyHolder();
        var params = new MapSqlParameterSource()
            .addValue("id", stage.getId() != null ? stage.getId() : UUID.randomUUID())
            .addValue("run_id", stage.getCiCdJobRunId())
            .addValue("stage_id", StringUtils.truncate(stage.getStageId(), 50))
            .addValue("name", StringUtils.truncate(MoreObjects.firstNonNull(stage.getName(), "stage#" + stage.getStageId()), 50))
            .addValue("description", StringUtils.truncate(MoreObjects.firstNonNull(stage.getDescription(), ""), 200))
            .addValue("result", StringUtils.truncate(stage.getResult().toUpperCase(), 30))
            .addValue("state", StringUtils.truncate(stage.getState(), 30))
            .addValue("duration", stage.getDuration())
            .addValue("start_time", stage.getStartTime().toEpochMilli())
            .addValue("url", StringUtils.truncate(StringUtils.isNotBlank(stage.getUrl()) ? stage.getUrl() : "", 500))
            .addValue("logs", StringUtils.truncate(stage.getLogs(), 300));
        try {
            params
                .addValue("child_job_runs", "{" + String.join(",", stage.getChildJobRuns().stream().map(UUID::toString).collect(Collectors.toList())) + "}") // no check since it is a mandatory fild
                .addValue("full_path", mapper.writeValueAsString(stage.getFullPath()) ); // no check since it is a mandatory fild
		} catch (JsonProcessingException e) {
            throw new SQLException("Unable to convert the full path into a json object: " + stage.getFullPath(), e);
		}
        int count = this.template.update(
            MessageFormat.format(insert, company, TABLE_NAME),
            params,
            keyHolder,
            new String[]{"id"}
        );
        if (count == 0) {
            var id = template.queryForObject(
                MessageFormat.format(queryForId, company, TABLE_NAME),
                params,
                UUID.class
            );
            return id.toString();
        }
        return keyHolder.getKeys().get("id").toString();
    }

    @Override
    public Boolean update(String company, JobRunStage stage) throws SQLException {
        return null;
    }

    @Override
    public Optional<JobRunStage> get(String company, String id) throws SQLException {
        var results = list(company, 0, 1, QueryFilter.builder().strictMatch("id", UUID.fromString(id)).build(), "start_time", SortingOrder.DESC);
        if(results.getCount() < 1 || CollectionUtils.isEmpty(results.getRecords())) {
            return Optional.empty();
        }
        return Optional.of(results.getRecords().get(0));
    }

    public Optional<JobRunStage> get(String company, String id, boolean onlyFirstLevel) throws SQLException {
        var results = list(company, 0, 1, QueryFilter.builder().strictMatch("id", UUID.fromString(id)).build(), "start_time", SortingOrder.DESC);
        if(results.getCount() < 1 || CollectionUtils.isEmpty(results.getRecords())) {
            return Optional.empty();
        }
        return Optional.of(results.getRecords().get(0));
    }

    @Override
    public DbListResponse<JobRunStage> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return list(company, pageNumber, pageSize, null);
    }

    public DbListResponse<JobRunStage> list(String company, Integer pageNumber, Integer pageSize, final QueryFilter filters) throws SQLException {
        return list(company, pageNumber, pageSize, filters, "start_time", SortingOrder.DESC);
    }

    
    public DbListResponse<JobRunStage> list(
            final String company,
            final Integer pageNumber,
            Integer pageSize,
            final QueryFilter filters,
            final String sortBy,
            final SortingOrder sortOrder,
            final boolean onlyFirstLevel) throws SQLException {
        return list(company, pageNumber, pageSize, filters, Pair.of(Set.of(sortBy), sortOrder), onlyFirstLevel);
    }

    
    public DbListResponse<JobRunStage> list(
            final String company,
            final Integer pageNumber,
            Integer pageSize,
            final QueryFilter filters,
            final String sortBy,
            final SortingOrder sortOrder) throws SQLException {
        return list(company, pageNumber, pageSize, filters, Pair.of(Set.of(sortBy), sortOrder));
    }

    
    public DbListResponse<JobRunStage> list(
            final String company,
            final Integer pageNumber,
            Integer pageSize,
            final QueryFilter filters,
            final Pair<Set<String>, SortingOrder> sortingOrder) throws SQLException {
        return list(company, pageNumber, pageSize, filters, sortingOrder, true);
    }

    @SuppressWarnings("unchecked")
    public DbListResponse<JobRunStage> list(
            final String company,
            final Integer pageNumber,
            Integer pageSize,
            final QueryFilter filters,
            final Pair<Set<String>, SortingOrder> sortingOrder,
            final Boolean onlyFirstLevel) throws SQLException {
        var params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();
        populateParams(filters, conditions, params);
        String where = "";
        if (conditions.size() > 0) {
            where = "WHERE " + String.join(" AND ", conditions) + " ";
        }
        String join = "LEFT OUTER JOIN {0}.jenkins_rule_hits j ON j.stage_id = c.id ";
        if (filters != null && CollectionUtils.isNotEmpty((Collection<String>)filters.getStrictMatches().get("job_ids"))) {
            join += ", {0}.cicd_job_runs r ";
            where += " AND r.id = c.cicd_job_run_id ";
        }
        String dedupGrouping = "c.id, c.cicd_job_run_id, c.stage_id, c.name, c.description, c.result, c.state, c.duration, c.start_time, c.logs, c.url, c.child_job_runs, c.full_path ";
        String groupBy = "GROUP BY c.id, c.cicd_job_run_id, c.stage_id, j.context ";
        String baseStatement = "FROM {0}.{1} c " + join + where + groupBy;
        String sorting = "";
        if (sortingOrder != null && CollectionUtils.isNotEmpty(sortingOrder.getLeft())){
            sorting = MessageFormat.format(
                "ORDER BY {0} {1} ",
                String.join(",", sortingOrder.getLeft().stream()
                    .filter(allowedSorting::contains)
                    .collect(Collectors.toSet())),
                sortingOrder.getRight());
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }
        String limit = MessageFormat.format("LIMIT {0} OFFSET {1}", pageSize, pageNumber*pageSize) ;
        var select = MessageFormat.format("SELECT c.*, array_to_json(array_remove(array_agg(context), NULL)) context_agg " + baseStatement  + sorting + limit, company, TABLE_NAME);
        var countSelect = MessageFormat.format("SELECT count(*) FROM (SELECT c.*, json_agg(j.context) " + baseStatement + ") as m", company, TABLE_NAME);
        if (!onlyFirstLevel) {
            var recursiveWhere = "WHERE c.cicd_job_run_id = ANY(s.child_job_runs) ";
            var extraConditions = conditions.stream()
                    .filter(condition -> !condition.startsWith("c.cicd_job_run_id"))
                    .collect(Collectors.joining(" AND "));
            if (Strings.isNotBlank(extraConditions)) {
                recursiveWhere += "AND " + extraConditions;
            }
            var base = MessageFormat.format(
                  "WITH RECURSIVE stages AS ( \n"
                + "    SELECT c.*, j.context " + baseStatement + "\n"
                + "    UNION ALL \n"
                + "        SELECT c.*, j.context \n"
                + "        FROM stages s, {0}.{1} c \n"
                + "        " + join + "\n"
                + "        " + recursiveWhere + " \n"
                + "        " + groupBy + ", j.context\n"
                + ") \n", company, TABLE_NAME);
            var finalSelect = "SELECT " + dedupGrouping + ", array_to_json(array_remove(array_agg(context), NULL)) context_agg FROM stages c GROUP BY " + dedupGrouping;
            select = base + finalSelect + sorting + limit;
            countSelect = base + "SELECT count(*) FROM (" + finalSelect + ") AS c ";
        }
        log.debug("select: {}", select);
        List<JobRunStage> records = template.query(
            select,
            params,
            (rs, row) -> {
                log.debug("Child Job Run: {}", rs.getString("child_job_runs"));
                var lineRef = "";
                var contextJson = rs.getString("context_agg");
                if (Strings.isNotBlank(contextJson) && !"[]".equalsIgnoreCase(contextJson)) {
                    // var context = ParsingUtils.parseMap(mapper, "context", String.class, Object.class, rs.getString("context"));
                    var context = ParsingUtils.parseJsonList(mapper, "context_agg", contextJson); // TODO: remove and switch to the commented one once we return a list of links of all the steps that matched
                    if (CollectionUtils.isNotEmpty(context)) {
                        var line = context.get(0).get("line") != null ? String.valueOf(context.get(0).get("line")) : null;
                        var step = context.get(0).get("step") != null ? String.valueOf(context.get(0).get("step")) : null;
                        lineRef = Strings.isNotBlank(line) && Strings.isNotBlank(step)
                            ? String.format("#step-%s-log-%s", step, line)
                            : "";
                    }
                }

                return JobRunStage.builder()
                    .id((UUID)rs.getObject("id"))
                    .ciCdJobRunId((UUID)rs.getObject("cicd_job_run_id"))
                    .stageId(rs.getString("stage_id"))
                    .name(rs.getString("name"))
                    .description(rs.getString("description"))
                    .result(rs.getString("result"))
                    .state(rs.getString("state"))
                    .logs(rs.getString("logs"))
                    .duration(rs.getInt("duration"))
                    .startTime(Instant.ofEpochMilli(rs.getLong("start_time")))
                    .childJobRuns(ParsingUtils.parseSet("child_job_runs", UUID.class, rs.getArray("child_job_runs")))
                    .fullPath(ParsingUtils.parseSet(mapper, "full_path", PathSegment.class, rs.getString("full_path")))
                    .url(getFullUrl(company, (UUID)rs.getObject("cicd_job_run_id"), rs.getString("stage_id"), lineRef))
                    .build();
            });
        var totalCount = records.size();
        if (totalCount == pageSize) {
            totalCount = template.queryForObject(countSelect, params, Integer.class);
        }
        return DbListResponse.of(records, totalCount);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void populateParams(final QueryFilter filters, final List<String> conditions, final MapSqlParameterSource params) {
        if (filters == null || filters.getStrictMatches() == null || filters.getStrictMatches().isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry: filters.getStrictMatches().entrySet()) {
            if (!allowedFilters.contains(entry.getKey()) 
                || filters.getStrictMatches().get(entry.getKey()) == null
                || entry.getValue() == null
                || (entry.getValue() instanceof Collection && CollectionUtils.isEmpty((Collection) entry.getValue()))
                || (Strings.isBlank(entry.getValue().toString()))) {
                continue;
            }
            if ("child_job_runs".equals(entry.getKey())) {
                // value must be a collection
                conditions.add(String.format("c.child_job_runs @> '''{%s}'''::UUID[]", String.join(",", (Collection) entry.getValue()) ));
                continue;
            }
            if("start_time".equalsIgnoreCase(entry.getKey())) {
                conditions.add(MessageFormat.format("c.start_time >= :{0}", entry.getKey()));
                params.addValue(entry.getKey(), Long.valueOf(entry.getValue().toString()));
                continue;
            }
            else if("end_time".equalsIgnoreCase(entry.getKey())) {
                conditions.add(MessageFormat.format("c.start_time <= :{0}", entry.getKey()));
                params.addValue(entry.getKey(), Long.valueOf(entry.getValue().toString()));
                continue;
            }

            if (entry.getValue() instanceof Collection) {
                var collection = ((Collection<String>) entry.getValue())
                            .stream()
                            .filter(Strings::isNotBlank)
                            // .map(s -> "''" + s.toUpperCase() + "''")
                            .map(s -> s.toUpperCase())
                            .collect(Collectors.toSet());
                var field = "job_ids".equalsIgnoreCase(entry.getKey()) ? "r.cicd_job_id" : "stage_ids".equalsIgnoreCase(entry.getKey()) ? "c.id" : "c." + entry.getKey();
                var uuidArrayKeys = Set.of("stage_ids", "job_ids");
                var fieldType = uuidArrayKeys.contains(entry.getKey().toLowerCase()) ? "::uuid[]" : "";
                var tmp = MessageFormat.format("{0} = ANY({2}{1})", field, fieldType, "'''{" + String.join(",", collection) + "}'''");
                log.debug("filter: {}", tmp);
                conditions.add(tmp);
                continue;
            }
            if (entry.getValue() instanceof UUID) {
                conditions.add(MessageFormat.format("c.{0} = :{0}::uuid", entry.getKey()));
            }
            else {
                conditions.add(MessageFormat.format("c.{0} = :{0}", entry.getKey()));
            }
            params.addValue(entry.getKey(), entry.getValue().toString());
        }
    }

    public String getFullUrl(String company, UUID jobRunId, String stageId, String lineRef) {
        try {
            if (Strings.isBlank(company) || jobRunId == null) {
                return "";
            }
            // Get instance url and full name
            var query = "SELECT j.id, j.job_full_name, i.url, r.job_run_number "
                        + "FROM {0}.cicd_job_runs r, {0}.cicd_jobs j, {0}.cicd_instances i "
                        + "WHERE j.id = r.cicd_job_id AND j.cicd_instance_id = i.id AND r.id = ''{1}''::uuid "
                        + "GROUP BY j.id, i.url, r.job_run_number";
            final Set<String> urls = new HashSet<>(1);
            template.query(MessageFormat.format(query, company, jobRunId.toString()), (rs) -> {
                // get url
                urls.add(CiCdJobsDatabaseService.getFullUrl(company, rs.getString("url"), rs.getString("job_full_name"), rs.getInt("job_run_number"), stageId, lineRef));
            });
            return urls.iterator().next();
        }
        catch(Exception e){
            return "";
        }
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        var count = template.update(MessageFormat.format(delete, company, TABLE_NAME), Map.of("id", id));
        return count > 0;
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        ddl.stream().map(statement -> MessageFormat.format(statement, company, TABLE_NAME))
        .peek(statement -> log.debug("db statement: {}", statement))
        .forEach(template.getJdbcTemplate()::execute);

        return true;
    }
    
}