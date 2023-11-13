package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.api.config.DefaultApiTestConfiguration;
import io.levelops.commons.databases.models.database.Organization;
import io.levelops.commons.databases.services.OrganizationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {DefaultApiTestConfiguration.class})
public class OrganizationsControllerTest {
    private MockMvc mvc;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private OrganizationService organizationService;

    @Before
    public void setup() {
        //The non-standalone setup will require authentication and everything to be done properly.
        mvc = MockMvcBuilders.standaloneSetup(new OrganizationsController(objectMapper,
                organizationService)).build();
    }

    @Test
    public void testCreateOrganization() throws Exception {
        when(organizationService.insert(anyString(), any())).thenReturn("id");
        mvc.perform(asyncDispatch(mvc.perform(post("/v1/organizations").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")
                .content("{\"name\":\"testorg\"}")).andReturn()))
                .andExpect(status().isOk())
                .andReturn();
        verify(organizationService, times(1))
                .insert(eq("test"), eq(Organization.builder().name("testorg").build()));
    }

    @Test
    public void testGetOrganization() throws Exception {
        when(organizationService.get(any(), any())).thenReturn(Optional.of(Organization.builder()
                .id("1").name("testorg").build()));
        mvc.perform(asyncDispatch(mvc.perform(get("/v1/organizations/1").contentType(MediaType.APPLICATION_JSON)
                .sessionAttr("company", "test")).andReturn()))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"name\":\"testorg\",\"id\":\"1\"}"))
                .andReturn();
        verify(organizationService, times(1))
                .get(eq("test"), eq("1"));
    }
}
