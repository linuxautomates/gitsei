package io.levelops.internal_api.services.plugins;

import io.levelops.commons.databases.models.database.Plugin;
import io.levelops.commons.databases.models.database.PluginResultDTO;
import io.levelops.commons.databases.models.database.plugins.DbPluginResult;
import io.levelops.commons.databases.services.MsTMTDatabaseService;
import io.levelops.commons.databases.services.PluginDatabaseService;
import io.levelops.commons.databases.services.PluginResultsDatabaseService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TagsService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.events.clients.EventsClient;
import io.levelops.internal_api.config.DefaultApiTestConfiguration;
import io.levelops.plugins.services.PluginResultsStorageService;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class PluginResultsServiceTest {
    @Autowired
    private PluginResultsService pluginResultsService;
    @Autowired
    private PluginDatabaseService pluginDatabaseService;
    @Autowired
    private PluginResultsDatabaseService pluginResultsDbService;
    @Autowired
    private PluginResultsStorageService pluginResultsStorageService;
    @Autowired
    private TagsService tagService;
    @Autowired
    private TagItemDBService tagItemService;
    @Autowired
    private EventsClient eventsclient;
    @Autowired
    private MsTMTDatabaseService msTMTDatabaseService;

    private static final String company = "test";

    @Test
    public void test() throws Exception {
        pluginResultsService = new PluginResultsService(DefaultObjectMapper.get(), pluginDatabaseService, pluginResultsDbService, pluginResultsStorageService, tagService, tagItemService, eventsclient, msTMTDatabaseService);
        when(pluginDatabaseService.getByTool(anyString(), anyString())).thenReturn(Optional.of(Plugin.builder().id(UUID.randomUUID().toString()).build()));
        when(pluginResultsDbService.insert(anyString(), any(DbPluginResult.class))).thenReturn(UUID.randomUUID().toString());
        var results = ResourceUtils.getResourceAsObject("plugins/ms_tmt_response.json", PluginResultDTO.class);
        var data = (Map<String, Map<String, Object>>) results.getResults().get("data");
        var id = pluginResultsService.createPluginResult(company, results);

        Assertions.assertThat(id).isNotNull();
    }
}
