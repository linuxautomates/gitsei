package io.levelops.internal_api.services;

import io.levelops.JsonDiffService;
import io.levelops.commons.databases.models.database.plugins.DbPluginResult;
import io.levelops.commons.databases.services.PluginDatabaseService;
import io.levelops.commons.databases.services.PluginResultsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.internal_api.models.PluginsSpec;
import io.levelops.models.JsonDiff;
import io.levelops.plugins.services.PluginResultsStorageService;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class PluginResultsDiffServiceTest {

    @Mock
    PluginResultsDatabaseService pluginResultsDatabaseService;
    @Mock
    PluginDatabaseService pluginService;
    @Mock
    PluginResultsStorageService pluginResultsStorageService;

    PluginResultsDiffService pluginResultsDiffService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        JsonDiffService jsonDiffService = new JsonDiffService(DefaultObjectMapper.get());
        PluginsSpec pluginsSpec = PluginsSpec.builder()
                .plugins(Map.of(
                        "my_tool", PluginsSpec.PluginSpec.builder()
                                .tool("my_tool")
                                .paths(List.of("/field1/", "/field2/", "/field3/"))
                                .build()))
                .build();
        pluginResultsDiffService = new PluginResultsDiffService(
            pluginResultsDatabaseService,
            pluginResultsStorageService,
            pluginService,
            jsonDiffService,
            pluginsSpec);
    }

    @Test
    public void diffResultsMissing() throws SQLException, BadRequestException, NotFoundException, IOException {
        when(pluginResultsDatabaseService.get(eq("foo"), eq("1"))).thenReturn(Optional.empty());
        when(pluginResultsDatabaseService.get(eq("foo"), eq("2"))).thenReturn(Optional.empty());
        assertThatThrownBy(() -> pluginResultsDiffService.diffResults("foo", "1", "2"))
                .isExactlyInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> pluginResultsDiffService.diffResults("foo", "2", "1"))
                .isExactlyInstanceOf(NotFoundException.class);
    }

    @Test
    public void diffResultsWrongTool() throws SQLException, BadRequestException, NotFoundException, IOException {
        when(pluginResultsDatabaseService.get(eq("foo"), eq("1"))).thenReturn(Optional.of(DbPluginResult.builder()
                .tool("a")
                .build()));
        when(pluginResultsDatabaseService.get(eq("foo"), eq("2"))).thenReturn(Optional.of(DbPluginResult.builder()
                .tool("b")
                .build()));
        assertThatThrownBy(() -> pluginResultsDiffService.diffResults("foo", "1", "2"))
                .isExactlyInstanceOf(BadRequestException.class);
    }

    @Test
    public void diffResultsNoData() throws SQLException, BadRequestException, NotFoundException, IOException {
        when(pluginResultsDatabaseService.get(eq("foo"), eq("1"))).thenReturn(Optional.of(DbPluginResult.builder()
                .tool("a")
                .createdAt(10L)
                .gcsPath("gcsPath1")
                .build()));
        when(pluginResultsDatabaseService.get(eq("foo"), eq("2"))).thenReturn(Optional.of(DbPluginResult.builder()
                .tool("a")
                .gcsPath("gcsPath2")
                .createdAt(20L)
                .build()));
        when(pluginResultsStorageService.downloadResultsAsString("gcsPath1")).thenReturn(null);
        when(pluginResultsStorageService.downloadResultsAsString("gcsPath2")).thenReturn(null);

        Map<String, JsonDiff> m = pluginResultsDiffService.diffResults("foo", "1", "2");
        assertThat(m).containsKey("/");
    }

    @Test
    public void diffResults() throws SQLException, BadRequestException, NotFoundException, IOException {
        when(pluginResultsDatabaseService.get(eq("foo"), eq("1"))).thenReturn(Optional.of(DbPluginResult.builder()
                .tool("my_tool")
                .createdAt(10L)
                .gcsPath("gcsPath1")
                .build()));
        when(pluginResultsDatabaseService.get(eq("foo"), eq("2"))).thenReturn(Optional.of(DbPluginResult.builder()
                .tool("my_tool")
                .gcsPath("gcsPath2")
                .createdAt(20L)
                .build()));
        when(pluginResultsStorageService.downloadResultsAsString("gcsPath1")).thenReturn("{\"field1\": {\"some\":\"data\"}, \"field2\": {}, \"field3\": {\"some\":\"data\"}}");
        when(pluginResultsStorageService.downloadResultsAsString("gcsPath2")).thenReturn("{\"field1\": {}, \"field2\":{\"new\":\"data\"}, \"field3\": {\"some\":\"changed\"}}");

        Map<String, JsonDiff> m = pluginResultsDiffService.diffResults("foo", "1", "2");
        DefaultObjectMapper.prettyPrint(m);
        assertThat(m).containsKey("/field1/");
        assertThat(m).containsKey("/field2/");
        assertThat(m).containsKey("/field3/");
    }
}