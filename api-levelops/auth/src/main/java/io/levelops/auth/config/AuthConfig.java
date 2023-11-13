package io.levelops.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.authz.acl.client.ACLClientFactory;
import io.levelops.auth.auth.config.Auth;
import io.levelops.auth.auth.config.JwtAuthenticationEntryPoint;
import io.levelops.auth.auth.filter.AuthRequestFilter;
import io.levelops.auth.auth.filter.HarnessAuthRequestFilter;
import io.levelops.auth.auth.saml.SamlService;
import io.levelops.auth.auth.service.AuthDetailsService;
import io.levelops.auth.services.TokenGenService;
import io.levelops.auth.utils.ControllerMethodFinder;
import io.levelops.auth.utils.JwtTokenUtil;
import io.levelops.auth.utils.TenantUtilService;
import io.levelops.commons.databases.services.AccessKeyService;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.databases.services.TenantService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.inventory.SecretsManagerServiceClient;
import io.levelops.commons.licensing.service.LicensingService;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.services.EmailService;
import io.levelops.services.TemplateService;
import okhttp3.OkHttpClient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.validation.constraints.NotEmpty;
import java.net.MalformedURLException;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@ConfigurationProperties("config")
@Configuration
@SuppressWarnings("unused")
public class AuthConfig {
    private static final Log LOGGER = LogFactory.getLog(AuthConfig.class);

    @NotEmpty
    @Value("${IDENTITY_SERVICE_SECRET}")
    private String identityServiceSecret;
    @Bean
    public SecretsManagerServiceClient secretsManagerServiceClient(OkHttpClient okHttpClient,
                                                                   ObjectMapper objectMapper,
                                                                   @Value("${SECRETS_MANAGER_SERVICE_URL:http://secrets-manager-service}") String secretsManagerServiceUrl) {
        return new SecretsManagerServiceClient(secretsManagerServiceUrl, okHttpClient, objectMapper);
    }

    @Bean
    public AuthDetailsService jwtUserDetailsService(UserService userService,
                                                    PasswordEncoder encoder,
                                                    AccessKeyService accessKeyService,
                                                    TenantConfigService configService,
                                                    final DashboardWidgetService dashboardWidgetsService,
                                                    @Value("${PASSWORD_RESET_TOKEN_EXPIRY_SECONDS:7776000}") Long resetExpiry,
                                                    SecretsManagerServiceClient secretsManagerServiceClient,
                                                    RedisConnectionFactory redisFactory,
                                                    TenantService tenanatService,
                                                    LicensingService licensingService,
                                                    @Value("${FORCE_ENABLE_DEV_PROD_FOR_TENANTS:razorpay}") String forceEnableDevProdForTenants,
                                                    TenantUtilService tenantUtilService) {
        return new AuthDetailsService(userService, accessKeyService, encoder, configService, resetExpiry, dashboardWidgetsService, secretsManagerServiceClient, redisFactory, tenanatService, licensingService,
                CommaListSplitter.splitToStream(forceEnableDevProdForTenants)
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet()),
                tenantUtilService);
    }

    @Bean
    public SamlService samlService(@Value("${ACS_URL:https://api.levelops.io/v1/saml_auth}") String acsUrl,
                                   @Value("${SP_IDENTITY_ID}") String spId,
                                   @Value("${ALTERNATE_SP_IDENTITY_ID}") String alternateSpId,
                                   @Value("${PINGONE_SSO_TENANTS:}") String pingOneSSOTenants)
            throws MalformedURLException {
        return new SamlService(acsUrl, spId, alternateSpId, pingOneSSOTenants);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthRequestFilter jwtRequestFilter(@Value("${ENFORCE_API_RESTRICTIONS:false}") Boolean enforceApiRestrictions,
                                              JwtTokenUtil tokenUtil,
                                              ObjectMapper objectMapper,
                                              AuthDetailsService authDetailsService,
                                              RedisConnectionFactory redisFactory,
                                              Auth auth) {
        return new AuthRequestFilter(tokenUtil, authDetailsService, objectMapper, redisFactory, enforceApiRestrictions, auth);
    }

    @Bean
    public ControllerMethodFinder controllerMethodFinder(RequestMappingHandlerMapping requestMappingHandlerMapping) {
        return new ControllerMethodFinder(requestMappingHandlerMapping);
    }

    @Bean
    public HarnessAuthRequestFilter harnessAuthzRquestFilter(ACLClientFactory aclClientFactory, ControllerMethodFinder controllerMethodFinder, ObjectMapper objectMapper, JwtTokenUtil jwtTokenUtil,
                                                             Auth auth,   @Value("${DEFAULT_ENTITELEMENTS:}") Set<String> defaultEntitelements,   AuthDetailsService authDetailsService) {
        return new HarnessAuthRequestFilter(aclClientFactory, controllerMethodFinder, objectMapper, jwtTokenUtil, identityServiceSecret, auth, defaultEntitelements, authDetailsService);
    }

    @Bean
    public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint() {
        return new JwtAuthenticationEntryPoint();
    }

    @Bean
    public JwtTokenUtil jwtTokenUtil(@Value("${JWT_SIGNING_KEY}") String jwtSigningKey) {
        return new JwtTokenUtil(jwtSigningKey);
    }

    @Bean
    public TokenGenService tokenGenService(JwtTokenUtil tokenUtil, AuthDetailsService authDetailsService) {
        return new TokenGenService(authDetailsService, tokenUtil);
    }

    @Bean
    public TemplateService templateService() {
        return new TemplateService();
    }

    @Bean
    public EmailService emailService(@Value("${SENDGRID_API_KEY}") String sendGridApiKey) {
        return new EmailService(sendGridApiKey);
    }
}
