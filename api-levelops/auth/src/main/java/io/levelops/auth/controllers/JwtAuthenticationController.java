package io.levelops.auth.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.auth.auth.authobject.ExtendedUser;
import io.levelops.auth.auth.saml.SamlService;
import io.levelops.auth.auth.service.AuthDetailsService;
import io.levelops.auth.auth.token.CustomAuthenticationToken;
import io.levelops.auth.httpmodels.ChangePassRequest;
import io.levelops.auth.httpmodels.ForgotPassRequest;
import io.levelops.auth.httpmodels.JwtRequest;
import io.levelops.auth.httpmodels.JwtResponse;
import io.levelops.auth.httpmodels.ResetToken;
import io.levelops.auth.httpmodels.UserValidation;
import io.levelops.auth.services.SystemNotificationService;
import io.levelops.auth.services.TokenGenService;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.SamlConfig;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.SamlConfigService;
import io.levelops.commons.inventory.SecretsManagerServiceClient;
import io.levelops.commons.password.RandomPasswordGenerator;
import io.levelops.exceptions.EmailException;
import io.levelops.notification.services.TenantManagementNotificationService;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.util.Strings;
import org.jboss.aerogear.security.otp.Totp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Nullable;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@Log4j2
public class JwtAuthenticationController {
    private static final String ACTIVITY_LOG_LOGIN_TEXT = "%s User Login: %s.";
    private static final String ACTIVITY_LOG_PASS_RESET_TEXT = "User Pass Reset: %s.";
    private static final String DEFAULT_SAML_CONFIG_ID = "1";

    private final SecretsManagerServiceClient secretsManagerServiceClient;
    private final SystemNotificationService systemNotificationService;
    private final AuthenticationManager authenticationManager;
    private final AuthDetailsService authDetailsService;
    private final ActivityLogService activityLogService;
    private final SamlConfigService samlConfigService;
    private final TokenGenService tokenGenService;
    private final TenantManagementNotificationService tenantManagementNotificationService;
    private final ObjectMapper objectMapper;
    private final SamlService samlService;
    private final String cookieDomain;
    //this endpoint is going to handle error responses too by containing url param: 'error="sometext"'
    private final String authRedirect;

    @Autowired
    public JwtAuthenticationController(TokenGenService tokenGenService,
                                       ActivityLogService activityLogService,
                                       AuthenticationManager authenticationManager,
                                       AuthDetailsService jwtInMemoryUserDetailsService,
                                       SystemNotificationService systemNotificationService,
                                       SamlConfigService configService, SamlService samlService,
                                       final SecretsManagerServiceClient secretsManagerServiceClient,
                                       TenantManagementNotificationService tenantManagementNotificationService,
                                       @Qualifier("custom") ObjectMapper objectMapper,
                                       @Value("${COOKIE_DOMAIN:levelops.io}") String cookieDomain,
                                       @Value("${AUTH_REDIRECT:https://app.levelops.io/auth-callback}") String authRedirect) {
        this.tenantManagementNotificationService = tenantManagementNotificationService;
        this.objectMapper = objectMapper;
        this.authenticationManager = authenticationManager;
        this.authDetailsService = jwtInMemoryUserDetailsService;
        this.samlConfigService = configService;
        this.tokenGenService = tokenGenService;
        this.samlService = samlService;
        this.cookieDomain = cookieDomain;
        this.authRedirect = authRedirect;
        this.activityLogService = activityLogService;
        this.systemNotificationService = systemNotificationService;
        this.secretsManagerServiceClient = secretsManagerServiceClient;
    }

    private String validateCompany(String company) {
        if (StringUtils.isEmpty(company) || (!company.equalsIgnoreCase("_levelops") && !company.matches("[a-zA-Z0-9]+"))) {
            log.info("Invalid company: {}", company);
            return null;
        }
        return company.toLowerCase(); //since postgres schema doesnt stop capitals
    }

