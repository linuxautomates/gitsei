package io.levelops.commons.databases.services.dev_productivity;

import com.google.api.client.util.Lists;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.dev_productivity.*;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.services.DatabaseService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ListUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
public class DevProductivityRelativeScoreService extends DatabaseService<RelativeScore> {
    private static final boolean RESPONSE_IS_V1 = true;
    private static final boolean RESPONSE_IS_NOT_V1 = false;

    private final NamedParameterJdbcTemplate template;
    private final OrgIdentityService orgIdentityService;
    private final IndustryDevProductivityReportDatabaseService industryDevProductivityReportDatabaseService;
    private final OrgDevProductivityReportDatabaseService orgDevProductivityReportDatabaseService;
    private final OrgDevProductivityESReportDatabaseService orgDevProductivityESReportDatabaseService;
    private final UserDevProductivityReportDatabaseService userDevProductivityReportDatabaseService;
    private final UserDevProductivityESReportDatabaseService userDevProductivityESReportDatabaseService;

    private final UserDevProductivityReportV2DatabaseService userDevProductivityReportV2DatabaseService;
    private final OrgDevProductivityReportV2DatabaseService orgDevProductivityReportV2DatabaseService;

    private static final String LEVELOPS_INVENTORY_SCHEMA = "_levelops";
    private static final Boolean LATEST_REPORTS = true;
    private static final Boolean NOT_LATEST_REPORTS = false;

    @Autowired
    public DevProductivityRelativeScoreService(DataSource dataSource, OrgIdentityService orgIdentityService, UserDevProductivityReportDatabaseService userDevProductivityReportDatabaseService,
                                               OrgDevProductivityReportDatabaseService orgDevProductivityReportDatabaseService, IndustryDevProductivityReportDatabaseService industryDevProductivityReportDatabaseService, OrgDevProductivityESReportDatabaseService orgDevProductivityESReportDatabaseService, UserDevProductivityESReportDatabaseService userDevProductivityESReportDatabaseService, UserDevProductivityReportV2DatabaseService userDevProductivityReportV2DatabaseService, OrgDevProductivityReportV2DatabaseService orgDevProductivityReportV2DatabaseService) {
        super(dataSource);
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.orgIdentityService = orgIdentityService;
        this.userDevProductivityReportDatabaseService = userDevProductivityReportDatabaseService;
        this.orgDevProductivityReportDatabaseService = orgDevProductivityReportDatabaseService;
        this.industryDevProductivityReportDatabaseService = industryDevProductivityReportDatabaseService;
        this.orgDevProductivityESReportDatabaseService = orgDevProductivityESReportDatabaseService;
        this.userDevProductivityESReportDatabaseService = userDevProductivityESReportDatabaseService;
        this.userDevProductivityReportV2DatabaseService = userDevProductivityReportV2DatabaseService;
        this.orgDevProductivityReportV2DatabaseService = orgDevProductivityReportV2DatabaseService;
    }

    @Override
    public String insert(String company, RelativeScore t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean update(String company, RelativeScore t) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<RelativeScore> get(String company, String param) throws SQLException {
        return Optional.empty();
    }

    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        return true;
    }

    @Override
    public DbListResponse<RelativeScore> list(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return listByFilter(company, null, null, null, false, false, pageNumber, pageSize);
    }

    public DbListResponse<RelativeScore> listByFilter(String company, Map<UUID,List<UUID>> ouOrOrgUserIdDevProdProfilesMap, AGG_INTERVAL intervalType, List<DevProductivityUserIds> comparisonList, Boolean noComparison, Boolean useES, Integer pageNumber, Integer pageSize) throws SQLException {
        return getRelativeScore(company, ouOrOrgUserIdDevProdProfilesMap, intervalType, comparisonList, noComparison, useES, pageNumber, pageSize, RESPONSE_IS_V1);
    }

    public DbListResponse<RelativeScore> listByFilterV2(String company, Map<UUID,List<UUID>> ouOrOrgUserIdDevProdProfilesMap, AGG_INTERVAL intervalType, List<DevProductivityUserIds> comparisonList, Boolean noComparison, Boolean useES, Integer pageNumber, Integer pageSize) throws SQLException {
        return getRelativeScore(company, ouOrOrgUserIdDevProdProfilesMap, intervalType, comparisonList, noComparison, useES, pageNumber, pageSize, RESPONSE_IS_NOT_V1);
    }

