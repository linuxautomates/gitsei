package io.levelops.aggregations.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.AckReplyConsumer;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.common.base.MoreObjects;
import com.google.common.hash.Hashing;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import io.levelops.aggregations.models.jenkins.JobRun;
import io.levelops.aggregations.models.jenkins.JobRunCompleteRequest;
import io.levelops.aggregations.models.jenkins.JobRunParam;
import io.levelops.aggregations.models.jenkins.Node;
import io.levelops.aggregations.models.jenkins.Step;
import io.levelops.aggregations.models.messages.JenkinsLogTriagingMessage;
import io.levelops.aggregations.models.messages.JenkinsLogTriagingMessage.JenkinsLogTriagingMessageBuilder;
import io.levelops.aggregations.models.messages.JenkinsPluginJobRunCompleteMessage;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.bullseye_converter_clients.BullseyeConverterClient;
import io.levelops.bullseye_converter_commons.models.ConversionRequest;
import io.levelops.bullseye_converter_commons.models.Result;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CiCdJobRunTest;
import io.levelops.commons.databases.models.database.bullseye.BullseyeBuild;
import io.levelops.commons.databases.models.database.bullseye.CodeCoverageReport;
import io.levelops.commons.databases.models.database.cicd.CiCdJobRunArtifact;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.JobRunStageStep;
import io.levelops.commons.databases.models.database.cicd.PathSegment;
import io.levelops.commons.databases.models.database.cicd.SegmentType;
import io.levelops.commons.databases.models.database.jenkins.JUnitTestReport;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.database.CiCDPreProcessTask;
import io.levelops.commons.databases.models.filters.BullseyeBuildFilter;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunArtifactsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageStepsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunTestDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.CiCdPreProcessTaskService;
import io.levelops.commons.databases.services.bullseye.BullseyeDatabaseService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.xml.DefaultXmlMapper;
import io.levelops.commons.zip.ZipService;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.plugins.services.JenkinsPluginJobRunCompleteStorageService;
import lombok.NonNull;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Log4j2
@Service
public class JenkinsMonitoringPluginJobRunCompleteController implements AckAggregationsController<JenkinsPluginJobRunCompleteMessage> {

    private static final String LEVELOPS_JUNIT_REPORTS_ZIP = "levelops_junit_reports.zip";
    private static final String LEVELOPS_JUNIT_REPORTS_DIR = "levelops_junit_reports";
    private static final String BULLSEYE_COVERAGE_REPORTS_ZIP = "levelops_code_coverage_xml.zip";
    private static final String BULLSEYE_COVERAGE_REPORTS_DIR = "levelops_code_coverage_xml";
    private static final String BULLSEYE_COVERAGE_COV_ZIP = "levelops_code_coverage.zip";
    private static final String BULLSEYE_COVERAGE_COV_DIR = "levelops_code_coverage";

    private final Integer DEFAULT_PAGE_NUMBER = 0;
    private final Integer DEFAULT_PAGE_SIZE = 300;
    private final String subscriptionName;
    private final JenkinsPluginJobRunCompleteStorageService jenkinsPluginJobRunCompleteStorageService;
    private final ObjectMapper objectMapper;
    private final CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private final CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private final CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private final CiCdJobRunStageDatabaseService jobRunStageDatabaseService;
    private final CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService;
    private final CiCdJobRunTestDatabaseService cicdJobRunTestDatabaseService;
    private final BullseyeDatabaseService bullseyeDatabaseService;
    private final CiCdJobRunArtifactsDatabaseService ciCdJobRunArtifactsDatabaseService;
    private final String bucketName;
    private final Storage storage;
    private final Publisher jenkinsLogTriagingPublisher;
    private final RedisConnectionFactory redisConnectionFactory;
    private final BullseyeConverterClient bullseyeConverterClient;
    private final UserIdentityService userIdentityService;
    private final CiCdPreProcessTaskService ciCdPreProcessTaskService;

    @Autowired
    public JenkinsMonitoringPluginJobRunCompleteController(DataSource dataSource,
                                                           @Value("${JENKINS_JOB_RUN_COMPLETE_SUB}") String subscriptionName,
                                                           JenkinsPluginJobRunCompleteStorageService jenkinsPluginJobRunCompleteStorageService,
                                                           @Qualifier("custom") ObjectMapper objectMapper, CiCdInstancesDatabaseService ciCdInstancesDatabaseService,
                                                           CiCdJobsDatabaseService ciCdJobsDatabaseService, CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService,
                                                           CiCdJobRunStageDatabaseService jobRunStageDatabaseService,
                                                           CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService,
                                                           CiCdJobRunTestDatabaseService cicdJobRunTestDatabaseService,
                                                           BullseyeDatabaseService bullseyeDatabaseService,
                                                           CiCdJobRunArtifactsDatabaseService ciCdJobRunArtifactsDatabaseService,
                                                           @Value("${CICD_JOB_RUN_STAGE_LOGS_BUCKET}") String bucketName, Storage storage,
                                                           final RedisConnectionFactory redisConnectionFactory,
                                                           @Value("${GOOGLE_CLOUD_PROJECT}") String project, @Value("${TRIAGE_RULES_TOPIC}") String triageRuleTopic,
                                                           BullseyeConverterClient bullseyeConverterClient, CiCdPreProcessTaskService ciCdPreProcessTaskService) throws IOException {
        this.subscriptionName = subscriptionName;
        this.jenkinsPluginJobRunCompleteStorageService = jenkinsPluginJobRunCompleteStorageService;
        this.objectMapper = objectMapper;
        this.ciCdInstancesDatabaseService = ciCdInstancesDatabaseService;
        this.ciCdJobsDatabaseService = ciCdJobsDatabaseService;
        this.ciCdJobRunsDatabaseService = ciCdJobRunsDatabaseService;
        this.jobRunStageDatabaseService = jobRunStageDatabaseService;
        this.ciCdJobRunStageStepsDatabaseService = ciCdJobRunStageStepsDatabaseService;
        this.cicdJobRunTestDatabaseService = cicdJobRunTestDatabaseService;
        this.bullseyeDatabaseService = bullseyeDatabaseService;
        this.ciCdJobRunArtifactsDatabaseService = ciCdJobRunArtifactsDatabaseService;
        this.redisConnectionFactory = redisConnectionFactory;
        this.bucketName = bucketName;
        this.storage = storage;
        this.ciCdPreProcessTaskService = ciCdPreProcessTaskService;
        this.jenkinsLogTriagingPublisher = Publisher.newBuilder(String.format("projects/%s/topics/%s", project, triageRuleTopic)).build();
        this.bullseyeConverterClient = bullseyeConverterClient;
        this.userIdentityService = new UserIdentityService(dataSource);
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.JENKINS;
    }