    private String getErrorRedirect(String errorText) {
        return authRedirect + "?error=" + URLEncoder.encode(errorText, StandardCharsets.UTF_8);
    }

    @Async
    @RequestMapping(value = "/v1/saml_auth", method = RequestMethod.POST)
    public CompletableFuture<Void> handleAssertion(@RequestParam("RelayState") final String relayState,
                                                   HttpServletRequest request, HttpServletResponse response) {
        return CompletableFuture.runAsync(() -> {
            try {
                String company = validateCompany(new String(Base64.getMimeDecoder().decode(relayState)));
                SamlConfig samlConfig = null;
                if (company == null) {
                    response.sendRedirect(getErrorRedirect("missing_company"));
                    return;
                }
                try {
                    samlConfig = samlConfigService.get(company, DEFAULT_SAML_CONFIG_ID).orElse(null);
                } catch (Exception e) {
                    log.error("Failed to get saml config for company={}", company, e);
                }
                if (samlConfig == null || !samlConfig.getEnabled()) {
                    response.sendRedirect(getErrorRedirect("sso_not_configured"));
                    return;
                }
                try {
                    final User userReq = samlService.handleSamlResponse(company, samlConfig, request, response);
                    String userEmail = userReq.getEmail();
                    if (StringUtils.isEmpty(userEmail)) {
                        response.sendRedirect(getErrorRedirect("invalid_saml_response"));
                        return;
                    }
                    ExtendedUser userDetails = authDetailsService.loadOrProvisionUser(userReq, company);
                    if (!userDetails.getSamlAuthEnabled()) {
                        response.sendRedirect(getErrorRedirect("sso_disabled_for_user"));
                        return;
                    }
                    final String token = tokenGenService.generateToken(company, userDetails);
                    Cookie cookie = new Cookie("token", token);
                    // determines whether the cookie should only be sent using a secure protocol, such as HTTPS or SSL
                    cookie.setSecure(true);
                    //10mins. A negative value means that the cookie is not stored persistently and will be deleted
                    //when the Web browser exits. A zero value causes the cookie to be deleted.
                    cookie.setMaxAge(600);
                    // The cookie is visible to all the pages in the directory you specify,
                    // and all the pages in that directory's subdirectories
                    cookie.setPath("/");
                    cookie.setDomain(cookieDomain);
                    response.addCookie(cookie);
                    response.sendRedirect(authRedirect);
                    final User user = authDetailsService.getUserFromDb(userEmail, company);
                    activityLogService.insert(company, ActivityLog.builder()
                            .targetItem(user.getId())
                            .email(userEmail)
                            .targetItemType(ActivityLog.TargetItemType.USER_LOGIN)
                            .body(String.format(ACTIVITY_LOG_LOGIN_TEXT, "SSO", user.getId()))
                            .details(Collections.emptyMap())
                            .action(ActivityLog.Action.SUCCESS)
                            .build());
                    tenantManagementNotificationService.checkFirstLoginAndNotifyIfNeeded(company, userEmail);
                } catch (Exception e) {
                    log.error("Received unparseable samlresponse for company={}", company, e);
                    response.sendRedirect(getErrorRedirect("unparsable_saml_response"));
                }
            } catch (Exception e) {
                log.error("Request to handle assertion failed with error.", e);
            }
        });
    }

