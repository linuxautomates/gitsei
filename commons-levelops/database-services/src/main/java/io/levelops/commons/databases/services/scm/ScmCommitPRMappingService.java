package io.levelops.commons.databases.services.scm;

import com.google.common.base.Stopwatch;
import io.levelops.commons.databases.converters.scm.ScmCommitPartialConverters;
import io.levelops.commons.databases.converters.scm.ScmPRPartialConverters;
import io.levelops.commons.databases.models.database.scm.DbScmCommitPRMapping;
import io.levelops.commons.databases.services.scm.ScmCommitPullRequestMappingDBService;
import io.levelops.commons.databases.models.database.scm.ScmCommitPartial;
import io.levelops.commons.databases.models.database.scm.ScmPRPartial;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmAggService.COMMITS_TABLE;
import static io.levelops.commons.databases.services.ScmAggService.PRS_TABLE;

@Log4j2
@Service
public class ScmCommitPRMappingService {
    private final NamedParameterJdbcTemplate template;
    private final ScmCommitPullRequestMappingDBService scmCommitPullRequestMappingDBService;
    private final Integer dbPageSize;

    @Autowired
    public ScmCommitPRMappingService(DataSource dataSource, ScmCommitPullRequestMappingDBService scmCommitPullRequestMappingDBService, @Value("${SCM_COMMIT_PR_MAPPING_DB_PAGE_SIZE:10000}") Integer dbPageSize) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.scmCommitPullRequestMappingDBService = scmCommitPullRequestMappingDBService;
        this.dbPageSize = dbPageSize;
    }

    private List<ScmPRPartial> readPRsSinglePage(final String company, Integer pageNumber, Integer pageSize, Instant createdAtAfter) {

        String selectSqlBase = "Select id, merge_sha, commit_shas from " + company + "." + PRS_TABLE;

        List<String> criterias = new ArrayList<>();
        HashMap<String, Object> params = new HashMap<>();
        if(createdAtAfter != null) {
            criterias.add("created_at > (:created_at_after)");
            params.put("created_at_after", createdAtAfter.getEpochSecond());
        }

        String criteria = "";
        if(CollectionUtils.isNotEmpty(criterias)) {
            criteria = " WHERE " + String.join(" AND ", criterias);
        }
        String orderBy = " ORDER BY created_at DESC ";

        String selectSql = selectSqlBase + criteria + orderBy + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);

        log.info("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<ScmPRPartial> results = template.query(selectSql, params, ScmPRPartialConverters.rowMapper());
        return results;
    }

    private Map<String, Set<UUID>> parsePRs(final List<ScmPRPartial> prsList, AtomicInteger prsRead) {
        if(CollectionUtils.isEmpty(prsList)) {
            return Collections.emptyMap();
        }
        prsRead.addAndGet(prsList.size());
        Map<String, Set<UUID>> commitsShasToPRIds = new HashMap<>();
        prsList.stream()
                .forEach(p -> {
                    //Add pr.merge_sha to pr.id mapping
                    if(StringUtils.isNotBlank(p.getMergeSha())) {
                        commitsShasToPRIds.computeIfAbsent(p.getMergeSha(), k -> new HashSet<>()).add(p.getId());
                    }
                    //Add pr.commit_shas to pr.id mapping
                    CollectionUtils.emptyIfNull(p.getCommitShas()).stream()
                            .filter(StringUtils::isNotBlank)
                            .forEach(cs -> commitsShasToPRIds.computeIfAbsent(cs, k -> new HashSet<>()).add(p.getId()));
                });
        return commitsShasToPRIds;
    }

    private List<ScmCommitPartial> fetchCommitsByCommitSha(final String company, List<String> commitShas) {
        String selectSqlBase = "SELECT id, commit_sha FROM " + company + "." + COMMITS_TABLE + " WHERE commit_sha IN (:commit_shas)";
        Map<String, Object> params = Map.of("commit_shas", commitShas);
        log.info("sql = " + selectSqlBase); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<ScmCommitPartial> results = template.query(selectSqlBase, params, ScmCommitPartialConverters.rowMapper());
        return results;
    }

    private Map<String, Set<UUID>> fetchCommitShaToCommitIDMapping(final String company, Set<String> commitShas) {
        if(CollectionUtils.isEmpty(commitShas)) {
            return Collections.emptyMap();
        }
        List<ScmCommitPartial> commits = new ArrayList<>();
        for (List<String> currentBatch : ListUtils.partition(commitShas.stream().collect(Collectors.toList()), 1000)) {
            commits.addAll(fetchCommitsByCommitSha(company, currentBatch));
        }
        if(CollectionUtils.isEmpty(commits)) {
            return Collections.emptyMap();
        }
        Map<String, Set<UUID>> commitShaToCommitIds = new HashMap<>();
        CollectionUtils.emptyIfNull(commits).stream()
                .forEach(cp -> {
                    if(StringUtils.isNotBlank(cp.getCommitSha())) {
                        commitShaToCommitIds.computeIfAbsent(cp.getCommitSha(), k -> new HashSet<>()).add(cp.getId());
                    }
                });
        return commitShaToCommitIds;
    }


    private List<DbScmCommitPRMapping> correlateCommitAndPR(Map<String, Set<UUID>> commitsShasToPRIds, Map<String, Set<UUID>> commitShaToCommitIds) {
        List<DbScmCommitPRMapping> mappings = new ArrayList<>();
        for(Map.Entry<String, Set<UUID>> e : commitsShasToPRIds.entrySet()) {
            String commitSha = e.getKey();
            Set<UUID> prsIds = e.getValue();
            Set<UUID> commitIds = commitShaToCommitIds.get(commitSha);
            for(UUID prId : CollectionUtils.emptyIfNull(prsIds)) {
                for(UUID commitId : CollectionUtils.emptyIfNull(commitIds)) {
                    mappings.add(DbScmCommitPRMapping.builder().scmCommitId(commitId).scmPullrequestId(prId).build());
                }
            }
        }
        return mappings;
    }

    private List<DbScmCommitPRMapping> buildScmCommitPRMapping(final String company, List<ScmPRPartial> prsList, AtomicInteger prsRead) {
        if(CollectionUtils.isEmpty(prsList)) {
            return Collections.EMPTY_LIST;
        }
        Map<String, Set<UUID>> commitsShasToPRIds = parsePRs(prsList, prsRead);
        if(MapUtils.isEmpty(commitsShasToPRIds)) {
            return Collections.EMPTY_LIST;
        }
        Map<String, Set<UUID>> commitShaToCommitIds = fetchCommitShaToCommitIDMapping(company, commitsShasToPRIds.keySet());
        return correlateCommitAndPR(commitsShasToPRIds, commitShaToCommitIds);
    }

    public boolean persistScmCommitPRMapping(final String company, Instant createdAtAfter) {
        Stopwatch st = Stopwatch.createStarted();
        AtomicInteger prsRead = new AtomicInteger();
        AtomicInteger commitPRMappingsCount = new AtomicInteger();
        boolean success = true;

        try {
            boolean fetchMore = true;
            int pageNumber = 0;
            while (fetchMore) {
                final List<ScmPRPartial> prsList = readPRsSinglePage(company, pageNumber, dbPageSize, createdAtAfter);
                List<DbScmCommitPRMapping> scmCommitPRMappings = buildScmCommitPRMapping(company, prsList, prsRead);
                scmCommitPullRequestMappingDBService.batchInsert(company, scmCommitPRMappings);
                commitPRMappingsCount.addAndGet(CollectionUtils.size(scmCommitPRMappings));

                fetchMore = (CollectionUtils.size(prsList) > 0);
                pageNumber++;
            }
            return success;
        } catch (Exception e) {
            log.error("Error!", e);
            success = false;
            return success;
        } finally {
            log.info("Scm Commit PR Mappings success = {}, prsRead = {}, mappingsCount = {}, full_time = {}", success, prsRead.get(), commitPRMappingsCount.get(), st.elapsed(TimeUnit.MILLISECONDS));
        }
    }

}
