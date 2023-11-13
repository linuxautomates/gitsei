package io.levelops.api.controllers.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.api.controllers.ProductsController;
import io.levelops.api.requests.TenantRequest;
import io.levelops.api.requests.TenantType;
import io.levelops.auth.auth.service.AuthDetailsService;
import io.levelops.auth.httpmodels.ResetToken;
import io.levelops.auth.utils.TenantUtilService;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.TenantState;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.OUDashboard;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.DefaultConfigService;
import io.levelops.commons.databases.services.OUDashboardService;
import io.levelops.commons.databases.services.TenantService;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.password.RandomPasswordGenerator;
import io.levelops.exceptions.EmailException;
import io.levelops.integrations.jira.client.JiraClientException;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.models.EmailContact;
import io.levelops.notification.services.NotificationService;
import io.levelops.notification.services.TenantManagementNotificationService;
import io.levelops.services.TemplateService;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

@Log4j2
@RestController
@RequestMapping("/v1/admin/tenant")
public class TenantManagementController {
    private static final String DEFAULT_TENANT_CONFIG_NAME = "DEFAULT_DASHBOARD";
    private static final String DEMO_DASHBOARD_NAME = "Demo Dashboard";
    private static final Pattern TENANT_ID_PATTERN = Pattern.compile("[a-zA-Z][a-zA-Z0-9]*");

    private final NotificationService notificationService;
    private final ClientHelper<Exception> clientHelper;
    private final TemplateService templateService;
    private final AuthDetailsService authDetailsService;
    private final DefaultConfigService configService;
    private final DashboardWidgetService dashboardWidgetService;
    private final TenantService tenantService;
    private final TenantManagementNotificationService tenantManagementNotificationService;
    private final OrgUnitCategoryDatabaseService categoryService;

    private final String adminUIContents;
    private final String baseURL;
    private final ObjectMapper mapper;

    private final String welcomeEmailContents;
    private final ProductsController productsController;
    private final OUDashboardService ouDashboardService;
    private final TenantUtilService tenantUtilService;

