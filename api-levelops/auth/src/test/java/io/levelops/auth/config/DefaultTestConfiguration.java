package io.levelops.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.authz.acl.client.ACLClientFactory;
import io.levelops.auth.auth.authobject.ExtendedUser;
import io.levelops.auth.auth.config.Auth;
import io.levelops.auth.auth.config.JwtAuthenticationEntryPoint;
import io.levelops.auth.auth.filter.AuthRequestFilter;
import io.levelops.auth.auth.filter.HarnessAuthRequestFilter;
import io.levelops.auth.auth.saml.SamlService;
import io.levelops.auth.auth.service.AuthDetailsService;
import io.levelops.auth.services.SystemNotificationService;
import io.levelops.auth.services.TokenGenService;
import io.levelops.auth.utils.ControllerMethodFinder;
import io.levelops.auth.utils.JwtTokenUtil;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.SamlConfigService;
import io.levelops.commons.inventory.SecretsManagerServiceClient;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.notification.services.TenantManagementNotificationService;
import okhttp3.OkHttpClient;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.net.MalformedURLException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Set;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@Configuration
public class DefaultTestConfiguration {
    @Bean
    public AuthDetailsService jwtUserDetailsService() throws SQLException {
        AuthDetailsService dataSource = Mockito.mock(AuthDetailsService.class);
        when(dataSource.loadUserByUsernameAndOrg(eq("harsh"), eq("test")))
                .thenAnswer(ans -> new ExtendedUser("harsh",
                        "$2a$10$zaL07.61u3VpZcq69V46Zu5ZAIrbtEXmUKAQwqXjL8UWDCpUzRCFy", true,
                        true, RoleType.ADMIN, "harsh", "harsh", Collections.emptyList(), false, false, false, null));
        when(dataSource.loadUserByUsernameAndOrg(eq("harsh2"), eq("test")))
                .thenAnswer(ans -> new ExtendedUser("harsh2",
                        "$2a$10$zaL07.61u3VpZcq69V46Zu5ZAIrbtEXmUKAQwqXjL8UWDCpUzRCFy", false,
                        false, RoleType.ADMIN, "harsh", "harsh", Collections.emptyList(), false, false, false, null));
        return dataSource;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public TokenGenService tokenGenService(JwtTokenUtil tokenUtil, AuthDetailsService authDetailsService) {
        return new TokenGenService(authDetailsService, tokenUtil);
    }

    @Bean
    public ActivityLogService activityLogService() {
        return Mockito.mock(ActivityLogService.class);
    }

    @Bean
    public SystemNotificationService systemNotificationService() {
        return Mockito.mock(SystemNotificationService.class);
    }

    @Bean
    public SamlService samlService(@Value("${ACS_URL:https://api.levelops.io/v1/saml_auth}") String acsUrl,
                                   @Value("${SP_IDENTITY_ID:levelops.io}") String spId,
                                   @Value("${ALTERNATE_SP_IDENTITY_ID:dev-levelops.io}") String alternateSpId)
            throws MalformedURLException {
        return new SamlService(acsUrl, spId,alternateSpId, null);
    }

    @Bean
    public SamlConfigService samlConfigService() {
        return Mockito.mock(SamlConfigService.class);
    }

    @Bean(name = "custom")
    public ObjectMapper objectMapper() {
        return DefaultObjectMapper.get();
    }

    @Bean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }

    @Bean
    public JwtTokenUtil jwtTokenUtil() {
        JwtTokenUtil tokenUtil = new JwtTokenUtil("asdasdasdasdasdasdasdasdasdasdasdasdasdasd");
        JwtTokenUtil spy = spy(tokenUtil);
        doReturn(1L).when(spy).getJwtTokenValiditySeconds();
        return spy;
    }

    @Bean
    public RedisConnectionFactory redisConnectionFactory(){
        return Mockito.mock(RedisConnectionFactory.class);
    }

    @Bean
    public AuthRequestFilter jwtRequestFilter(ObjectMapper mapper,
                                              JwtTokenUtil jwtTokenUtil,
                                              AuthDetailsService authDetailsService,
                                              RedisConnectionFactory redisConnectionFactory, Auth auth) {
        return new AuthRequestFilter(jwtTokenUtil, authDetailsService, mapper, redisConnectionFactory, false, auth);
    }

    @Bean
    public SecretsManagerServiceClient secretsManagerServiceClient(){
        return Mockito.mock(SecretsManagerServiceClient.class);
    }

    @Bean
    public TenantManagementNotificationService tenantManagementNotificationService(){
        return Mockito.mock(TenantManagementNotificationService.class);
    }

    @Bean
    public Auth getAuth(){
        return new Auth(true);
    }
    @Bean
    public HarnessAuthRequestFilter harnessAuthzRquestFilter(ACLClientFactory aclClientFactory, ControllerMethodFinder controllerMethodFinder, ObjectMapper objectMapper, JwtTokenUtil jwtTokenUtil,
                                                             Auth auth,  AuthDetailsService authDetailsService) {
        return new HarnessAuthRequestFilter(aclClientFactory, controllerMethodFinder, objectMapper, jwtTokenUtil, "identityServiceSecret", auth, Set.of(), authDetailsService);
    }

    @Bean
    public ACLClientFactory aclClientFactory(ObjectMapper objectMapper,
                                             @Value("${HARNESS_ACS_URL:http://access-control:9006/api/}") String baseUrl){

        return ACLClientFactory.builder()
                .okHttpClient(new OkHttpClient())
                .objectMapper(objectMapper)
                .aclUrl(baseUrl)
                .build();
    }

    @Bean
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping(){
        return new RequestMappingHandlerMapping();
    }

    @Bean
    public ControllerMethodFinder controllerMethodFinder(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        return new ControllerMethodFinder(requestMappingHandlerMapping);
    }

}
