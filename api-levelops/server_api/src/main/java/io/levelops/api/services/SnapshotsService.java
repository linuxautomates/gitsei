package io.levelops.api.services;

import io.levelops.api.converters.organization.OUConverter;
import io.levelops.api.model.organization.OrgIdType;
import io.levelops.api.model.organization.OrgUnitDTO;
import io.levelops.api.services.dev_productivity.DevProductivityAndOUDBService;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfileInfo;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityUserIds;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDetails;
import io.levelops.commons.databases.models.database.dev_productivity.IdType;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.levelops.commons.databases.models.database.dev_productivity.UserDevProductivityReport;
import io.levelops.commons.databases.services.dev_productivity.OrgIdentityService;
import io.levelops.commons.databases.services.dev_productivity.UserDevProductivityReportV2DatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.lang.Boolean.FALSE;

@Service
@Log4j2
public class SnapshotsService {
    private static final Boolean RAW_RESPONSE_NOT_NEEDED = FALSE;
    private static final Boolean USE_PARENT_PROFILES = true;
    private final OrgIdentityService orgIdentityService;
    private final OrgUnitsDatabaseService orgUnitsDatabaseService;
    private final UserDevProductivityReportV2DatabaseService userDevProductivityReportV2DatabaseService;
    private final DevProductivityAndOUDBService devProductivityAndOUDBService;
    private final Set<String> readDevProductivityV2UserReportTenants;
    private final boolean readDevProductivityV2Enabled;
    private final Boolean devProdProfilesV2Enabled;
    private final Set<String> parentProfilesEnabledTenants;

    @Autowired
    public SnapshotsService(OrgIdentityService orgIdentityService, OrgUnitsDatabaseService orgUnitsDatabaseService, UserDevProductivityReportV2DatabaseService userDevProductivityReportV2DatabaseService, DevProductivityAndOUDBService devProductivityAndOUDBService,
                            @Qualifier("readDevProductivityV2UserReportTenants") Set<String> readDevProductivityV2UserReportTenants,
                            @Qualifier("readDevProductivityV2Enabled") boolean readDevProductivityV2Enabled, @Value("${DEV_PROD_PROFILES_V2_ENABLED:false}") Boolean devProdProfilesV2Enabled, @Qualifier("parentProfilesEnabledTenants") Set<String> parentProfilesEnabledTenants) {
        this.orgIdentityService = orgIdentityService;
        this.orgUnitsDatabaseService = orgUnitsDatabaseService;
        this.userDevProductivityReportV2DatabaseService = userDevProductivityReportV2DatabaseService;
        this.devProductivityAndOUDBService = devProductivityAndOUDBService;
        this.readDevProductivityV2UserReportTenants = readDevProductivityV2UserReportTenants;
        this.readDevProductivityV2Enabled = readDevProductivityV2Enabled;
        this.devProdProfilesV2Enabled = devProdProfilesV2Enabled;
        this.parentProfilesEnabledTenants = parentProfilesEnabledTenants;
    }

    public OrgUserDetails getUserSnapshotV1(final String company, IdType userIdType, String userId) throws NotFoundException {
        DevProductivityUserIds devProductivityUserIds = DevProductivityUserIds.builder().userIdType(userIdType).userIdList(List.of(UUID.fromString(userId))).build();
        log.info("devProductivityUserIds = {}", devProductivityUserIds);
        List<OrgUserDetails> orgUserDetailsList = orgIdentityService.getUserIdentityForAllIntegrations(company, devProductivityUserIds, 0, 10).getRecords();
        log.info("orgUserDetailsList = {}", orgUserDetailsList);
        if(CollectionUtils.isEmpty(orgUserDetailsList)) {
            String error = String.format("Company %s, User Type %s, User Id %s NOT FOUND!", company, userIdType.toString(), userId);
            throw new NotFoundException(error);
        }
        return orgUserDetailsList.get(0);
    }
    public OrgUserDetails getUserSnapshot(final String company, IdType userIdType, String userId) throws NotFoundException, SQLException {
        if(readDevProductivityV2Enabled || readDevProductivityV2UserReportTenants.contains(company)) {
            log.info("Snapshot company {} using v2", company);
            return getUserSnapshotV2(company, userIdType, userId);
        } else {
            return getUserSnapshotV1(company, userIdType, userId);
        }
    }

