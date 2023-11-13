package io.levelops.api.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.api.config.DefaultApiTestConfiguration;
import io.levelops.api.config.ServerConfig;
import io.levelops.api.services.UserClientHelperService;
import io.levelops.api.services.dev_productivity.DevProductivityOpsService;
import io.levelops.auth.controllers.MFAController;
import io.levelops.auth.services.SystemNotificationService;
import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.inventory.SecretsManagerServiceClient;
import io.levelops.commons.inventory.SecretsManagerServiceClient.KeyValue;
import io.levelops.commons.inventory.exceptions.SecretsManagerServiceClientException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.licensing.exception.LicensingException;
import io.levelops.commons.licensing.model.License;
import io.levelops.commons.licensing.service.LicensingService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.users.clients.UsersRESTClient;
import io.levelops.users.requests.ModifyUserRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {ServerConfig.class, DefaultApiTestConfiguration.class})
@SuppressWarnings("unused")
public class UsersControllerTest {
    private MockMvc mvc;
    @Autowired
    private UserService userService;
    @Autowired
    private ActivityLogService activityLogService;
    @Autowired
    private DevProductivityOpsService devProductivityOpsService;
    @Autowired
    private TenantConfigService tenantConfigService;
    @Autowired
    private SystemNotificationService systemNotificationService;
    @Autowired
    private RedisConnectionFactory redisConnectionFactory;
    @Autowired
    private OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;

    @Mock
    private UsersRESTClient restClient;

    @Mock
    private UserClientHelperService userClientHelperService;

    @Mock
    private SecretsManagerServiceClient secretsManagerServiceClient;
    @Mock
    private LicensingService licensingService;

    private UsersController usersController;


    @Before
    public void setup() throws LicensingException {
        MockitoAnnotations.initMocks(this);
        when(licensingService.getLicense(anyString())).thenReturn(License.builder().license("full").entitlements(List.of("ALL_FEATURES")).build());
        usersController = new UsersController(userService, activityLogService, tenantConfigService, restClient, userClientHelperService, secretsManagerServiceClient, licensingService, devProductivityOpsService, orgUnitCategoryDatabaseService);
        //The non-standalone setup will require authentication and everything to be done properly.
        mvc = MockMvcBuilders.standaloneSetup(usersController).build();

        var redis = Mockito.mock(RedisConnection.class);
        when(redisConnectionFactory.getConnection()).thenReturn(redis);
        when(redis.exists(any(byte[].class))).thenReturn(false);
    }

