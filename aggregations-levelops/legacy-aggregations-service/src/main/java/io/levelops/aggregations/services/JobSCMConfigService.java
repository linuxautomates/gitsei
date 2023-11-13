package io.levelops.aggregations.services;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

// /var/jenkins-home/levelops/jobs/Pipe1/run-history.txt
@SuppressWarnings("unused")
@Log4j2
@Service
public class JobSCMConfigService {
    @JsonProperty("object_mapper")
    private final ObjectMapper objectMapper;

    @Autowired
    public JobSCMConfigService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }


    public SCMConfig readSCMConfig(File scmConfigFile) throws IOException {
        if(!scmConfigFile.exists()){
            return null;
        }
        String data = Files.readString(scmConfigFile.toPath());
        SCMConfig scmConfig = objectMapper.readValue(data, SCMConfig.class);
        return scmConfig;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder(toBuilder = true)
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SCMConfig{
        @JsonProperty("url")
        private String url;
        @JsonProperty("user_name")
        private String userName;
    }

}