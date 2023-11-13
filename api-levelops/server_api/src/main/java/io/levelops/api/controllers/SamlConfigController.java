package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.exceptions.ServerApiException;
import io.levelops.api.responses.SamlConfigResponse;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.SamlConfig;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.SamlConfigService;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;

@RestController
@RequestMapping("/v1/samlconfig")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@Log4j2
public class SamlConfigController {
    private static final String ACTIVITY_LOG_TEXT = "%s Saml Config.";
    private static final String SAML_CONFIG_ID = "1";

    private final String spId;
    private final String acsUrl;
    private final SamlConfigService samlConfigService;
    private final ActivityLogService activityLogService;

    @Autowired
    public SamlConfigController(SamlConfigService configService, ActivityLogService activityLogService,
                                @Value("${ACS_URL:https://api.levelops.io/v1/saml_auth}") String acsUrl,
                                @Value("${SP_IDENTITY_ID}") String spId) {
        this.spId = spId;
        this.acsUrl = acsUrl;
        this.samlConfigService = configService;
        this.activityLogService = activityLogService;
    }

    private void verifyX509Cert(String cert) throws CertificateException {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate certificate =
                (X509Certificate) certFactory.generateCertificate(new ByteArrayInputStream(cert.getBytes()));
        certificate.checkValidity();
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_EDIT)
    @RequestMapping(method = RequestMethod.PUT, produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> updateSamlConfig(@RequestBody SamlConfig config,
                                                                 @SessionAttribute(name = "session_user") String sessionUser,
                                                                 @SessionAttribute(name = "company") String company) {
        final SamlConfig finalConfig = SamlConfig.builder().id(SAML_CONFIG_ID).enabled(config.getEnabled())
                .idpCert(config.getIdpCert()).idpId(config.getIdpId())
                .idpSsoUrl(config.getIdpSsoUrl()).build();
        return SpringUtils.deferResponse(() -> {
            try {
                if (StringUtils.isEmpty(finalConfig.getIdpId()) || StringUtils.isEmpty(finalConfig.getIdpCert())
                        || StringUtils.isEmpty(finalConfig.getIdpSsoUrl()) || finalConfig.getEnabled() == null) {
                    throw new ServerApiException(HttpStatus.BAD_REQUEST, "Not all required fields present.");
                }
                verifyX509Cert(new String(Base64.getMimeDecoder().decode(finalConfig.getIdpCert())));
                if (samlConfigService.get(company, SAML_CONFIG_ID).isPresent())
                    samlConfigService.update(company, finalConfig);
                else
                    samlConfigService.insert(company, finalConfig);
                activityLogService.insert(company, ActivityLog.builder()
                        .targetItem(SAML_CONFIG_ID)
                        .email(sessionUser)
                        .targetItemType(ActivityLog.TargetItemType.SSO_CONFIG)
                        .body(String.format(ACTIVITY_LOG_TEXT, "Edited"))
                        .details(Collections.singletonMap("item", finalConfig))
                        .action(ActivityLog.Action.EDITED)
                        .build());
                return ResponseEntity.accepted().build();
            } catch (CertificateException e) {
                log.error(e);
                throw new BadRequestException("Invalid certificate. Either expired or not pem format.");
            }
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_DELETE)
    @RequestMapping(method = RequestMethod.DELETE, produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> samlConfigDelete(@SessionAttribute(name = "company") String company,
                                                                 @SessionAttribute(name = "session_user") String sessionUser) {
        return SpringUtils.deferResponse(() -> {
                samlConfigService.delete(company, SAML_CONFIG_ID);
                activityLogService.insert(company, ActivityLog.builder()
                        .targetItem(SAML_CONFIG_ID)
                        .email(sessionUser)
                        .targetItemType(ActivityLog.TargetItemType.SSO_CONFIG)
                        .body(String.format(ACTIVITY_LOG_TEXT, "Deleted"))
                        .details(Collections.emptyMap())
                        .action(ActivityLog.Action.DELETED)
                        .build());
                return ResponseEntity.ok().build();
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    public DeferredResult<ResponseEntity<SamlConfigResponse>> getSamlConfig(@SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(new SamlConfigResponse(samlConfigService.get(company, SAML_CONFIG_ID)
                .orElse(SamlConfig.builder().build()), new String(Base64.getEncoder().encode(company.getBytes())), acsUrl, spId)));
    }
}