    private List<DevProductivityProfileInfo> fetchDevProductivityProfileInfo(final String company, final UUID orgUserId) throws SQLException {
        List<UserDevProductivityReport> userReports = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 10000, null,
                List.of(orgUserId), null, List.of(ReportIntervalType.LAST_MONTH), null, RAW_RESPONSE_NOT_NEEDED, null).getRecords();
        if (CollectionUtils.isEmpty(userReports)) {
            return Collections.EMPTY_LIST;
        }
        List<UUID> devProductivityProfileIds = userReports.stream().map(r -> r.getDevProductivityProfileId()).distinct().collect(Collectors.toList());
        if(CollectionUtils.isEmpty(devProductivityProfileIds)) {
            return Collections.EMPTY_LIST;
        }

        List<DevProductivityProfileInfo> devProductivityProfileInfos = null;
        if(BooleanUtils.isTrue(devProdProfilesV2Enabled) || CollectionUtils.emptyIfNull(parentProfilesEnabledTenants).contains(company)){
            devProductivityProfileInfos = devProductivityAndOUDBService.getDevProductivityProfileInfo(company, devProductivityProfileIds, USE_PARENT_PROFILES);
        } else {
            devProductivityProfileInfos = devProductivityAndOUDBService.getDevProductivityProfileInfo(company, devProductivityProfileIds);
        }

        return devProductivityProfileInfos;
    }

    public OrgUserDetails getUserSnapshotV2(final String company, IdType userIdType, String userId) throws NotFoundException, SQLException {
        DevProductivityUserIds devProductivityUserIds = DevProductivityUserIds.builder().userIdType(userIdType).userIdList(List.of(UUID.fromString(userId))).build();
        log.info("devProductivityUserIds = {}", devProductivityUserIds);
        List<OrgUserDetails> orgUserDetailsList = orgIdentityService.getUserIdentityForAllIntegrations(company, devProductivityUserIds, 0, 10).getRecords();
        log.info("orgUserDetailsList = {}", orgUserDetailsList);
        if(CollectionUtils.isEmpty(orgUserDetailsList)) {
            String error = String.format("Company %s, User Type %s, User Id %s NOT FOUND!", company, userIdType.toString(), userId);
            throw new NotFoundException(error);
        }
        OrgUserDetails orgUserDetails = orgUserDetailsList.get(0);
        log.info("Before orgUserDetails = {}", orgUserDetails);
        List<DevProductivityProfileInfo> devProductivityProfileInfos = fetchDevProductivityProfileInfo(company, orgUserDetails.getOrgUserId());
        orgUserDetails = orgUserDetails.toBuilder()
                .devProductivityProfiles(devProductivityProfileInfos)
                .build();
        log.info("After orgUserDetails = {}", orgUserDetails);
        return orgUserDetails;
    }


    public Optional<OrgUnitDTO> getOrgSnapShot(String company, OrgIdType orgIdType, String orgId) throws SQLException, BadRequestException {
        Validate.notBlank(company, "company cannot be blank!");
        Validate.notNull(orgIdType, "orgIdType cannot be null!");
        Validate.notBlank(orgId, "orgId cannot be blank!");

        if(OrgIdType.ORG_UUID.equals(orgIdType)) {
            return orgUnitsDatabaseService.get(company, orgId).map(OUConverter::map);
        }
        throw new BadRequestException("orgIdType " + orgIdType.toString() + " is not supported");
    }
}
