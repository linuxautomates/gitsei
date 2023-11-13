package io.levelops.controlplane.controllers;

import io.levelops.controlplane.services.TriggerSchedulingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/v1/scheduling")
public class TriggerSchedulingController {

    private final TriggerSchedulingService triggerSchedulingService;

    @Autowired
    public TriggerSchedulingController(TriggerSchedulingService triggerSchedulingService) {
        this.triggerSchedulingService = triggerSchedulingService;
    }

    @GetMapping("/enable")
    public void enableSchedling() {
        triggerSchedulingService.enableScheduling(true);
    }

    @GetMapping("/disable")
    public void disableSchedling() {
        triggerSchedulingService.enableScheduling(false);
    }

    @GetMapping("/enabled")
    public Map<String, Boolean> schedulingEnabled() {
        return Map.of("scheduling_enabled", triggerSchedulingService.isSchedulingEnabled());
    }
}
