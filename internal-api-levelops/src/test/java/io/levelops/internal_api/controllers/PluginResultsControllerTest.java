package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import io.levelops.commons.databases.models.database.Plugin;
import io.levelops.commons.databases.models.database.Plugin.PluginClass;
import io.levelops.commons.databases.models.database.PluginResultDTO;
import io.levelops.commons.databases.models.database.Tag;
import io.levelops.commons.databases.models.database.mappings.TagItemMapping.TagItemType;
import io.levelops.commons.databases.models.database.plugins.DbPluginResult;
import io.levelops.commons.databases.services.ComponentProductMappingService;
import io.levelops.commons.databases.services.MsTMTDatabaseService;
import io.levelops.commons.databases.services.PluginDatabaseService;
import io.levelops.commons.databases.services.PluginResultsDatabaseService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TagsService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DBMapResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.events.clients.EventsClient;
import io.levelops.internal_api.config.DefaultApiTestConfiguration;
import io.levelops.internal_api.models.PluginResultsFilter;
import io.levelops.internal_api.services.plugins.PluginResultsService;
import io.levelops.internal_api.services.plugins.preprocess.CsvPluginResultPreProcessService;
import io.levelops.internal_api.services.plugins.preprocess.JenkinsPluginResultPreProcessService;
import io.levelops.internal_api.services.MessagePubService;
import io.levelops.internal_api.services.PluginResultsDiffService;
import io.levelops.models.JsonDiff;
import io.levelops.plugins.services.PluginResultsStorageService;
import io.levelops.web.exceptions.BadRequestException;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        IntegrationsController.class,
        DefaultApiTestConfiguration.class
})
public class PluginResultsControllerTest {
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PluginResultsDatabaseService pluginResultsDatabaseService;
    @Autowired
    private PluginResultsStorageService pluginResultsStorageService;
    @Autowired
    private PluginDatabaseService pluginDatabaseService;
    @Autowired
    private PluginResultsDiffService pluginResultsDiffService;
    @Autowired
    private TagItemDBService tagItemDBService;
    @Autowired
    private TagsService tagsService;
    @Autowired
    private ComponentProductMappingService componentProductMappingService;
    @Autowired
    private EventsClient eventsClient;
    @Autowired
    private MessagePubService messagePubService;
    @Autowired
    private JenkinsPluginResultPreProcessService jenkinsPluginResultPreProcessService;
    @Autowired
    CsvPluginResultPreProcessService csvPluginResultPreProcessService;
    @Autowired
    MsTMTDatabaseService msTMTDatabaseService;

    private MockMvc mvc;

    @Before
    public void setUp() throws Exception {
        Mockito.reset(pluginDatabaseService, pluginResultsStorageService, pluginResultsDatabaseService);

        when(pluginDatabaseService.getByTool(eq("foo"), eq("report_praetorian")))
                .thenReturn(Optional.of(Plugin.builder().build()));

        when(pluginResultsDatabaseService.insert(eq("foo"), any(DbPluginResult.class)))
                .thenAnswer(ans -> ans.getArgument(1, DbPluginResult.class).getId());

        PluginResultsService pluginResultsService = new PluginResultsService(objectMapper, pluginDatabaseService, pluginResultsDatabaseService, pluginResultsStorageService, tagsService, tagItemDBService, eventsClient, msTMTDatabaseService);

        PluginResultsController pluginResultsController = new PluginResultsController(
                objectMapper, pluginResultsService, pluginResultsDatabaseService, pluginResultsStorageService,
                pluginResultsDiffService, tagsService, tagItemDBService, componentProductMappingService,
                jenkinsPluginResultPreProcessService, csvPluginResultPreProcessService);
        mvc = MockMvcBuilders.standaloneSetup(pluginResultsController).build();
    }

