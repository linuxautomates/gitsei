package io.levelops.ingestion.agent.controllers;

import io.levelops.commons.models.AgentResponse;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.agent.model.Entity;
import io.levelops.ingestion.agent.services.ResponseFactory;
import io.levelops.ingestion.engine.IngestionEngine;
import io.levelops.web.exceptions.NotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/entities")
public class EntityController {

    private IngestionEngine ingestionEngine;
    private ResponseFactory responseFactory;

    @Autowired
    public EntityController(IngestionEngine ingestionEngine,
                            ResponseFactory responseFactory) {
        this.ingestionEngine = ingestionEngine;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    @ResponseBody
    public AgentResponse<ListResponse<Entity>> getEntities(@RequestParam(name = "component_type", required = false) String componentType) {
        return responseFactory.build(ListResponse.<Entity>builder()
                .records(ingestionEngine.getEntitiesByComponentType(componentType).stream()
                        .map(Entity::fromEngineEntity)
                        .collect(Collectors.toList()))
                .build());
    }

    @GetMapping("/{id}")
    @ResponseBody
    public AgentResponse<Entity> getEntityById(@PathVariable String id) throws NotFoundException {
        return ingestionEngine.getEntityById(id)
                .map(Entity::fromEngineEntity)
                .map(responseFactory::build)
                .orElseThrow(NotFoundException::new);
    }
}
