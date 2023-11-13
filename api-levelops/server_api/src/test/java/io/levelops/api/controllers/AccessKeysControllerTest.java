package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.api.config.DefaultApiTestConfiguration;
import io.levelops.api.converters.AccessKeyRequestConverter;
import io.levelops.commons.databases.models.database.AccessKey;
import io.levelops.commons.databases.services.AccessKeyService;
import io.levelops.commons.databases.services.ActivityLogService;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class AccessKeysControllerTest {
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AccessKeyService keyService;
    @Autowired
    private ActivityLogService logService;
    private AccessKeyRequestConverter converter = Mockito.mock(AccessKeyRequestConverter.class);

    @Before
    public void setup() {
        //The non-standalone setup will require authentication and everything to be done properly.
        mvc = MockMvcBuilders.standaloneSetup(new AccessKeysController(keyService, logService,
                converter, objectMapper)).build();
    }

    @Test
    public void testCreateAccessKey() throws Exception {
        when(converter.convertToAccessKey(any())).thenReturn(ImmutablePair.of(AccessKey.builder().build(), "asd"));
        mvc.perform(asyncDispatch(
                mvc.perform(
                        post("/v1/apikeys")
                                .contentType(MediaType.APPLICATION_JSON)
                                .sessionAttr("company", "test")
                                .sessionAttr("session_user", "asd123123")
                                .content("{\"name\":\"testdash\", \"description\":\"teams\", \"role\":\"ADMIN\"}"))
                        .andReturn()))
                .andExpect(status().isOk())
                .andExpect(content().json(
                        "{\"id\":\"null\",\"key\":\"eyJrZXkiOiJhc2QiLCJjb21wYW55IjoidGVzdCJ9\"}"))
                .andReturn();
    }

}
