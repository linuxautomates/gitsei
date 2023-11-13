package io.levelops.internal_api.controllers;

import io.levelops.commons.databases.models.database.Component;
import io.levelops.commons.databases.services.ComponentsDatabaseService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.ComponentType;
import io.levelops.commons.models.DbListResponse;
import io.levelops.internal_api.config.DefaultApiTestConfiguration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(controllers = ComponentsController.class)
@ContextConfiguration(classes = {ComponentsController.class, DefaultApiTestConfiguration.class})
public class ComponentsControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ComponentsDatabaseService componentsDatabaseService;

    private String company = "test";

    @Test
    public void getComponentByTypeAndNameTest() throws Exception {
        when(componentsDatabaseService.getByTypeName(eq(company), eq(ComponentType.INTEGRATION), eq("jira")))
            .thenReturn(Optional.of(
                    Component.builder()
                    .id(UUID.randomUUID())
                    .name("jira")
                    .type(ComponentType.INTEGRATION)
                    .subComponents(List.of())
                    .build()
                ));

        var getComponentTypes = get("/v1/tenants/test/components/integration/jira");
        mvc.perform(getComponentTypes)
            .andExpect(status().isOk())
            .andReturn();
    }

    @Test
    public void listComponentsTest() throws Exception {
        when(componentsDatabaseService.list(eq(company), eq(0), eq(10), any(), any()))
            .thenReturn(DbListResponse.of(List.of(
                Component.builder()
                    .id(UUID.randomUUID())
                    .name("jira")
                    .type(ComponentType.INTEGRATION)
                    .subComponents(List.of())
                    .build()
                ), 
                1));

        var listRequest = DefaultObjectMapper.get().writeValueAsString(Map.of("page", 0, "page_size", 10));
        var postListComponents = post("/v1/tenants/test/components/list").contentType(MediaType.APPLICATION_JSON).content(listRequest);
        mvc.perform(postListComponents)
            .andExpect(status().isOk())
            .andReturn();
    }
}