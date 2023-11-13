package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import io.levelops.commons.databases.models.database.AggregationRecord;
import io.levelops.commons.databases.services.AggregationsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.internal_api.config.DefaultApiTestConfiguration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
        IntegrationsController.class,
        DefaultApiTestConfiguration.class
})
public class PluginAggregationsControllerTest {

    private ObjectMapper objectMapper;
    @Mock
    private AggregationsDatabaseService aggregationsDatabaseService;
    @Value("${AGG_OUTPUT_BUCKET:aggregations-levelops}") private String aggBucket;
    @Mock
    private Storage storage;
    private MockMvc mvc;

    @Before
    public void setUp() throws Exception {
        objectMapper = DefaultObjectMapper.get();
        Mockito.reset(aggregationsDatabaseService);
        when(aggregationsDatabaseService.get(eq("foo"), eq("f73dbe99-5318-4d0c-abaa-fe3c397e63e0")))
                .thenReturn(Optional.of(AggregationRecord.builder().build()));
        PluginAggregationsController pluginAggregationsController = new PluginAggregationsController(objectMapper, aggregationsDatabaseService,
                aggBucket, storage);
        mvc = MockMvcBuilders.standaloneSetup(pluginAggregationsController).build();
    }

    @Test
    public void testGetPluginAggregation() throws Exception {
        mvc.perform(get("/internal/v1/tenants/foo/plugin_aggs/f73dbe99-5318-4d0c-abaa-fe3c397e63e0'%20and%201=cast((SELECT%20table_name%20FROM%20information_schema.tables%20LIMIT%201%20OFFSET%20OFFSET_GOES_HERE)%20as%20int)%20and%201=1--")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test"))
                .andExpect(status().is(HttpStatus.BAD_REQUEST.value()));

        mvc.perform(get("/internal/v1/tenants/foo/plugin_aggs/f73dbe99-5318-4d0c-abaa-fe3c397e63e0")
                .contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .sessionAttr("session_user", "test"))
                .andExpect(status().is(HttpStatus.OK.value()));
    }
}
