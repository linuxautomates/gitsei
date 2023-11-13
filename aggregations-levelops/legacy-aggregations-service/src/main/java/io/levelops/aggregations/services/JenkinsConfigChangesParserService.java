package io.levelops.aggregations.services;

import io.levelops.aggregations.models.jenkins.JobAllConfigChanges;
import io.levelops.aggregations.models.jenkins.JobConfigChangeDetail;
import io.levelops.aggregations.models.jenkins.JobNameDetails;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import static io.levelops.aggregations.services.PluginResultPreProcessorService.CONF_HISTORY_FILE;

@Log4j2
@Service
public class JenkinsConfigChangesParserService {
    private final JobFullNameDetailsService jobFullNameDetailsService;
    private final int jenkinsResultsCutoffInDays;

    @Autowired
    public JenkinsConfigChangesParserService(JobFullNameDetailsService jobFullNameDetailsService, @Value("${JENKINS_RESULTS_CUTOFF_IN_DAYS:7}") int jenkinsResultsCutoffInDays) {
        this.jobFullNameDetailsService = jobFullNameDetailsService;
        this.jenkinsResultsCutoffInDays = jenkinsResultsCutoffInDays;
    }

    private JobAllConfigChanges extractAllConfigChangesFromJobDirectory(File jobDirectory){
        Instant cutOffDate = Instant.now().minus(jenkinsResultsCutoffInDays, ChronoUnit.DAYS);
        Long cutOffTimeInSeconds = cutOffDate.getEpochSecond();

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

        File configChangeHistoryFile = new File(jobDirectory, CONF_HISTORY_FILE);
        if(!configChangeHistoryFile.exists()){
            return null;
        }
        List<String> configChanges = null;
        try {
            configChanges = Files.readAllLines(configChangeHistoryFile.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error reading configChangeHistoryFile {}",configChangeHistoryFile, e);
            return null;
        }
        if(CollectionUtils.isEmpty(configChanges)){
            return null;
        }
        List<JobConfigChangeDetail> configChangeDetails = new ArrayList<>();
        for(String configChange : configChanges){
            String[] parts = configChange.split(",");
            if((parts == null) || (parts.length != 4)){
                continue;
            }

            Long changeTime = Long.parseLong(parts[0]);
            String operation = parts[1];
            String userId = parts[2];
            String userFullName = parts[3];
            JobConfigChangeDetail jobConfigChangeDetail = JobConfigChangeDetail.builder()
                    .changeTime(TimeUnit.MILLISECONDS.toSeconds(changeTime)).operation(operation.toLowerCase()).userId(userId).userFullName(userFullName).build();
            if(jobConfigChangeDetail.getChangeTime() >= cutOffTimeInSeconds) {
                configChangeDetails.add(jobConfigChangeDetail);
            } else {
                log.debug("current change time {} is less than cutoff time {} discarding this data!", jobConfigChangeDetail.getChangeTime(), cutOffTimeInSeconds);
            }
        }
        JobAllConfigChanges jobAllConfigChanges = JobAllConfigChanges.builder()
                .jobName(jobNameDetails.getJobName())
                .jobFullName(jobNameDetails.getJobFullName())
                .jobNormalizedFullName(jobNameDetails.getJobNormalizedFullName())
                .branchName(jobNameDetails.getBranchName())
                .moduleName(jobNameDetails.getModuleName())
                .configChangeDetails(configChangeDetails).build();
        return jobAllConfigChanges;
    }

    public List<JobAllConfigChanges> parse(File jobsDirectory) throws IOException {
        log.debug("JenkinsConfigChangesParserService.parse");
        if(jobsDirectory == null){
            return Collections.emptyList();
        }
        if((!jobsDirectory.exists()) || (!jobsDirectory.isDirectory())){
            return Collections.emptyList();
        }
        List<JobAllConfigChanges> allJobsAllConfigChanges = new ArrayList<>();
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
            JobAllConfigChanges jobAllConfigChanges = extractAllConfigChangesFromJobDirectory(currentDir);
            if(jobAllConfigChanges == null){
                continue;
            }
            allJobsAllConfigChanges.add(jobAllConfigChanges);
        }
        return allJobsAllConfigChanges;
    }
}