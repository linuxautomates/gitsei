package io.levelops.aggregations.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations.models.JobRunParamsMessage;

import io.levelops.aggregations.models.jenkins.JobRunParam;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static io.levelops.aggregations.services.PluginResultPreProcessorService.RUN_PARAMS_HISTORY_FILE;

@Log4j2
@Service
public class JobRunParamsService {
    public static final Pattern PARAM_TYPE_REGEX = Pattern.compile("\\((.*)\\)(.*)", Pattern.DOTALL);
    private final ObjectMapper mapper;

    public JobRunParamsService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public List<JobRunParamsMessage> readBuildMessages(File jobsDataDirectory) throws IOException {
        File jobRunParamsFile = Paths.get(jobsDataDirectory.getAbsolutePath(), RUN_PARAMS_HISTORY_FILE).toFile();
        if(!jobRunParamsFile.exists()){
            log.info("JobRunParamsService jobRunParamsFile does not exist!! jobRunParamsFile = " + jobRunParamsFile.getAbsolutePath());
            return Collections.emptyList();
        }

        List<JobRunParamsMessage> jobRunsParams = new ArrayList<>();
        List<String> messagesStrings = Files.readAllLines(jobRunParamsFile.toPath(), StandardCharsets.UTF_8);
        log.info("JobRunParamsService messagesStrings.size() = " + messagesStrings.size());
        for(String messageString : messagesStrings){
            String[] build = messageString.split(",");
            if((build == null) || (build.length != 2)){
                log.info("Current jobParamMessageLine is not valid!! messageString = " + messageString);
                continue;
            }
            Long buildNumber = Long.parseLong(build[0]);
            String decodedJson = URLDecoder.decode(build[1], StandardCharsets.UTF_8);
            List<JobRunParam> currentJobParams = mapper.readValue(decodedJson, mapper.getTypeFactory().constructCollectionType(List.class, JobRunParam.class));
            jobRunsParams.add(JobRunParamsMessage.builder().buildNumber(buildNumber).jobRunParams(currentJobParams).build());
        }
        Collections.sort(jobRunsParams);
        return jobRunsParams;
    }
}