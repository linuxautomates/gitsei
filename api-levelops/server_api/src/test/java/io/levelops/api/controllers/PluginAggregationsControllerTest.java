package io.levelops.api.controllers;

import io.levelops.aggregations.plugins.clients.PluginResultAggregationsClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class PluginAggregationsControllerTest {

    private MockMvc mvc;

    @Mock
    private PluginResultAggregationsClient pluginResultAggregationsClient;

    private PluginAggregationsController pluginAggregationsController;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        pluginAggregationsController = new PluginAggregationsController(pluginResultAggregationsClient);
        mvc = MockMvcBuilders
                .standaloneSetup(pluginAggregationsController)
                .build();
    }

    @Test
    public void testGetPluginAggregation() throws Exception {
        mvc.perform(get("/v1/plugin_aggs/f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(asyncDispatch(mvc.perform(get("/v1/plugin_aggs/f73dbe99-5318-4d0c-abaa-fe3c397e63e0")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test"))
                .andReturn()))
                .andExpect(status().is(HttpStatus.OK.value()));
    }
}
