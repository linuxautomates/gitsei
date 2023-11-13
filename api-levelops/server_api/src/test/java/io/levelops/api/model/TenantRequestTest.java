package io.levelops.api.model;

import io.levelops.api.requests.TenantRequest;
import io.levelops.api.requests.TenantType;
import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import java.io.IOException;

public class TenantRequestTest {

    @Test
    public void testDeserialize() throws IOException{
        var request = DefaultObjectMapper .get().readValue(ResourceUtils.getResourceAsString("model/tenant_request.json"), TenantRequest.class);
        Assertions.assertThat(request).isNotNull();
        Assertions.assertThat(request.getTenantName()).isNotNull();
        Assertions.assertThat(request.getUserName()).isNotNull();
        Assertions.assertThat(request.getUserEmail()).isNotNull();
        Assertions.assertThat(request.getTenantType()).isNotNull();
        Assertions.assertThat(request.getTenantType()).isEqualByComparingTo(TenantType.TRIAL_TENANT);
    }

    private TenantRequest createTenantWithType(TenantType type) {
       return TenantRequest.builder()
               .tenantName("S")
               .userName("Sid")
               .userLastname("B")
               .userEmail("s@propelo.ai")
               .tenantType(type)
               .build();
    }
    @Test
    public void testSerializeDeserialize() throws IOException {
        var fullTenant = createTenantWithType(TenantType.FULL_TENANT);
        var trialTenant= createTenantWithType(TenantType.TRIAL_TENANT);

        var tenants = new TenantRequest[]{fullTenant, trialTenant};

        for (var tenant: tenants) {
            var s = DefaultObjectMapper.get().writeValueAsString(tenant);
            var deserialized = DefaultObjectMapper .get().readValue(s, TenantRequest.class);
            Assertions.assertThat(tenant).isEqualTo(deserialized);
        }
    }

    @Test(expected = NullPointerException.class)
    public void testNullTenantType() throws IOException {
        var fullTenant = TenantRequest.builder()
                .tenantName("S")
                .userName("Sid")
                .userLastname("B")
                .userEmail("s@propelo.ai")
                .tenantType(null)
                .build();
    }
    
}
