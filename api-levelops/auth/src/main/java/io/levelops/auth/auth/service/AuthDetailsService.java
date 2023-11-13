package io.levelops.auth.auth.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.auth.auth.authobject.ExtendedUser;
import io.levelops.auth.controllers.MFAController;
import io.levelops.auth.httpmodels.Entitlements;
import io.levelops.auth.utils.TenantUtilService;
import io.levelops.commons.databases.models.database.AccessKey;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.services.*;
import io.levelops.commons.inventory.SecretsManagerServiceClient;
import io.levelops.commons.licensing.exception.LicensingException;
import io.levelops.commons.licensing.model.License;
import io.levelops.commons.licensing.service.LicensingService;
import io.levelops.commons.password.RandomPasswordGenerator;
import io.levelops.commons.utils.MapUtils;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.database.access.RoleType.LIMITED_USER;

@Log4j2
@SuppressWarnings("unused")
public class AuthDetailsService {
    private static final String AUTO_PROVISIONED_ROLE_CONFIG_NAME = "AUTO_PROVISIONED_ROLE";
    private static final String MFA_ENFORCED_KEY = "";
    private final Long resetTokenExpiry;
    private final UserService userService;
    private final PasswordEncoder encoder;
    private final AccessKeyService keyService;
    private final TenantConfigService configService;
    private final DashboardWidgetService dashboardWidgetsService;
    private final SecretsManagerServiceClient secretsService;
    private final RedisConnectionFactory redisConnectionFactory;
    private final TenantService tenantService;
    private final LicensingService licensingService;
    private final Set<String> forceEnableDevProdForTenants;
    private LoadingCache<String, List<Entitlements>> cache;
    private final TenantUtilService tenantUtilService;

    public AuthDetailsService(UserService userService,
                              AccessKeyService accessKeyService,
                              PasswordEncoder encoder,
                              TenantConfigService configService,
                              Long resetExpiry,
                              DashboardWidgetService dashboardWidgetsService,
                              SecretsManagerServiceClient secretsService,
                              RedisConnectionFactory redisConnectionFactory,
                              TenantService tenantService,
                              LicensingService licensingService,
                              Set<String> forceEnableDevProdForTenants,
                              TenantUtilService tenantUtilService) {
        this.encoder = encoder;
        this.userService = userService;
        this.keyService = accessKeyService;
        this.configService = configService;
        this.resetTokenExpiry = resetExpiry;
        this.dashboardWidgetsService = dashboardWidgetsService;
        this.secretsService = secretsService;
        this.redisConnectionFactory = redisConnectionFactory;
        this.tenantService = tenantService;
        this.licensingService = licensingService;
        cache = CacheBuilder.from("maximumSize=250")
                .build(CacheLoader.<String, List<Entitlements>>from(this::getEntitlements));
        this.forceEnableDevProdForTenants = forceEnableDevProdForTenants;
        this.tenantUtilService= tenantUtilService;
    }

    private Map<String, List<String>> sanitizeScopes(String company, Map<String, List<String>> scopes) {
        if (forceEnableDevProdForTenants.contains(company)) {
            return MapUtils.append(scopes, "dev_productivity_write", List.of());
        }
        return scopes;
    }

    @Cacheable(value = "dashboards")
    public boolean isPublicDashboard(final String company, final String dashboardId) {
        var dash = dashboardWidgetsService.get(company, dashboardId);
        if (dash.isEmpty()) {
            return false;
        }
        return dash.get().isPublic();
    }