    @Async
    @RequestMapping(value = "/v1/generate_authn", method = RequestMethod.GET)
    public CompletableFuture<Void> getAuthnRequest(HttpServletRequest request, HttpServletResponse response) {
        return CompletableFuture.runAsync(() -> {
            String company = null;
            try {
                company = validateCompany(request.getParameter("company"));
                SamlConfig samlConfig = null;
                if (company == null) {
                    response.sendRedirect(getErrorRedirect("missing_company"));
                    return;
                }
                try {
                    samlConfig = samlConfigService.get(company, DEFAULT_SAML_CONFIG_ID).orElse(null);
                } catch (Exception e) {
                    log.error("Failed to get saml config for company={}", company, e);
                }
                if (samlConfig == null || !samlConfig.getEnabled()) {
                    response.sendRedirect(getErrorRedirect("sso_not_configured"));
                    return;
                }
                try {
                    samlService.startAuthFlow(company, samlConfig, request, response);
                } catch (Exception e) {
                    log.error("Invalid saml config. Could not generate authn for company={}", company, e);
                    response.sendRedirect(getErrorRedirect("unsuccessful_saml_response"));
                }

            } catch (Exception e) {
                log.error("Request to get authn failed with error for company={}", company, e);
            }
        });
    }

    @RequestMapping(value = "/v1/forgot_password", method = RequestMethod.POST)
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPassRequest forgotPassRequest) {
        //All Results will return 200 ok to prevent information leak;
        String company = validateCompany(forgotPassRequest.getCompany());
        if (company == null) {
            return ResponseEntity.status(HttpStatus.OK).build();
        }
        try {
            User user = authDetailsService.getUserFromDb(forgotPassRequest.getUsername(),
                    company);
            String resetToken = RandomPasswordGenerator.nextString();
            //exit out if passwordresetdetails update fails
            if (!authDetailsService.updatePasswordResetToken(user.getId(),
                    company, resetToken)) {
                return ResponseEntity.ok().build();
            }
            user = User.builder().userType(user.getUserType())
                    .firstName(user.getFirstName()).lastName(user.getLastName())
                    .samlAuthEnabled(user.getSamlAuthEnabled())
                    .passwordAuthEnabled(user.getPasswordAuthEnabled())
                    .email(user.getEmail())
                    .id(user.getId()).build();
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(user.getId())
                    .email(user.getEmail())
                    .targetItemType(ActivityLog.TargetItemType.USER)
                    .body(String.format(ACTIVITY_LOG_PASS_RESET_TEXT, user.getId()))
                    .details(Collections.emptyMap())
                    .action(ActivityLog.Action.PASSWORD_RESET_STARTED)
                    .build());
            systemNotificationService.sendPasswordResetMessage(company, user, resetToken);
            return ResponseEntity.ok().build();
        } catch (BadCredentialsException | SQLException | JsonProcessingException | EmailException | UsernameNotFoundException e) {
            log.error("Error in forgot password", e);
            return ResponseEntity.status(HttpStatus.OK).build();
        }
    }

    @RequestMapping(value = "/v1/change_password", method = RequestMethod.POST)
    public ResponseEntity<?> changePassword(@RequestBody ChangePassRequest changePassRequest) {
        try {
            ResetToken resetToken = objectMapper.readValue(
                    new String(Base64.getMimeDecoder().decode(changePassRequest.getToken())),
                    ResetToken.class);
            String company = validateCompany(resetToken.getCompany());
            if (company == null) {
                log.warn("Reset prevented because company invalid in token.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("{\"error\":\"Invalid token provided - please restart the reset flow.\"}");
            }
            User user = authDetailsService.getUserFromDb(resetToken.getUsername(), company);
            //verify token equality and validity
            if (user.getPasswordResetDetails().getExpiry() == null ||
                    user.getPasswordResetDetails().getExpiry() < (System.currentTimeMillis() / 1000)) {
                log.warn("Reset prevented because token has expired.");
                return ResponseEntity.status(HttpStatus.GONE).body("{\"error\":\"Refresh-token has expired. " +
                        "Please restart the reset flow.\"}");
            }
            if (authDetailsService.isMatchInvalid(resetToken.getToken(),
                    user.getPasswordResetDetails().getToken())) {
                log.warn("Reset prevented because token did not match.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("{\"error\":\"Invalid token provided - please restart the reset flow.\"}");
            }
            //exit out if password update fails
            if (!authDetailsService.updatePassword(user.getId(), company,
                    changePassRequest.getNewPassword())) {
                log.warn("Reset failed because db rejected update.");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("{\"error\":\"Invalid token provided - please restart the reset flow.\"}");
            }
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(user.getId())
                    .email(resetToken.getUsername())
                    .targetItemType(ActivityLog.TargetItemType.USER)
                    .body(String.format(ACTIVITY_LOG_PASS_RESET_TEXT, user.getId()))
                    .details(Collections.emptyMap())
                    .action(ActivityLog.Action.PASSWORD_RESET_FINISHED)
                    .build());
            return ResponseEntity.ok().build();
        } catch (BadCredentialsException | UsernameNotFoundException | IOException | SQLException e) {
            log.error("error in change password", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @RequestMapping(value = "/v1/authenticate", method = RequestMethod.POST)
    public ResponseEntity<?> createAuthenticationToken(@RequestBody JwtRequest authenticationRequest) {
        String company = validateCompany(authenticationRequest.getCompany());
        if (company == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            Authentication at = authenticationManager.authenticate(
                    new CustomAuthenticationToken(
                            authenticationRequest.getUsername(),
                            authenticationRequest.getPassword(),
                            company));
            final ExtendedUser userDetails = (ExtendedUser) at.getPrincipal();
            if ((userDetails.getMfaRequired() || userDetails.getMfaEnrollment())) {
                log.info("[{}] MFA required for user '{}'", company, userDetails.getUsername());
                if (Strings.isBlank(authenticationRequest.getOtp()) || userDetails.getMfaEnrollment()) {
                    // if global enforcement is enabled and the user is not enrolled, ask for enrollment flow
                    if (userDetails.getMfaEnrollment()) {
                        final String token = tokenGenService.generateTokenForMFAEnrollment(company, userDetails);
                        return ResponseEntity.ok(Map.of("mfa_enrollment", true, "token", token));
                    }
                    // if not enforced, check if MFA is configured, if so ask for otp 
                    if (userDetails.getMfaEnabled()) {
                        return ResponseEntity.ok(Map.of("mfa_required", true));
                    }
                    return ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("The user authentication is misconfigured, contact an admin or support.");
                }
                // validate OTP
                // if not valid, call for unauthorized
                var secret = MFAController.getMFASecret(secretsManagerServiceClient, company, userDetails.getUsername());
                var otp = new Totp(secret);
                if (!otp.now().equals(authenticationRequest.getOtp())) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
            } else {
                log.info("[{}] No MFA required for user '{}'", company, userDetails.getUsername());
            }
            final String token = tokenGenService.generateToken(company, userDetails);
            User user = authDetailsService.getUserFromDb(authenticationRequest.getUsername(), company);
            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(user.getId())
                    .email(authenticationRequest.getUsername())
                    .targetItemType(ActivityLog.TargetItemType.USER_LOGIN)
                    .body(String.format(ACTIVITY_LOG_LOGIN_TEXT, "Password", user.getId()))
                    .details(Collections.emptyMap())
                    .action(ActivityLog.Action.SUCCESS)
                    .build());
            tenantManagementNotificationService.checkFirstLoginAndNotifyIfNeeded(
                    company, authenticationRequest.getUsername());
            return ResponseEntity.ok(new JwtResponse(token));
        } catch (BadCredentialsException | SQLException | AuthenticationServiceException e) {
            log.error("failed to authenticate for company={}: {}", company, e.getMessage());
            try {
                activityLogService.insert(company, ActivityLog.builder()
                        .targetItem("UNKNOWN")
                        .email(authenticationRequest.getUsername())
                        .targetItemType(ActivityLog.TargetItemType.USER_LOGIN)
                        .body(String.format(ACTIVITY_LOG_LOGIN_TEXT, "Password",
                                authenticationRequest.getUsername()))
                        .details(Collections.emptyMap())
                        .action(ActivityLog.Action.FAIL)
                        .build());
            } catch (SQLException ex) {
                log.error("Failed to insert activity log", ex);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @RequestMapping(value = "/v1/refresh", method = RequestMethod.POST)
    public ResponseEntity<?> refreshAuthenticationToken(@SessionAttribute(name = "session_user") String username,
                                                        @SessionAttribute(name = "company") String company)
            throws SQLException {
        company = validateCompany(company);
        if (company == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        final ExtendedUser userDetails = authDetailsService.loadUserByUsernameAndOrg(username, company);
        final String token = tokenGenService.generateToken(company, userDetails);
        final User user = authDetailsService.getUserFromDb(username, company);
        activityLogService.insert(company, ActivityLog.builder()
                .targetItem(user.getId())
                .email(username)
                .targetItemType(ActivityLog.TargetItemType.USER_LOGIN)
                .body(String.format(ACTIVITY_LOG_LOGIN_TEXT, "Password", user.getId()))
                .details(Collections.singletonMap("extra_info", "Token Refreshed."))
                .action(ActivityLog.Action.SUCCESS)
                .build());
        return ResponseEntity.ok(new JwtResponse(token));
    }

    @Nullable
    protected static String sanitizeEmail(@Nullable String email) {
        if (email == null) {
            return null;
        }
        // email cannot contain spaces, but it can contain + signs
        // since + can be interpreted as a space because of html encoding, we can put them back manually:
        return email.replaceAll(" ", "+").toLowerCase();
    }

    @RequestMapping(path = "/v1/validate/email", method = RequestMethod.GET)
    public DeferredResult<ResponseEntity<UserValidation>> isMultiTenantUser(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {

        return SpringUtils.deferResponse(() -> {

            String email = sanitizeEmail(request.getParameter("email"));

            if (email == null) {
                return ResponseEntity.ok(UserValidation.builder()
                        .errorMessage("Invalid Email parameter provided")
                        .build());
            }

            List<User> userList = authDetailsService.getUsersAcrossTenants(email);

            if (CollectionUtils.isEmpty(userList)) {
                return ResponseEntity.ok(UserValidation.builder()
                        .isValidEmail(false)
                        .errorMessage("Invalid email id provided")
                        .build());
            }

            UserValidation validUser = UserValidation.builder()
                    .isValidEmail(true)
                    .firstName(userList.get(0).getFirstName())
                    .isMultiTenant(true)
                    .build();

            if (userList.size() == 1) {
                User user = userList.get(0);
                validUser = validUser.toBuilder()
                        .isMultiTenant(false)
                        .company(user.getCompany())
                        .build();

                if (user.getSamlAuthEnabled()) {
                    validUser = validUser.toBuilder()
                            .isSSOEnabled(true)
                            .build();
                }
            }
            return ResponseEntity.ok(validUser);
        });
    }

    @RequestMapping(value = "/v1/validate/company", method = RequestMethod.GET)
    public DeferredResult<ResponseEntity<UserValidation>> validateCompany(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        return SpringUtils.deferResponse(() -> {
            String company = validateCompany(request.getParameter("company"));
            String email = sanitizeEmail(request.getParameter("email"));

            if (company == null || email == null) {
                return ResponseEntity.ok(UserValidation.builder()
                        .status(false)
                        .errorMessage("User email or company cannot be empty")
                        .build());
            }
            UserValidation validUser = UserValidation.builder()
                    .status(true)
                    .isSSOEnabled(false)
                    .build();
            try {
                User user = authDetailsService.getUserFromDb(email, company);
                if (user.getSamlAuthEnabled()) {
                    validUser = validUser.toBuilder()
                            .isSSOEnabled(true)
                            .build();
                }
            } catch (Exception e) {
                log.warn("Failed to get user from db: company={}, email={}", company, email, e);
                return ResponseEntity.ok(UserValidation.builder()
                        .status(false)
                        .errorMessage("Invalid user email or company provided")
                        .build());
            }

            return ResponseEntity.ok(validUser);
        });
    }

}
