package io.levelops.aggregations.services;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.levelops.aggregations.services.PluginResultPreProcessorService.JENKINS_INSTANCE_GUID_FILE;
import static io.levelops.aggregations.services.PluginResultPreProcessorService.JENKINS_INSTANCE_NAME_FILE;
import static io.levelops.aggregations.services.PluginResultPreProcessorService.JENKINS_INSTANCE_URL_FILE;

@Log4j2
@Service
public class JenkinsInstanceInfoParserService {
    public static final String JENKINS_INSTANCE_GUID = "JENKINS_INSTANCE_GUID";
    public static final String JENKINS_INSTANCE_NAME = "JENKINS_INSTANCE_NAME";
    public static final String JENKINS_INSTANCE_URL = "JENKINS_INSTANCE_URL";
    public JenkinsInstanceInfoParserService() {
    }

    public Map<String, String> parse(File dataDirectory) {
        log.debug("JenkinsInstanceGuidAndNameParserService.parse");
        if ((dataDirectory == null) || (!dataDirectory.exists())){
            log.debug("Data Directory is null or empty!!");
            return Collections.emptyMap();
        }

        String jenkinsInstanceGuid = null;
        File jenkinsInstanceGuidFile = new File(dataDirectory, JENKINS_INSTANCE_GUID_FILE);
        if((!jenkinsInstanceGuidFile.exists()) || (!jenkinsInstanceGuidFile.isFile())){
            log.warn("cannot read jenkinsInstanceGuid, jenkinsInstanceGuidFile is not a file are does not exist! {}", jenkinsInstanceGuidFile);
        } else {
            try {
                jenkinsInstanceGuid = Files.readString(jenkinsInstanceGuidFile.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.warn("cannot read jenkinsInstanceGuid", e);
            }
        }
        log.debug("jenkinsInstanceGuid = {}", jenkinsInstanceGuid);

        String jenkinsInstanceName = null;
        File jenkinsInstanceNameFile = new File(dataDirectory, JENKINS_INSTANCE_NAME_FILE);
        if((!jenkinsInstanceNameFile.exists()) || (!jenkinsInstanceNameFile.isFile())){
            log.warn("cannot read jenkinsInstanceName, jenkinsInstanceNameFile is not a file are does not exist! {}", jenkinsInstanceNameFile);
        } else {
            try {
                jenkinsInstanceName = Files.readString(jenkinsInstanceNameFile.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.warn("cannot read jenkinsInstanceName", e);
            }
        }
        log.debug("jenkinsInstanceName = {}", jenkinsInstanceName);

        String jenkinsInstanceUrl = null;
        File jenkinsInstanceUrlFile = new File(dataDirectory, JENKINS_INSTANCE_URL_FILE);
        if((!jenkinsInstanceUrlFile.exists()) || (!jenkinsInstanceUrlFile.isFile())){
            log.warn("cannot read jenkinsInstanceUrl, jenkinsInstanceUrlFile is not a file are does not exist! {}", jenkinsInstanceUrlFile);
        } else {
            try {
                jenkinsInstanceUrl = Files.readString(jenkinsInstanceUrlFile.toPath(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.warn("cannot read jenkinsInstanceUrl", e);
            }
        }
        log.debug("jenkinsInstanceUrl = {}", jenkinsInstanceUrl);

        Map<String, String> results = new HashMap<>();
        results.put(JENKINS_INSTANCE_GUID, jenkinsInstanceGuid);
        results.put(JENKINS_INSTANCE_NAME, jenkinsInstanceName);
        results.put(JENKINS_INSTANCE_URL, jenkinsInstanceUrl);
        return results;
    }
}