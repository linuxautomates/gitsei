package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations.models.jenkins.JobAllRuns;
import io.levelops.commons.databases.converters.PluginResultConverters;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.database.PluginResultDTO;
import io.levelops.commons.databases.models.database.Tag;
import io.levelops.commons.databases.models.database.mappings.ComponentProductMapping;
import io.levelops.commons.databases.models.database.mappings.ComponentProductMapping.Key;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping.TagItemType;
import io.levelops.commons.databases.models.database.plugins.DbPluginResult;
import io.levelops.commons.databases.models.database.plugins.DbPluginResultLabel;
import io.levelops.commons.databases.services.ComponentProductMappingService;
import io.levelops.commons.databases.services.PluginResultsDatabaseService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TagsService;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DBMapResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.internal_api.models.PluginResultsFilter;
import io.levelops.internal_api.services.PluginResultsDiffService;
import io.levelops.internal_api.services.plugins.PluginResultsService;
import io.levelops.internal_api.services.plugins.preprocess.CsvPluginResultPreProcessService;
import io.levelops.internal_api.services.plugins.preprocess.JenkinsPluginResultPreProcessService;
import io.levelops.models.JsonDiff;
import io.levelops.plugins.models.StoredPluginResultDTO;
import io.levelops.plugins.services.PluginResultsStorageService;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.internal_api.services.plugins.PluginResultsService.CSV_TOOL;
import static io.levelops.internal_api.services.plugins.PluginResultsService.JENKINS_CONFIG_TOOL;
import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
@Log4j2
@RequestMapping("/internal/v1/tenants/{company}/plugins/results")
public class  PluginResultsController {
    private static final int TAGS_PER_ITEM_MULTIPLIER = 5;

    private final ObjectMapper objectMapper;
    private final PluginResultsService pluginResultsService;
    private final PluginResultsDatabaseService pluginResultsDatabaseService;
    private final PluginResultsStorageService pluginResultsStorageService;
    private final PluginResultsDiffService pluginResultsDiffService;
    private final TagItemDBService tagItemDbService;
    private final TagsService tagsService;
    private final ComponentProductMappingService componentProductMappingService;
    private final JenkinsPluginResultPreProcessService jenkinsPluginResultPreProcessService;
    private final CsvPluginResultPreProcessService csvPluginResultPreProcessService;

    @Autowired
    public PluginResultsController(ObjectMapper objectMapper,
                                   PluginResultsService pluginResultsService,
                                   PluginResultsDatabaseService pluginResultsDatabaseService,
                                   PluginResultsStorageService pluginResultsStorageService,
                                   PluginResultsDiffService pluginResultsDiffService,
                                   TagsService tagsService,
                                   TagItemDBService tagItemDbService,
                                   ComponentProductMappingService componentProductMappingService,
                                   JenkinsPluginResultPreProcessService jenkinsPluginResultPreProcessService,
                                   CsvPluginResultPreProcessService csvPluginResultPreProcessService) {
        this.objectMapper = objectMapper;
        this.pluginResultsService = pluginResultsService;
        this.pluginResultsDatabaseService = pluginResultsDatabaseService;
        this.pluginResultsStorageService = pluginResultsStorageService;
        this.pluginResultsDiffService = pluginResultsDiffService;
        this.tagsService = tagsService;
        this.tagItemDbService = tagItemDbService;
        this.componentProductMappingService = componentProductMappingService;
        this.jenkinsPluginResultPreProcessService = jenkinsPluginResultPreProcessService;
        this.csvPluginResultPreProcessService = csvPluginResultPreProcessService;
    }

