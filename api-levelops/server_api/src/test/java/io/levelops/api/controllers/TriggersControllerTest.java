package io.levelops.api.controllers;

import io.levelops.api.config.DefaultApiTestConfiguration;
import io.levelops.auth.auth.authobject.ExtendedUser;
import io.levelops.auth.auth.config.WebSecurityConfig;
import io.levelops.auth.utils.JwtTokenUtil;
import io.levelops.commons.databases.models.database.Component;
import io.levelops.commons.models.ComponentType;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.KvField;
import io.levelops.commons.databases.models.database.TriggerSchema;
import io.levelops.commons.databases.models.database.TriggerType;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.triggers.clients.TriggersRESTClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(controllers = TriggersController.class)
@ContextConfiguration(classes = {TriggersController.class, WebSecurityConfig.class, DefaultApiTestConfiguration.class})
public class TriggersControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private JwtTokenUtil tokenUtil;
    @Autowired
    private TriggersRESTClient triggersClient;
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    private final String company = "test";
    
    @Test
    public void test() throws Exception {
        var redis = Mockito.mock(RedisConnection.class);
        when(redisConnectionFactory.getConnection()).thenReturn(redis);
        when(redis.exists(any(byte[].class))).thenReturn(false);
        when(triggersClient.listTriggerSchemas(eq(company), any()))
            .thenReturn(PaginatedResponse.of(0, 10, 2, List.of(
                TriggerSchema.builder()
                    .triggerType(TriggerType.SCHEDULED)
                    .description("description")
                    .fields(Map.of(
                            "event_type", KvField.builder().key("event_type").build()
                        ))
                    .build(),
                TriggerSchema.builder()
                    .triggerType(TriggerType.COMPONENT_EVENT)
                    .description("description")
                    .fields(Map.of())
                    .build())));
        when(triggersClient.listEventTypes(eq(company), any()))
            .thenReturn(PaginatedResponse.of(0, 10, 1, List.of(EventType.builder()
                .type("jira_issue_created")
                .description("New Issue Created")
                .data(Map.of(
                    "component_type", KvField.builder().key("component_type").build(),
                    "component_name", KvField.builder().key("component_name").build()
                    ))
                .build())));
        when(triggersClient.getEventType(eq(company), any())).thenReturn(Optional.empty());
        when(triggersClient.getEventType(eq(company), eq(EventType.JIRA_ISSUE_CREATED))).thenReturn(Optional.empty());
        String token = tokenUtil.generateToken(new ExtendedUser("admin1", "", false, true, RoleType.ADMIN,
        "User1", "LastName1", Collections.emptyList(), false, false, false, null), company);
        RequestBuilder post = post("/v1/playbooks/triggers/schemas/list").contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token)
            .content("{\"page\":0, \"page_size\": 10}")
            .characterEncoding("utf-8");
        mvc.perform(post)
            .andExpect(status().isOk())
//            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            // .andExpect(content().json("{}"))
            .andReturn();
    }
    
    @Test
    public void testTypefilter() throws Exception {
        var redis = Mockito.mock(RedisConnection.class);
        when(redisConnectionFactory.getConnection()).thenReturn(redis);
        when(redis.exists(any(byte[].class))).thenReturn(false);
        var componentEventTriggerSchema = TriggerSchema.builder()
                .triggerType(TriggerType.COMPONENT_EVENT)
                .description("description")
                .fields(Map.of("event_type", KvField.builder().key("event_type").build()))
                .build();
        when(triggersClient.listTriggerSchemas(any(), any()))
            .thenReturn(PaginatedResponse.of(0, 10, 2, List.of(
                TriggerSchema.builder()
                    .triggerType(TriggerType.SCHEDULED)
                    .description("description")
                    .fields(Map.of())
                    .build(),
                    componentEventTriggerSchema
                )));
        when(triggersClient.getTriggerSchemas(eq(company), eq(TriggerType.COMPONENT_EVENT))).thenReturn(Optional.of(componentEventTriggerSchema));
        var jiraEvent = EventType.builder()
                .type("jira_issue_created")
                .description("New Issue Created")
                .data(Map.of(
                        "component_type", KvField.builder().key("component_type").build(),
                        "component_name", KvField.builder().key("component_name").build()
                ))
                .component(Component.builder().name("jira").type(ComponentType.INTEGRATION).build())
                .build();
        when(triggersClient.listEventTypes(any(), any()))
            .thenReturn(PaginatedResponse.of(0, 10, 1, List.of(jiraEvent)));
        when(triggersClient.getEventType(eq(company), any())).thenReturn(Optional.empty());
        when(triggersClient.getEventType(eq(company), eq(EventType.JIRA_ISSUE_CREATED))).thenReturn(Optional.of(jiraEvent));
        String token = tokenUtil.generateToken(new ExtendedUser("admin1", "", false, true, RoleType.ADMIN,
        "User1", "LastName1", Collections.emptyList(), false, false, false, null), company);
        var searchQuery = DefaultListRequest.builder().filter(Map.of("type", "jira_issue_created")).page(0).pageSize(10).build();
        var content = DefaultObjectMapper.get().writeValueAsString(searchQuery);
        RequestBuilder post = post("/v1/playbooks/triggers/schemas/list")
                .header("Authorization", "Bearer " + token)
                .characterEncoding("utf-8")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content);
        mvc.perform(post)
            .andExpect(status().isOk())
//            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            // .andExpect(content().json("{}"))
            .andReturn();
            // .wait(TimeUnit.SECONDS.toMillis(20));
        RequestBuilder get = get("/v1/playbooks/triggers/schemas/jira_issue_created")
                .header("Authorization", "Bearer " + token);
        mvc.perform(get)
            .andExpect(status().isOk())
//            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            // .andExpect(content().json("{}"))
            .andReturn();
            // .wait(TimeUnit.SECONDS.toMillis(20));
    }
}