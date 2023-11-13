package io.levelops.aggregations.helpers;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmTag;
import io.levelops.commons.databases.models.database.scm.converters.bitbucket.BitbucketCommitConverters;
import io.levelops.commons.databases.models.database.scm.converters.bitbucket.BitbucketPullRequestConverters;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.integrations.bitbucket.models.BitbucketRepository;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Log4j2
@Service
@SuppressWarnings("unused")
public class BitbucketAggHelper {
    private static final String PULL_REQUEST_DATATYPE = "pull_requests";
    private static final String COMMITS_DATATYPE = "commits";
    private static final String TAGS_DATATYPE = "tags";
    public static final int TOTAL_LINES_OF_CHANGE = 5000;
    private static final Set<String> RELEVANT_TENANT_IDS = Set.of("broadcom");

    private final JobDtoParser jobDtoParser;
    private final ScmAggService scmAggService;
    private final IntegrationService integrationService;

    @Autowired
    public BitbucketAggHelper(JobDtoParser jobDtoParser, ScmAggService aggService, IntegrationService integrationService) {
        this.jobDtoParser = jobDtoParser;
        this.scmAggService = aggService;
        this.integrationService = integrationService;
    }

    public boolean insertBitbucketCommits(String customer,
                                          String integrationId,
                                          MultipleTriggerResults results) {
        return jobDtoParser.applyToResults(customer,
                COMMITS_DATATYPE,
                BitbucketRepository.class,
                results.getTriggerResults().get(0), //theres only one trigger result today.
                repository -> repository.getCommits()
                        .forEach(commit -> {
                            log.debug("repository = {}", repository);
                            Long eventTime = commit.getDate().getTime();
                            DbScmCommit dbScmCommit = BitbucketCommitConverters.fromBitbucketCommit(
                                    commit, repository.getFullName(), repository.getProject().getName(), integrationId, eventTime);
                            log.debug("bitBucketCommit {}", dbScmCommit);
                            Optional<DbScmCommit> optionalDbScmCommit = scmAggService.getCommit(customer, dbScmCommit.getCommitSha(), repository.getFullName(), integrationId);
                            if (optionalDbScmCommit.isEmpty()) {
                                log.debug("bitBucketCommit does not exist in db");
                                List<DbScmFile> dbScmFiles = DbScmFile.fromBitbucketCommit(
                                        commit, repository.getFullName(), integrationId, eventTime);
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
                        }),
                List.of()); //do delete of old data in the function?
    }

    public boolean insertBitbucketTags(String customer,
                                          String integrationId,
                                          MultipleTriggerResults results) {
        return jobDtoParser.applyToResults(customer,
                TAGS_DATATYPE,
                BitbucketRepository.class,
                results.getTriggerResults().get(0), //theres only one trigger result today.
                repository -> repository.getTags()
                        .forEach(tag -> {
                            DbScmTag gitTag = DbScmTag.fromBitbucketTag(tag, repository.getUuid(), integrationId);
                            try {
                                scmAggService.insertTag(customer, gitTag);
                            } catch (SQLException e) {
                                log.error("Failed to insert the tag for customer:{}, integrationId:{}, repo:{}, tag:{}", customer, integrationId, repository.getUuid(), tag.getName());
                            }
                        }),
                List.of());
    }

    /**
     * If for partial sha we are able to successfully get exactly one full sha use it. In all other conditions use partial sha
     * All cases:
     * If partial sha matches only one full sha use the full sha.
     * If partial sha does not match any full sha use partial sha.
     * If partial sha matches more than one full sha use partial sha.
     * If cache has ExecutionException use
     * @param commitPartialShaCache
     * @param partialSha
     * @return
     */
    private String getFullShaMatchingPartialSha(final LoadingCache<String, List<String>> commitPartialShaCache, final String partialSha) {
        if(StringUtils.isEmpty(partialSha)) {
            return partialSha;
        }
        try {
            List<String> fullShas = commitPartialShaCache.get(partialSha);
            if (CollectionUtils.isEmpty(fullShas) || (fullShas.size() != 1)) {
                return partialSha;
            }
            return fullShas.get(0);
        } catch (ExecutionException e) {
            log.error("ExecutionException fetching full sha for partial sha {}!", partialSha, e);
            return partialSha;
        }
    }
    public boolean insertBitbucketPrs(String customer,
                                      String integrationId,
                                      MultipleTriggerResults results) {
        LoadingCache<String, List<String>> commitPartialShaCache = CacheBuilder.from("maximumSize=10000")
                .build(CacheLoader.from(partialSha -> {
                    List<String> completeShas = scmAggService.findCommitShasMatchingPartialShas(customer, Integer.parseInt(integrationId), partialSha);
                    log.info("bitbucket db call partialSha = {}, completeShas = {}", partialSha, completeShas);
                    return completeShas;
                }));
        Integration it = integrationService.get(customer, integrationId).orElse(null);
        boolean result = jobDtoParser.applyToResults(customer,
                PULL_REQUEST_DATATYPE,
                BitbucketRepository.class,
                results.getTriggerResults().get(0), //theres only one trigger result today.
                repository -> repository.getPullRequests()
                        .forEach(pr -> {
                            String mergeCommitSha = getFullShaMatchingPartialSha(commitPartialShaCache, (pr.getMergeCommit() != null)? pr.getMergeCommit().getHash() : null);
                            DbScmPullRequest bitbucketPr = BitbucketPullRequestConverters.fromBitbucketPullRequest(
                                    pr, repository.getFullName(), repository.getProject().getName(), integrationId, mergeCommitSha,it);
                            try {
                                DbScmPullRequest tmpPr = scmAggService.getPr(
                                        customer, bitbucketPr.getNumber(), repository.getFullName(), integrationId)
                                        .orElse(null);
                                if (tmpPr == null || tmpPr.getPrUpdatedAt() < bitbucketPr.getPrUpdatedAt()) {
                                    scmAggService.insert(customer, bitbucketPr);
                                }
                            } catch (SQLException e) {
                                log.error("Unable to fetch PR from db for customer={}, integration={}, repository={}, prNumber={}",
                                        customer, integrationId, repository.getFullName(), bitbucketPr.getNumber(), e);
                            }
                        }),
                List.of());//do delete of old data in the function?
        updateOlderPRsWithPartialShas(commitPartialShaCache, customer, Integer.parseInt(integrationId));
        return result;
    }

    private void updateOlderPRsWithPartialShas(final LoadingCache<String, List<String>> commitPartialShaCache, final String company, final Integer integrationId) {
        Set<String> cannotBeFixed = new HashSet<>();
        List<String> partialShas = CollectionUtils.emptyIfNull(scmAggService.findPartialShasInBitbucketPRs(company, integrationId, 100)).stream().filter(s -> !cannotBeFixed.contains(s)).collect(Collectors.toList());
        while (CollectionUtils.isNotEmpty(partialShas)) {
            for(String partialSha : partialShas) {
                String fullSha = getFullShaMatchingPartialSha(commitPartialShaCache, partialSha);
                if((StringUtils.isBlank(fullSha)) || fullSha.equals(partialSha)) {
                    cannotBeFixed.add(partialSha);
                    continue;
                }
                int affectedRows = scmAggService.updatePartialShasInBitbucketPRs(company, integrationId, partialSha, fullSha);
            }
            partialShas = CollectionUtils.emptyIfNull(scmAggService.findPartialShasInBitbucketPRs(company, integrationId, 100)).stream().filter(s -> !cannotBeFixed.contains(s)).collect(Collectors.toList());
        }
    }
}
