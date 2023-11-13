package io.levelops.aggregations.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations.models.jenkins.JobNameDetails;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static io.levelops.aggregations.services.PluginResultPreProcessorService.JOB_FULL_NAME_FILE;

@Log4j2
@Service
public class JobFullNameDetailsService {
    private final ObjectMapper objectMapper;

    @Autowired
    public JobFullNameDetailsService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JobNameDetails parseJobFullNameDetails(final File jobDirectory){
        if(jobDirectory == null){
            return null;
        }
        if(!jobDirectory.exists()){
            return null;
        }
        final JobNameDetails jobNameDetails;
        File jobFullNameFile = new File(jobDirectory, JOB_FULL_NAME_FILE);
        if(!jobFullNameFile.exists()){
            jobNameDetails = JobNameDetails.builder().jobName(jobDirectory.getName()).jobFullName(jobDirectory.getName()).jobNormalizedFullName(null).branchName(null).moduleName(null).build();
            log.debug("File does not exist jobNameDetails = {}", jobNameDetails);
        } else {
            String jobFullNameData = null;
            try {
                jobFullNameData = Files.readString(jobFullNameFile.toPath());
            } catch (IOException e) {
                log.error("Error reading from jobFullNameFile {}", jobFullNameFile, e);
                return null;
            }
            log.debug("jobFullNameData = {}", jobFullNameData);
            try {
                jobNameDetails = objectMapper.readValue(jobFullNameData, JobNameDetails.class);
                log.debug("jobNameDetails = {}", jobNameDetails);
            } catch (JsonProcessingException e) {
                log.error("Error deserializing JobNameDetails", e);
                return null;
            }
        }
        return jobNameDetails;
    }
}