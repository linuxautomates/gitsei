package io.levelops.aggregations.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDJobTrigger;
import io.levelops.commons.jackson.DefaultObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import static io.levelops.aggregations.services.PluginResultPreProcessorService.RUN_TRIGGER_FILE;

@Log4j2
@Service
public class JobRunTriggersParserService {
    public static final Pattern PARAM_TYPE_REGEX = Pattern.compile("\\((.*)\\)(.*)", Pattern.DOTALL);
    private final ObjectMapper mapper;

    public JobRunTriggersParserService(ObjectMapper mapper) {
        this.mapper = DefaultObjectMapper.get();
    }

    public Map<Long, Set<CICDJobTrigger>> readBuildMessages(File jobsDataDirectory) throws IOException {
        File dataFile = Paths.get(jobsDataDirectory.getAbsolutePath(), RUN_TRIGGER_FILE).toFile();
        if(!dataFile.exists()){
            log.info("JobRunTriggersParserService data file does not exist!! data file={}", dataFile.getAbsolutePath());
            return Collections.emptyMap();
        }

        List<String> dataLines = Files.readAllLines(dataFile.toPath(), StandardCharsets.UTF_8);
        log.info("JobRunTriggersParserService data lines = {}", dataLines.size());
        Map<Long, Set<CICDJobTrigger>> triggers = new HashMap<>();
        for(String entry : dataLines){
            String[] data = entry.split(",", 2);
            if((data == null) || (data.length != 2)){
                log.info("Unexpected number of values for the entry: {}", entry);
                continue;
            }
            Long buildNumber = Long.parseLong(data[0]);
            String decodedJson = URLDecoder.decode(data[1], StandardCharsets.UTF_8);
            Set<CICDJobTrigger> allTriggers = mapper.readValue(decodedJson, mapper.getTypeFactory().constructCollectionType(Set.class, CICDJobTrigger.class));

            triggers.put(buildNumber, allTriggers);
        }
        return triggers;
    }
}