    private DbListResponse<RelativeScore> getRelativeScore(String company, Map<UUID,List<UUID>> ouOrOrgUserIdDevProdProfilesMap,
                                                           AGG_INTERVAL intervalType,
                                                           List<DevProductivityUserIds> comparisonList,
                                                           Boolean noComparison,
                                                           Boolean useES,
                                                           int pageNumber,
                                                           int pageSize, boolean isResponseV1) throws SQLException {

        if (CollectionUtils.isEmpty(comparisonList)) {
            return DbListResponse.of(List.of(), 0);
        }

        if (comparisonList.size() == 1 && !comparisonList.get(0).getIdType().equals(IdType.ORG_IDS) && BooleanUtils.isNotTrue(noComparison)) {

            UUID id = comparisonList.get(0).getId();
            List<UUID> ids;
            if (comparisonList.get(0).getIdType().equals(IdType.OU_USER_IDS)) {
                if(isResponseV1) {
                    ids = orgIdentityService.getOrgUnitForOrgUser(company, id, intervalType);
                } else {
                    ids = orgIdentityService.getOrgUnitForOrgUserV2(company, id);
                }
            } else {
                ids = List.of(orgIdentityService.getOrgUserIdFromIntegrationUser(company, id));
                if (CollectionUtils.isNotEmpty(ids)) {
                    if(isResponseV1) {
                        ids = orgIdentityService.getOrgUnitForOrgUser(company, ids.get(0), intervalType);
                    } else {
                        ids = orgIdentityService.getOrgUnitForOrgUserV2(company, ids.get(0));
                    }
                }
            }

            if (CollectionUtils.isNotEmpty(ids)) {
                ouOrOrgUserIdDevProdProfilesMap.put(ids.get(0), ouOrOrgUserIdDevProdProfilesMap.get(id));
                DevProductivityUserIds newDevProductivityId = DevProductivityUserIds.builder()
                        .idType(IdType.ORG_IDS)
                        .orgIds(ids)
                        .build();

                comparisonList.add(newDevProductivityId);
            }
        }

        List<List<RelativeDevProductivityReport>> allReports = Lists.newArrayList();

        if(BooleanUtils.isNotTrue(noComparison)){
            List<RelativeDevProductivityReport> industryReport = getIndustryReport(intervalType, pageNumber, pageSize);
            allReports.add(industryReport);
        }

        List<List<RelativeDevProductivityReport>> devOrgReports = getDevOrOrgReports(company, ouOrOrgUserIdDevProdProfilesMap, comparisonList, intervalType, BooleanUtils.isTrue(noComparison), useES, pageNumber, pageSize, isResponseV1);

        allReports.addAll(devOrgReports);

        Collections.sort(allReports, new Comparator<List<RelativeDevProductivityReport>>() {
            @Override
            public int compare(List<RelativeDevProductivityReport> o1, List<RelativeDevProductivityReport> o2) {
                return o2.size() - o1.size();
            }
        });

        List<RelativeScore> scoreList = DevProductivityUtils.getRelativeScores(allReports);

        return DbListResponse.of(scoreList, scoreList.size());
    }

    private List<RelativeDevProductivityReport> getIndustryReport(AGG_INTERVAL intervalType, int pageNumber, int pageSize) throws SQLException {

        List<ReportIntervalType> reportIntervalTypeList = getReportInterval(intervalType);

        List<IndustryDevProductivityReport> industryDevProductivityReportList = industryDevProductivityReportDatabaseService.listByFilter(LEVELOPS_INVENTORY_SCHEMA, pageNumber, pageSize, null, reportIntervalTypeList, null).getRecords();

        return industryDevProductivityReportList.stream().
                map(report -> {
                    return RelativeDevProductivityReport.builder()
                            .id(report.getId())
                            .interval(report.getInterval())
                            .score(report.getScore())
                            .report(report.getReport())
                            .createdAt(report.getCreatedAt())
                            .updatedAt(report.getUpdatedAt())
                            .build();
                }).collect(Collectors.toList());

    }

