package io.levelops.aggregations.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations.models.BuildMessage;
import io.levelops.aggregations.models.JobRunGitChangesMessage;
import io.levelops.aggregations.models.JobRunParamsMessage;
import io.levelops.aggregations.models.jenkins.JobAllRuns;
import io.levelops.aggregations.models.jenkins.JobNameDetails;
import io.levelops.aggregations.models.jenkins.JobRunDetails;
import io.levelops.aggregations.models.jenkins.JobRunParam;
import io.levelops.commons.databases.models.database.CICDJobTrigger;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import static io.levelops.aggregations.services.PluginResultPreProcessorService.CONF_SCM_FILE;
import static io.levelops.aggregations.services.PluginResultPreProcessorService.JOB_RUN_COMPLETE_FILE_PATTERN;
import static io.levelops.aggregations.services.PluginResultPreProcessorService.RUN_HISTORY_FILE;

@Log4j2
@Service
public class JenkinsJobRunsParserService {
    private final ObjectMapper objectMapper;
    private final BuildRunMessageService buildRunMessageService;
    private final JobRunParamsService jobRunParamsService;
    private final JobRunTriggersParserService jobRunTriggersParserService;
    private final JobSCMConfigService jobSCMConfigService;
    private final JobRunGitChangesService jobRunGitChangesService;
    private final JobFullNameDetailsService jobFullNameDetailsService;
    private final int jenkinsResultsCutoffInDays;

    public JenkinsJobRunsParserService(ObjectMapper objectMapper, BuildRunMessageService buildRunMessageService, JobRunParamsService jobRunParamsService, JobSCMConfigService jobSCMConfigService, JobRunGitChangesService jobRunGitChangesService, JobFullNameDetailsService jobFullNameDetailsService, JobRunTriggersParserService jobRunTriggersParserService, @Value("${JENKINS_RESULTS_CUTOFF_IN_DAYS:7}") int jenkinsResultsCutoffInDays) {
        this.objectMapper = objectMapper;
        this.buildRunMessageService = buildRunMessageService;
        this.jobRunParamsService = jobRunParamsService;
        this.jobSCMConfigService = jobSCMConfigService;
        this.jobRunGitChangesService = jobRunGitChangesService;
        this.jobFullNameDetailsService = jobFullNameDetailsService;
        this.jobRunTriggersParserService = jobRunTriggersParserService;
        this.jenkinsResultsCutoffInDays = jenkinsResultsCutoffInDays;
    }

    private void parseJobRunDetails(String company, File jobDirectory, String jobFullName, Map<String, Map<Long, File>> jobRunDetailsMap) {
        log.debug("parseJobRunDetails, jobDirectory = {}", jobDirectory);
        File[] children = jobDirectory.listFiles();
        if(children == null) {
            return;
        }
        for(File current : children) {
            String fileName = current.getName();
            Matcher matcher = JOB_RUN_COMPLETE_FILE_PATTERN.matcher(fileName);
            if(!matcher.matches()) {
                log.debug("parseJobRunDetails, jobFullName = {}, does not match", current);
                continue;
            }
            log.debug("parseJobRunDetails, jobFullName = {}, matches", current);
            Long jobRunNumber = Long.valueOf(matcher.group(1));
            log.debug("jobRunNumber = {}", jobRunNumber);
            if(!jobRunDetailsMap.containsKey(jobFullName)) {
                jobRunDetailsMap.put(jobFullName, new HashMap<>());
            }
            jobRunDetailsMap.get(jobFullName).put(jobRunNumber, current);
            log.debug("Found jobRunDetail File jobFullName = {}, jobRunNumber = {}, current = {}", jobFullName,jobRunNumber, current);
        }
    }