    @Override
    public Class<JenkinsPluginJobRunCompleteMessage> getMessageType() {
        return JenkinsPluginJobRunCompleteMessage.class;
    }

    @Override
    public String getSubscriptionName() {
        return this.subscriptionName;
    }

    @Override
    @Async("jenkinsPluginJobRunCompleteTaskExecutor")
    public void doTask(JenkinsPluginJobRunCompleteMessage task, AckReplyConsumer consumer) {
        String company = task.getCustomer();
        File jsonFile = null;
        File zipFile = null;
        File unzipFolder = null;
        boolean success = true;
        try {
            LoggingUtils.setupThreadLocalContext(task.getMessageId(), task.getCustomer(), "jenkins_run_complete", null);

            ciCdPreProcessTaskService.updateStatusAndAttemptCount(company, "PENDING", UUID.fromString(task.getTaskId()));
            log.info("Starting jenkins pre processing for task = {}", task.getMessageId());
            if (StringUtils.isBlank(task.getJsonFilePath())) {
                log.info("json file path is null or empty: {}!", task);
                return;
            }

            // -- extract json data
            jsonFile = downloadJsonFile(company, task.getJsonFilePath());
            JobRunCompleteRequest request = parseJsonFile(jsonFile);
            if (!validateJenkinsInstance(company, request.getJenkinsInstanceGuid())) {
                return;
            }

            // -- create temp folder that will be used to unzip the rest of the data
            unzipFolder = Files.createTempDirectory("job-run-unzip").toFile();
            log.debug("company {} Creates unzip folder {}", company, unzipFolder);

            // -- download and extract the zip data, if there is one
            if (StringUtils.isBlank(task.getResultFilePath())) {
                log.debug("company {} zip file path in task message is null or empty", company);
            } else {
                try {
                    zipFile = downloadZipFile(company, task.getResultFilePath());
                    extractZipFile(zipFile, unzipFolder);
                } catch (IOException e) {
                    log.warn("Failed to download the zip data for company={}, task={} - will proceed with json data only", company, task, e);
                }
            }

            // -- process data (includes persisting to db)
            log.debug("company {} processJobRunCompleteData starting", company);
            processJobRunCompleteData(company, task.getMessageId(), request, unzipFolder);
            log.debug("company {} processJobRunCompleteData completed for messageId={}", company, task.getMessageId());

        } catch (IOException | SQLException e) {
            log.error("Company: " + company + ", task: " + task + " Error pre processing Jenkins Results!", e);
            success=false;
        } catch (Throwable e) {
            log.error("Company: " + company + ", task: " + task + " Exception!", e);
            success=false;
        } finally {
            if (jsonFile != null) {
                try {
                    Files.deleteIfExists(jsonFile.toPath());
                } catch (IOException e) {
                    log.warn("Unable to delete json file: {}", jsonFile, e);
                }
            }
            if (zipFile != null) {
                try {
                    Files.deleteIfExists(zipFile.toPath());
                } catch (IOException e) {
                    log.warn("Unable to delete zip file: {}", zipFile, e);
                }
            }
            if (unzipFolder != null && unzipFolder.exists() && !FileSystemUtils.deleteRecursively(unzipFolder)) {
                log.warn("Unable to delete unzip directory: {}", unzipFolder);
            }
            if(success){
                ciCdPreProcessTaskService.updateStatus(company, "SUCCESS", UUID.fromString(task.getTaskId()));
            }else{
                ciCdPreProcessTaskService.updateStatus(company, "FAILURE", UUID.fromString(task.getTaskId()));
            }
            consumer.ack();
            LoggingUtils.clearThreadLocalContext();
        }
    }

    private File downloadJsonFile(String company, String jsonFilePath) throws IOException {
        File jsonFile = Files.createTempFile("job-run", ".json").toFile();
        log.debug("company {} Create json file temp file", company);
        jenkinsPluginJobRunCompleteStorageService.downloadResult(jsonFilePath, jsonFile);
        log.debug("company {} Create json file with gcp dta", company);
        return jsonFile;
    }

    private File downloadZipFile(String company, String resultFilePath) throws IOException {
        File zipFile = Files.createTempFile("job-run", ".zip").toFile();
        log.debug("company {} Create result zip file temp file", company);
        jenkinsPluginJobRunCompleteStorageService.downloadResult(resultFilePath, zipFile);
        log.debug("company {} Create zip file with gcp dta", company);
        return zipFile;
    }

    private void extractZipFile(File zipFile, File unzipFolder) throws IOException {
        log.debug("before unzipping");
        ZipService zipService = new ZipService();
        zipService.unZip(zipFile, unzipFolder);
        log.debug("after unzipping");
    }

    private JobRunCompleteRequest parseJsonFile(File jsonFile) throws IOException {
        return objectMapper.readValue(Files.readString(jsonFile.toPath()), JobRunCompleteRequest.class);
    }

    private boolean validateJenkinsInstance(String company, String instanceId) throws SQLException {
        Optional<CICDInstance> cicdInstance = ciCdInstancesDatabaseService.get(company, instanceId);
        if (cicdInstance.isEmpty()) {
            log.error("Error processing the job for company={}: no instance with id={}", company, instanceId);
            return false;
        }
        log.info("for company={} and instanceId={}, cicdInstance={}", company, instanceId, cicdInstance.get());
        if (StringUtils.isEmpty(cicdInstance.get().getIntegrationId())) {
            log.error("Error processing the job for company={} and instanceId={}: no associated integration id", company, instanceId);
            return false;
        }
        return true;
    }