    private List<List<RelativeDevProductivityReport>> getDevOrOrgReports(String company, Map<UUID,List<UUID>> ouOrOrgUserIdDevProdProfilesMap, List<DevProductivityUserIds> comparisonList, AGG_INTERVAL intervalType, boolean noComparison, Boolean useEs, int pageNumber, int pageSize, boolean isResponseV1) throws SQLException {

        Set<UUID> ouUserIdsSet = ListUtils.emptyIfNull(comparisonList).stream()
                .filter(Objects::nonNull)
                .filter(dev -> IdType.OU_USER_IDS.equals(dev.getIdType()))
                .map(DevProductivityUserIds::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        ListUtils.emptyIfNull(comparisonList).stream()
                .filter(Objects::nonNull)
                .filter(dev -> IdType.INTEGRATION_USER_IDS.equals(dev.getIdType()))
                .map(dev -> orgIdentityService.getOrgUserIdFromIntegrationUser(company, dev.getId()))
                .filter(Objects::nonNull)
                .forEach(ouUserIdsSet::add);

        List<UUID> ouUserIds = new ArrayList<>(ouUserIdsSet);

        List<UUID> ouIds = ListUtils.emptyIfNull(comparisonList).stream()
                .filter(Objects::nonNull)
                .filter(dev -> IdType.ORG_IDS.equals(dev.getIdType()))
                .map(DevProductivityUserIds::getOrgIds)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toList());

        List<ReportIntervalType> intervalTypeList = getReportInterval(intervalType);
        Map<String, SortingOrder> sortOrder = getSortOrder(intervalType);

        List<List<RelativeDevProductivityReport>> relativeDevProductivityReports = Lists.newArrayList();

        if (CollectionUtils.isNotEmpty(ouUserIds)) {


            List<UserDevProductivityReport> userDevProductivityReportList = Lists.newArrayList();

            ouUserIds.forEach(ouUserId -> {
                try {
                    if(isResponseV1) {
                        if(useEs){
                            userDevProductivityReportList.addAll(userDevProductivityESReportDatabaseService.listByFilter(company, pageNumber, pageSize, null, List.of(ouUserId), ouOrOrgUserIdDevProdProfilesMap.get(ouUserId),
                                    intervalTypeList, sortOrder, noComparison).getRecords());
                        }else {
                            userDevProductivityReportList.addAll(userDevProductivityReportDatabaseService.listByFilter(company, pageNumber, pageSize, null, List.of(ouUserId), ouOrOrgUserIdDevProdProfilesMap.get(ouUserId),
                                    intervalTypeList, sortOrder, noComparison).getRecords());
                        }
                    } else {
                        if(intervalTypeList.contains(ReportIntervalType.LAST_WEEK) || intervalTypeList.contains(ReportIntervalType.LAST_TWO_WEEKS)){
                            userDevProductivityReportList.addAll(userDevProductivityReportV2DatabaseService.listByFilter(company, pageNumber, pageSize, null, List.of(ouUserId), ouOrOrgUserIdDevProdProfilesMap.get(ouUserId),
                                    intervalTypeList, sortOrder, noComparison, null, NOT_LATEST_REPORTS).getRecords());
                        } else{
                            userDevProductivityReportList.addAll(userDevProductivityReportV2DatabaseService.listByFilter(company, pageNumber, pageSize, null, List.of(ouUserId), ouOrOrgUserIdDevProdProfilesMap.get(ouUserId),
                                    intervalTypeList, sortOrder, noComparison, null, LATEST_REPORTS).getRecords());
                        }

                    }
                } catch (SQLException e) {
                    log.error("Unable to fetch dev productivity report for orgUserId {} ",ouUserId, e);
                }
            });


            List<RelativeDevProductivityReport> subList = userDevProductivityReportList.stream().
                    map(report -> {
                        return RelativeDevProductivityReport.builder()
                                .id(report.getId())
                                .orgUserId(report.getOrgUserId())
                                .devProductivityProfileId(report.getDevProductivityProfileId())
                                .interval(report.getInterval())
                                .score(report.getScore())
                                .report(report.getReport())
                                .startTime(report.getStartTime())
                                .endTime(report.getEndTime())
                                .weekOfYear(report.getWeekOfYear())
                                .year(report.getYear())
                                .createdAt(report.getCreatedAt())
                                .updatedAt(report.getUpdatedAt())
                                .build();
                    }).collect(Collectors.toList());

            relativeDevProductivityReports.add(subList);
        }

        if (CollectionUtils.isNotEmpty(ouIds)) {
            //We are using ouIds not ou_ref_ids so isOUActive is not important.
            List<OrgDevProductivityReport> orgDevProductivityReportList = Lists.newArrayList();
            for (UUID ouId : ouIds) {
                if(isResponseV1) {
                    if(useEs){
                        orgDevProductivityReportList.addAll( orgDevProductivityESReportDatabaseService.listByFilter(company, pageNumber, pageSize, null, ouIds, ouOrOrgUserIdDevProdProfilesMap.get(ouId),
                                intervalTypeList, null, null, sortOrder, false).getRecords());
                    }else{
                        orgDevProductivityReportList.addAll(orgDevProductivityReportDatabaseService.listByFilter(company, pageNumber, pageSize, null, ouIds, ouOrOrgUserIdDevProdProfilesMap.get(ouId),
                                intervalTypeList, null, null, sortOrder, false).getRecords());
                    }
                } else {
                    if(intervalTypeList.contains(ReportIntervalType.LAST_WEEK) || intervalTypeList.contains(ReportIntervalType.LAST_TWO_WEEKS)){
                        orgDevProductivityReportList.addAll(orgDevProductivityReportV2DatabaseService.listByFilter(company, pageNumber, pageSize, null, ouIds, ouOrOrgUserIdDevProdProfilesMap.get(ouId),
                                intervalTypeList, null, sortOrder, false, NOT_LATEST_REPORTS).getRecords());
                    } else{
                        orgDevProductivityReportList.addAll(orgDevProductivityReportV2DatabaseService.listByFilter(company, pageNumber, pageSize, null, ouIds, ouOrOrgUserIdDevProdProfilesMap.get(ouId),
                                intervalTypeList, null, sortOrder, false, LATEST_REPORTS).getRecords());
                    }

                }
            }

            List<RelativeDevProductivityReport> subList = orgDevProductivityReportList.stream().
                    map(report -> {
                        return RelativeDevProductivityReport.builder()
                                .id(report.getId())
                                .ouID(report.getOuID())
                                .devProductivityProfileId(report.getDevProductivityProfileId())
                                .interval(report.getInterval())
                                .score(report.getScore())
                                .report(report.getReport())
                                .startTime(report.getStartTime())
                                .endTime(report.getEndTime())
                                .weekOfYear(report.getWeekOfYear())
                                .year(report.getYear())
                                .createdAt(report.getCreatedAt())
                                .updatedAt(report.getUpdatedAt())
                                .build();
                    }).collect(Collectors.toList());

            relativeDevProductivityReports.add(subList);
        }

        return relativeDevProductivityReports;
    }

