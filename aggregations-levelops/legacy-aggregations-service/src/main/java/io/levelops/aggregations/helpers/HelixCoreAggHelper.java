package io.levelops.aggregations.helpers;

import com.google.common.base.MoreObjects;
import com.perforce.p4java.core.file.FileAction;
import io.levelops.aggregations.controllers.AckAggregationsController;
import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.aggregations_shared.models.IntegrationWhitelistEntry;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmFileCommit;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.models.ChangeVolumeStats;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import io.levelops.integrations.helixcore.models.HelixCoreChangeListUtils;
import io.levelops.integrations.helixcore.models.HelixCoreFile;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class HelixCoreAggHelper {

    private static final Long COMMIT_INGESTED_AT_CONTROL_VALUE = 631152000l;

    public static final String CHANGELIST_DATA_TYPE = "changelists";
    public static final String NEWLY_ADDED_FILE_DIFFERENCES = "\n";
    public static final int TOTAL_LINES_OF_CHANGE = 5000;
    private static final Set<String> RELEVANT_TENANT_IDS = Set.of("broadcom");
    private final JobDtoParser jobDtoParser;
    private final ScmAggService scmAggService;
    private final IntegrationTrackingService trackingService;
    private final List<IntegrationWhitelistEntry> scmCommitsInsertV2integrationIdWhitelist;

    @Autowired
    public HelixCoreAggHelper(JobDtoParser jobDtoParser,
                              ScmAggService scmAggService,
                              IntegrationTrackingService trackingService,
                              @Qualifier("scmCommitsInsertV2integrationIdWhitelist") List<IntegrationWhitelistEntry> scmCommitsInsertV2integrationIdWhitelist) {
        this.jobDtoParser = jobDtoParser;
        this.scmAggService = scmAggService;
        this.trackingService = trackingService;
        this.scmCommitsInsertV2integrationIdWhitelist = scmCommitsInsertV2integrationIdWhitelist;
    }
    void processHelixChangeList(final String customer, final String integrationId, final List<IntegrationConfig.RepoConfigEntry> configEntries,
                                final HelixCoreChangeList changeList, Map<String, Object> integrationMetadata, boolean useScmCommitInsertV2) {
        if(changeList.getStatus() != HelixCoreChangeList.ChangelistStatus.SUBMITTED) {
            log.info("Skipping Helix CL Customer {}, integrationId {}, clId {}, status {} : Changelist is not submitted", customer, integrationId, changeList.getId(), changeList.getStatus());
            return;
        }
        List<DbScmFile> dbScmFiles = DbScmFile.fromHelixCoreChangeList(changeList, integrationId, configEntries);
        List<String> fileExtns = dbScmFiles.stream().map(DbScmFile::getFiletype).distinct().collect(Collectors.toList());
        Set<String> repoIds = dbScmFiles.stream().map(DbScmFile::getRepoId).collect(Collectors.toSet());
        DbScmCommit helixCoreCommit = DbScmCommit.fromHelixCoreChangeList(changeList, repoIds, integrationId);
        DbScmCommit.DbScmCommitBuilder dbScmCommitBuilder = helixCoreCommit.toBuilder();
        dbScmCommitBuilder = dbScmCommitBuilder.fileTypes(fileExtns);
        boolean nullifyIntegCommitsLoc = Boolean.TRUE.equals(integrationMetadata.get("nullify_integ_commits_loc"));
        if (nullifyIntegCommitsLoc) {
            dbScmCommitBuilder = dbScmCommitBuilder
                    .additions(0)
                    .deletions(0)
                    .changes(0);
        }
        DbScmCommit finalDbScmCommitWithFileTypes = dbScmCommitBuilder.build();
        if ((CollectionUtils.isNotEmpty(repoIds)) && (dbScmFiles.size() > 0)) {
            Optional<DbScmCommit> opt = scmAggService.getCommit(customer, finalDbScmCommitWithFileTypes.getCommitSha(),
                    finalDbScmCommitWithFileTypes.getRepoIds(), integrationId);
            if (opt.isEmpty()) {
                try {
                    boolean skipIntegCommits = Boolean.TRUE.equals(integrationMetadata.get("skip_integ_commits"));
                    if (skipIntegCommits && changeList.isIntegrationCommit()) {
                        log.info("Process Helix CL Customer {}, integrationId {}, clId {} : Integration Changelist ignored",
                                customer, integrationId, changeList.getId());
                    } else {
                        if (ScmAggUtils.isRelevant(customer, RELEVANT_TENANT_IDS)) {
                            if (ScmAggUtils.isChangeVolumeLessThanXLines(finalDbScmCommitWithFileTypes, TOTAL_LINES_OF_CHANGE)) {
                                if(useScmCommitInsertV2) {
                                    scmAggService.insertV2(customer, finalDbScmCommitWithFileTypes, dbScmFiles);
                                } else {
                                    scmAggService.insert(customer, finalDbScmCommitWithFileTypes, dbScmFiles);
                                }
                            } else {
                                log.info("Commit not inserted as lines of change greater than {} for Customer {}, integrationId {}," +
                                        " commitSha {} commit ", TOTAL_LINES_OF_CHANGE, customer, integrationId, finalDbScmCommitWithFileTypes.getCommitSha());
                            }
                        } else {
                            if (useScmCommitInsertV2) {
                                scmAggService.insertCommit(customer, finalDbScmCommitWithFileTypes);
                                scmAggService.insertFilesV2(customer, integrationId, finalDbScmCommitWithFileTypes.getCommitSha(), dbScmFiles);
                            } else {
                                scmAggService.insertCommit(customer, finalDbScmCommitWithFileTypes);
                                dbScmFiles.forEach(dbScmFile -> scmAggService.insertFile(customer, dbScmFile));
                            }

                        }
                        log.info("Process Helix CL Customer {}, integrationId {}, clId {} commit saved to db", customer, integrationId, changeList.getId());
                    }
                } catch (SQLException e) {
                    log.error("Failed to process Helix CL Customer {}, integrationId {}, clId {} commit saved to db",
                            customer, integrationId, changeList.getId(), e);
                }
            } else {
                fixScmCommits(customer, integrationId, opt.get(), finalDbScmCommitWithFileTypes, dbScmFiles);
            }
            // endregion
        } else {
            log.debug("HelixCoreAggHelper:insertHelixCoreCommits - Repo ids or files list is empty will not insert change list with id : "
                    + changeList.getId() + ", and integration id : " + integrationId);
        }
    }

    private void fixScmCommits(final String customer, final String integrationId, final DbScmCommit existingCommit, final DbScmCommit updatedCommit, final List<DbScmFile> updatedScmFiles) {
        //SCM Commit's ingested_at is control value we will:
        // 1) Update the commits stats
        // 2) Update the commit files & files
        UUID commitId = UUID.fromString(existingCommit.getId());
        //ToDo: Delete Later
        if("42052457".equals(existingCommit.getCommitSha())) {
            log.info("Customer {}, commit sha {}, commit id {}, ingestedAt = {}!", customer, existingCommit.getCommitSha(), commitId, existingCommit.getIngestedAt());
        }
        if(! COMMIT_INGESTED_AT_CONTROL_VALUE.equals(existingCommit.getIngestedAt())) {
            return;
        }

        log.info("Customer {}, commit id {}, ingestedAt = {} is COMMIT_INGESTED_AT_CONTROL_VALUE, will fix scm commit!", customer, commitId, existingCommit.getIngestedAt());
        try {
            Boolean updateCommitChangesStats = scmAggService.updateCommitChangeVolumeStats(customer, commitId, updatedCommit.getAdditions(), updatedCommit.getDeletions(), updatedCommit.getChanges());
            log.info("Customer {}, commit id {}, updateCommitChangesStats = {}", customer, commitId, updateCommitChangesStats);

            for(DbScmFile scmFile : CollectionUtils.emptyIfNull(updatedScmFiles)) {
                Optional<DbScmFile> existingScmFile = scmAggService.getFile(customer, scmFile.getFilename(), scmFile.getRepoId(), scmFile.getProject(), integrationId);
                if (existingScmFile.isEmpty()) {
                    scmAggService.insertFile(customer, scmFile);
                } else {
                    updateFileCommitStats(customer, existingScmFile.get(), updatedCommit, scmFile);
                }
            }
            Boolean updateCommitIngestedAt = scmAggService.updateCommitIngestedAt(customer, commitId, updatedCommit.getIngestedAt());
            log.info("Customer {}, commit id {}, updateCommitIngestedAt = {}", customer, commitId, updateCommitIngestedAt);
        } catch (SQLException e) {
            log.error("Error fixing scm commit, customer {}, commit id {}", customer, commitId);
        }
    }

    private void updateFileCommitStats(String customer, DbScmFile existingDbScmFile, DbScmCommit updatedCommit, DbScmFile updatedFile ) {
        try {
            Optional<DbScmFileCommit> existingFileCommit = scmAggService.getFileCommit(customer, updatedCommit.getCommitSha(), existingDbScmFile.getId());
            Optional<DbScmFileCommit> optFileCommit = updatedFile.getFileCommits().stream().findFirst();
            if (existingFileCommit.isPresent() && optFileCommit.isPresent()) {
                DbScmFileCommit updatedFileCommit = optFileCommit.get();
                scmAggService.updateFileCommitStats(customer, UUID.fromString(existingFileCommit.get().getId()),
                        updatedFileCommit.getAddition(), updatedFileCommit.getDeletion(), updatedFileCommit.getChange());
            }
        } catch (SQLException e) {
            log.error(String.format("Failed to update file stats for company %s, commit_sha %s, file_id %s",
                    customer, updatedCommit.getCommitSha(), existingDbScmFile.getId()), e);
        }
    }

    public boolean insertHelixCoreCommits(String customer,
                                          String integrationId,
                                          List<IntegrationConfig.RepoConfigEntry> configEntries,
                                          MultipleTriggerResults results,
                                          Date ingestedAt,
                                          Map<String, Object> integrationMetadata) {
        boolean useScmCommitInsertV2 = ScmAggUtils.useScmCommitsInsertV2(scmCommitsInsertV2integrationIdWhitelist, customer, integrationId);
        log.info("company {}, integrationId {}, useScmCommitInsertV2 {}", customer, integrationId, useScmCommitInsertV2);
        boolean result = jobDtoParser.applyToResults(customer,
                CHANGELIST_DATA_TYPE,
                HelixCoreChangeList.class,
                results.getTriggerResults().get(0),
                changeList -> processHelixChangeList(customer, integrationId, configEntries, changeList, integrationMetadata, useScmCommitInsertV2),
                List.of());
        if (result)
            trackingService.upsert(customer,
                    IntegrationTracker.builder()
                            .integrationId(integrationId)
                            .latestIngestedAt(io.levelops.commons.dates.DateUtils.truncate(ingestedAt, Calendar.DATE))
                            .build());
        return result;
    }

    static HelixCoreChangeList getHelixCoreChangeList(HelixCoreChangeList changeList, boolean nullifyIntegCommitsLoc) {
        final String TOTAL = "_total_";
        List<HelixCoreFile> files = changeList.getFiles();
        Map<String, ChangeVolumeStats> diffStatsMap = Map.of();
        final HelixCoreChangeList.HelixCoreChangeListBuilder builder = changeList.toBuilder();
        try {
            String differences = changeList.getDifferences();
            boolean isNewlyAddedFile = differences.equalsIgnoreCase(NEWLY_ADDED_FILE_DIFFERENCES);
            if (isNewlyAddedFile) {
                builder.additions(changeList.getAdditions())
                        .deletions(changeList.getDeletions())
                        .changes(changeList.getChanges());
            } else if (nullifyIntegCommitsLoc && changeList.isIntegrationCommit()) {
                builder.additions(0)
                        .deletions(0)
                        .changes(0);
            } else {
                diffStatsMap = HelixCoreChangeListUtils.getDiffFromHelixCoreFiles(differences, files);
                int additions = diffStatsMap.get(TOTAL).getAdditions();
                int deletions = diffStatsMap.get(TOTAL).getDeletions();
                int changes = diffStatsMap.get(TOTAL).getChanges();
                builder.additions(additions)
                        .deletions(deletions)
                        .changes(changes);
            }
            Map<String, ChangeVolumeStats> finalDiffStatsMap = diffStatsMap;
            builder.filesCount(files.size())
                    .files(files.stream().map(file -> fromHelixCoreFile(file, finalDiffStatsMap, changeList, isNewlyAddedFile))
                            .collect(Collectors.toList()));
        } catch (Exception e) {
            builder.parseError(e.getMessage());
            log.error("Failed to get diff for changelist " + changeList.getId() + " " + e.getMessage(), e);
        }
        return builder.build();
    }

    public static HelixCoreFile fromHelixCoreFile(HelixCoreFile helixCoreFile, Map<String, ChangeVolumeStats> diffStatsMap,
                                                  HelixCoreChangeList changeList, boolean isNewlyAddedFile) {
        String fullyQualifiedFileName = HelixCoreChangeListUtils.getFullyQualifiedFileName(helixCoreFile);
        String fileName = HelixCoreChangeListUtils.getFileName(fullyQualifiedFileName);
        Optional<Integer> fileAddition = changeList.getFiles().stream().filter(file -> file.getName().equalsIgnoreCase(fileName))
                .map(HelixCoreFile::getAdditions).findFirst();
        Optional<Integer> fileDeletion = changeList.getFiles().stream().filter(file -> file.getName().equalsIgnoreCase(fileName))
                .map(HelixCoreFile::getDeletions).findFirst();
        int additions = isNewlyAddedFile ? fileAddition.orElse(0) :
                MoreObjects.firstNonNull(diffStatsMap.get(fullyQualifiedFileName).getAdditions(), 0);
        int deletions = isNewlyAddedFile ? fileDeletion.orElse(0)
                : MoreObjects.firstNonNull(diffStatsMap.get(fullyQualifiedFileName).getDeletions(), 0);
        if (helixCoreFile.getFileAction() != null && helixCoreFile.getFileAction().equalsIgnoreCase(FileAction.INTEGRATE.toString())) {
            additions = 0;
            deletions = 0;
        }
        return helixCoreFile.toBuilder()
                .additions(additions)
                .deletions(deletions)
                .changes(0)
                .build();
    }


    //endregion
}
