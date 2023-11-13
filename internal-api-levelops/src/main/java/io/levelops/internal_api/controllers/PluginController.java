package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.database.Plugin;
import io.levelops.commons.databases.services.PluginDatabaseService;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

@RestController

@RequestMapping("/internal/v1/tenants/{company}/plugins")
public class PluginController {

    private final PluginDatabaseService pluginDatabaseService;

    @Autowired
    public PluginController(PluginDatabaseService pluginDatabaseService) {
        this.pluginDatabaseService = pluginDatabaseService;
    }

    @GetMapping(path = "/tools/{tool}", produces = "application/json")
    public DeferredResult<ResponseEntity<Plugin>> getById(@PathVariable("company") String company,
                                                          @PathVariable("tool") String tool) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(pluginDatabaseService.getByTool(company, tool)
                .orElseThrow(() -> new NotFoundException("Plugin not found for tool: " + tool))));
    }

}
