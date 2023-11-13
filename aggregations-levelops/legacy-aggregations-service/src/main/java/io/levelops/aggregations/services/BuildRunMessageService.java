package io.levelops.aggregations.services;

import io.levelops.aggregations.models.BuildMessage;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// /var/jenkins-home/levelops/jobs/Pipe1/run-history.txt
@SuppressWarnings("unused")
@Log4j2
@Service
public class BuildRunMessageService {
    public BuildRunMessageService() {
    }

    public List<BuildMessage> readBuildMessages(File buildRunMessageFile) throws IOException {
        if(!buildRunMessageFile.exists()){
            return Collections.emptyList();
        }
        List<String> messagesStrings = Files.readAllLines(buildRunMessageFile.toPath(), StandardCharsets.UTF_8);
        List<BuildMessage> buildMessages = new ArrayList<>();
        for(String messageString : messagesStrings){
            String[] build = messageString.split(",");
            if((build == null) || (build.length != 5)){
                continue;
            }
            buildMessages.add(new BuildMessage(Long.parseLong(build[0]),
                    Long.parseLong(build[1]), Long.parseLong(build[2]), build[3],build[4]));
        }
        Collections.sort(buildMessages);
        return buildMessages;
    }
}