    @Test
    public void testMissingTool() throws Exception {
        MvcResult result = mvc.perform(
                post("/internal/v1/tenants/foo/plugins/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("company", "foo")
                        .content("{\"tool\":\"missing_tool\"}"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        mvc.perform(asyncDispatch(result))
                .andExpect(status().isBadRequest())
                .andExpect(r -> assertThat(r.getResolvedException())
                        .isExactlyInstanceOf(BadRequestException.class)
                        .hasMessageContaining("Tool does not exist: missing_tool"));

        verify(pluginResultsStorageService, never()).uploadResults(anyString(), anyString(), anyString(), anyString(), (byte[])any());
        verify(pluginResultsDatabaseService, never()).insert(anyString(), any(DbPluginResult.class));
    }

    @Test
    public void testJsonResult() throws Exception {
        when(pluginDatabaseService.getByTool(eq("foo"), eq("report_praetorian")))
            .thenReturn(Optional.of(Plugin.builder().id(UUID.randomUUID().toString()).tool("report_praetorian").pluginClass(PluginClass.REPORT_FILE).build()));
        MvcResult result = mvc.perform(
                post("/internal/v1/tenants/foo/plugins/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(PluginResultDTO.builder()
                                .tags(List.of("tag1", "tag2", "tag3"))
                                .productIds(List.of("123"))
                                .tool("report_praetorian")
                                .version("1.0")
                                .productIds(List.of("123"))
                                .results(Map.of("some", "stuff"))
                                .successful(true)
                                .build())))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        mvc.perform(asyncDispatch(result))
                .andExpect(status().is2xxSuccessful())
                .andExpect(r -> assertThat(r.getResponse().getContentAsString()).contains("{\"id\":"));

        verifyCreatePluginResult("{\"some\":\"stuff\"}");
    }

    @Test
    public void testMultipartResult() throws Exception {
        when(pluginDatabaseService.getByTool(eq("foo"), eq("report_praetorian")))
            .thenReturn(Optional.of(Plugin.builder().id(UUID.randomUUID().toString()).build()));
        MvcResult result = mvc.perform(
                multipart("/internal/v1/tenants/foo/plugins/results/multipart")
                        .file(new MockMultipartFile("result", "report.json", "application/json", "{\"some\":{\"more\":\"stuff\"}}".getBytes(StandardCharsets.UTF_8)))
                        .file("json", objectMapper.writeValueAsString(
                                PluginResultDTO.builder()
                                        .productIds(List.of("123"))
                                        .tool("report_praetorian")
                                        .version("1.0")
                                        .build()).getBytes(StandardCharsets.UTF_8))
                        .sessionAttr("company", "foo"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        mvc.perform(asyncDispatch(result))
                .andExpect(status().is2xxSuccessful())
                .andExpect(r -> assertThat(r.getResponse().getContentAsString()).contains("{\"id\":"));

        verifyCreatePluginResult("{\"some\":{\"more\":\"stuff\"}}");
    }

    @Test
    public void testMultipartResultWithoutFile() throws Exception {
        when(pluginDatabaseService.getByTool(eq("foo"), eq("report_praetorian")))
            .thenReturn(Optional.of(Plugin.builder().id(UUID.randomUUID().toString()).build()));
        MvcResult result = mvc.perform(
                multipart("/internal/v1/tenants/foo/plugins/results/multipart")
                        .file("json", objectMapper.writeValueAsString(
                                PluginResultDTO.builder()
                                        .productIds(List.of("123"))
                                        .tool("report_praetorian")
                                        .version("1.0")
                                        .results(Map.of("hello", "world"))
                                        .build()).getBytes(StandardCharsets.UTF_8))
                        .sessionAttr("company", "foo"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        mvc.perform(asyncDispatch(result))
                .andExpect(status().is2xxSuccessful())
                .andExpect(r -> assertThat(r.getResponse().getContentAsString()).contains("{\"id\":"));

        verifyCreatePluginResult("{\"hello\":\"world\"}");
    }

    private void verifyCreatePluginResult(String content) throws SQLException {
        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(pluginResultsStorageService, times(1)).uploadResults(eq("foo"), eq("report_praetorian"), anyString(), eq("application/json"), bytesCaptor.capture());
        assertThat(new String(bytesCaptor.getValue())).isEqualTo(content);

        ArgumentCaptor<DbPluginResult> pluginResultArgumentCaptor = ArgumentCaptor.forClass(DbPluginResult.class);
        verify(pluginResultsDatabaseService, times(1)).insert(eq("foo"), pluginResultArgumentCaptor.capture());
        assertThat(pluginResultArgumentCaptor.getValue().getTool()).isEqualTo("report_praetorian");
        assertThat(pluginResultArgumentCaptor.getValue().getVersion()).isEqualTo("1.0");
        assertThat(pluginResultArgumentCaptor.getValue().getProductIds()).containsExactly(123);
    }

    
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void testList() throws Exception {
        Mockito.reset(pluginResultsDatabaseService, pluginResultsStorageService);
        when(pluginResultsStorageService.downloadResults(eq("/path"))).thenReturn(Map.of("some", "data"));
        when(pluginResultsDatabaseService.filterByLabels(
                eq("foo"), 
                anySet(), 
                anySet(), 
                anySet(), 
                anySet(), 
                anyBoolean(), 
                anyList(), 
                nullable(Date.class), 
                nullable(Date.class), 
                any(Integer.class), 
                any(Integer.class), 
                any())
            )
            .thenReturn(DbListResponse.of(List.of(DbPluginResult.builder().id("123").pluginClass("my class").pluginName("My Tool").gcsPath("/path").build()), null));
        List<Tag> tags = List.of(
                Tag.builder().id("tag1").name("value1").build(), 
                Tag.builder().id("tag2").name("value2").build(), 
                Tag.builder().id("tag3").name("value3").build()
                    );
        when(tagItemDBService.listTagsForItems(
                eq("foo"), 
                anyList(), 
                anyInt(), 
                anyInt())
            ).thenReturn(DBMapResponse.of(Map.of("123PLUGIN_RESULT", tags), 3));
        when(tagItemDBService.listTagIdsForItem(eq("foo"), eq(TagItemType.PLUGIN_RESULT), anyList(), anyInt(), anyInt())).thenReturn(DbListResponse.of(List.of(Pair.of("tag1", "value1")), 1));
        PluginResultsFilter filter = PluginResultsFilter.builder()
                .labels(Map.of("a", List.of("1", "2")))
                .productIds(Set.of("abc"))
                .successful(true)
                .ids(Set.of(UUID.fromString("cdc818a0-13b4-4385-b909-9cbbf2fb450d")))
                .versions(Set.of("1.0"))
                .build();
        Map filterMap = objectMapper.convertValue(filter, Map.class);

        MvcResult result = mvc.perform(
                post("/internal/v1/tenants/foo/plugins/results/list")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(DefaultListRequest.builder()
                                .filter(filterMap)
                                .sort(List.of(Map.of("id", "created_at", "desc", true)))
                                .build())))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        mvc.perform(asyncDispatch(result))
                .andExpect(status().is2xxSuccessful())
                .andExpect(r -> {
                    String responseJson = r.getResponse().getContentAsString();
                    PaginatedResponse<PluginResultDTO> response = objectMapper.readValue(responseJson, objectMapper.getTypeFactory().constructParametricType(
                            PaginatedResponse.class, PluginResultDTO.class));
                    List<PluginResultDTO> records = response.getResponse().getRecords();
                    assertThat(records).hasSize(1);
                    assertThat(records.get(0).getId()).isEqualTo("123");
                    assertThat(records.get(0).getPluginClass()).isEqualTo("my class");
                    assertThat(records.get(0).getPluginName()).isEqualTo("My Tool");
                    assertThat(records.get(0).getResults()).isNull();
                    assertThat(records.get(0).getTags()).containsExactlyElementsOf(tags.stream().map(Tag::getId).collect(Collectors.toList()));
                });
    }

    @Test
    public void testGet() throws Exception {
        Mockito.reset(pluginResultsDatabaseService, pluginResultsStorageService);
        when(pluginResultsStorageService.downloadResults(eq("/path"))).thenReturn(Map.of("some", "data"));
        List<Tag> tags = List.of(
                        Tag.builder().id("tag1").name("value1").build(), 
                        Tag.builder().id("tag2").name("value2").build());
        when(tagItemDBService.listTagsForItems(eq("foo"), anyList(), anyInt(), anyInt()))
                .thenReturn(DBMapResponse.of(Map.of("123PLUGIN_RESULT",tags), 2));
        when(tagItemDBService.listTagsForItem(eq("foo"), eq(TagItemType.PLUGIN_RESULT), eq("f73dbe99-5318-4d0c-abaa-fe3c397e63e0"), anyInt(), anyInt()))
                .thenReturn(DbListResponse.of(tags, 2));
        when(tagItemDBService.listTagIdsForItem(eq("foo"), eq(TagItemType.PLUGIN_RESULT), anyList(), anyInt(), anyInt()))
                .thenReturn(DbListResponse.of(List.of(
                        Pair.of("tag1", "value1"),
                        Pair.of("tag2", "value2")), 2));

        when(pluginResultsDatabaseService.get(eq("foo"), eq("f73dbe99-5318-4d0c-abaa-fe3c397e63e0")))
                .thenReturn(Optional.of(DbPluginResult.builder().id("f73dbe99-5318-4d0c-abaa-fe3c397e63e0").pluginClass("my class").pluginName("My Tool").gcsPath("/path").build()));

        MvcResult result = mvc.perform(
                get("/internal/v1/tenants/foo/plugins/results/f73dbe99-5318-4d0c-abaa-fe3c397e63e0"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        mvc.perform(asyncDispatch(result))
                .andExpect(status().is2xxSuccessful())
                .andExpect(r -> {
                    String responseJson = r.getResponse().getContentAsString();
                    System.out.println(responseJson);
                    PluginResultDTO response = objectMapper.readValue(responseJson, PluginResultDTO.class);
                    assertThat(response.getId()).isEqualTo("f73dbe99-5318-4d0c-abaa-fe3c397e63e0");
                    assertThat(response.getResults()).containsEntry("some", "data");
                    assertThat(response.getPluginClass()).isEqualTo("my class");
                    assertThat(response.getPluginName()).isEqualTo("My Tool");
                    assertThat(response.getTags()).containsExactly("tag1", "tag2");
                    assertThat(response.getTags()).containsExactlyElementsOf(tags.stream().map(Tag::getId).collect(Collectors.toList()));
                });
    }

    @Test
    public void testDiff() throws Exception {
        Mockito.reset(pluginResultsDatabaseService, pluginResultsStorageService, pluginResultsDiffService);
        when(pluginResultsDiffService.diffResults(anyString(), anyString(), anyString())).thenReturn(Map.of("/path", JsonDiff.builder()
                .added(List.of("a"))
                .removed(List.of("b"))
                .changed(List.of("c"))
                .dataChanges(Map.of("a", JsonDiff.DataChange.builder()
                        .operation(JsonDiff.Operation.ADDED)
                        .before(NullNode.getInstance())
                        .after(NullNode.getInstance())
                        .build()))
                .build()));

        when(pluginResultsStorageService.downloadResults(eq("/path"))).thenReturn(Map.of("some", "data"));
        when(pluginResultsDatabaseService.filterByLabels(eq("foo"), anySet(), anySet(), anySet(), anySet(), anyBoolean(), anyList(), any(Date.class), any(Date.class), any(Integer.class), any(Integer.class), any()))
                .thenReturn(DbListResponse.of(List.of(DbPluginResult.builder().id("f73dbe99-5318-4d0c-abaa-fe3c397e63e0").gcsPath("/path").build()), null));
        PluginResultsFilter filter = PluginResultsFilter.builder()
                .labels(Map.of("a", List.of("1", "2")))
                .productIds(Set.of("abc"))
                .successful(true)
                .ids(Set.of(UUID.fromString("cdc818a0-13b4-4385-b909-9cbbf2fb450d")))
                .versions(Set.of("1.0"))
                .build();
        objectMapper.convertValue(filter, Map.class);

        MvcResult result = mvc.perform(get("/internal/v1/tenants/foo/plugins/results/diff?before_id=f73dbe99-5318-4d0c-abaa-fe3c397e63e0&after_id=8d4b8020-7f52-49f3-9daf-15688206eaaa"))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        mvc.perform(asyncDispatch(result))
                .andExpect(status().is2xxSuccessful())
                .andExpect(r -> {
                    String responseJson = r.getResponse().getContentAsString();
                    System.out.println(responseJson);
                    assertThat(responseJson).isEqualTo("{\"/path\":{\"base_path\":null,\"added\":[\"a\"],\"removed\":[\"b\"],\"changed\":[\"c\"],\"data_changes\":{\"a\":{\"operation\":\"ADDED\",\"before\":null,\"after\":null}}}}");

                });

        mvc.perform(get("/internal/v1/tenants/foo/plugins/results/diff?before_id=f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--/oldest_job_run_start_time&after_id=8d4b8020-7f52-49f3-9daf-15688206eaaa"))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void testGetById() throws Exception {
        Mockito.reset(pluginResultsDatabaseService, pluginResultsStorageService);
        when(pluginResultsStorageService.downloadResults(eq("/path"))).thenReturn(Map.of("some", "data"));
        List<Tag> tags = List.of(
                Tag.builder().id("tag1").name("value1").build(),
                Tag.builder().id("tag2").name("value2").build());
        when(tagItemDBService.listTagsForItems(eq("foo"), anyList(), anyInt(), anyInt()))
                .thenReturn(DBMapResponse.of(Map.of("123PLUGIN_RESULT",tags), 2));
        when(tagItemDBService.listTagsForItem(eq("foo"), eq(TagItemType.PLUGIN_RESULT), eq("123"), anyInt(), anyInt()))
                .thenReturn(DbListResponse.of(tags, 2));
        when(tagItemDBService.listTagIdsForItem(eq("foo"), eq(TagItemType.PLUGIN_RESULT), anyList(), anyInt(), anyInt()))
                .thenReturn(DbListResponse.of(List.of(
                        Pair.of("tag1", "value1"),
                        Pair.of("tag2", "value2")), 2));

        when(pluginResultsDatabaseService.get(eq("foo"), eq("f73dbe99-5318-4d0c-abaa-fe3c397e63e0")))
                .thenReturn(Optional.of(DbPluginResult.builder().id("f73dbe99-5318-4d0c-abaa-fe3c397e63e0").pluginClass("my class").pluginName("My Tool").gcsPath("/path").build()));

        mvc.perform(get("/internal/v1/tenants/foo/plugins/results/f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--"))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void testOldestJobStartTime() throws Exception {
        Mockito.reset(pluginResultsDatabaseService, pluginResultsStorageService);
        when(pluginResultsDatabaseService.get(eq("foo"), eq("f73dbe99-5318-4d0c-abaa-fe3c397e63e0")))
                .thenReturn(Optional.of(DbPluginResult.builder().id("f73dbe99-5318-4d0c-abaa-fe3c397e63e0").pluginClass("my class").pluginName("My Tool").gcsPath("/path").build()));

        mvc.perform(get("/internal/v1/tenants/foo/plugins/results/f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--/oldest_job_run_start_time"))
                .andExpect(status().isBadRequest())
                .andReturn();
    }

    @Test
    public void testBulkDelete() throws Exception {
        Mockito.reset(pluginResultsDatabaseService, pluginResultsStorageService);
        when(pluginResultsDatabaseService.deleteBulkPluginResult(eq("foo"), anyList()))
                .thenReturn(0);

        mvc.perform(delete("/internal/v1/tenants/foo/plugins/results")
                .content(DefaultObjectMapper.get().writeValueAsString(List.of("f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--", "8d4b8020-7f52-49f3-9daf-15688206eaaa"))))
                .andExpect(status().isBadRequest())
                .andReturn();
    }
}