    private Map<String, SortingOrder> getSortOrder(AGG_INTERVAL intervalType) {
        Map<String, SortingOrder> sortingOrderMap = new LinkedHashMap<>();
        if(intervalType.equals(AGG_INTERVAL.week) || intervalType.equals(AGG_INTERVAL.biweekly)){
            sortingOrderMap.put("year",SortingOrder.DESC);
            sortingOrderMap.put("week_of_year",SortingOrder.DESC);
        }
        sortingOrderMap.put("start_time", SortingOrder.ASC);
        return sortingOrderMap;
    }

    private List<ReportIntervalType> getReportInterval(AGG_INTERVAL interval) {


        if (AGG_INTERVAL.month.equals(interval)) {
            return Arrays.stream(ReportIntervalType.values()).filter(intervalType -> intervalType.toString().toLowerCase().startsWith(interval.name().toLowerCase())).collect(Collectors.toList());
        } else if (AGG_INTERVAL.week.equals(interval)) {
            return List.of(ReportIntervalType.LAST_WEEK);
        }else if (AGG_INTERVAL.biweekly.equals(interval)) {
            return List.of(ReportIntervalType.LAST_TWO_WEEKS);
        }else {
            return Arrays.stream(ReportIntervalType.values()).filter(intervalType -> intervalType.toString().toLowerCase().startsWith("past_" + interval.name().toLowerCase())).collect(Collectors.toList());
        }
    }

}