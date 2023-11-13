package io.levelops.api.controllers;

import io.harness.atlassian_connect.AtlassianConnectServiceClient;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@RequestMapping("/v1/atlassian-connect")
@PreAuthorize("hasAnyAuthority('ADMIN','LIMITED_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
@Log4j2
public class AtlassianConnectController {
    private final AtlassianConnectServiceClient atlassianConnectServiceClient;

    public AtlassianConnectController(AtlassianConnectServiceClient atlassianConnectServiceClient) {
        this.atlassianConnectServiceClient = atlassianConnectServiceClient;
    }

    @RequestMapping(method = RequestMethod.POST, path = "/otp/generate", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> generateOtp(
            @SessionAttribute("company") final String company) {
        return SpringUtils.deferResponse(() -> {
            return ResponseEntity.ok(atlassianConnectServiceClient.generateOtp(company));
        });
    }

    @RequestMapping(method = RequestMethod.POST, path = "/otp/claim", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> claim(
            @SessionAttribute("company") final String company,
            @RequestParam final String otp) {
        return SpringUtils.deferResponse(() -> {
            var claimResponse = atlassianConnectServiceClient.claimOtp(company, otp);
            if (claimResponse.isEmpty()) {
                return ResponseEntity.accepted().build();
            } else {
                return ResponseEntity.ok(claimResponse.get());
            }
        });
    }
}