    @Autowired
    public TenantManagementController(
            final ObjectMapper mapper,
            final OkHttpClient client,
            final NotificationService notificationService,
            final TemplateService templateService,
            final AuthDetailsService authDetailsService,
            final DefaultConfigService configService,
            final DashboardWidgetService dashboardWidgetService,
            final TenantService tenantService,
            /* TODO remove OAUTH_BASE_URL in favor of APP_BASE_URL when both URL have been harmonized */
            final TenantManagementNotificationService tenantManagementNotificationService,
            @Value("${OAUTH_BASE_URL:https://testui1.propelo.ai}") final String baseURL,
            final ProductsController productsController,
            final OrgUnitCategoryDatabaseService categoryService,
            final OUDashboardService ouDashboardService, TenantUtilService tenantUtilService) {
        this.mapper = mapper;
        this.notificationService = notificationService;
        this.tenantManagementNotificationService = tenantManagementNotificationService;
        this.tenantUtilService = tenantUtilService;
        this.clientHelper = new ClientHelper<>(client, mapper, Exception.class);
        this.baseURL = baseURL;
        this.templateService = templateService;
        this.authDetailsService = authDetailsService;
        this.configService = configService;
        this.dashboardWidgetService = dashboardWidgetService;
        this.tenantService = tenantService;
        this.productsController = productsController;
        this.categoryService = categoryService;
        this.ouDashboardService = ouDashboardService;
        var adminUIContents = "Page not available. Contact support";
        try {
            adminUIContents = templateService.evaluateTemplate(
                    String.join("\n", IOUtils.readLines(new FileReader("/app/propelo/admin_login.html"))),
                    Map.of("baseUrl", baseURL));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.adminUIContents = adminUIContents;

        var welcomeEmailContents = "<h2>Welcome to Propelo.ai. <a href=\"$baseURL\">Login here</a></h2>";
        try {
            welcomeEmailContents = String.join("\n", IOUtils.readLines(new FileReader("/app/propelo/welcome_email.html")));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.welcomeEmailContents = welcomeEmailContents;
    }

    @GetMapping(path = "", produces = MediaType.TEXT_HTML_VALUE)
    public DeferredResult<ResponseEntity<String>> adminUI() {
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok(adminUIContents);
        });
    }

    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(path = "/enroll", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<Map<String, String>>> createTenant(
            @SessionAttribute("session_user") final String user,
            @RequestBody final TenantRequest tenantRequest) {
        return SpringUtils.deferResponse(() -> {
            if (StringUtils.isBlank(tenantRequest.getTenantName()) || !TENANT_ID_PATTERN.matcher(tenantRequest.getTenantName()).matches()) {
                throw new BadRequestException("Invalid company name: '" + tenantRequest.getTenantName() + "'. Please only use alphanumerical characters. Must start with a letter.");
            }
            String tenantId = tenantRequest.getTenantName().toLowerCase().trim();
            String companyName = tenantRequest.getTenantName();
            String userEmail = tenantRequest.getUserEmail();
            String userFirstName = tenantRequest.getUserName();
            String userLastName = tenantRequest.getUserLastname();
            Boolean createInitechWorkspace = tenantRequest.getCreateInitechWorkspace();
            log.info("[{}] Tenant request for {}: {}", user, tenantId, tenantRequest);

            // check if tenant already exists
            var tenant = tenantService.get("", tenantId);
            if (tenant.isPresent()) {
                log.info("Tenant '{}'' already exists! ", tenantId);
                return ResponseEntity.badRequest().body(Map.of("message", "Tenant already exists"));
            }

            // create license
            TenantType tenantType = MoreObjects.firstNonNull(tenantRequest.getTenantType(), TenantType.TRIAL_TENANT);
            createLicense(tenantId, tenantType.getLicenseType());

            // create tenant schema
            createTenantSchema(tenantId, companyName, userFirstName, userLastName, userEmail);
            // create default workspace
                var defaultProduct = Product.builder()
                        .bootstrapped(true)
                        .immutable(true)
                        .name("Default Workspace")
                        .description("Default Workspace")
                        .key("DEFAULT")
                        .demo(false)
                        .build();
                productsController.createWorkspace(defaultProduct, "do-not-reply@propelo.ai", tenantId);

                // create Demo workspace
                productsController.createDemoWorkspace("do-not-reply@propelo.ai", tenantId);

            // generate password reset token
            String token;
            try {
                token = generateResetPasswordToken(tenantId, userEmail);
            } catch (UsernameNotFoundException e) {
                log.error("Unable to find the requested user in the new tenant (didn't get created?): {}", userEmail, e);
                return ResponseEntity.ok(Map.of("status", "tenant created but unable to create the user '" + userEmail + "'. CustomerSuccess account should be able to create the user from the UI (a different welcome email will be sent to the customer).", "url", baseURL));
            }

            // send email
            sendEmailNotification(tenantId, userEmail, userFirstName, token);

            // create jira ticket and send internal notification
            Map<String, String> responseBody = handleInternalJiraAndNotification(tenantId, userEmail, user);

            return ResponseEntity.ok(responseBody);
        });
    }
    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(path = "/get_active_tenant", produces = "application/json")
    public ResponseEntity<List<String>> getActiveTenants() {
        try {
            List<String> activeTenants = tenantUtilService.getActiveTenants();
            return ResponseEntity.ok(activeTenants);
        } catch (ExecutionException e) {
            // Handling the exception
            return ResponseEntity.status(500).body(List.of());
        }
    }



    @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @PostMapping(path = "/create_demo_workspace", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public DeferredResult<ResponseEntity<String>> createDemoWorkspace(
            @SessionAttribute("session_user") final String user,
            @RequestBody final TenantRequest tenantRequest) {
        return SpringUtils.deferResponse(() -> {
            // create Demo workspace
            String tenantId = tenantRequest.getTenantName();
            if(StringUtils.isEmpty(tenantRequest.getTenantName())){
                throw new BadRequestException("tenant_name missing from body");
            }
            productsController.createDemoWorkspace("do-not-reply@propelo.ai", tenantId);
            return ResponseEntity.ok(tenantId);
        });
    }

    private void createLicense(String tenantId, String licenseType) throws Exception {
        var urlLicensing = HttpUrl.parse("http://licensing").newBuilder()
                .addPathSegment("v1")
                .addPathSegment("licensing")
                .addPathSegment(tenantId)
                .build();
        var licensePayload = Map.of("license", licenseType);
        var requestLicensing = new Request.Builder()
                .url(urlLicensing)
                .post(clientHelper.createJsonRequestBody(licensePayload))
                .build();

        var responseLicensing = clientHelper.executeRequest(requestLicensing);
        log.info("[{}] tenant license assignment: {}", tenantId, responseLicensing);
    }

    private void createTenantSchema(String tenantId, String companyName, String userFirstName, String userLastName, String userEmail) throws Exception {
        var url = HttpUrl.parse("http://internal-api-lb").newBuilder()
                .addPathSegment("internal")
                .addPathSegment("v1")
                .addPathSegment("_ensure_tenant_schema")
                .addQueryParameter("company", tenantId)
                .addQueryParameter("company_name", companyName)
                .addQueryParameter("default_user_name", userFirstName)
                .addQueryParameter("default_user_lastname", userLastName)
                .addQueryParameter("default_user_email", userEmail)
                .build();
        var request = new Request.Builder()
                .url(url)
                .post(okhttp3.RequestBody.create("{}", okhttp3.MediaType.get("application/json")))
                .build();

        var response = clientHelper.executeRequest(request);
        log.info("[{}] tenant schema creation response: {}", tenantId, response);

    }

    private void setDemoDashboardConfig(String tenantId) throws SQLException {
        IterableUtils.getFirst(dashboardWidgetService.listByFilters(tenantId,
                        DashboardWidgetService.DashboardFilter.builder()
                                .isDemo(true)
                                .exactName(DEMO_DASHBOARD_NAME)
                                .build(), 0, 1, List.of()).getRecords())
                .map(Dashboard::getId)
                .ifPresent(id -> {
                    try {
                        // set default in settings
                        configService.handleDefault(tenantId, id, DEFAULT_TENANT_CONFIG_NAME);

                        // set as default for the "All Teams" OU
                        var teamsCategoryResults = categoryService.filter(tenantId, QueryFilter.builder()
                            .strictMatch("name", "Teams")
                            .build(), 0, 1);
                        log.info("[{}] demo dashboard category count:{}", tenantId, teamsCategoryResults.getCount());
                        teamsCategoryResults.getRecords().forEach(category -> {
                                try {
                                    log.info("[{}] All teams root OU id: {}", tenantId, category.getRootOuId());
                                    ouDashboardService.insert(tenantId, OUDashboard.builder()
                                        .dashboardId(Integer.parseInt(id))
                                        .ouId(category.getRootOuId())
                                        .dashboardOrder(1)
                                        .build());
                                    log.info("[{}] Demo dashboard associated to Teams Category", tenantId);
                                } catch (SQLException e) {
                                    log.warn("[{}] Unable to associate the demo dashboard to the 'All Teams' OU from the default workspace..", tenantId);
                                }
                            });;
                    } catch (SQLException e) {
                        log.warn("Failed to store demo dashboard id in tenant config table", e);
                    }
                });
    }

    private String generateResetPasswordToken(String tenantId, String userEmail) throws SQLException, JsonProcessingException {
        User u = authDetailsService.getUserFromDb(userEmail, tenantId);
        String resetToken = RandomPasswordGenerator.nextString();
        if (!authDetailsService.updatePasswordResetToken(u.getId(), tenantId, resetToken, 48 * 60 * 60)) {
            log.error("Error setting the password reset token");
        }
        return new String(Base64.getEncoder().encode(
                mapper.writeValueAsBytes(
                        ResetToken.builder()
                                .company(tenantId)
                                .token(resetToken)
                                .username(userEmail)
                                .build())));
    }

    private void sendEmailNotification(String tenantId, String userEmail, String userFirstName, String token) throws EmailException {
        var from = EmailContact.builder()
                .email("do-not-reply@propelo.ai")
                .name("Propelo")
                .build();
        notificationService.sendEmailNotification(
                "Welcome to Propelo",
                templateService.evaluateTemplate(welcomeEmailContents, Map.of(
                        "baseUrl", baseURL,
                        "name", userFirstName,
                        "username", userEmail,
                        "tenant", tenantId,
                        "token", token,
                        "company_info", "",
                        "logo", "",
                        "unsubscribe", "")),
                from,
                userEmail
        );

    }

    private Map<String, String> handleInternalJiraAndNotification(String companyName, String userEmail, String sessionUser) {
        Optional<String> issueCreationError = Optional.empty();
        Optional<String> notificationSendError = Optional.empty();
        String jiraIssueUrl = "";
        String status;

        try {
            Optional<JiraIssue> issue = this.tenantManagementNotificationService.createTenantCreationJiraTicket(
                    companyName, userEmail, sessionUser);
            if (issue.isPresent()) {
                jiraIssueUrl = issue.get().getKey();
            }
        } catch (JiraClientException e) {
            issueCreationError = Optional.of("Jira Creation Failed");
            log.error("Failed to create jira ticket for " + companyName + ". All other tenant creation steps" +
                    "succeeded, so responding with success. Exception: " + e.getMessage(), e);
        }

        try {
            this.tenantManagementNotificationService.sendTenantCreationSuccessfulNotifications(companyName, jiraIssueUrl);
        } catch (Exception e) {
            notificationSendError = Optional.of("Tenant creation notification failed to send");
            log.error("Failed to send tenant creation notifications for " + companyName + ". All other tenant" +
                    " creation steps succeeded, so responding with success. Exception: " + e.getMessage(), e);
        }

        if (issueCreationError.isPresent() || notificationSendError.isPresent()) {
            status = "All essential steps successful\n" +
                    issueCreationError.orElse("") + " " + notificationSendError.orElse("");
        } else {
            status = "ok";
        }

        return Map.of("status", status, "url", baseURL, "jiraUrl", jiraIssueUrl);
    }

    // @PreAuthorize("hasAuthority('TENANT_ADMIN')")
    @GetMapping(path = "/state", produces = "application/json")
    public DeferredResult<ResponseEntity<DbListResponse<TenantState>>> getTenantState(
            @SessionAttribute("company") final String company) {
        return SpringUtils.deferResponse(() -> {
            var url = HttpUrl.parse("http://internal-api-lb").newBuilder()
                    .addPathSegment("internal")
                    .addPathSegment("v1")
                    .addPathSegment("tenants")
                    .addPathSegment(company)
                    .addPathSegment("state")
                    .build();
            var request = new Request.Builder()
                    .url(url)
                    .get()
                    .build();
            DbListResponse<TenantState> response = clientHelper.executeAndParse(request, DbListResponse.class);
            return ResponseEntity.ok(response);
        });
    }

}