    @Test
    public void testGetUserMe() throws Exception {
        when(tenantConfigService.listByFilter(eq("asd"), eq("LIMITED_USER_LANDING_PAGE"), eq(0), eq(1)))
                .thenReturn(DbListResponse.<TenantConfig>builder().records(List.of()).build());
        when(tenantConfigService.listByFilter(eq("bsd"), eq("LIMITED_USER_LANDING_PAGE"), eq(0), eq(1)))
                .thenReturn(DbListResponse.<TenantConfig>builder().records(
                        List.of(TenantConfig.builder().name("LIMITED_USER_LANDING_PAGE").value("null").build())).build());

        when(userService.getByEmail(eq("asd"), anyString())).thenReturn(Optional.of(User.builder()
                .userType(RoleType.LIMITED_USER).build()));
        when(userService.getByEmail(eq("bsd"), anyString())).thenReturn(Optional.of(User.builder()
                .userType(RoleType.LIMITED_USER).build()));

        when(tenantConfigService.listByFilter(eq("asd"), eq("MFA_ENFORCED"), eq(0), eq(1)))
                .thenReturn(DbListResponse.<TenantConfig>builder().records(List.of()).build());
        when(tenantConfigService.listByFilter(eq("bsd"), eq("MFA_ENFORCED"), eq(0), eq(1)))
                .thenReturn(DbListResponse.<TenantConfig>builder().records(List.of()).build());
        mvc.perform(asyncDispatch(mvc.perform(get("/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("company", "asd")
                        .sessionAttr("session_user", "asd")).andReturn()))
                .andExpect(status().isOk()).andExpect(content().string("{\"user_type\":\"LIMITED_USER\",\"mfa_enforced\":false,\"license\":\"full\",\"entitlements\":[\"ALL_FEATURES\"]}"));
        mvc.perform(asyncDispatch(mvc.perform(get("/v1/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .sessionAttr("company", "bsd")
                        .sessionAttr("session_user", "bsd")).andReturn()))
                .andExpect(status().isOk()).andExpect(content().string("{\"user_type\":\"LIMITED_USER\",\"mfa_enforced\":false,\"landing_page\":\"null\",\"license\":\"full\",\"entitlements\":[\"ALL_FEATURES\"]}"));
    }

    public static class TestClass {
        @JsonProperty("date")
        Long date;
    }

    @Test
    public void testUpdateUserModel() throws IOException {
        var request = ResourceUtils.getResourceAsObject("model/modify_user_request.json", ModifyUserRequest.class);

        assertThat(request).isNotNull();
        assertThat(request.getEmail()).isEqualTo("test@test.test");
        assertThat(request.getMfaEnabled()).isEqualTo(true);
        assertThat(request.getMfaEnrollmentWindowExpiry()).isEqualTo(1627372281);
        assertThat(request.getMfaEnrollmentWindowExpiryInstant()).isEqualTo(Instant.parse("2021-07-27T07:51:21Z"));
        assertThat(request.getMfaResetAt()).isEqualTo(1627583399);
        assertThat(request.getMfaResetAtInstant()).isEqualTo(Instant.parse("2021-07-29T18:29:59Z"));
    }

    @Test
    public void testUserUpdate() throws SQLException, SecretsManagerServiceClientException, IOException {
        var key = MFAController.getSecretsManagerKey("test1", "test@test.test");
        when(tenantConfigService.listByFilter(eq("test"), eq("MFA_ENFORCED"), eq(0), eq(1)))
                .thenReturn(DbListResponse.<TenantConfig>builder().records(List.of()).build());
        when(tenantConfigService.listByFilter(eq("test1"), eq("MFA_ENFORCED"), eq(0), eq(1)))
                .thenReturn(DbListResponse.<TenantConfig>builder().records(List.of(TenantConfig.builder().id("1").name("MFA_ENFORCED").value("1").build())).build());
        when(secretsManagerServiceClient.getKeyValue(eq("test1"), eq(SecretsManagerServiceClient.DEFAULT_CONFIG_ID), eq(key)))
                .thenReturn(KeyValue.builder().key("key").value("secret").build());

        when(restClient.getUser(eq("test1"), eq("user1"))).thenReturn(User.builder().email("test@test.test").build());

        var request = new ModifyUserRequest(null, null, null, null, null, null, null, false, null, Instant.now().getEpochSecond(), Instant.now().getEpochSecond(),null);
        var response = usersController.userUpdate(request, "test@test.test", "admin@test.test", "test");
        var count = 0;
        while (!response.hasResult() && count < 10) {
            try {
                Thread.sleep(300);
                count++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        assertThat(response).isNotNull();

        var response2 = usersController.userUpdate(request, "test@test.test", "admin@test.test", "test1");
        count = 0;
        while (!response2.hasResult() && count < 10) {
            try {
                Thread.sleep(300);
                count++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertThat(response2).isNotNull();

        request = new ModifyUserRequest(null, null, null, null, null, null, null, false, null, null, Instant.now().getEpochSecond(), null);
        var response3 = usersController.userUpdate(request, "user1", "admin@test.test", "test1");
        count = 0;
        while (!response3.hasResult() && count < 10) {
            try {
                Thread.sleep(300);
                count++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        assertThat(response3).isNotNull();
    }

    @Test
    public void testMfaEnrollmentEndZero() throws JsonProcessingException {
        ModifyUserRequest request = DefaultObjectMapper.get().readValue("{\"mfa_enrollment_end\":0}", ModifyUserRequest.class);
        assertThat(request.getMfaEnrollmentWindowExpiry()).isEqualTo(0);
    }

    @Test
    public  void test() throws JsonProcessingException {
        ObjectMapper mapper = DefaultObjectMapper.get();
        Map<String,List<String>> scopes = new HashMap<>();
        scopes.put("dev_prod_write",List.of());
        System.out.println("scopes = "+mapper.writeValueAsString(scopes));
        scopes.remove("dev_prod_write");
        System.out.println("scopes = "+mapper.writeValueAsString(scopes));
    }
}
