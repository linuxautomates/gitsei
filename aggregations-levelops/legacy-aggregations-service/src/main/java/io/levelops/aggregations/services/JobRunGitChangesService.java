package io.levelops.aggregations.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations.models.JobRunGitChangesMessage;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.levelops.aggregations.services.PluginResultPreProcessorService.RUN_GIT_CHANGES_HISTORY_FILE;

@Log4j2
@Service
public class JobRunGitChangesService {
    private final ObjectMapper mapper;

    @Autowired
    public JobRunGitChangesService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public List<JobRunGitChangesMessage> readJobRunsGitChanges(File jobsDataDirectory) throws IOException {
        File jobRunGitChangesFile = Paths.get(jobsDataDirectory.getAbsolutePath(), RUN_GIT_CHANGES_HISTORY_FILE).toFile();
        if(!jobRunGitChangesFile.exists()){
            log.debug("JobRunGitChangesService jobRunGitChangesFile does not exist!! jobRunGitChangesFile = " + jobRunGitChangesFile.getAbsolutePath());
            return Collections.emptyList();
        }

        List<JobRunGitChangesMessage> jobRunsGitChanges = new ArrayList<>();
        List<String> messagesStrings = Files.readAllLines(jobRunGitChangesFile.toPath(), StandardCharsets.UTF_8);
        log.debug("JobRunGitChangesService messagesStrings.size() = " + messagesStrings.size());
        for(String messageString : messagesStrings){
            JobRunGitChangesMessage currentJobRunGitChanges = mapper.readValue(messageString, JobRunGitChangesMessage.class);
            jobRunsGitChanges.add(currentJobRunGitChanges);
        }
        Collections.sort(jobRunsGitChanges);
        return jobRunsGitChanges;
    }
}