    public ExtendedUser loadUserByUsernameAndOrg(String username, String company)
            throws UsernameNotFoundException, SQLException {
        return userService.getForAuthOnly(company, username, true)
                .map(levelopsUser -> {
                    if ("_levelops".equalsIgnoreCase(company)) {
                        return new ExtendedUser(
                                levelopsUser.getEmail(),
                                levelopsUser.getBcryptPassword(),
                                levelopsUser.getPasswordAuthEnabled(),
                                levelopsUser.getSamlAuthEnabled(),
                                levelopsUser.getUserType(),
                                levelopsUser.getFirstName(),
                                levelopsUser.getLastName(),
                                new ArrayList<>(),
                                false,
                                false,
                                false, levelopsUser.getScopes());
                    }
                    var mfaEnforced = MFAController.isMFAEnforced(configService, company);
                    var mfaEnrolled = MFAController.isMFAEnrolled(secretsService, company, username);
                    var mfaEnrollment = false;
                    if (!mfaEnrolled && (mfaEnforced || levelopsUser.getMfaEnrollmentEndAt() != null)) {
                        // if the enrollment window is still active, allow enrollment
                        var globalMFAEnforcement = MFAController.getGlobalEnrollmentWindowExpiration(configService, company);
                        if ((globalMFAEnforcement.isPresent() && Instant.now().isBefore(globalMFAEnforcement.get()))
                                || (levelopsUser.getMfaEnrollmentEndAt() != null && Instant.now().isBefore(levelopsUser.getMfaEnrollmentEndAt()))) {
                            mfaEnrollment = true;
                        }
                    }

                    return new ExtendedUser(
                            levelopsUser.getEmail(),
                            levelopsUser.getBcryptPassword(),
                            levelopsUser.getPasswordAuthEnabled(),
                            levelopsUser.getSamlAuthEnabled(),
                            levelopsUser.getUserType(),
                            levelopsUser.getFirstName(),
                            levelopsUser.getLastName(),
                            new ArrayList<>(),
                            mfaEnforced || mfaEnrolled,
                            mfaEnrollment,
                            mfaEnrolled,
                            sanitizeScopes(company, levelopsUser.getScopes()));
                })
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    public User getUserFromDb(String username, String org)
            throws UsernameNotFoundException, SQLException {
        return userService.getForAuthOnly(org, username, true)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
    }

    public List<User> getUsersAcrossTenants(String email) throws SQLException, ExecutionException {
        List<String> tenantsList = tenantUtilService.getActiveTenants();
        return userService.getUserDetailsAcrossTenants(email, tenantsList);
    }

    //this is a separate method so we can cache with spring.
    private AccessKey getAccessKeyFromDb(String org, String keyId)
            throws UsernameNotFoundException, SQLException {
        try {
            return keyService.getForAuthOnly(org, keyId)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + keyId));
        } catch (SQLException e) {
            tenantUtilService.reloadCache();
            throw e;
        }
    }

    //note this function uses org and keyid unlike most other functions in this class
    public RoleType validateKeyAndGetRole(String org, String keyId, String secret)
            throws UsernameNotFoundException, SQLException, ExecutionException, IllegalAccessException {
        tenantUtilService.validateTenant(org);
        AccessKey key = getAccessKeyFromDb(org, keyId);
        if (isMatchInvalid(secret, key.getBcryptSecret())) {
            throw new BadCredentialsException("Invalid credentials provided for authkey.");
        }
        return key.getRoleType();
    }

    public boolean isMatchInvalid(String rawPass, String encodedToken) {
        return StringUtils.isEmpty(rawPass)
                || StringUtils.isEmpty(encodedToken)
                || !encoder.matches(rawPass, encodedToken);
    }

    public Boolean updatePassword(String userId, String company, String newPassword)
            throws UsernameNotFoundException, SQLException {
        //reset user to no longer have a password reset value so that it cannot be reused
        return userService.update(company, User.builder()
                .passwordResetDetails(new User.PasswordReset(null, null))
                .bcryptPassword(encoder.encode(newPassword)).id(userId).build(), true);
    }

    public Boolean updatePasswordResetToken(String userId, String company, String passwordToken)
            throws UsernameNotFoundException, SQLException {
        return userService.update(company, User.builder().passwordResetDetails(
                        new User.PasswordReset(encoder.encode(passwordToken),
                                (System.currentTimeMillis() / 1000) + resetTokenExpiry))
                .id(userId).build(), true);
    }

    public Boolean updatePasswordResetToken(String userId, String company, String passwordToken, int timeoutSeconds)
            throws UsernameNotFoundException, SQLException {
        return userService.update(company, User.builder().passwordResetDetails(
                        new User.PasswordReset(encoder.encode(passwordToken),
                                (System.currentTimeMillis() / 1000) + timeoutSeconds))
                .id(userId).build(), true);
    }

    public ExtendedUser loadOrProvisionUser(User u, String org)
            throws UsernameNotFoundException, SQLException {
        return userService.getForAuthOnly(org, u.getEmail(), true)
                .map(levelopsUser -> new ExtendedUser(
                        levelopsUser.getEmail(),
                        levelopsUser.getBcryptPassword(),
                        levelopsUser.getPasswordAuthEnabled(),
                        levelopsUser.getSamlAuthEnabled(),
                        levelopsUser.getUserType(),
                        levelopsUser.getFirstName(),
                        levelopsUser.getLastName(),
                        new ArrayList<>(),
                        false,
                        false,
                        false, levelopsUser.getScopes()))
                .or(() -> {
                    ExtendedUser user = null;
                    RoleType role = LIMITED_USER;
                    try {
                        role = configService.listByFilter(org, AUTO_PROVISIONED_ROLE_CONFIG_NAME, 0, 1)
                                .getRecords()
                                .stream()
                                .findFirst()
                                .map(obj -> RoleType.fromString(obj.getValue()))
                                .orElse(LIMITED_USER);
                    } catch (SQLException throwables) {
                        log.warn("Error fetching config entry: ", throwables);
                    }
                    try {
                        User levelopsUser = User.builder()
                                .userType(role)
                                .passwordAuthEnabled(false)
                                .samlAuthEnabled(true)
                                .email(u.getEmail())
                                .firstName(u.getFirstName())
                                .lastName(u.getLastName())
                                .bcryptPassword(encoder.encode(RandomPasswordGenerator.nextString()))
                                .build();
                        userService.insert(org, levelopsUser);
                        user = new ExtendedUser(
                                levelopsUser.getEmail(),
                                levelopsUser.getBcryptPassword(),
                                levelopsUser.getPasswordAuthEnabled(),
                                levelopsUser.getSamlAuthEnabled(),
                                levelopsUser.getUserType(),
                                levelopsUser.getFirstName(),
                                levelopsUser.getLastName(),
                                new ArrayList<>(),
                                false,
                                false,
                                false,
                                sanitizeScopes(org, levelopsUser.getScopes()));
                    } catch (SQLException e) {
                        return Optional.empty();
                    }
                    return Optional.of(user);
                }).orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + u.getEmail()));
    }

    public List<Entitlements> getCompanyEntitlements(String company) throws ExecutionException {
        return cache.get(company);
    }

    public List<Entitlements> getEntitlements(String company) {
        try {
            License license = licensingService.getLicense(company);
            return license.getEntitlements().stream().map(entitlement -> {
                try {
                    return Entitlements.fromString(entitlement);
                } catch (Exception e) {
                    log.warn("Entitlement mapping not found for entitlement {}", entitlement);
                    return null;
                }
            }).filter(en -> en != null).collect(Collectors.toList());
        } catch (LicensingException e) {
            throw new RuntimeException();
        }
    }
}