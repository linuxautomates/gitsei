package io.levelops.api.controllers;

import io.levelops.api.config.DefaultApiTestConfiguration;
import io.levelops.api.services.IntegrationManagementService;
import io.levelops.auth.auth.authobject.ExtendedUser;
import io.levelops.auth.auth.config.WebSecurityConfig;
import io.levelops.auth.utils.JwtTokenUtil;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.GitRepositoryService;
import io.levelops.commons.models.DbListResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;

//this class tests both the integrationscontroller AND the authority-based-access.
@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(controllers = IntegrationsController.class)
@ContextConfiguration(classes = {IntegrationsController.class, WebSecurityConfig.class, DefaultApiTestConfiguration.class})
public class IntegrationsControllerTest {
    @Autowired
    private IntegrationManagementService integrationManagementService;
    @Autowired
    private GitRepositoryService gitRepositoryService;
    @Autowired
    private JwtTokenUtil tokenUtil;
    @Autowired
    private ActivityLogService activityLogService;
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;
    @Autowired
    private MockMvc mvc;

    @Test
    public void testCreateIntegration() throws Exception {
        var redis = Mockito.mock(RedisConnection.class);
        when(redisConnectionFactory.getConnection()).thenReturn(redis);
        when(redis.exists(any(byte[].class))).thenReturn(false);
        String toke = tokenUtil.generateToken(new ExtendedUser("harsh", "", false, true, RoleType.ADMIN,
                "hello", null, Collections.emptyList(), false, false, false, null), "test");
        mvc.perform(asyncDispatch(mvc.perform(post("/v1/integrations").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + toke)
                .content("{\"name\":\"asd@asd.com\",\"description\":\"test1\",\"url\":\"wompwomp\"," +
                        "\"method\":\"apikey\",\"application\":\"github\"}")).andReturn()))
                .andExpect(status().isOk())
                .andReturn();
        toke = tokenUtil.generateToken(new ExtendedUser("limiteduser", "", false, true, RoleType.LIMITED_USER,
                "hello", null, Collections.emptyList(), false, false, false, null), "test");
        mvc.perform(post("/v1/integrations").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + toke)
                .content("{\"name\":\"asd@asd.com\",\"description\":\"test1\",\"url\":\"wompwomp\"," +
                        "\"method\":\"apikey\",\"application\":\"github\"}"))
                .andExpect(status().isForbidden())
                .andReturn();
        verify(activityLogService, times(1))
                .insert(anyString(), any());
        mvc.perform(asyncDispatch(mvc.perform(post("/v1/integrations").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Apikey eyJjb21wYW55IjoidGVzdCIsICJpZCI6ImhhcnNoIiwgImtleSI6ImFzZGFzZCJ9")
                .content("{\"name\":\"asd@asd.com\",\"description\":\"test1\",\"url\":\"wompwomp\"," +
                        "\"method\":\"apikey\",\"application\":\"github\"}")).andReturn()))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    public void testGetIntegration() throws Exception {
        var redis = Mockito.mock(RedisConnection.class);
        when(redisConnectionFactory.getConnection()).thenReturn(redis);
        when(redis.exists(any(byte[].class))).thenReturn(false);
        String toke = tokenUtil.generateToken(new ExtendedUser("harsh", "", false, true, RoleType.ADMIN,
                "hello", null, Collections.emptyList(), false, false, false, null), "test");
        when(integrationManagementService.getIntegration(any(), any())).thenReturn(Integration.builder()
                .id("1").application("github").build());
        when(gitRepositoryService.listByFilter(eq("test"), eq("1"), any(), any(), any(), any()))
                .thenReturn(DbListResponse.of(Collections.emptyList(), 0));
        mvc.perform(asyncDispatch(mvc.perform(get("/v1/integrations/1").contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + toke)).andReturn()))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"application\":\"github\",\"id\":\"1\"}"))
                .andReturn();
        verify(integrationManagementService, times(1))
                .getIntegration(eq("test"), eq("1"));
    }
}
