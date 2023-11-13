package io.levelops.api.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.api.utils.SelfServeEndpointUtils;
import io.levelops.commons.licensing.model.License;
import io.levelops.commons.licensing.service.LicensingService;
import io.levelops.web.exceptions.ForbiddenException;
import io.levelops.web.util.SpringUtils;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;

@RestController
@RequestMapping("/v1/entitlements/")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@Log4j2
@SuppressWarnings("unused")
public class EntitlementsController {
    @JsonProperty("licensing_service")
    private final LicensingService licensingService;

    @Autowired
    public EntitlementsController(LicensingService licensingService) {
        this.licensingService = licensingService;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = AppendEntitlementsResult.AppendEntitlementsResultBuilder.class)
    public static final class AppendEntitlementsResult {
        @JsonProperty("before")
        private final License before;
        @JsonProperty("after")
        private final License after;
    }

    @PatchMapping("/append")
    public DeferredResult<ResponseEntity<AppendEntitlementsResult>> appendEntitlements(
            @SessionAttribute(name = "company") String company,
            @SessionAttribute(name = "session_user") String sessionUser,
            @RequestBody List<String> addEntitlements) throws ForbiddenException {
        SelfServeEndpointUtils.validateUser(sessionUser);
        return SpringUtils.deferResponse(() -> {
            Validate.isTrue(CollectionUtils.isNotEmpty(addEntitlements), "entitlements to append cannot be null or empty.");
            License before = licensingService.getLicense(company);
            License updated = licensingService.appendEntitlements(company, addEntitlements);
            return ResponseEntity.ok(AppendEntitlementsResult.builder().before(before).after(updated).build());
        });
    }

}
