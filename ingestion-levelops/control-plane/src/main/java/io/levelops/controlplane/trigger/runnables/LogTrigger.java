package io.levelops.controlplane.trigger.runnables;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.trigger.TriggerRunnable;
import io.levelops.controlplane.trigger.TriggerType;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class LogTrigger implements TriggerRunnable {

    @Autowired
    public LogTrigger() {
    }

    @Override
    public void run(DbTrigger trigger) throws JsonProcessingException {
        log.info("[LOG_TRIGGER] trigger={}, metadata={}", trigger.getId(), trigger.getMetadata());
    }

    @Override
    public String getTriggerType() {
        return "log";
    }
}
