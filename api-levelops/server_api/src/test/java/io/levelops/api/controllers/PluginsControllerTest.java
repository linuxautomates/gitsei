package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import io.levelops.commons.databases.services.PluginDatabaseService;
import io.levelops.commons.databases.services.TagsService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.plugins.clients.PluginsClient;
import io.levelops.plugins.models.PluginTrigger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class PluginsControllerTest {

    private MockMvc mvc;

    @Value("${PLUGINS_BUCKET_NAME:levelops-plugins}") String bucketName;

    private ObjectMapper objectMapper;

    @Mock
    private Storage storage;
    @Mock
    private PluginDatabaseService pluginDatabaseService;
    @Mock
    private PluginsClient pluginsClient;
    @Mock
    private TagsService tagsService;

    private PluginsController pluginsController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        this.objectMapper = DefaultObjectMapper.get();
        pluginsController = new PluginsController(bucketName, objectMapper, pluginDatabaseService, pluginsClient, storage, tagsService);
        mvc = MockMvcBuilders
                .standaloneSetup(pluginsController)
                .build();
    }

    @Test
    public void test() throws Exception {
        mvc.perform(get("/v1/plugins/f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--/download")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(get("/v1/plugins/f73dbe99-5318-4d0c-abaa-fe3c397e63e0/download")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test"))
                .andExpect(status().is(HttpStatus.OK.value()));

        mvc.perform(get("/v1/plugins/f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(get("/v1/plugins/f73dbe99-5318-4d0c-abaa-fe3c397e63e0")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test"))
                .andExpect(status().is(HttpStatus.OK.value()));

        mvc.perform(post("/v1/plugins/f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(PluginTrigger.builder().build())))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(post("/v1/plugins/f73dbe99-5318-4d0c-abaa-fe3c397e63e0/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test")
                .content(DefaultObjectMapper.get().writeValueAsString(PluginTrigger.builder().build())))
                .andExpect(status().is(HttpStatus.OK.value()));
    }
}
