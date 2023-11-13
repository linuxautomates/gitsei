package io.levelops.api.controllers;

import io.levelops.api.config.DefaultApiTestConfiguration;
import io.levelops.api.services.TagItemService;
import io.levelops.commons.databases.models.database.Section;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.SectionsService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class SectionsControllerTest {
    private MockMvc mvc;

    @Autowired
    private SectionsService sectionsService;
    @Autowired
    private TagItemService tagItemService;
    @Autowired
    private ActivityLogService logService;

    @Before
    public void setup() {
        //The non-standalone setup will require authentication and everything to be done properly
        mvc = MockMvcBuilders.standaloneSetup(new SectionsController(sectionsService, logService, tagItemService))
                .build();
    }

    @Test
    public void testCreateSection() throws Exception {
        reset(logService);
        when(sectionsService.insert(anyString(),any(Section.class))).thenReturn(UUID.randomUUID().toString());
        mvc.perform(asyncDispatch(mvc.perform(post("/v1/sections").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("session_user", "a")
                .sessionAttr("company", "test")
                .content("{"
                        + "\"name\": \"question1\","
                        + "\"tags\": ["
                        + "    \"tag1\""
                        + "],"
                        + " \"description\": \"question1\","
                        + " \"questions\": ["
                        + "     {"
                        + "         \"name\": \"assertion1\","
                        + "         \"type\": \"multi-select\","
                        + "         \"options\": ["
                        + "             {"
                        + "                 \"value\": \"option1\","
                        + "                \"score\": 10"
                        + "            },"
                        + "            {"
                        + "                \"value\": \"option2\","
                        + "                \"score\": 10"
                        + "            }"
                        + "        ],"
                        + "        \"custom\": true,"
                        + "        \"verifiable\": true,"
                        + "        \"verification_mode\": \"auto\","
                        + "        \"verification_assets\": [],"
                        + "        \"training\": []"
                        + "    }"
                        + "]"
                        + "}"))
                .andReturn()))
                .andExpect(status().isOk())
                .andExpect(h1 -> verify(logService, times(1))
                        .insert(eq("test"), any()));
    }

    @Test
    public void testDeleteSection() throws Exception  {
        String id1 = UUID.randomUUID().toString();
        when(sectionsService.delete(anyString(), eq(id1))).thenReturn(true);
        mvc.perform(asyncDispatch(mvc.perform(delete("/v1/sections/" + id1)
                .sessionAttr("session_user", "a")
                .sessionAttr("company", "test"))
                .andReturn()))
                .andExpect(status().is(200));
        verify(sectionsService, times(1)).delete(anyString(), anyString());
    }
}
