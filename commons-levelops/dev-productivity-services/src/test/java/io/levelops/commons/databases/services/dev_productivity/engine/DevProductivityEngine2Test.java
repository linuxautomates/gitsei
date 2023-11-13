package io.levelops.commons.databases.services.dev_productivity.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.TenantSCMSettings;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityResponse;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureBreakDown;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureResponse;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDetails;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.services.dev_productivity.handlers.DevProductivityFeatureHandler;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DevProductivityEngine2Test {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();
    private static final String COMPANY = "test";

    @Test
    public void test() throws IOException, SQLException {
        DevProductivityFeatureHandler handler = new DummyHandler();

        Map<DevProductivityProfile.FeatureType, DevProductivityFeatureHandler> map = new HashMap<>();
        for(DevProductivityProfile.FeatureType ft : DevProductivityProfile.FeatureType.values()) {
            map.put(ft, handler);
        }
        DevProductivityProfile profile = MAPPER.readValue(ResourceUtils.getResourceAsString("json/databases/dev_prod/dev_prod_profile_3_duplicates.json"), DevProductivityProfile.class);
        DevProductivityFilter devProductivityFilter = DevProductivityFilter.builder().build();
        OrgUserDetails orgUserDetails = OrgUserDetails.builder().build();
        Map<String, Long> latestIngestedAtByIntegrationId = Map.of();

        DevProductivityEngine engine = new DevProductivityEngine(map);
        DevProductivityResponse response = engine.calculateDevProductivity(COMPANY, profile, devProductivityFilter,
                orgUserDetails,
                latestIngestedAtByIntegrationId, null, null, null );

        Assert.assertNotNull(response);
    }


    public static final class DummyHandler implements DevProductivityFeatureHandler {
        @Override
        public Set<DevProductivityProfile.FeatureType> getSupportedFeatureTypes() {
            return null;
        }

        @Override
        public FeatureResponse calculateFeature(String company, Integer sectionOrder, DevProductivityProfile.Feature feature, Map<String, Object> profileSettings, DevProductivityFilter devProductivityFilter, OrgUserDetails orgUserDetails, Map<String, Long> latestIngestedAtByIntegrationId, TenantSCMSettings tenantSCMSettings) throws SQLException, IOException {
            return FeatureResponse.builder()
                    .sectionOrder(sectionOrder)
                    .order(feature.getOrder())
                    .result(1l)
                    .build();
        }

        @Override
        public FeatureBreakDown getBreakDown(String company, DevProductivityProfile.Feature feature, Map<String, Object> profileSettings, DevProductivityFilter devProductivityFilter, OrgUserDetails orgUserDetails, Map<String, Long> latestIngestedAtByIntegrationId, TenantSCMSettings tenantSCMSettings, Map<String, SortingOrder> sortBy, Integer pageNumber, Integer pageSize) throws SQLException, IOException {
            return null;
        }
    }
}