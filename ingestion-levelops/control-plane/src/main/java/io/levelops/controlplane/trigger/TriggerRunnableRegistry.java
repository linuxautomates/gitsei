package io.levelops.controlplane.trigger;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class TriggerRunnableRegistry {

    Map<String, TriggerRunnable> typeMap;

    @Autowired
    public TriggerRunnableRegistry(List<TriggerRunnable> triggerRunnables) {
        typeMap = new HashMap<>();
        triggerRunnables.forEach(triggerRunnable -> typeMap.put(triggerRunnable.getTriggerType(), triggerRunnable));
    }

    public Optional<TriggerRunnable> get(String triggerType) {
        if (StringUtils.isEmpty(triggerType)) {
            return Optional.empty();
        }
        return Optional.ofNullable(typeMap.get(triggerType));
    }

}
