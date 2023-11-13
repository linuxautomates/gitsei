package io.levelops.aggregations.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.sendgrid.helpers.mail.objects.Attachments;
import io.levelops.aggregations.models.TenantActivityReport;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.TenantService;
import io.levelops.commons.enviornment.PropeloEnvironmentType;
import io.levelops.commons.enviornment.PropeloEnvironmentUtils;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.licensing.exception.LicensingException;
import io.levelops.commons.licensing.model.LicenseType;
import io.levelops.commons.licensing.service.LicensingService;
import io.levelops.exceptions.EmailException;
import io.levelops.models.Email;
import io.levelops.models.EmailContact;
import io.levelops.notification.clients.SlackBotClient;
import io.levelops.notification.clients.SlackBotInternalClientFactory;
import io.levelops.notification.clients.SlackClientException;
import io.levelops.notification.utils.SlackHelper;
import io.levelops.services.EmailService;
import lombok.extern.log4j.Log4j2;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Creates a global report for ALL tenant activity. It looks at the activity logs for every tenant, gets relevant
 * data and pushes it to a CSV file via Slack and email. Currently, we're only looking at latest non-Propelo login's
 * to all tenants, but this is built to be extensible.
 */
@Component
@Log4j2
public class InternalTenantActivityReportService {
    private final TenantService tenantService;
    private final LicensingService licensingService;
    private final ActivityLogService activityLogService;
    private final EmailService emailService;
    private SlackBotClient slackClient;
    private final SlackHelper slackHelper;

    private final String slackChannelId;
    private final PropeloEnvironmentType currentEnvironment;

    private static final String DAILY_PROD_REPORT_EMAIL_DESTINATION = "tenantStatus@propelo.ai";

    @Autowired
    InternalTenantActivityReportService(
            TenantService tenantService,
            LicensingService licensingService,
            ActivityLogService activityLogService,
            SlackBotInternalClientFactory slackBotInternalClientFactory,
            EmailService emailService,
            ObjectMapper objectMapper,
            @Value("${TENANT_STATUS_SLACK_CHANNEL_ID:C03NCTPS6CW}") String slackChannelId,
            @Value("${GOOGLE_CLOUD_PROJECT}") String aggProject) {
        this.tenantService = tenantService;
        this.licensingService = licensingService;
        this.activityLogService = activityLogService;
        this.emailService = emailService;
        this.slackChannelId = slackChannelId;
        this.slackHelper = new SlackHelper(objectMapper);
        this.currentEnvironment = PropeloEnvironmentUtils.getEnvironmentFromAggProject(aggProject);
        try {
            slackClient = slackBotInternalClientFactory.get();
        } catch (SlackClientException e) {
            log.error("Failed to initialize slack client: " + e.getMessage(), e);
            slackClient = null;
        }
    }

    @Scheduled(cron = "0 0 6 * * *")
    @SchedulerLock(name = "TenantActivityReport")
    public void runAndSendReport() throws IOException, SlackClientException, EmailException {
        try {
            Stopwatch stopWatch = Stopwatch.createStarted();
            List<TenantActivityReport> results = runReportForAllTenants();
            sendReportNotification(results);
            stopWatch.stop();
            log.info("Tenant activity report took: " + stopWatch);
        } catch (Exception e) {
            log.error("Tenant activity report failed: " + e.getMessage(), e);
            sendSlackMessageIfAvailable("Failed to run tenant activity report. Please check aggs logs.");
        }

    }

    private void sendSlackMessageIfAvailable(String message) throws SlackClientException, JsonProcessingException {
        if (slackClient != null) {
            slackClient.postChatInteractiveMessage(
                    slackChannelId, message, slackHelper.getPlainTextSlackBlock("Tenant activty report"));
        }
    }

    private void sendReportNotification(List<TenantActivityReport> results) throws IOException, SlackClientException, EmailException {
        String xmlStr = TenantActivityReport.toXml(results);
        LocalDateTime dateTime = LocalDateTime.now(ZoneId.of("America/Los_Angeles"));
        var fileName = "Daily-Tenant-Report-" +
                currentEnvironment.toString() + "-" + dateTime.format(DateTimeFormatter.ISO_DATE_TIME) + ".csv";

        if (slackClient != null) {
            slackClient.fileUpload(List.of(slackChannelId), fileName, xmlStr, "activity-report");
        }

        if(currentEnvironment.isProd()) {
            var from = EmailContact.builder()
                    .email("do-not-reply@propelo.ai")
                    .name("Propelo")
                    .build();
            emailService.send(
                    Email.builder()
                            .subject("Daily " + currentEnvironment + " Tenant Activity Report")
                            .content("Tenant activity report for " + currentEnvironment + " attached below")
                            .contentType("text/html")
                            .recipient(DAILY_PROD_REPORT_EMAIL_DESTINATION)
                            .attachments(List.of(
                                    new Attachments.Builder(fileName,
                                            IOUtils.toInputStream(xmlStr, "UTF-8")).build()
                                    )
                            )
                            .from(from)
                            .build()
            );
        }

    }

    public List<TenantActivityReport> runReportForAllTenants() {
        return PaginationUtils.stream(0, 1, RuntimeStreamException.wrap(page ->
                        tenantService.list("", page, 100).getRecords()))
                .parallel()
                .map(RuntimeStreamException.wrap(this::runReportForTenant))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted()
                .collect(Collectors.toList());
    }

    public Optional<TenantActivityReport> runReportForTenant(Tenant tenant) throws LicensingException {
        try {
            log.info("Running report for tenant:" + tenant);
            var activityLogs = activityLogService.listMostRecentLogins(
                    tenant.getTenantName(), List.of(ActivityLog.Action.SUCCESS), List.of("propelo", "levelops"));
            var activity_log = activityLogs.getRecords().stream().findFirst();
            var licenseType = getLicenseTypeForTenant(tenant.getTenantName());
            return Optional.of(TenantActivityReport.builder()
                    .tenantId(tenant.getId())
                    .tenantName(tenant.getTenantName())
                    .licenseType(licenseType)
                    .lastLoggedInUser(activity_log.map(ActivityLog::getEmail).orElse(""))
                    .loginTimeEpochSeconds(activity_log.map(ActivityLog::getCreatedAt).orElse(null))
                    .build());
        } catch (Exception e) {
            log.error("Failed to generate activity report for tenant: " + tenant.getTenantName() +
                    ". Error: " + e.getMessage(), e);
            log.error("", e);
            return Optional.empty();
        }
    }

    private LicenseType getLicenseTypeForTenant(String tenantName) throws LicensingException {
        try {
            return LicenseType.fromString(licensingService.getLicense(tenantName).getLicense());
        } catch (LicensingException e) {
            log.error("Failed to get license information. Error: " + e.getMessage(), e);
            return LicenseType.UNKNOWN;
        }
    }
}