    private JobAllRuns extractAllJobRunsFromJobDirectory(String company, File jobDirectory, Map<String, Map<Long, File>> jobRunDetailsMap) throws IOException {
        log.debug("extractAllJobRunsFromJobDirectory, jobDirectory = {}", jobDirectory);
        if(jobDirectory == null){
            return null;
        }
        if((!jobDirectory.exists()) || (!jobDirectory.isDirectory())){
            return null;
        }

        JobNameDetails jobNameDetails = jobFullNameDetailsService.parseJobFullNameDetails(jobDirectory);
        if(jobNameDetails == null){
            return null;
        }

        JobSCMConfigService.SCMConfig scmConfig = jobSCMConfigService.readSCMConfig(new File(jobDirectory, CONF_SCM_FILE));
        List<BuildMessage> buildRunMessages = buildRunMessageService.readBuildMessages(new File(jobDirectory, RUN_HISTORY_FILE));
        log.info("company = {} jobNameFullName = {} buildRunMessages.size() = {}", company, jobNameDetails.getJobFullName(), buildRunMessages.size());
        if(CollectionUtils.isEmpty(buildRunMessages)){
            return null;
        }

        Instant cutOffDate = Instant.now().minus(jenkinsResultsCutoffInDays, ChronoUnit.DAYS);
        Long cutOffTimeInSeconds = cutOffDate.getEpochSecond();

        List<JobRunParamsMessage> jobRunParamsMessages = jobRunParamsService.readBuildMessages(jobDirectory);
        Map<Long, List<JobRunParam>> jobRunParamsMessagesMap = jobRunParamsMessages.stream().collect(
                Collectors.toMap(JobRunParamsMessage::getBuildNumber, x -> x,
                        (existing, replacement) -> { log.warn("Duplicate Job Run Param messages found for company {} jobFullName {}, key {}", company, jobNameDetails.getJobFullName(), existing.getBuildNumber()); return existing;})
        ).entrySet().stream().collect(
                Collectors.toMap(e-> e.getKey(), e -> e.getValue().getJobRunParams())
        );

        Map<Long, Set<CICDJobTrigger>> jobRunTriggersMap = jobRunTriggersParserService.readBuildMessages(jobDirectory);

        List<JobRunGitChangesMessage> jobRunGitChangesMessages = jobRunGitChangesService.readJobRunsGitChanges(jobDirectory);
        Map<Long, List<String>> jobRunGitChangesMessagesMap = jobRunGitChangesMessages.stream().collect(
                Collectors.toMap(JobRunGitChangesMessage::getBuildNumber, x -> x,
                        (existing, replacement) -> { log.warn("Duplicate Job Run Git Changes found for company {} jobFullName {}, key {}", company, jobNameDetails.getJobFullName(), existing.getBuildNumber()); return existing;})
        ).entrySet().stream().collect(
                Collectors.toMap(e -> e.getKey(), e-> e.getValue().getCommitIds())
        );

        List<JobRunDetails> runs = new ArrayList<>();
        for(BuildMessage buildRunMessage : buildRunMessages){
            Long buildNumber = buildRunMessage.getBuildNumber();
            Long startTime = buildRunMessage.getStartTime();
            String userId = buildRunMessage.getUserId();
            Long duration = buildRunMessage.getDuration();
            String res = buildRunMessage.getResult();

            JobRunDetails.JobRunDetailsBuilder currentJobRunDetailsBldr = JobRunDetails.builder()
                    .number(buildNumber)
                    .duration(TimeUnit.MILLISECONDS.toSeconds(duration))
                    .startTime(TimeUnit.MILLISECONDS.toSeconds(startTime))
                    .status(res)
                    .userId(userId);
            if(jobRunParamsMessagesMap.containsKey(buildNumber)){
                currentJobRunDetailsBldr.params(jobRunParamsMessagesMap.get(buildNumber));
            }
            if(jobRunTriggersMap.containsKey(buildNumber)){
                currentJobRunDetailsBldr.triggers(jobRunTriggersMap.get(buildNumber));
            }
            if(jobRunGitChangesMessagesMap.containsKey(buildNumber)){
                currentJobRunDetailsBldr.commitIds(jobRunGitChangesMessagesMap.get(buildNumber));
            }
            JobRunDetails currentJobRunDetails = currentJobRunDetailsBldr.build();

            if(currentJobRunDetails.getStartTime() >= cutOffTimeInSeconds) {
                runs.add(currentJobRunDetails);
            } else {
                log.debug("current start time {} is less than cutoff time {} discarding this data!", currentJobRunDetails.getStartTime(), cutOffTimeInSeconds);
            }
        }

        JobAllRuns.JobAllRunsBuilder bldr = JobAllRuns.builder()
                .jobName(jobNameDetails.getJobName())
                .jobFullName(jobNameDetails.getJobFullName())
                .jobNormalizedFullName(jobNameDetails.getJobNormalizedFullName())
                .branchName(jobNameDetails.getBranchName())
                .moduleName(jobNameDetails.getModuleName())
                .runs(runs);
        if(scmConfig != null){
            bldr.scmUrl(scmConfig.getUrl());
            bldr.scmUserId(scmConfig.getUserName());
        }
        JobAllRuns currentJobAllRuns = bldr.build();

        parseJobRunDetails(company, jobDirectory, jobNameDetails.getJobFullName(), jobRunDetailsMap);

        return currentJobAllRuns;
    }

    public List<JobAllRuns> parse(String company, File jobsDirectory, Map<String, Map<Long, File>> jobRunDetailsMap) throws IOException {
        log.info("JobRunAnalyticsService.computeAnalytics, jobsDirectory = {}", jobsDirectory);
        if(jobsDirectory == null){
            return Collections.emptyList();
        }
        if((!jobsDirectory.exists()) || (!jobsDirectory.isDirectory())){
            return Collections.emptyList();
        }
        List<JobAllRuns> allJobsAllRuns = new ArrayList<>();
        Queue<File> dirs = new LinkedList<>();
        dirs.offer(jobsDirectory);
        while (dirs.peek() != null){
            File currentDir = dirs.poll();
            if(!currentDir.isDirectory()){
                continue;
            }

            File[] children = currentDir.listFiles();
            if (children == null){
                continue;
            }
            for(File currentChild : children){
                if(currentChild.isDirectory()){
                    dirs.offer(currentChild);
                }
            }
            JobAllRuns currentJobAllRuns = extractAllJobRunsFromJobDirectory(company, currentDir, jobRunDetailsMap);
            if(currentJobAllRuns == null){
                continue;
            }
            allJobsAllRuns.add(currentJobAllRuns);
        }
        return allJobsAllRuns;
    }
}