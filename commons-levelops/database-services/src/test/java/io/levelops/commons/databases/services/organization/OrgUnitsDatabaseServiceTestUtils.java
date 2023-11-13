package io.levelops.commons.databases.services.organization;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.organization.DBOrgContentSection;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.OrgUnitCategory;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.ingestion.models.IntegrationType;

import java.sql.SQLException;
import java.util.*;

public class OrgUnitsDatabaseServiceTestUtils {
    public static DBOrgUnit createDBOrgUnit(OrgUnitsDatabaseService orgUnitsDatabaseService, OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService, String company, int i, Integration integration1, Integration integration2) throws SQLException {
        OrgUnitCategory orgGroup1 = OrgUnitCategory.builder()
                .name("TEAM A")
                .description("Sample team")
                .isPredefined(true)
                .build();
        String orgGroupId1 = orgUnitCategoryDatabaseService.insert(company, orgGroup1);
        DBOrgUnit unit = DBOrgUnit.builder()
                .name("The unit" + i)
                .description("My unit" + i)
                .active(true)
                .managers(Set.of())
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .sections(Set.of(
                        DBOrgContentSection.builder().integrationId(Integer.parseInt(integration1.getId())).users(Set.of(1, 2)).integrationType(IntegrationType.fromString(integration1.getApplication())).integrationName(integration1.getName()).build(),
                        DBOrgContentSection.builder().integrationId(Integer.parseInt(integration2.getId())).integrationFilters(Map.of("test", "ok")).users(Set.of(1, 2)).integrationType(IntegrationType.fromString(integration2.getApplication())).integrationName(integration2.getName()).build()))
                .active(true)
                .build();
        var id = orgUnitsDatabaseService.insertForId(company, unit);
        unit = unit.toBuilder().id(id.getLeft()).refId(id.getRight()).build();
        orgUnitsDatabaseService.update(company, unit.getId(), true);
        return unit;
    }

    public static List<DBOrgUnit> createDBOrgUnits(OrgUnitsDatabaseService orgUnitsDatabaseService, OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService, String company, int n, Integration integration1, Integration integration2) throws SQLException {
        List<DBOrgUnit> result = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            DBOrgUnit dbOrgUnit = createDBOrgUnit(orgUnitsDatabaseService, orgUnitCategoryDatabaseService, company, i, integration1, integration2);
            result.add(dbOrgUnit);
        }
        return result;
    }

    public static List<DBOrgUnit> updateDBOrgUnits(OrgUnitsDatabaseService orgUnitsDatabaseService, OrgUnitHelper orgUnitHelper, String company, List<DBOrgUnit> orgUnits) throws SQLException {
        orgUnitHelper.updateUnits(company, orgUnits.stream());
        List<DBOrgUnit> updatedOrgUnits = new ArrayList<>();
        for (DBOrgUnit o : orgUnits) {
            DBOrgUnit updated = orgUnitsDatabaseService.get(company, o.getRefId(), true).get();
            updatedOrgUnits.add(updated);
        }
        return updatedOrgUnits;
    }
}