    private void processJobRunCompleteData(String company, String messageId, JobRunCompleteRequest jobRunCompleteRequest, @Nullable File unzipFolder) throws IOException, SQLException {
        log.debug("Company {}, jobRunCompleteRequest = {}", company, jobRunCompleteRequest);
        // Persist Jenkins Instance
        CICDInstance cicdInstance = CICDInstance.builder()
                .id(UUID.fromString(jobRunCompleteRequest.getJenkinsInstanceGuid()))
                .name(jobRunCompleteRequest.getJenkinsInstanceName())
                .url(jobRunCompleteRequest.getJenkinsInstanceUrl())
                .type(CICD_TYPE.jenkins.toString())
                .build();
        log.debug("Company {}, saving cicdInstace starting {}", company, cicdInstance);
        String instanceIdString = ciCdInstancesDatabaseService.upsert(company, cicdInstance);
        log.debug("Company {}, saving cicdInstace completed {}", company, instanceIdString);
        cicdInstance = cicdInstance.toBuilder().id(UUID.fromString(instanceIdString)).build();
        processJobRunUsers(company, jobRunCompleteRequest, instanceIdString);
        // Persist Job Complete Data
        persistJobRun(company, messageId, cicdInstance, jobRunCompleteRequest, unzipFolder);
    }

    private void processJobRunUsers(String company, JobRunCompleteRequest jobRunCompleteRequest, String instanceIdString) {
        try {
            Optional<CICDInstance> cicdInstance = ciCdInstancesDatabaseService.get(company, instanceIdString);
            if (cicdInstance.isPresent() && cicdInstance.get().getIntegrationId() != null) {
                userIdentityService.batchUpsert(company,
                        List.of(DbScmUser.builder()
                                .integrationId(cicdInstance.get().getIntegrationId())
                                .cloudId(jobRunCompleteRequest.getUserId())
                                .displayName(jobRunCompleteRequest.getUserId())
                                .originalDisplayName(jobRunCompleteRequest.getUserId())
                                .build()));
            }
        } catch (SQLException throwables) {
            log.error("Failed to insert into integration users for user id: " + jobRunCompleteRequest.getUserId() + ", company: " + company);
        }
    }

    private List<CICDJobRun.JobRunParam> convertJobRunParams(List<JobRunParam> jobRunParams) {
        if (CollectionUtils.isEmpty(jobRunParams)) {
            return Collections.emptyList();
        }
        return jobRunParams.stream().map(x -> CICDJobRun.JobRunParam.builder()
                        .name(x.getName()).type(x.getType()).value(x.getValue()).build())
                .collect(Collectors.toList());
    }

    /**
     * Persists the top level job
     *
     * @param company
     * @param cicdInstance
     * @param jobRunCompleteRequest
     * @param unzipFolder
     * @throws SQLException
     * @throws IOException
     */
    private void persistJobRun(String company, String messageId, CICDInstance cicdInstance, JobRunCompleteRequest jobRunCompleteRequest, File unzipFolder) throws SQLException, IOException {
        //Persist Job
        CICDJob cicdJob = CICDJob.builder()
                .cicdInstanceId(cicdInstance.getId())
                .jobName(jobRunCompleteRequest.getJobName())
                .jobFullName(jobRunCompleteRequest.getJobFullName())
                .jobNormalizedFullName(jobRunCompleteRequest.getJobNormalizedFullName())
                .branchName(jobRunCompleteRequest.getBranchName())
                .moduleName(jobRunCompleteRequest.getModuleName())
                .scmUrl(CICDJob.sanitizeScmUrl(jobRunCompleteRequest.getRepoUrl()))
                .scmUserId(jobRunCompleteRequest.getScmUserId())
                .build();
        log.info("Company {}, saving job starting {}", company, cicdJob);
        String jobIdString = ciCdJobsDatabaseService.insert(company, cicdJob);
        log.info("Company {}, saving job completed {}", company, jobIdString);
        cicdJob = cicdJob.toBuilder().id(UUID.fromString(jobIdString)).build();
        //Persist Job Run
        Instant jobRunStartTime = Instant.ofEpochMilli(jobRunCompleteRequest.getStartTime());
        final int duration = (int) TimeUnit.MILLISECONDS.toSeconds(jobRunCompleteRequest.getDuration());
        Instant endTime = jobRunStartTime.plus(duration, ChronoUnit.SECONDS);
        CICDJobRun cicdJobRun = CICDJobRun.builder()
                .cicdJobId(cicdJob.getId())
                .jobRunNumber(jobRunCompleteRequest.getBuildNumber())
                .status(jobRunCompleteRequest.getResult())
                .startTime(jobRunStartTime)
                .duration(duration)
                .cicdUserId(jobRunCompleteRequest.getUserId())
                .endTime(endTime)
                .ci(jobRunCompleteRequest.getCi())
                .cd(jobRunCompleteRequest.getCd())
                .source(CICDJobRun.Source.JOB_RUN_COMPLETE_EVENT)
                .referenceId(messageId)
                .scmCommitIds((CollectionUtils.isEmpty(jobRunCompleteRequest.getScmCommitIds()) ? Collections.emptyList() : jobRunCompleteRequest.getScmCommitIds()))
                .triggers(CollectionUtils.isEmpty(jobRunCompleteRequest.getTriggerChain()) ? Collections.emptySet() : jobRunCompleteRequest.getTriggerChain())
                .params(convertJobRunParams(jobRunCompleteRequest.getJobRunParams()))
                .build();
        log.info("Company {}, saving job run starting {}", company, cicdJobRun);
        String jobRunId = ciCdJobRunsDatabaseService.insert(company, cicdJobRun);
        log.info("Company {}, saving job run completed {}", company, jobRunId);
        cicdJobRun = cicdJobRun.toBuilder().id(UUID.fromString(jobRunId)).build();

        // insert artifacts
        insertArtifacts(company, cicdJobRun,  jobRunCompleteRequest);

        //persist tests
        persistJobRunTests(company, unzipFolder, cicdJobRun.getId());

        String zipFileName = getPostBuildPublishedZipFile(unzipFolder);
        String project = getProjectFromPostBuildPublishedZipFileName(zipFileName);
        String directoryName = getDirectoryNameFromZipFileName(zipFileName);

        // Persist bullseye code coverage report
        log.info("Company {}, Persisting Coverage Xml Reports Starting! Message Id {}", company, messageId);
        persistCoverageReports(company, unzipFolder, cicdJobRun.getId(), cicdJob.getJobName(),
                BULLSEYE_COVERAGE_REPORTS_ZIP, BULLSEYE_COVERAGE_REPORTS_DIR, project);
        log.info("Company {}, Persisting Coverage Xml Reports Completed! Message Id {}", company, messageId);

        log.info("Company {}, Persisting Coverage Xml PostBuild Reports Starting! Message Id {}", company, messageId);
        persistCoverageReports(company, unzipFolder, cicdJobRun.getId(), cicdJob.getJobName(),
                zipFileName, directoryName, project);
        log.info("Company {}, Persisting Coverage Xml PostBuild Reports Completed! Message Id {}", company, messageId);

        //Persist bullseye cov files
        persistCoverageCovFiles(company, unzipFolder, cicdJobRun.getId(), cicdJob.getJobName(), messageId,
                BULLSEYE_COVERAGE_COV_ZIP, BULLSEYE_COVERAGE_COV_DIR);

        //Persist Stages
        JobRun jobRun = jobRunCompleteRequest.getJobRun();
        persistJobRunStages(company, messageId, cicdInstance, cicdJob, cicdJobRun, jobRunStartTime, jobRun, unzipFolder, Set.of());

        // upload log if exists
        String path = "";
        if (jobRun != null && jobRun.getLog() != null && unzipFolder != null) {
            File logFile = new File(unzipFolder, jobRun.getLog().toString());
            if (logFile.exists()) {
                log.debug("Uploading jobRun log to gcs starting");
                UUID uploadId = UUID.randomUUID();
                path = generateJobRunLogsPath(company, Instant.now(), uploadId.toString());
                try {
                    uploadDataToGcs(bucketName, path, Files.readAllBytes(logFile.toPath()));
                } catch (IOException e) {
                    log.error("Failed to upload jobRun log file to gcs, company = {}, jobId = {}, jobRunId = {}", company, cicdJob.getId(), jobRun.getId(), e);
                    return;
                }
                log.debug("Uploading jobRun log to gcs completed");
                ciCdJobRunsDatabaseService.update(company, cicdJobRun.toBuilder().logGcspath(path).build());
            }
        } else {
            log.debug("state log is null or unzipFolder is null, cannot upload logs to gcs!");
        }
        // send log scan message if log present
        sendLogScanMessage(company, cicdInstance, cicdJob, cicdJobRun, null, null, path);
    }

