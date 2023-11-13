package io.levelops.api.services;

import io.levelops.api.model.dev_productivity.DevProductivityFixedIntervalFilter;
import io.levelops.api.model.dev_productivity.EffectiveOUs;
import io.levelops.api.services.dev_productivity.DevProductivityRBACService;
import io.levelops.auth.auth.config.Auth;
import io.levelops.commons.databases.services.organization.OrgAccessValidationService;
import io.levelops.web.exceptions.ForbiddenException;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DevProductivityRBACServiceTest {

    private static final String company = "test";
    OrgAccessValidationService orgAccessValidationService;
    @Test
    public void test() throws ForbiddenException {

        Map<String, List<String>> scopes = Map.of("dev_productivity_write", List.of());
        String requestorEmail = "ashish@harness.io";
        List<UUID> ouIds = List.of(UUID.randomUUID());
        List<Integer> ouRefIds = List.of(1);
        DevProductivityFixedIntervalFilter devProductivityFilter = DevProductivityFixedIntervalFilter.builder()
                .ouIds(ouIds)
                .ouRefIds(ouRefIds)
                .build();

        //checking legacy flow
        orgAccessValidationService = Mockito.mock(OrgAccessValidationService.class);
        DevProductivityRBACService rbacService = new DevProductivityRBACService(orgAccessValidationService, new Auth(true));
        EffectiveOUs effectiveOUs = rbacService.getEffectiveOUsWithAccess(company, scopes, requestorEmail, devProductivityFilter);
        Assert.assertNotNull(effectiveOUs);
        Assertions.assertThat(effectiveOUs.getOuIds()).isEqualTo(ouIds);
        Assertions.assertThat(effectiveOUs.getOrgRefIds()).isEqualTo(ouRefIds);

        //checking harness flow
        scopes = Map.of("no_legacy_scope", List.of());
        rbacService = new DevProductivityRBACService(orgAccessValidationService, new Auth(false));
        effectiveOUs = rbacService.getEffectiveOUsWithAccess(company, scopes, requestorEmail, devProductivityFilter);
        Assert.assertNotNull(effectiveOUs);
        Assertions.assertThat(effectiveOUs.getOuIds()).isEqualTo(ouIds);
        Assertions.assertThat(effectiveOUs.getOrgRefIds()).isEqualTo(ouRefIds);

    }
}
