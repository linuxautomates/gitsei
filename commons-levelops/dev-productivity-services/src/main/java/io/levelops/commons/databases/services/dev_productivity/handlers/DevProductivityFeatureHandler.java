package io.levelops.commons.databases.services.dev_productivity.handlers;

import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.TenantSCMSettings;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureBreakDown;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureResponse;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDetails;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

public interface DevProductivityFeatureHandler {
    Set<DevProductivityProfile.FeatureType> getSupportedFeatureTypes();
    FeatureResponse calculateFeature(final String company, final Integer sectionOrder, final DevProductivityProfile.Feature feature, Map<String, Object> profileSettings, final DevProductivityFilter devProductivityFilter,
                                     final OrgUserDetails orgUserDetails, final Map<String, Long> latestIngestedAtByIntegrationId, final TenantSCMSettings tenantSCMSettings) throws SQLException, IOException;
    FeatureBreakDown getBreakDown(final String company, final DevProductivityProfile.Feature feature, Map<String, Object> profileSettings, final DevProductivityFilter devProductivityFilter,
                                  final OrgUserDetails orgUserDetails, final Map<String, Long> latestIngestedAtByIntegrationId, final TenantSCMSettings tenantSCMSettings,
                                  Map<String, SortingOrder> sortBy, Integer pageNumber, Integer pageSize) throws SQLException, IOException;

}
