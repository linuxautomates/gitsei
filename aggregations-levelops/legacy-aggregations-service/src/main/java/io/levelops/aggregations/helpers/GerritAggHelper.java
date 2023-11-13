package io.levelops.aggregations.helpers;

import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.converters.gerrit.GerritCommitConverters;
import io.levelops.commons.databases.models.database.scm.converters.gerrit.GerritFileConverters;
import io.levelops.commons.databases.models.database.scm.converters.gerrit.GerritPullRequestConverters;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.integrations.gerrit.models.ChangeInfo;
import io.levelops.integrations.gerrit.models.ProjectInfo;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@SuppressWarnings("unused")
@Log4j2
@Service
public class GerritAggHelper {

    private static final String PULL_REQUEST_DATATYPE = "pull_requests";

    private static final String MERGED_STATE = "MERGED";
    public static final int TOTAL_LINES_OF_CHANGE = 5000;
    private static final Set<String> RELEVANT_TENANT_IDS = Set.of("broadcom");

    private final JobDtoParser jobDtoParser;
    private final ScmAggService scmAggService;

    @Autowired
    public GerritAggHelper(JobDtoParser jobDtoParser, ScmAggService scmAggService) {
        this.jobDtoParser = jobDtoParser;
        this.scmAggService = scmAggService;
    }

    public void processPullRequestsDataType(String customer,
                                            String integrationId,
                                            MultipleTriggerResults results) {
        jobDtoParser.applyToResults(customer,
                PULL_REQUEST_DATATYPE,
                ProjectInfo.class,
                results.getTriggerResults().get(0), //theres only one trigger result today.
                project -> project.getChanges().forEach(changeInfo -> {
                    insertGerritCommit(customer, integrationId, changeInfo);
                    insertGerritPr(customer, integrationId, changeInfo);
                }),
                List.of());
    }

    public void insertGerritCommit(String customer, String integrationId, ChangeInfo changeInfo) {
        Optional<DbScmCommit> parsedCommitOpt = GerritCommitConverters.parseCommit(integrationId, changeInfo);
        if (parsedCommitOpt.isEmpty()) {
            // ignoring commit, see GerritCommitConverters.parseCommit documentation
            return;
        }
        DbScmCommit parsedCommit = parsedCommitOpt.get();

        Optional<DbScmCommit> dbCommit = scmAggService.getCommit(customer, parsedCommit.getCommitSha(), changeInfo.getProject(), integrationId);
        if (dbCommit.isPresent()) {
            return;
        }

        List<DbScmFile> dbScmFiles = GerritFileConverters.parseCommitFiles(integrationId, changeInfo, parsedCommit.getCommitSha());

        if (ScmAggUtils.isRelevant(customer, RELEVANT_TENANT_IDS) && !ScmAggUtils.isChangeVolumeLessThanXLines(parsedCommit, TOTAL_LINES_OF_CHANGE)) {
            log.info("Commit not inserted as lines of change greater than {} for Customer {}, integrationId {}," +
                    " commitSha {} commit ", TOTAL_LINES_OF_CHANGE, customer, integrationId, parsedCommit.getCommitSha());
            return;
        }

        try {
            scmAggService.insert(customer, parsedCommit, dbScmFiles);
        } catch (SQLException e) {
            log.error("Failed to insert SCM commits and files for customer={}, integrationId={}, changeId={}", customer, integrationId, changeInfo.getId(), e);
        }
    }

    public void insertGerritPr(String customer, String integrationId, ChangeInfo changeInfo) {
        DbScmPullRequest pr = GerritPullRequestConverters.parsePullRequest(integrationId, changeInfo);
        try {
            Optional<DbScmPullRequest> dbPr = scmAggService.getPr(customer, pr.getNumber(), pr.getProject(), integrationId);
            if (dbPr.isEmpty() || dbPr.get().getPrUpdatedAt() < pr.getPrUpdatedAt()) {
                scmAggService.insert(customer, pr);
            }
        } catch (SQLException e) {
            log.error("Failed to insert Gerrit PR for customer={}, integrationId={}, changeId={}", customer, integrationId, changeInfo.getId(), e);
        }
    }
}