    protected String getPostBuildPublishedZipFile(@Nullable File unzipFolder) {
        if ((unzipFolder == null) || (!unzipFolder.exists())) {
            return null;
        }
        File[] files = unzipFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                String filename = file.getName();
                if (filename.startsWith("levelops_code_coverage_") && filename.endsWith("_xml.zip") &&
                        !filename.equals(BULLSEYE_COVERAGE_REPORTS_ZIP)) {
                    return filename;
                }
            }
        }
        return null;
    }

    protected String getProjectFromPostBuildPublishedZipFileName(@Nullable String zipFileName) {
        if (StringUtils.isEmpty(zipFileName)) {
            return "";
        }
        return zipFileName.substring("levelops_code_coverage_".length(), zipFileName.length() - "_xml.zip".length());
    }

    protected String getDirectoryNameFromZipFileName(@Nullable String zipFileName) {
        if (StringUtils.isEmpty(zipFileName)) {
            return StringUtils.EMPTY;
        }
        return zipFileName.substring(0, zipFileName.length() - ".zip".length());
    }

    private void persistCoverageCovFiles(String company, File unzipFolder, UUID cicdJobRunId, String jobName, String referenceId, String zipFile,
                                         String directory) throws IOException {
        log.info("Company {}, Persisting Coverage Cov File Starting! Message Id {}", company, referenceId);
        final File codeCoverageCovsZip = new File(unzipFolder, zipFile);
        if (!codeCoverageCovsZip.exists()) {
            log.info("Company {}, Coverage Cov Zip file does not exist! {}", company, codeCoverageCovsZip);
            return;
        }
        ZipService zipService = new ZipService();
        final File codeCoverageCovsUnzipDir = new File(unzipFolder, directory);
        Files.createDirectory(codeCoverageCovsUnzipDir.toPath());
        zipService.unZip(codeCoverageCovsZip, codeCoverageCovsUnzipDir);
        final File[] files = codeCoverageCovsUnzipDir.listFiles();
        if (files == null) {
            log.info("Company {}, Code Coverage Unzpip Dir does not contain any files!", company);
            return;
        }
        for (File file : files) {
            ConversionRequest conversionRequest = ConversionRequest.builder()
                    .customer(company)
                    .jobRunId(cicdJobRunId)
                    .referenceId(referenceId)
                    .fileName(file.getName())
                    .build();
            log.info("conversionRequest = {}", conversionRequest);
            try {
                log.info("code coverage unzipped dir path : {} and file name : {}, company: {}, jobName: {}",
                        codeCoverageCovsUnzipDir.getAbsolutePath(), file.getName(), company, jobName);
                ImmutablePair<String, String> pathAndName = extractPathAndFileName(codeCoverageCovsUnzipDir, file);
                String filePath = pathAndName.getLeft();
                String fileName = pathAndName.getRight();
                log.info("code coverage file relative path : {} and original file name : {}, company: {}, jobName: {} ",
                        filePath, fileName, company, jobName);

                // -- check hash to dedupe old cov files
                String fileHash = generateFileHash(file);
                if (isCovReportDuplicate(company, fileName, fileHash)) {
                    log.info("Ignoring duplicate COV file for company={}, jobName={}, fileName={}, hash={}", company, jobName, fileName, fileHash);
                    continue;
                }
                // -- convert COV to xml
                Result result = bullseyeConverterClient.convertCovToXml(conversionRequest, file);
                if (!result.isSuccess() || StringUtils.isBlank(result.getStandardOutput())) {
                    continue;
                }
                // -- parse XML to report
                CodeCoverageReport codeCoverageReport = DefaultXmlMapper.getXmlMapper().readValue(result.getStandardOutput(), CodeCoverageReport.class);
                BullseyeBuild project = BullseyeBuild.fromCodeCoverageReport(codeCoverageReport, cicdJobRunId, jobName, fileHash);
                project = project.toBuilder()
                        .name(fileName)
                        .directory(filePath)
                        .build();
                // -- persist
                log.info("Inserting Bullseye project with name : {} and directory : {}, company: {}, jobName: {} ",
                        project.getName(), project.getDirectory(), company, jobName);
                String projectId = bullseyeDatabaseService.insert(company, project);
                log.info("Inserted Bullseye project with id {} in company {}, jobname {}", projectId, company, jobName);

            } catch (Exception e) {
                log.error("Error storing Bullseye code coverage report for " + file, e);
            }
        }
        log.info("Persisting Coverage Cov File Completed! Message Id {}", referenceId);
    }

    public ImmutablePair<String, String> extractPathAndFileName(File codeCoverageCovsUnzipDir, File file) {
        String nameWithoutExtn = FilenameUtils.getBaseName(file.getAbsolutePath());
        int lastIndexOfUnderscore = nameWithoutExtn.lastIndexOf('_');
        if (lastIndexOfUnderscore > -1) {
            nameWithoutExtn = nameWithoutExtn.substring(0, lastIndexOfUnderscore);
        }
        String extn = FilenameUtils.getExtension(file.getAbsolutePath());
        String filePath = FilenameUtils.getPath(file.getAbsolutePath());
        String relPath = filePath.substring(codeCoverageCovsUnzipDir.getAbsolutePath().length());
        String fileName = nameWithoutExtn + "." + extn;
        return ImmutablePair.of(relPath, fileName);
    }

    private void persistCoverageReports(String company, File unzipFolder, UUID cicdJobRunId, String jobName, String zipFile,
                                        String directory, String projectName) throws IOException {
        if (StringUtils.isEmpty(zipFile)) {
            return;
        }
        final File codeCoverageResults = new File(unzipFolder, zipFile);
        if (!codeCoverageResults.exists()) {
            return;
        }
        ZipService zipService = new ZipService();
        final File coverageReports = new File(unzipFolder, directory);
        Files.createDirectory(coverageReports.toPath());
        zipService.unZip(codeCoverageResults, coverageReports);
        final File[] files = coverageReports.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            try {
                String fileHash = generateFileHash(file);
                CodeCoverageReport codeCoverageReport = DefaultXmlMapper.getXmlMapper().readValue(file,
                        CodeCoverageReport.class);
                BullseyeBuild project = BullseyeBuild.fromCodeCoverageReport(codeCoverageReport, cicdJobRunId,
                        StringUtils.defaultIfEmpty(projectName, jobName), fileHash);
                if (isCovReportDuplicate(company, project.getName(), fileHash)) {
                    log.info("Ignoring duplicate COV file for company={}, jobName={}, fileName={}, hash={}", company, jobName, project.getName(), fileHash);
                    continue;
                }
                bullseyeDatabaseService.insert(company, project);
            } catch (Exception e) {
                log.error("Company " + company + ", Error storing Bullseye code coverage report for " + file, e);
            }
        }
    }

    private String generateFileHash(File file) throws IOException {
        //noinspection UnstableApiUsage
        return Hashing.sha256().hashBytes(Files.readAllBytes(file.toPath())).toString();
    }

    private boolean isCovReportDuplicate(String company, String fileName, String fileHash) {
        return CollectionUtils.isNotEmpty(bullseyeDatabaseService.listProjects(company, BullseyeBuildFilter.builder()
                .names(List.of(fileName))
                .fileHashes(List.of(fileHash))
                .build(), Map.of(), 0, 1).getRecords());
    }

    private void persistJobRunTests(String company, File unzipFolder, UUID cicdJobRunId) throws IOException {
        final File junitTestResults = new File(unzipFolder, LEVELOPS_JUNIT_REPORTS_ZIP);
        if (junitTestResults.exists()) {
            ZipService zipService = new ZipService();
            final File junitReports = new File(unzipFolder, LEVELOPS_JUNIT_REPORTS_DIR);
            Files.createDirectory(junitReports.toPath());
            zipService.unZip(junitTestResults, junitReports);
            final File[] files = junitReports.listFiles();
            if (files != null) {
                int numTests = 0;
                int numSuccess = 0;
                for (File file : files) {
                    try {
                        final JUnitTestReport jUnitTestReport = DefaultXmlMapper.getXmlMapper().readValue(file,
                                JUnitTestReport.class);
                        final List<CiCdJobRunTest> tests = CiCdJobRunTest.fromJUnitTestSuite(jUnitTestReport, cicdJobRunId);
                        numTests += tests.size();
                        final List<String> insertedTests = cicdJobRunTestDatabaseService.batchInsert(company, tests);
                        numSuccess += insertedTests.size();
                    } catch (Exception e) {
                        log.error("persistJobRunTests: error storing junit tests for report: " + file, e);
                    }
                }
                log.info("persistJobRun: stored junit tests for the run {}, success: {}/{}", cicdJobRunId,
                        numTests, numSuccess);
            }
        }
    }

    /**
     * Persists the stages in a job run
     *
     * @param company
     * @param cicdJob
     * @param cicdJobRun
     * @param jobRunStartTime
     * @param jobRun
     * @param unzipFolder
     * @param parentFullPath  the path to the job containing the stages
     */
    private void persistJobRunStages(String company, String messageId, CICDInstance instance, CICDJob cicdJob, CICDJobRun cicdJobRun, Instant jobRunStartTime, JobRun jobRun, File unzipFolder, Set<PathSegment> parentFullPath) {
        if ((jobRun == null) || (CollectionUtils.isEmpty(jobRun.getStages()))) {
            log.debug("job run or stages is empty");
            return;
        }

        parentFullPath = new HashSet<>(parentFullPath);
        parentFullPath.add(PathSegment.builder().id(cicdJobRun.getId().toString()).name(cicdJob.getJobName()).position(parentFullPath.size() + 1).type(SegmentType.CICD_JOB).build());
        for (Node stage : jobRun.getStages()) {
            log.debug("stage = {}", stage);
            persistJobRunStage(company, messageId, instance, cicdJob, cicdJobRun, jobRunStartTime, stage, unzipFolder, parentFullPath);
        }
    }

    private UUID persistJobRun(String company, String messageId, Instant parentJobRunStartTime, CICDInstance instance, JobRun jobRun, File unzipFolder, Set<PathSegment> parentFullPath) {
        log.debug("persistJobRun jobRun = {}", jobRun);
        String jobNormalizedFullName = jobRun.getJobNormalizedFullName();
        if (StringUtils.isBlank(jobNormalizedFullName)) {
            log.debug("jobNormalizedFullName is null or empty!");
            return null;
        }
        //Do NOT Persist Job - Only fetch existing Job from DB
        DbListResponse<CICDJob> dbListResponse = null;
        try {
            dbListResponse = ciCdJobsDatabaseService.listByFilter(company, DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE,
                    null, null, List.of(jobNormalizedFullName), null, null);
        } catch (SQLException e) {
            log.error("Error fetching CICDJobRun using company = {}, jobNormalizedFullName = {}", company, jobNormalizedFullName, e);
            return null;
        }
        if ((dbListResponse == null) || (CollectionUtils.isEmpty(dbListResponse.getRecords())) || (dbListResponse.getRecords().size() != 1)) {
            log.warn("Search CICDJob using company = {}, jobNormalizedFullName = {} did not return exactly one record", company, jobNormalizedFullName);
            return null;
        }
        CICDJob cicdJob = dbListResponse.getRecords().get(0);
        log.debug("For child job fetched existing job from db cicdJob = {}", cicdJob);

        //Persist Job Run
        CICDJobRun cicdJobRun = parseCICDJobRunFromJobRun(cicdJob.getId(), messageId, parentJobRunStartTime, jobRun);
        if (cicdJobRun == null) {
            log.warn("cicdJobRun is null cannot persist to db!");
            return null;
        }
        log.debug("saving child job run starting {}", cicdJobRun);
        String jobRunId = null;
        try {
            jobRunId = ciCdJobRunsDatabaseService.insert(company, cicdJobRun);
        } catch (SQLException e) {
            log.error("Error persisting child job run to db, company = {}, cicdJobRun = {}", company, cicdJobRun);
            return null;
        }
        log.debug("saving child job run completed {}", jobRunId);
        cicdJobRun = cicdJobRun.toBuilder().id(UUID.fromString(jobRunId)).build();

        //Persist Stages
        log.info("persist job run stages starting for cicdJobRunId={}", cicdJobRun.getId());
        persistJobRunStages(company, messageId, instance, cicdJob, cicdJobRun, cicdJobRun.getStartTime(), jobRun, unzipFolder, parentFullPath);
        log.debug("persist job run stages completed");
        return cicdJobRun.getId();
    }

    private CICDJobRun parseCICDJobRunFromJobRun(UUID cicdJobId, String messageId, Instant parentJobRunStartTime, JobRun jobRun) {
        if ((jobRun == null) || (StringUtils.isBlank(jobRun.getId()))) {
            return null;
        }
        Instant jobRunStartTime = null;
        if (StringUtils.isBlank(jobRun.getStartTime())) {
            jobRunStartTime = parentJobRunStartTime;
        } else {
            jobRunStartTime = DateUtils.parseDateTime(jobRun.getStartTime());
        }
        final int duration = (int) TimeUnit.MILLISECONDS.toSeconds(jobRun.getDurationInMillis());
        final Instant endTime = jobRunStartTime.plus(duration, ChronoUnit.SECONDS);
        CICDJobRun cicdJobRun = CICDJobRun.builder()
                .cicdJobId(cicdJobId)
                .jobRunNumber(Long.valueOf(jobRun.getId()))
                .status(jobRun.getResult())
                .startTime(jobRunStartTime)
                .duration(duration)
                .endTime(endTime)
                .cicdUserId("UNKNOWN")
                .source(CICDJobRun.Source.JOB_RUN_COMPLETE_EVENT)
                .referenceId(messageId)
                .scmCommitIds(Collections.emptyList())
                .triggers(Collections.emptySet())
                .params(Collections.emptyList())
                .build();
        return cicdJobRun;
    }

    protected void insertArtifacts(String company, CICDJobRun insertedJobRun, JobRunCompleteRequest jobRunCompleteRequest) {
        List<CiCdJobRunArtifact> artifacts = jobRunCompleteRequest.getArtifacts();
        if(CollectionUtils.isNotEmpty(artifacts)) {
            UUID insertedJobRunId = insertedJobRun.getId();
            artifacts = artifacts.stream().map(a->a.toBuilder().cicdJobRunId(insertedJobRunId).build()).collect(Collectors.toList());

            try {
                ciCdJobRunArtifactsDatabaseService.replace(company, insertedJobRunId.toString(), artifacts);
            } catch (SQLException e) {
                log.error("Failed to insert artifacts for cicdJobRunId={}", insertedJobRunId.toString(), e);
            } catch (Exception e) {
                log.error("Failed to parse artifacts for cicdJobRunId={}", insertedJobRunId.toString(), e);
            }
        }
    }

    private void persistJobRunStage(String company, String messageId, CICDInstance instance, CICDJob job, CICDJobRun jobRun, Instant jobRunStartTime, Node stage, File unzipFolder, Set<PathSegment> parentFullPath) {
        //Persist child Jobs
        var stageId = UUID.randomUUID();
        var fullPath = new HashSet<>(parentFullPath);
        fullPath.add(PathSegment.builder().name(stage.getDisplayName()).position(parentFullPath.size() + 1).type(SegmentType.CICD_STAGE).id(stageId.toString()).build());
        Set<UUID> childJobRunIds = Set.of();
        if (CollectionUtils.isNotEmpty(stage.getChildJobRuns())) {
            childJobRunIds = new HashSet<>();
            for (JobRun currentChildJobRun : stage.getChildJobRuns()) {
                log.debug("currentChildJobRun = {}", currentChildJobRun);
                log.debug("For company {}, jobRunId = {}, stage id = {}, processing chid job run id = {} starting", company, jobRun.getId(), stage.getId(), currentChildJobRun.getId());
                UUID currentChildJobRunId = persistJobRun(company, messageId, jobRunStartTime, instance, currentChildJobRun, unzipFolder, fullPath);
                log.debug("For company {}, jobRunId = {}, stage id = {}, processing chid job run id = {} completed", company, jobRun.getId(), stage.getId(), currentChildJobRun.getId());
                if (currentChildJobRunId != null) {
                    childJobRunIds.add(currentChildJobRunId);
                }
            }
        } else {
            log.debug("For company {}, jobRunId = {}, stage id = {}, child job runs is null or empty!", company, jobRun.getId(), stage.getId());
        }

        UUID uploadId = UUID.randomUUID();
        log.debug("Before uploading stage log to gcs");
        //Save Stage Logs to GCS
        String path = "";
        if ((stage.getLog() != null) && (unzipFolder != null)) {
            File stageLogFile = new File(unzipFolder, stage.getLog().toString());
            if (stageLogFile.exists()) {
                log.debug("Uploading stage log to gcs starting");
                path = generateJobRunStageLogsPath(company, Instant.now(), uploadId.toString());
                try {
                    uploadDataToGcs(bucketName, path, Files.readAllBytes(stageLogFile.toPath()));
                } catch (IOException e) {
                    log.error("Failed to upload stage log file to gcs, company = {}, jobId = {}, jobRunId = {}, stageId = {}", company, job.getId(), jobRun.getId(), stage.getId(), e);
                    return;
                }
                log.debug("Uploading stage log to gcs completed");
            }
        } else {
            log.debug("state log is null or unzipFolder is null, cannot upload logs to gcs!");
        }

        //Persist Stage
        JobRunStage jobRunStage = JobRunStage.builder()
                .id(stageId)
                .ciCdJobRunId(jobRun.getId())
                .stageId(stage.getId())
                .name(StringUtils.abbreviateMiddle(stage.getDisplayName(), "...", 50))
                .description(stage.getDisplayDescription())
                .result(MoreObjects.firstNonNull(stage.getResult(), "UNKNOWN"))
                .state(MoreObjects.firstNonNull(stage.getState(), "UNKNOWN"))
                .duration((int) TimeUnit.MILLISECONDS.toSeconds(stage.getDurationInMillis()))
                .logs(path)
                .startTime((StringUtils.isNotBlank(stage.getStartTime()) ? DateUtils.parseDateTime(stage.getStartTime()) : jobRunStartTime))
                .fullPath(fullPath)
                .childJobRuns(childJobRunIds)//ToDo: VA Fix later
                .url(jobRunStageDatabaseService.getFullUrl(company, jobRun.getId(), stage.getId(), ""))
                .build();
        log.debug("Before inserting jobRunStage = {}", jobRunStage);
        String stageIdString = null;
        try {
            stageIdString = jobRunStageDatabaseService.insert(company, jobRunStage);
        } catch (SQLException e) {
            log.error("Failed to save stage, company = {}, jobId = {}, jobRunId = {}, stageId = {}", company, job.getId(), jobRun.getId(), stage.getId(), e);
            return;
        }
        log.debug("After inserting jobRunStage id = {}", stageIdString);
        jobRunStage = jobRunStage.toBuilder().id(UUID.fromString(stageIdString)).build();

        //Persist job run stage steps
        if (CollectionUtils.isNotEmpty(stage.getSteps())) {
            for (Step step : stage.getSteps()) {
                processNodeStep(company, instance, job, jobRun, jobRunStage, step, unzipFolder);
            }
        } else {
            log.debug("For company {}, jobRunId = {}, stage id = {}, states is null or empty!", company, jobRun.getId(), stage.getId());
        }

        // send message if the path is not null (the path is null when either not a failure or when this is a job with stages and steps)
        sendLogScanMessage(company, instance, job, jobRun, jobRunStage, null, path);
    }

    private void processNodeStep(String company, CICDInstance instance, CICDJob job, CICDJobRun jobRun, JobRunStage jobRunStage, Step step,
                                 File unzipFolder) {
        //Persist Node Step to db
        JobRunStageStep jobRunStageStep = JobRunStageStep.builder()
                .cicdJobRunStageId(jobRunStage.getId())
                .stepId(step.getId())
                .displayName(StringUtils.abbreviateMiddle(step.getDisplayName(), "...", 50))
                .displayDescription(StringUtils.abbreviateMiddle(step.getDisplayDescription(), "...", 50))
                .startTime((StringUtils.isNotBlank(step.getStartTime())) ? DateUtils.parseDateTime(step.getStartTime()) : jobRunStage.getStartTime())
                .result(MoreObjects.firstNonNull(step.getResult(), "UNKNOWN")).state(MoreObjects.firstNonNull(step.getState(), "UNKNOWN"))
                .duration((int) TimeUnit.MILLISECONDS.toSeconds(step.getDurationInMillis()))
                .build();
        log.debug("Before inserting jobRunStageStep = {}", jobRunStageStep);
        String stepIdString = null;
        try {
            stepIdString = ciCdJobRunStageStepsDatabaseService.insert(company, jobRunStageStep);
        } catch (SQLException e) {
            log.error("Failed to save job run step, company = {}, jobId = {}, jobRunId = {}, stageId = {}, stepId = {}", company, job.getId(), jobRunStage.getCiCdJobRunId(), jobRunStage.getId(), step.getId(), e);
            return;
        }
        log.debug("After inserting jobRunStageStep id = {}", stepIdString);
        jobRunStageStep = jobRunStageStep.toBuilder().id(UUID.fromString(stepIdString)).build();

        //Write Node Log to GCS
        log.debug("Before uploading stage step log to gcs");
        //Save Stage Logs to GCS
        String path = "";
        if ((step.getLog() != null) && (unzipFolder != null)) {
            File stepLogFile = new File(unzipFolder, step.getLog().toString());
            if (stepLogFile.exists()) {
                log.debug("Uploading step log to gcs starting");
                path = generateJobRunStageStepLogsPath(company, Instant.now(), jobRunStageStep.getId().toString());
                try {
                    uploadDataToGcs(bucketName, path, Files.readAllBytes(stepLogFile.toPath()));
                } catch (IOException e) {
                    log.error("Failed to upload step log file to gcs, company = {}, jobId = {}, jobRunId = {}, stageId = {}, stepId = {}", company, job.getId(), jobRunStage.getCiCdJobRunId(), jobRunStage.getId(), jobRunStageStep.getId(), e);
                    return;
                }
                log.debug("Uploading step log to gcs completed");
            }
        } else {
            log.debug("step log is null or unzipFolder is null, cannot upload logs to gcs!");
        }

        //Update Node Step in DB with path
        if (StringUtils.isNotBlank(path)) {
            jobRunStageStep = jobRunStageStep.toBuilder().gcsPath(path).build();
            log.debug("Before updating jobRunStageStep = {}", jobRunStageStep);
            try {
                boolean success = ciCdJobRunStageStepsDatabaseService.update(company, jobRunStageStep);
                log.debug("After inserting jobRunStageStep id = {}, success = {}", jobRunStageStep.getId(), success);
            } catch (SQLException e) {
                log.error("Failed to update step, company = {}, jobId = {}, jobRunId = {}, stageId = {}, stepId = {}", company, job.getId(), jobRunStage.getCiCdJobRunId(), jobRunStage.getId(), step.getId(), e);
                return;
            }
        }

        //Send Jenkins Log Triaging Message
        sendLogScanMessage(company, instance, job, jobRun, jobRunStage, jobRunStageStep, path);
    }

    private Blob uploadDataToGcs(String bucketName, String gcsPath, byte[] content) {
        BlobId blobId = BlobId.of(bucketName, gcsPath);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType("application/json")
                .build();
        log.info("Uploading content to {}:{}", bucketName, gcsPath);
        return storage.create(blobInfo, content);
    }

    private static String generatePath(String prefix, String tenantId, Instant date, String stageId) {
        return String.format("%s/tenant-%s/%s/%s",
                prefix,
                tenantId,
                DateTimeFormatter.ofPattern("uuuu/MM/dd", Locale.US).format(date.atZone(ZoneOffset.UTC)),
                stageId);
    }

    private static String generateJobRunLogsPath(String tenantId, Instant date, String jobRunId) {
        return generatePath("cicd-job-run-logs", tenantId, date, jobRunId);
    }

    private static String generateJobRunStageLogsPath(String tenantId, Instant date, String stageId) {
        return generatePath("cicd-job-run-stage-logs", tenantId, date, stageId);
    }

    private static String generateJobRunStageStepLogsPath(String tenantId, Instant date, String stageId) {
        return generatePath("cicd-job-run-stage-step-logs", tenantId, date, stageId);
    }

    /**
     *
     */
    private void sendLogScanMessage(@NonNull final String company, @NonNull final CICDInstance instance, @NonNull final CICDJob job, @NonNull final CICDJobRun jobRun, final JobRunStage jobRunStage, final JobRunStageStep jobRunStep, final String path) {
        if (StringUtils.isBlank(path)) {
            log.debug("path is blank not sending {} log triage message!", (jobRunStep != null ? "step" : jobRunStage != null ? "stage" : "job run"));
            return;
        }

        JenkinsLogTriagingMessageBuilder jenkinsLogTriagingMessageBuilder = JenkinsLogTriagingMessage.builder()
                .company(company)
                .instanceId(instance.getId())
                .instanceName(instance.getName())
                .jobId(job.getId())
                .jobName(job.getJobName())
                .jobStatus(StringUtils.defaultString(jobRun.getStatus()))
                .logBucket(bucketName)
                .logLocation(path);

        if (jobRun != null) {
            jenkinsLogTriagingMessageBuilder
                    .jobRunId(jobRun.getId());
        }

        if (jobRunStep != null) {
            jenkinsLogTriagingMessageBuilder
                    .stepId(jobRunStep.getId());
        }

        if (jobRunStage != null) {
            jenkinsLogTriagingMessageBuilder
                    .stageId(jobRunStage.getId())
                    .url(jobRunStage.getUrl());
        } else {
            jenkinsLogTriagingMessageBuilder.url(jobRunStageDatabaseService.getFullUrl(company, jobRun.getId(), "", ""));
        }

        try (var redis = redisConnectionFactory.getConnection()) {
            JenkinsLogTriagingMessage jenkinsLogTriagingMessage = jenkinsLogTriagingMessageBuilder.build();
            log.debug("sending stage log triage message starting {}", jenkinsLogTriagingMessage);
            PubsubMessage pubsubMessage = PubsubMessage.newBuilder().setData(ByteString.copyFromUtf8(objectMapper.writeValueAsString(jenkinsLogTriagingMessage))).build();
            jenkinsLogTriagingPublisher.publish(pubsubMessage);
            log.debug("sending stage log triage message completed");

            // set the distributed tracking mark in redis
            var id = String.format("%s_%s", company, (job.getCicdInstanceId().toString() + jobRun.getId().toString()));
            var key = id.getBytes();
            var count = redis.incrBy(key, 1);
            log.debug("count in redis: {}", count);
        } catch (JsonProcessingException e) {
            log.error("Error creating stage pubsub message, company = {}, jobId = {}, jobRunId = {}, stageId = {}, stepId = {}", company, job.getId(), jobRun.getId(), jobRunStage != null ? jobRunStage.getId() : "N/A", jobRunStep != null ? jobRunStep.getId() : "N/A", e);
        }
    }

}