    @GetMapping(path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<PluginResultDTO>> getById(@PathVariable("company") String company,
                                                                   @PathVariable("id") UUID id) {
        return SpringUtils.deferResponse(() -> {
            DbPluginResult dbPluginResult = pluginResultsDatabaseService.get(company, id.toString())
                    .orElseThrow(() -> new NotFoundException("Could not find Plugin result with id=" + id));
            Map<String, Object> results = pluginResultsStorageService.downloadResults(dbPluginResult.getGcsPath());
            PluginResultDTO dto = PluginResultConverters.convertToDTO(dbPluginResult, results);
            DbListResponse<Tag> tagsResponse = tagItemDbService.listTagsForItem(company, TagItemType.PLUGIN_RESULT, id.toString(), 0, 20);
            if (tagsResponse.getCount() > 0) {
                dto = dto.toBuilder().tags(tagsResponse.getRecords().stream().map(Tag::getId).collect(Collectors.toList())).build();
            }
            return ResponseEntity.ok().body(dto);
        });
    }

    private Long parseOldestJobRunStartTime(DbPluginResult dbPluginResult) {
        if(dbPluginResult.getMetadata() == null) {
            return null;
        }
        if(!dbPluginResult.getMetadata().containsKey("start_time")) {
            return null;
        }
        Integer oldestJobRunStartTime = (Integer) dbPluginResult.getMetadata().get("start_time");
        log.info("oldestJobRunStartTime {}", oldestJobRunStartTime);
        return oldestJobRunStartTime.longValue();
    }
    
    @SuppressWarnings("unchecked")
    private Long getOldestJobRunStartTimeByPluginResultId(String company, String id) throws JsonProcessingException, SQLException, NotFoundException {
        DbPluginResult dbPluginResult = pluginResultsDatabaseService.get(company, id)
                .orElseThrow(() -> new NotFoundException("Could not find Plugin result with id=" + id));
        log.info("dbPluginResult {}", dbPluginResult);

        Long oldestJobRunStartTimeFromMetadata = parseOldestJobRunStartTime(dbPluginResult);
        log.info("oldestJobRunStartTimeFromMetadata {}", oldestJobRunStartTimeFromMetadata);
        if (oldestJobRunStartTimeFromMetadata != null) {
            return oldestJobRunStartTimeFromMetadata;
        }

        Map<String, Object> results = pluginResultsStorageService.downloadResults(dbPluginResult.getGcsPath());
        log.debug("results {}", results);

        Map<String, Object> jenkinsConfig = (Map<String, Object>) results.getOrDefault("jenkins_config", Map.of());
        log.info("jenkinsConfig is empty {}", CollectionUtils.isEmpty(jenkinsConfig));
        List<JobAllRuns> jenkins = objectMapper.convertValue(jenkinsConfig.get("job_runs"),
                objectMapper.getTypeFactory().constructCollectionLikeType(ArrayList.class,
                        JobAllRuns.class));
        log.info("jenkins is empty {}", CollectionUtils.isEmpty(jenkins));
        var oldest = org.apache.commons.collections4.CollectionUtils.emptyIfNull(jenkins).stream().flatMap(job -> job.getRuns().stream())
                .mapToLong(run -> run.getStartTime()).min()
                .orElse(-1); // defaults to last 24
        log.info("oldest {}", oldest);
        Long oldestJobRunStartTime = (oldest != -1) ? oldest : null;
        log.info("oldestJobRunStartTime {}", oldestJobRunStartTime);
        return oldestJobRunStartTime;
    }


    @GetMapping(path = "/{id}/oldest_job_run_start_time", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, Object>>> getOldestJobRunStartTimeById(@PathVariable("company") String company,
                                                                                            @PathVariable("id") UUID id) {
        return SpringUtils.deferResponse(() -> {
            Long oldestJobRunStartTime = getOldestJobRunStartTimeByPluginResultId(company, id.toString());
            Map<String, Object> result = (oldestJobRunStartTime != null) ? Map.of("start_time", oldestJobRunStartTime) : Collections.emptyMap();
            return ResponseEntity.ok().body(result);
        });
    }

    @PostMapping(path = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<PluginResultDTO>>> list(@PathVariable("company") String company,
                                                                                   @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            PluginResultsFilter pluginResultsFilter = PluginResultsFilter.fromListRequest(objectMapper, filter);
            List<DbPluginResultLabel> labels = PluginResultConverters.convertLabelsFromDTO(pluginResultsFilter.getLabels());
            var resultIds = new HashSet<UUID>();
            if (!CollectionUtils.isEmpty(pluginResultsFilter.getTagIds())) {
                var results = tagItemDbService.listItemIdsForTagIds(company, TagItemType.PLUGIN_RESULT, List.copyOf(pluginResultsFilter.getTagIds()), 0, 20);
                if (results.getCount() == 0) {
                    return ResponseEntity.ok().body(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), 0, Collections.emptyList()));
                }
                resultIds.addAll(results.getRecords().stream().map(UUID::fromString).collect(Collectors.toSet()));
            }
            if (!CollectionUtils.isEmpty(pluginResultsFilter.getResultIds())) {
                resultIds.addAll(pluginResultsFilter.getResultIds());
            }
            DbListResponse<DbPluginResult> dbPluginResultDbListResponse = pluginResultsDatabaseService.filterByLabels(
                    company,
                    resultIds,
                    pluginResultsFilter.getIds(),
                    pluginResultsFilter.getVersions(),
                    pluginResultsFilter.getProductIds(),
                    pluginResultsFilter.getSuccessful(),
                    labels,
                    pluginResultsFilter.getCreatedAt().getFrom(), // null safe
                    pluginResultsFilter.getCreatedAt().getTo(), // null safe
                    filter.getPage(),
                    filter.getPageSize(),
                    filter.getSort() != null ? SortingConverter.fromFilter(filter.getSort()) : Map.of()
            );
            List<PluginResultDTO> results = dbPluginResultDbListResponse.getRecords().stream()
                    .map(dbPluginResult1 -> PluginResultConverters.convertToDTO(dbPluginResult1, null))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            DBMapResponse<String, List<Tag>> tagsResponse = tagItemDbService.listTagsForItems(
                    company,
                    results.stream()
                            .map(item -> TagItemMapping.builder().itemId(item.getId()).tagItemType(TagItemType.PLUGIN_RESULT).build())
                            .collect(Collectors.toList()),
                    0,
                    filter.getPageSize() * TAGS_PER_ITEM_MULTIPLIER);
            if (tagsResponse.getCount() > 0) {
                results = results.stream()
                        .map(item -> item.toBuilder()
                                .tags(tagsResponse.getRecords().get(item.getId() + TagItemType.PLUGIN_RESULT) == null
                                        ? Collections.emptyList()
                                        : tagsResponse.getRecords().get(item.getId() + TagItemType.PLUGIN_RESULT).stream()
                                        .map(Tag::getId).collect(Collectors.toList())).build())
                        .collect(Collectors.toList());
            }
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(filter.getPage(), filter.getPageSize(), dbPluginResultDbListResponse.getTotalCount(), results));
        });
    }

    /**
     * Updates the result associations to product, tags and/or labels
     *
     * @return
     */
    @PutMapping(path = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> updateResult(@PathVariable("company") String company,
                                                               @PathVariable("id") UUID id,
                                                               @RequestBody PluginResultDTO pluginResultDto) {
        return SpringUtils.deferResponse(() -> {
            if (!CollectionUtils.isEmpty(pluginResultDto.getTags())) {
                List<String> tagIds = tagsService.forceGetTagIds(company, pluginResultDto.getTags());
                tagItemDbService.updateItemMappings(
                        company,
                        TagItemType.PLUGIN_RESULT,
                        id.toString(),
                        tagIds.stream()
                                .map(tagId -> TagItemMapping.builder()
                                        .tagId(tagId)
                                        .itemId(id.toString())
                                        .tagItemType(TagItemType.PLUGIN_RESULT)
                                        .build()
                                )
                                .collect(Collectors.toList())
                );
            }
            if (!CollectionUtils.isEmpty(pluginResultDto.getProductIds())) {
                var newMapping = ComponentProductMapping.builder()
                        .componentId(id)
                        .componentType("plugin_result")
                        .productIds(pluginResultDto.getProductIds().stream().map(Integer::valueOf).collect(Collectors.toList()))
                        .build();
                var oldMapping = componentProductMappingService.get(company, Key.builder().componentId(id).componentType("plugin_result").build());
                if (oldMapping.isEmpty()) {
                    componentProductMappingService.insert(company, newMapping);
                } else {
                    componentProductMappingService.update(company, oldMapping.get(), newMapping);
                }
            }
            if (!CollectionUtils.isEmpty(pluginResultDto.getLabels())) {
                // TODO:
            }
            return ResponseEntity.ok().body("ok");
        });
    }

    @PostMapping(path = "/labels/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<String>>> listLabelKeys(@PathVariable("company") String company,
                                                                                   @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            List<String> labelKeys = pluginResultsDatabaseService.searchLabelKeys(company,
                    filter.getFilterValue("partial", Map.class).map(m -> (String) m.get("key")).orElse(null),
                    filter.getPage(),
                    filter.getPageSize());
            return ResponseEntity.ok().body(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), labelKeys));
        });
    }

    @PostMapping(path = "/labels/values/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<String>>> listLabelValues(@PathVariable("company") String company,
                                                                                     @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            List<String> labelKeys = pluginResultsDatabaseService.searchLabelValues(company,
                    filter.getFilterValue("key", String.class).orElse(null), // exact match
                    filter.getFilterValue("partial", Map.class).map(m -> (String) m.get("value")).orElse(null),
                    filter.getPage(), filter.getPageSize());
            return ResponseEntity.ok().body(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), labelKeys));
        });
    }

    @PostMapping(path = "/labels", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<String>>> getLabelKeys(@PathVariable("company") String company,
                                                                                  @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            List<String> labelKeys = pluginResultsDatabaseService.distinctLabelKeys(company, filter.getPage(), filter.getPageSize());
            return ResponseEntity.ok().body(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), labelKeys));
        });
    }

    @PostMapping(path = "/labels/{key}/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<String>>> getLabelValues(@PathVariable("company") String company,
                                                                                    @PathVariable("key") String key,
                                                                                    @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            List<String> labelKeys = pluginResultsDatabaseService.distinctLabelValues(company, key, filter.getPage(), filter.getPageSize());
            return ResponseEntity.ok().body(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), labelKeys));
        });
    }

    @GetMapping(path = "/diff", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, JsonDiff>>> diff(@PathVariable("company") String company,
                                                                      @RequestParam("before_id") UUID beforeId,
                                                                      @RequestParam("after_id") UUID afterId) {
        return SpringUtils.deferResponse(() -> {
            Map<String, JsonDiff> diff = pluginResultsDiffService.diffResults(company, beforeId.toString(), afterId.toString());
            return ResponseEntity.ok().body(diff);
        });
    }

    @PostMapping(produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, ?>>> createPluginResult(@PathVariable("company") String company,
                                                                             @RequestBody PluginResultDTO pluginResultDTO) {
        return SpringUtils.deferResponse(() -> {
            String resultId = pluginResultsService.createPluginResult(company, pluginResultDTO);

            return ResponseEntity.accepted().body(Map.of("id", resultId));
        });
    }

    @PostMapping(path = "/multipart", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, ?>>> createPluginResultMultiPart(@PathVariable(name = "company") String company,
                                                                                      @RequestPart("json") MultipartFile pluginResultDTOStr,
                                                                                      @RequestPart(name = "result", required = false) MultipartFile resultFile) {
        return SpringUtils.deferResponse(() -> {
            String resultId = pluginResultsService.createPluginResult(company, pluginResultDTOStr, resultFile);
            return ResponseEntity.accepted().body(Map.of("id", resultId));
        });
    }

    @PostMapping(path = "/multipart/pre-process", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, ?>>> createPluginResultMultiPartWithPreProcessing(@PathVariable(name = "company") String company,
                                                                                                       @RequestPart("json") MultipartFile jsonFile,
                                                                                                       @RequestPart(name = "result", required = false) MultipartFile resultFile) {
        return SpringUtils.deferResponse(() -> {
            UUID resultId = UUID.randomUUID();

            PluginResultDTO pluginResultDTO;
            try (InputStream inputStream = jsonFile.getInputStream()) {
                pluginResultDTO = objectMapper.readValue(inputStream, PluginResultDTO.class);
            } catch (IOException e) {
                log.error("Failed to parse JSON request! input {}", new String(jsonFile.getBytes(), UTF_8), e);
                throw new BadRequestException("Failed to parse JSON request", e);
            }
            switch (StringUtils.defaultString(pluginResultDTO.getTool())) {
                case JENKINS_CONFIG_TOOL:
                    jenkinsPluginResultPreProcessService.submitJenkinsResultsForPreProcess(company, resultId, jsonFile, resultFile, pluginResultDTO);
                    break;
                case CSV_TOOL:
                    csvPluginResultPreProcessService.preprocess(company, resultId, jsonFile, resultFile, pluginResultDTO);
                    break;
                default:
                    throw new BadRequestException("Unsupported tool: '" + pluginResultDTO.getTool() + "'");
            }
            return ResponseEntity.accepted().body(Map.of("id", resultId));
        });
    }

    @PostMapping(path = "/stored", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, ?>>> createStoredPluginResult(@PathVariable("company") String company,
                                                                                   @RequestBody StoredPluginResultDTO storedPluginResultDTO) {
        return SpringUtils.deferResponse(() -> {
            log.info("received StoredPluginResultDTO, company {}", company);
            log.debug("storedPluginResultDTO = {}", storedPluginResultDTO);
            String resultId = pluginResultsService.createPluginResult(company, storedPluginResultDTO.getPluginResult(), null, storedPluginResultDTO.getResultId(), storedPluginResultDTO.getPluginResultStoragePath());
            return ResponseEntity.accepted().body(Map.of("id", resultId));
        });
    }

    @DeleteMapping(produces = "application/json")
    public DeferredResult<ResponseEntity<BulkDeleteResponse>> deleteBulkPluginResult(@PathVariable("company") String company,
                                                                                     @RequestBody List<UUID> uuids) {
        return SpringUtils.deferResponse(() -> {
            final List<String> ids = uuids.stream().map(UUID::toString).collect(Collectors.toList());
            try {
                pluginResultsDatabaseService.deleteBulkPluginResult(company, ids);
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(ids, true, null));
            } catch (Exception e) {
                return ResponseEntity.ok(BulkDeleteResponse.createBulkDeleteResponse(ids, false, e.getMessage()));
            }
        });
    }

}
