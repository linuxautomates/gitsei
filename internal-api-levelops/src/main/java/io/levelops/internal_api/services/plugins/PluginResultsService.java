package io.levelops.internal_api.services.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.converters.PluginResultConverters;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.Plugin;
import io.levelops.commons.databases.models.database.PluginResultDTO;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping;
import io.levelops.commons.databases.models.database.plugins.DbPluginResult;
import io.levelops.commons.databases.models.database.plugins.MsTmtVulnerability;
import io.levelops.commons.databases.services.MsTMTDatabaseService;
import io.levelops.commons.databases.services.PluginDatabaseService;
import io.levelops.commons.databases.services.PluginResultsDatabaseService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TagsService;
import io.levelops.events.clients.EventsClient;
import io.levelops.plugins.services.PluginResultsStorageService;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.ConflictException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.threeten.bp.Instant;

import javax.annotation.Nullable;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class PluginResultsService {

    public static final String JENKINS_CONFIG_TOOL = "jenkins_config";
    public static final String CSV_TOOL = "csv";
    private final ObjectMapper objectMapper;
    private final PluginDatabaseService pluginDatabaseService;
    private final PluginResultsDatabaseService pluginResultsDatabaseService;
    private final PluginResultsStorageService pluginResultsStorageService;
    private final TagsService tagsService;
    private final TagItemDBService tagItemDbService;
    private final EventsClient eventsClient;
    private final MsTMTDatabaseService msTmtService;

    private final static Map<Plugin.PluginClass, String> CLASS_TO_EVENT_MAP = Map.of(
            Plugin.PluginClass.REPORT_FILE, "report",
            Plugin.PluginClass.MONITORING, ""
    );

    @Autowired
    public PluginResultsService(ObjectMapper objectMapper,
                                final PluginDatabaseService pluginDatabaseService,
                                final PluginResultsDatabaseService pluginResultsDatabaseService,
                                final PluginResultsStorageService pluginResultsStorageService,
                                final TagsService tagsService,
                                final TagItemDBService tagItemDbService,
                                final EventsClient eventsClient,
                                final MsTMTDatabaseService msTmtService) {
        this.objectMapper = objectMapper;
        this.pluginDatabaseService = pluginDatabaseService;
        this.pluginResultsDatabaseService = pluginResultsDatabaseService;
        this.pluginResultsStorageService = pluginResultsStorageService;
        this.tagsService = tagsService;
        this.tagItemDbService = tagItemDbService;
        this.eventsClient = eventsClient;
        this.msTmtService = msTmtService;
    }

    public String createPluginResult(String company, MultipartFile jsonFile, @Nullable MultipartFile resultFile) throws Exception {
        try (InputStream inputStream = jsonFile.getInputStream()) {
            PluginResultDTO pluginResultDTO = objectMapper.readValue(inputStream, PluginResultDTO.class);
            return createPluginResult(company, pluginResultDTO, resultFile, null, null);
        }
    }

    public String createPluginResult(String company, PluginResultDTO pluginResultDTO) throws Exception {
        return createPluginResult(company, pluginResultDTO, null, null, null);
    }

    public String createPluginResult(String company, PluginResultDTO pluginResultDTO, @Nullable MultipartFile resultFile, @Nullable UUID resultIdGuid, @Nullable String pluginResultGcsPath) throws Exception {
        resultIdGuid = MoreObjects.firstNonNull(resultIdGuid, UUID.randomUUID());
        String resultId = resultIdGuid.toString();

        if (!checkIfToolExists(company, pluginResultDTO.getTool())) {
            throw new BadRequestException("Tool does not exist: " + pluginResultDTO.getTool());
        }

        String gcsPath;
        if (StringUtils.isNotBlank(pluginResultGcsPath)) {
            log.info("pluginResultGcsPath is not empty");
            gcsPath = pluginResultGcsPath;
        } else {
            // upload content to gcs
            byte[] content;
            String contentType;
            if (resultFile != null) {
                content = resultFile.getBytes();
                contentType = resultFile.getContentType();
            } else {
                String resultString = objectMapper.writeValueAsString(pluginResultDTO.getResults());
                content = resultString.getBytes(StandardCharsets.UTF_8);
                contentType = "application/json";
            }
            gcsPath = pluginResultsStorageService.uploadResults(company, pluginResultDTO.getTool(), resultId, contentType, content);
        }

        PluginResultsInsertStatus insertStatus = insertResults(company, resultId, pluginResultDTO, gcsPath);
        String id = insertStatus.getId();
        if (id == null) {
            throw new ConflictException("Plugin Result already exists: " + resultId);
        }
        var plugin = pluginDatabaseService.getByTool(company, pluginResultDTO.getTool())
                .orElseThrow(() -> new BadRequestException("Unsupported tool: " + pluginResultDTO.getTool()));
        if (Plugin.PluginClass.REPORT_FILE == plugin.getPluginClass() || JENKINS_CONFIG_TOOL.equalsIgnoreCase(plugin.getTool()) || CSV_TOOL.equalsIgnoreCase(plugin.getTool())) {
            EventType eventType = null;
            if (CSV_TOOL.equalsIgnoreCase(plugin.getTool())) {
                eventType = EventType.CSV_CREATED;
            } else if (Plugin.PluginClass.REPORT_FILE == plugin.getPluginClass()) {
                var classToEventReplacement = CLASS_TO_EVENT_MAP.get(plugin.getPluginClass());
                var subject = plugin.getTool().replace(classToEventReplacement + "_", "");
                eventType = EventType.fromString(MessageFormat.format("{1}_{0}_created", classToEventReplacement, subject));
            } else if (JENKINS_CONFIG_TOOL.equalsIgnoreCase(plugin.getTool())) {
                eventType = EventType.JENKINS_CONFIG_CREATED;
            }
            try {
                eventsClient.emitEvent(company, eventType,
                        Map.of(
                                "id", id,
                                "products", ListUtils.emptyIfNull(pluginResultDTO.getProductIds()),
                                "tags", ListUtils.emptyIfNull(insertStatus.getTagIds()),
                                "successful", BooleanUtils.isNotFalse(pluginResultDTO.getSuccessful())
                        ));
            } catch (NullPointerException e) {
                log.error("Couldn't emit the event for the tool '{}', event type '{}' and of plugin type: {}", pluginResultDTO.getTool(), eventType, plugin, e);
            }
        }
        return resultId;
    }

    private boolean checkIfToolExists(String company, String tool) {
        if (Strings.isEmpty(tool)) {
            return false;
        }
        return pluginDatabaseService.getByTool(company, tool).isPresent();
    }

    @Value
    @Builder(toBuilder = true)
    public static class PluginResultsInsertStatus {
        String id;
        List<String> tagIds;
    }

    @SuppressWarnings("unchecked")
    private PluginResultsInsertStatus insertResults(String company, String resultId, PluginResultDTO pluginResultDTO, String gcsPath) throws Exception {
        // persist to database
        try {
            Plugin plugin = pluginDatabaseService.getByTool(company, pluginResultDTO.getTool())
                    .orElseThrow(() -> new BadRequestException("Tool does not exist: " + pluginResultDTO.getTool()));
            DbPluginResult pluginResult = PluginResultConverters.convertFromDTO(pluginResultDTO).toBuilder()
                    .id(resultId)
                    .pluginId(UUID.fromString(plugin.getId()))
                    .gcsPath(gcsPath)
                    .build();
            List<String> tagIds = List.of();
            if (CollectionUtils.isNotEmpty(pluginResultDTO.getTags())) {
                tagIds = tagsService.forceGetTagIds(company, pluginResultDTO.getTags());
                List<TagItemMapping> mappings = tagIds.stream()
                        .map(tagId -> TagItemMapping.builder()
                                .tagItemType(TagItemMapping.TagItemType.PLUGIN_RESULT)
                                .itemId(resultId)
                                .tagId(tagId)
                                .build())
                        .collect(Collectors.toList());
                tagItemDbService.batchInsert(company, mappings);
            }
            String id = pluginResultsDatabaseService.insert(company, pluginResult);
            if ("report_ms_tmt".equalsIgnoreCase(pluginResultDTO.getTool())) {
                var meta = (Map<String, String>) pluginResultDTO.getResults().get("metadata");
                var data = (Map<String, Map<String, Object>>) pluginResultDTO.getResults().get("data");
                data.entrySet().forEach(entry -> {
                    var issueData = new HashMap<String, Object>();
                    issueData.putAll(entry.getValue());
                    var item = MsTmtVulnerability.builder()
                        .pluginResultId(UUID.fromString(id))
                        .model(meta.get("threat_model_name"))
                        .owner(meta.get("owner"))
                        .createdAt(meta.get("created_at"))
                        .ingestedAt(Long.valueOf(Instant.now().getEpochSecond()).intValue())
                        .name((String) issueData.remove("summary"))
                        .description((String) issueData.remove("description"))
                        .priority((String) issueData.remove("priority"))
                        .category((String) issueData.remove("category"))
                        .state((String) issueData.remove("state"))
                        .extraData(issueData)
                        .build();
                    try {
                        msTmtService.insert(company, item);
                    } catch (SQLException e) {
                        log.error("Unable to persist the ms tmt issue: {}", item, e);
                    }
                });
            }
            return PluginResultsInsertStatus.builder()
                    .id(id)
                    .tagIds(tagIds)
                    .build();
        } catch (Exception e) {
            // rollback upload to gcs
            pluginResultsStorageService.deleteResults(gcsPath);
            throw e;
        }
    }

}
