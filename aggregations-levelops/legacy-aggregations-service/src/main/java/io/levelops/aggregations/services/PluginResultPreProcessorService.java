package io.levelops.aggregations.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.ArrayMap;
import io.levelops.aggregations.models.jenkins.JenkinsMonitoringResult;
import io.levelops.aggregations.models.jenkins.JobAllConfigChanges;
import io.levelops.aggregations.models.jenkins.JobAllRuns;
import io.levelops.aggregations.models.jenkins.JobConfigChangeDetail;
import io.levelops.aggregations.models.jenkins.JobNameDetails;
import io.levelops.aggregations.models.jenkins.JobRunDetails;
import io.levelops.commons.databases.models.database.PluginResultDTO;
import io.levelops.commons.zip.ZipService;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.levelops.aggregations.services.JenkinsInstanceInfoParserService.JENKINS_INSTANCE_GUID;
import static io.levelops.aggregations.services.JenkinsInstanceInfoParserService.JENKINS_INSTANCE_NAME;
import static io.levelops.aggregations.services.JenkinsInstanceInfoParserService.JENKINS_INSTANCE_URL;

@Log4j2
@Service
@SuppressWarnings("unused")
public class PluginResultPreProcessorService {
    public static final String PLUGIN_TOOL_JENKINS_CONFIG = "jenkins_config";
    private static final String REPORTS_DIR_NAME = "reports";
    private static final String DATA_DIR_NAME = "data";
    public static final String JOBS_DATA_DIR_NAME = "jobs";
    public static final String CONF_HISTORY_FILE = "config-history.txt";
    public static final String CONF_SCM_FILE = "config-scm.txt";
    public static final String RUN_HISTORY_FILE = "run-history.txt";
    public static final String RUN_TRIGGER_FILE = "run-triggers.txt";
    public static final String RUN_PARAMS_HISTORY_FILE = "run-params.txt";
    public static final String RUN_GIT_CHANGES_HISTORY_FILE = "run-git.txt";
    public static final String JOB_FULL_NAME_FILE = "job-full-name.txt";
    public static final String LEVELOPS_DATA_ZIP_FILE = "levelops-data.zip";
    public static final String JENKINS_INSTANCE_GUID_FILE = "jenkins-instance-guid.txt";
    public static final String JENKINS_INSTANCE_NAME_FILE = "jenkins-instance-name.txt";
    public static final String JENKINS_INSTANCE_URL_FILE = "jenkins-instance-url.txt";

    public static final String RUN_COMPLETE_DATA_FILE_SUFFIX = "run-complete-data-";
    public static final String RUN_COMPLETE_DATA_FILE_PREFIX = ".txt";
    public static Pattern JOB_RUN_COMPLETE_FILE_PATTERN = Pattern.compile("run-complete-data-(\\d*)\\.txt",  Pattern.MULTILINE);

    private final ObjectMapper objectMapper;
    private final JenkinsConfigChangesParserService jenkinsConfigChangesParserService;
    private final JenkinsJobRunsParserService jenkinsJobRunsParserService;
    private final JenkinsInstanceInfoParserService jenkinsInstanceInfoParserService;

    @Autowired
    public PluginResultPreProcessorService(ObjectMapper objectMapper, JenkinsConfigChangesParserService jenkinsConfigChangesParserService, JenkinsJobRunsParserService jenkinsJobRunsParserService, JenkinsInstanceInfoParserService jenkinsInstanceInfoParserService) {
        this.objectMapper = objectMapper;
        this.jenkinsConfigChangesParserService = jenkinsConfigChangesParserService;
        this.jenkinsJobRunsParserService = jenkinsJobRunsParserService;
        this.jenkinsInstanceInfoParserService = jenkinsInstanceInfoParserService;
    }

