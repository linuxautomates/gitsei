package io.levelops.internal_api.services;

import io.levelops.JsonDiffService;
import io.levelops.commons.databases.models.database.Plugin;
import io.levelops.commons.databases.models.database.plugins.DbPluginResult;
import io.levelops.commons.databases.models.database.plugins.DbPluginResultLabel;
import io.levelops.commons.databases.services.PluginDatabaseService;
import io.levelops.commons.databases.services.PluginResultsDatabaseService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.internal_api.models.PluginsSpec;
import io.levelops.models.JsonDiff;
import io.levelops.plugins.services.PluginResultsStorageService;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Log4j2
@Service
public class PluginResultsDiffService {

    private final PluginResultsDatabaseService pluginResultsDatabaseService;
    private final PluginResultsStorageService pluginResultsStorageService;
    private final PluginDatabaseService pluginService;
    private final JsonDiffService jsonDiffService;
    private final PluginsSpec pluginsSpec;

    @Autowired
    public PluginResultsDiffService(
            PluginResultsDatabaseService pluginResultsDatabaseService,
            PluginResultsStorageService pluginResultsStorageService,
            PluginDatabaseService pluginService,
            JsonDiffService jsonDiffService,
            PluginsSpec pluginsSpec) {
        this.pluginResultsDatabaseService = pluginResultsDatabaseService;
        this.pluginResultsStorageService = pluginResultsStorageService;
        this.pluginService = pluginService;
        this.jsonDiffService = jsonDiffService;
        this.pluginsSpec = pluginsSpec;
    }

    public Map<String, JsonDiff> diffLatestResults(
        String company, 
        UUID pluginId,
        Set<String> versions,
        Set<String> productIds,
        List<DbPluginResultLabel> labels) throws IOException {
        DbListResponse<DbPluginResult> dbPluginResultDbListResponse = pluginResultsDatabaseService
                .filterByLabels(company, null, Set.of(pluginId), versions, productIds, true, labels, null, null, 0, 2, null);
        if (CollectionUtils.size(dbPluginResultDbListResponse.getRecords()) != 2) {
            return null;
        }

        // results are returned by descending chronological order
        DbPluginResult currentResult = dbPluginResultDbListResponse.getRecords().get(0);
        DbPluginResult previousResult = dbPluginResultDbListResponse.getRecords().get(1);
        Optional<Plugin> plugin = pluginService.get(company, pluginId.toString());

        return diff(plugin.get().getTool(), previousResult, currentResult);
    }

    public Map<String, JsonDiff> diffResults(String company, String beforeResultId, String afterResultId) 
            throws IOException, SQLException, NotFoundException, BadRequestException {
        DbPluginResult beforeResult = pluginResultsDatabaseService.get(company, beforeResultId)
                .orElseThrow(() -> new NotFoundException("Could not find plugin result with id=" + beforeResultId));
        DbPluginResult afterResult = pluginResultsDatabaseService.get(company, afterResultId)
                .orElseThrow(() -> new NotFoundException("Could not find plugin result with id=" + afterResultId));

        if (!Objects.equals(beforeResult.getTool(), afterResult.getTool())) {
            throw new BadRequestException("Cannot diff plugin results of different tools");
        }

        return diff(beforeResult.getTool(), beforeResult, afterResult);
    }

    private Map<String, JsonDiff> diff(String tool, DbPluginResult beforeResult, DbPluginResult afterResult) 
            throws IOException {
        if (beforeResult.getCreatedAt() > afterResult.getCreatedAt()) {
            DbPluginResult tmp = afterResult;
            afterResult = beforeResult;
            beforeResult = tmp;
        }

        List<String> basePathList = pluginsSpec.getSpec(tool)
                .map(PluginsSpec.PluginSpec::getPaths)
                .orElse(null);

        log.info("Diffing results of tool={}: before={}, after={}, spec={}", tool, beforeResult.getGcsPath(), afterResult.getGcsPath(), basePathList);

        String afterData = pluginResultsStorageService.downloadResultsAsString(afterResult.getGcsPath());
        String beforeData = pluginResultsStorageService.downloadResultsAsString(beforeResult.getGcsPath());
        return jsonDiffService.diff(beforeData, afterData, basePathList);
    }

}
