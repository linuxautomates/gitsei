package io.levelops.aggregations.helpers;

import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmTag;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerCommit;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerEnrichedProjectData;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerPullRequest;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerRepository;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerTag;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Log4j2
@Service
public class BitbucketServerAggHelper {

    private static final String PULL_REQUEST_DATATYPE = "pull_requests";
    private static final String COMMITS_DATATYPE = "commits";
    private static final String TAGS_DATATYPE = "tags";
    public static final int TOTAL_LINES_OF_CHANGE = 5000;
    private static final Set<String> RELEVANT_TENANT_IDS = Set.of("broadcom");

    private final JobDtoParser jobDtoParser;
    private final ScmAggService scmAggService;

    @Autowired
    public BitbucketServerAggHelper(JobDtoParser jobDtoParser, ScmAggService aggService) {
        this.jobDtoParser = jobDtoParser;
        this.scmAggService = aggService;
    }

    public boolean insertBitbucketServerCommits(String customer,
                                                String integrationId,
                                                MultipleTriggerResults results,
                                                Date currentTime) {
        Long truncatedDate = DateUtils.truncate(currentTime, Calendar.DATE);
        return jobDtoParser.applyToResults(customer, COMMITS_DATATYPE, BitbucketServerEnrichedProjectData.class,
                results.getTriggerResults().get(0),
                enrichedProjectData -> {
                    BitbucketServerRepository repository = enrichedProjectData.getRepository();
                    List<BitbucketServerCommit> commits = enrichedProjectData.getCommits();
                    for (BitbucketServerCommit commit : commits) {
                        processCommit(commit, customer, repository.getName(), integrationId, truncatedDate);
                    }
                },
                List.of());
    }

    private void processCommit(BitbucketServerCommit commit, String customer, String repoName, String integrationId, Long truncatedDate){
        DbScmCommit dbScmCommit = DbScmCommit.fromBitbucketServerCommit(commit,
                repoName, integrationId, truncatedDate);
        Long eventTime = TimeUnit.MILLISECONDS.toSeconds(commit.getCommitterTimestamp());
        log.debug("dbBitbucketServerCommit {}", dbScmCommit);
        if (scmAggService.getCommit(
                        customer, dbScmCommit.getCommitSha(), repoName, integrationId)
                .isEmpty()) {
            log.debug("dbBitbucketServerCommit does not exist in db");
            List<DbScmFile> dbScmFiles = DbScmFile.fromBitbucketServerCommit(commit, integrationId, eventTime);
            try {
                if (ScmAggUtils.isRelevant(customer, RELEVANT_TENANT_IDS)) {
                    if (ScmAggUtils.isChangeVolumeLessThanXLines(dbScmCommit, TOTAL_LINES_OF_CHANGE)) {
                        scmAggService.insert(customer, dbScmCommit, dbScmFiles);
                    } else {
                        log.info("Commit not inserted as lines of change greater than {} for Customer {}, integrationId {}," +
                                " commitSha {} commit ", TOTAL_LINES_OF_CHANGE, customer, integrationId, dbScmCommit.getCommitSha());
                    }
                } else {
                    scmAggService.insert(customer, dbScmCommit, dbScmFiles);
                }
            } catch (SQLException e) {
                log.error("Failed to insert SCM commits and files for customer={}, integrationId={}", customer, integrationId, e);
            }
        }
    }

    public boolean insertBitbucketServerTags(String customer,
                                             String integrationId,
                                             MultipleTriggerResults results) {
        return jobDtoParser.applyToResults(customer, TAGS_DATATYPE, BitbucketServerEnrichedProjectData.class,
                results.getTriggerResults().get(0),
                enrichedProjectData -> {
                    BitbucketServerRepository repository = enrichedProjectData.getRepository();
                    List<BitbucketServerTag> tags = enrichedProjectData.getTags();
                    for (BitbucketServerTag tag : tags) {
                        DbScmTag bitbucketServerTag = DbScmTag.fromBitbucketServerTag(tag, String.valueOf(repository.getScmId()), integrationId);
                        try {
                            scmAggService.insertTag(customer, bitbucketServerTag);
                        } catch (SQLException e) {
                            log.error("Failed to insert the tag for customer:{}, integrationId:{}, repo:{}, tag:{}", customer,
                                    integrationId, String.valueOf(repository.getScmId()), tag.getDisplayId());
                        }
                    }
                },
                List.of());
    }

    public boolean insertBitbucketServerPrs(String customer,
                                            String integrationId,
                                            MultipleTriggerResults results,
                                            Date currentTime) {
        Long truncatedDate = DateUtils.truncate(currentTime, Calendar.DATE);
        return jobDtoParser.applyToResults(customer, PULL_REQUEST_DATATYPE, BitbucketServerEnrichedProjectData.class,
                results.getTriggerResults().get(0),
                enrichedProjectData -> {
                    BitbucketServerRepository repository = enrichedProjectData.getRepository();
                    List<BitbucketServerPullRequest> pullRequests = enrichedProjectData.getPullRequests();
                    for (BitbucketServerPullRequest pullRequest : pullRequests) {
                        DbScmPullRequest dbScmPullRequest = DbScmPullRequest.fromBitbucketServerPullRequest(pullRequest,
                                repository.getName(), integrationId);
                        log.debug("dbBitbucketServerPullRequest {}", dbScmPullRequest);
                        try {
                            DbScmPullRequest existingPr = scmAggService.getPr(customer, dbScmPullRequest.getNumber(), repository.getName(), integrationId)
                                    .orElse(null);
                            if (existingPr == null || existingPr.getPrUpdatedAt() < dbScmPullRequest.getPrUpdatedAt()) {
                                scmAggService.insert(customer, dbScmPullRequest);
                            }
                        } catch (SQLException e) {
                            log.error("Unable to fetch PR from db for customer={}, integration={}, repository={}, prNumber={}",
                                    customer, integrationId, repository.getName(), dbScmPullRequest.getNumber(), e);
                        }
                        processPrCommits(pullRequest, customer, repository.getName(), integrationId, truncatedDate);
                    }
                },
                List.of());
    }

    public void processPrCommits(BitbucketServerPullRequest pullRequest, String customer, String repoName, String integrationId, Long truncatedDate) {
        List<BitbucketServerCommit> commits = pullRequest.getPrCommits();
        for (BitbucketServerCommit commit : commits) {
            processCommit(commit, customer, repoName, integrationId, truncatedDate);
        }
    }

}