    private PluginResultDTO parseJsonFile(final File jsonFile) throws IOException {
        try (InputStream inputStream = new FileInputStream(jsonFile)) {
            PluginResultDTO pluginResultDTO = objectMapper.readValue(inputStream, PluginResultDTO.class);
            return pluginResultDTO;
        }
    }

    private List<File> getListOfAllDataDirectories(File unzipFolder){
        File[] dataDirectories = unzipFolder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                if(!file.isDirectory()){
                    return false;
                }
                String directoryName = file.getName();
                return directoryName.startsWith("data-");
            }
        });
        if(dataDirectories == null){
            return Collections.emptyList();
        }
        List<File> dataDirectoriesList = Arrays.asList(dataDirectories);
        Collections.sort(dataDirectoriesList, Collections.reverseOrder());
        return dataDirectoriesList;
    }

    List<JobAllConfigChanges> mergeConfigChanges(List<List<JobAllConfigChanges>> configChangesList){
        if(CollectionUtils.isEmpty(configChangesList)){
            return Collections.emptyList();
        }
        Map<JobNameDetails, List<JobConfigChangeDetail>> jobNameDetailsConfigChangesMap = new ArrayMap<>();
        for(List<JobAllConfigChanges> currentList : configChangesList){
            for(JobAllConfigChanges currentJobAllConfigChanges : currentList){
                if(StringUtils.isBlank(currentJobAllConfigChanges.getJobFullName())){
                    continue;
                }
                JobNameDetails jobNameDetails = JobNameDetails.builder()
                        .jobName(currentJobAllConfigChanges.getJobName())
                        .jobFullName(currentJobAllConfigChanges.getJobFullName())
                        .jobNormalizedFullName(currentJobAllConfigChanges.getJobNormalizedFullName())
                        .branchName(currentJobAllConfigChanges.getBranchName())
                        .moduleName(currentJobAllConfigChanges.getModuleName()).build();
                if(!jobNameDetailsConfigChangesMap.containsKey(jobNameDetails)){
                    jobNameDetailsConfigChangesMap.put(jobNameDetails, new ArrayList<>());
                }
                jobNameDetailsConfigChangesMap.get(jobNameDetails).addAll(currentJobAllConfigChanges.getConfigChangeDetails());
            }
        }

        List<JobAllConfigChanges> mergedAllJobsAllConfigChanges = new ArrayList<>();
        for(Map.Entry<JobNameDetails, List<JobConfigChangeDetail>> e : jobNameDetailsConfigChangesMap.entrySet()){
            JobNameDetails jobNameDetails = e.getKey();
            List<JobConfigChangeDetail> allConfigChanges = e.getValue();
            Collections.sort(allConfigChanges);
            JobAllConfigChanges currentJobAllConfigChanges = JobAllConfigChanges.builder()
                    .jobName(jobNameDetails.getJobName())
                    .jobFullName(jobNameDetails.getJobFullName())
                    .jobNormalizedFullName(jobNameDetails.getJobNormalizedFullName())
                    .branchName(jobNameDetails.getBranchName())
                    .moduleName(jobNameDetails.getModuleName())
                    .configChangeDetails(allConfigChanges)
                    .build();

            mergedAllJobsAllConfigChanges.add(currentJobAllConfigChanges);
        }
        return mergedAllJobsAllConfigChanges;
    }
    List<JobAllRuns> mergeJobRuns(List<List<JobAllRuns>> allJobsAllRunsList){
        if(CollectionUtils.isEmpty(allJobsAllRunsList)){
            return Collections.emptyList();
        }
        Map<JobNameDetails, List<JobRunDetails>> jobNameDetailsJobRunDetailsMap = new ArrayMap<>();
        Map<JobNameDetails, String> jobNameDetailsScmUrlMap = new ArrayMap<>();
        Map<JobNameDetails, String> jobNameDetailsScmUserIdMap = new ArrayMap<>();
        for(List<JobAllRuns> currentList : allJobsAllRunsList){
            for(JobAllRuns currentJobAllJobRuns : currentList){
                if(StringUtils.isBlank(currentJobAllJobRuns.getJobFullName())){
                    continue;
                }
                JobNameDetails jobNameDetails = JobNameDetails.builder()
                        .jobName(currentJobAllJobRuns.getJobName())
                        .jobFullName(currentJobAllJobRuns.getJobFullName())
                        .jobNormalizedFullName(currentJobAllJobRuns.getJobNormalizedFullName())
                        .branchName(currentJobAllJobRuns.getBranchName())
                        .moduleName(currentJobAllJobRuns.getModuleName()).build();
                if(!jobNameDetailsJobRunDetailsMap.containsKey(jobNameDetails)){
                    jobNameDetailsJobRunDetailsMap.put(jobNameDetails, new ArrayList<>());
                }
                jobNameDetailsJobRunDetailsMap.get(jobNameDetails).addAll(currentJobAllJobRuns.getRuns());
                jobNameDetailsScmUrlMap.put(jobNameDetails, currentJobAllJobRuns.getScmUrl());
                jobNameDetailsScmUserIdMap.put(jobNameDetails, currentJobAllJobRuns.getScmUserId());
            }
        }

        List<JobAllRuns> mergedAllJobsAllJobRuns = new ArrayList<>();
        for(Map.Entry<JobNameDetails, List<JobRunDetails>> e : jobNameDetailsJobRunDetailsMap.entrySet()){
            JobNameDetails jobNameDetails = e.getKey();
            List<JobRunDetails> allJobRuns = e.getValue();
            Collections.sort(allJobRuns);
            JobAllRuns currentJobAllConfigChanges = JobAllRuns.builder()
                    .jobName(jobNameDetails.getJobName())
                    .jobFullName(jobNameDetails.getJobFullName())
                    .jobNormalizedFullName(jobNameDetails.getJobNormalizedFullName())
                    .branchName(jobNameDetails.getBranchName())
                    .moduleName(jobNameDetails.getModuleName())
                    .scmUrl(jobNameDetailsScmUrlMap.getOrDefault(jobNameDetails, null))
                    .scmUserId(jobNameDetailsScmUserIdMap.getOrDefault(jobNameDetails, null))
                    .runs(allJobRuns)
                    .build();

            mergedAllJobsAllJobRuns.add(currentJobAllConfigChanges);
        }
        return mergedAllJobsAllJobRuns;
    }
    private JenkinsMonitoringResult preprocessJenkinsPluginResults(String company, File resultFile, File unzipFolder, Map<String, Map<Long, File>> jobRunDetailsMap) throws IOException {
        File sourceZipFile = Files.createTempFile("jenkinsPluginZip", ".zip").toFile();
        try (InputStream inputStream = new FileInputStream(resultFile)) {
            Files.copy(inputStream, sourceZipFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            ZipService zipService = new ZipService();
            zipService.unZip(sourceZipFile, unzipFolder);

            JenkinsMonitoringResult.JenkinsMonitoringResultBuilder bldr = JenkinsMonitoringResult.builder();

            Map<String,String> jenkinsInstanceInfo = jenkinsInstanceInfoParserService.parse(unzipFolder);
            String jenkinsInstanceGuidString = jenkinsInstanceInfo.getOrDefault(JENKINS_INSTANCE_GUID, null);
            bldr.jenkinsInstanceGuid(StringUtils.isNotBlank(jenkinsInstanceGuidString) ? UUID.fromString(jenkinsInstanceGuidString) : null);
            bldr.jenkinsInstanceName(jenkinsInstanceInfo.getOrDefault(JENKINS_INSTANCE_NAME, null));
            bldr.jenkinsInstanceUrl(jenkinsInstanceInfo.getOrDefault(JENKINS_INSTANCE_URL, null));

            List<List<JobAllConfigChanges>> configChangesList = new ArrayList<>();
            List<List<JobAllRuns>> allJobsAllRunsList = new ArrayList<>();

            File jobsDirectory = Paths.get(unzipFolder.getAbsolutePath(), "jobs").toFile();
            configChangesList.add(jenkinsConfigChangesParserService.parse(jobsDirectory));
            allJobsAllRunsList.add(jenkinsJobRunsParserService.parse(company, jobsDirectory, jobRunDetailsMap));

            List<File> dataDirectories = getListOfAllDataDirectories(unzipFolder);
            List<File> jobDirectories = dataDirectories.stream().map(x -> new File(x, "jobs")).collect(Collectors.toList());
            for(File currentJobDirectory : jobDirectories){
                configChangesList.add(jenkinsConfigChangesParserService.parse(currentJobDirectory));
                allJobsAllRunsList.add(jenkinsJobRunsParserService.parse(company, currentJobDirectory, jobRunDetailsMap));
            }

            List<JobAllConfigChanges> configChanges = mergeConfigChanges(configChangesList);
            List<JobAllRuns> allJobsAllRuns = mergeJobRuns(allJobsAllRunsList);

            bldr.configChanges(configChanges);
            bldr.jobRuns(allJobsAllRuns);

            JenkinsMonitoringResult jenkinsMonitoringResult = bldr.build();
            return jenkinsMonitoringResult;
        } catch (IOException e) {log.error("On file: {}", resultFile, e); throw e;}
        finally {
            if((sourceZipFile != null) && (sourceZipFile.exists())){
                Files.delete(sourceZipFile.toPath());
            }
        }
    }

    private Long getOldestJobRunStartTime(JenkinsMonitoringResult jenkinsMonitoringResult) {
        if(jenkinsMonitoringResult == null) {
            return null;
        }
        long oldest = org.apache.commons.collections4.CollectionUtils.emptyIfNull(jenkinsMonitoringResult.getJobRuns()).stream().flatMap(job -> job.getRuns().stream())
                .mapToLong(r -> r.getStartTime()).min()
                .orElse(-1);
        log.debug("oldest {}", oldest);
        Long oldestJobRunStartTime = (oldest != -1) ? oldest : null;
        log.info("oldestJobRunStartTime {}", oldestJobRunStartTime);
        return oldestJobRunStartTime;
    }

    public PluginResultDTO preProcess(String company, String messageId, File jsonFile, File resultFile, File upzipFolder, Map<String, Map<Long, File>> jobRunDetailsMap) throws IOException, BadRequestException {
        PluginResultDTO pluginResultDTO = parseJsonFile(jsonFile);
        if(!PLUGIN_TOOL_JENKINS_CONFIG.equals(pluginResultDTO.getTool())){
            throw new BadRequestException("Plugin Result preprocessing is not suported for tool " + pluginResultDTO.getTool() + " . It is supported only for " + PLUGIN_TOOL_JENKINS_CONFIG);
        }
        Map<String, Object> results = new HashMap<>();
        if(!CollectionUtils.isEmpty(pluginResultDTO.getResults())){
            if(pluginResultDTO.getResults().containsKey("jenkins_config")){
                results.put("jenkins_settings", pluginResultDTO.getResults().get("jenkins_config"));
            }
        }

        JenkinsMonitoringResult jenkinsMonitoringResult = preprocessJenkinsPluginResults(company, resultFile, upzipFolder, jobRunDetailsMap);
        results.put("jenkins_config", jenkinsMonitoringResult);
        results.put("reference_id", messageId);

        Long oldestJobRunStartTime = getOldestJobRunStartTime(jenkinsMonitoringResult);

        PluginResultDTO.PluginResultDTOBuilder bldr = pluginResultDTO.toBuilder().results(results);
        if(oldestJobRunStartTime != null) {
            bldr.metadata(Map.of("start_time", oldestJobRunStartTime));
        }

        return bldr.build();
    }
}
