package io.levelops.commons.databases.services.dev_productivity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.common.collect.Maps;
import io.levelops.commons.databases.models.database.dev_productivity.*;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

@Log4j2
@Service
public class OrgIdentityService {

    private static final String INTEGRATIION_USERS = "integration_users";
    private static final String INTEGRATIONS = "integrations";
    private static final String ORG_USER_CLOUD_ID_MAPPING = "org_user_cloud_id_mapping";
    private static final String ORG_AND_USER_DEV_PROD_REPORTS_MAPPINGS = "org_and_user_dev_prod_reports_mappings";
    private static final String ORG_USERS = "org_users";
    private static final String OU_MEMBERSHIP = "ou_membership";
    private static final String OUS = "ous";
    private static final String OU_CONTENT_SECTION_TABLE_NAME = "ou_content_sections";

    private final NamedParameterJdbcTemplate template;

    @Autowired
    public OrgIdentityService(DataSource dataSource) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }


    public DbListResponse<OrgUserDetails> getUserIdentityForAllIntegrations(String company, DevProductivityUserIds userId, Integer pageNumber, Integer pageSize) {

        if (CollectionUtils.isEmpty(userId.getUserIdList()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, " Null or Empty user id list is provided ");

        Map<String, Object> params = new HashMap<>();
        params.put("offset", pageNumber * pageSize);
        params.put("limit", pageSize);

        String sqlSelectStmt = "SELECT iu.integration_id, iu.cloud_id, iu.display_name, ou.full_name, ou.email, ou.custom_fields, ouc.org_user_id, ouc.integration_user_id, intg.application ";
        String orgUserCloudIdJoin = " FROM " + company + "." + INTEGRATIION_USERS + " iu "
                + " INNER JOIN " + company + "." + ORG_USER_CLOUD_ID_MAPPING + " ouc ON iu.id = ouc.integration_user_id ";
        String orgUsersJoin = " INNER JOIN " + company + "." + ORG_USERS + " ou on ouc.org_user_id = ou.id ";
        String integrationsJoin = " INNER JOIN " + company + "." + INTEGRATIONS + " intg on iu.integration_id = intg.id ";
        String orgUserIdsClause = " AND ouc.org_user_id IN (:ouc.org_user_id) ";

        String orgNamesSelect = ", to_json ((SELECT array_agg (org_dev_prod_profile_details) FROM ( " +
                " select orgunits.id as ou_id, orgunits.ref_id as ou_ref_id, orgunits.name as ou_name, dpp.id as profile_id, dpp.name as profile_name from " + company + ".ous orgunits, " +
                company + ".dev_productivity_profiles dpp" +
                " where (orgunits.id,dpp.id) in ( " +
                " select ou_id,dev_productivity_profile_id from " + company + ".org_and_user_dev_prod_reports_mappings where interval='LAST_MONTH' " +
                " and  org_user_ids @> :org_user_id_array) and active = true" +
                " ) org_dev_prod_profile_details )) as org_dev_prod_profile_details ";


        List<UUID> orgUserIdList = userId.getUserIdList();
        if (IdType.INTEGRATION_USER_IDS.equals(userId.getUserIdType())) {
            orgUserIdList = getOrgUserIdListFromIntegrationUserList(company, userId.getUserIdList());
            if (CollectionUtils.isEmpty(orgUserIdList))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, " No org user ids are associated with provided integration ids ");
        }
        params.put("ouc.org_user_id", orgUserIdList);
        params.put("org_user_id_array", new String[]{String.join(",", orgUserIdList.stream().map(r -> r.toString()).collect(Collectors.toList()))});


        String sql = sqlSelectStmt
                + orgNamesSelect
                + orgUserCloudIdJoin
                + orgUserIdsClause
                + orgUsersJoin
                + integrationsJoin;

        String query = sql + " OFFSET :offset LIMIT :limit";

        log.info("sql is {}", query);
        log.info("params are {}", params);

        List<OrgUserDetails> result = List.of();
        ObjectMapper mapper = new ObjectMapper();

        result = template.query(query, params, new ResultSetExtractor<List<OrgUserDetails>>() {
            @Override
            public List<OrgUserDetails> extractData(ResultSet rs) throws SQLException, DataAccessException {
                Map<UUID, OrgUserDetails> tempMap = Maps.newHashMap();
                while (rs.next()) {
                    UUID orgUserId = rs.getObject("org_user_id", java.util.UUID.class);

                    Map<UUID, DevProductivityProfileInfo> devProductivityProfileIdProfileMap = Maps.newHashMap();
                    List<Map<String, Object>> orgDevProdProfileDetails = ParsingUtils.parseJsonList(mapper, "org_dev_prod_profile_details", rs.getString("org_dev_prod_profile_details"));
                    if (CollectionUtils.isNotEmpty(orgDevProdProfileDetails)) {
                        orgDevProdProfileDetails.stream().forEach(o -> {
                            UUID devProductivityProfileId = UUID.fromString((String) o.get("profile_id"));
                            if (devProductivityProfileIdProfileMap.containsKey(devProductivityProfileId)) {
                                List<OrgUnitInfo> orgUnitInfos = devProductivityProfileIdProfileMap.get(devProductivityProfileId).getAssociatedOUs();
                                orgUnitInfos.add(
                                        OrgUnitInfo.builder()
                                                .ouId((UUID.fromString((String) o.get("ou_id"))))
                                                .ouName((String) o.get("ou_name"))
                                                .ouRefId((Integer) o.get("ou_ref_id"))
                                                .build()
                                );
                            } else {
                                List<OrgUnitInfo> orgUnitInfos = Lists.newArrayList();
                                orgUnitInfos.add(OrgUnitInfo.builder()
                                        .ouId((UUID.fromString((String) o.get("ou_id"))))
                                        .ouName((String) o.get("ou_name"))
                                        .ouRefId((Integer) o.get("ou_ref_id"))
                                        .build());
                                DevProductivityProfileInfo devProductivityProfileInfo = DevProductivityProfileInfo.builder()
                                        .id((devProductivityProfileId))
                                        .name((String) o.get("profile_name"))
                                        .associatedOUs(orgUnitInfos)
                                        .build();
                                devProductivityProfileIdProfileMap.put(devProductivityProfileId, devProductivityProfileInfo);
                            }
                        });
                    }

                    OrgUserDetails orgUser = tempMap.getOrDefault(orgUserId, OrgUserDetails.builder()
                            .orgUserId(orgUserId)
                            .fullName(rs.getString("full_name"))
                            .email(rs.getString("email"))
                            .customFields(ParsingUtils.parseJsonObject(mapper, "custom_fields", rs.getString("custom_fields")))
                            .devProductivityProfiles(new ArrayList<DevProductivityProfileInfo>(devProductivityProfileIdProfileMap.values()))
                            .IntegrationUserDetailsList(new ArrayList<IntegrationUserDetails>())
                            .build());

                    IntegrationUserDetails integrationUserDetails = IntegrationUserDetails.builder()
                            .integrationId(rs.getInt("integration_id"))
                            .integrationType(IntegrationType.fromString(rs.getString("application")))
                            .integrationUserId(rs.getObject("integration_user_id", java.util.UUID.class))
                            .cloudId(rs.getString("cloud_id"))
                            .displayName(rs.getString("display_name"))
                            .build();

                    orgUser.getIntegrationUserDetailsList().add(integrationUserDetails);
                    tempMap.put(orgUserId, orgUser);
                }
                return new ArrayList<OrgUserDetails>(tempMap.values());
            }
        });

        Integer count = result.size() + pageNumber * pageSize;

        if (result.size() == pageSize) {
            String countSql = "SELECT COUNT(*) FROM (" + sql + ") as x";
            log.info("countSql = {}", countSql);
            log.info("params = {}", params);
            count = template.queryForObject(countSql, params, Integer.class);
        }

        return DbListResponse.of(result, count);
    }

    public UUID getOrgUserIdFromIntegrationUser(String company, UUID integrationUser) {
        Map<String, Object> map = Maps.newHashMap();
        map.put("integration_user_id", integrationUser);

        String query = "SELECT ou.id FROM " + company + "." + ORG_USERS + " ou " +
                " INNER JOIN " + company + "." + ORG_USER_CLOUD_ID_MAPPING + " ON " +
                " ou.id =  org_user_id AND integration_user_id = :integration_user_id " +
                " ORDER BY versions DESC LIMIT 1";

        log.info("sql {}", query);
        log.info("params {}", map);

        List<UUID> list = template.queryForList(query, map, UUID.class);

        if (CollectionUtils.isEmpty(list))
            return null;

        return list.get(0);
    }


    public List<UUID> getOrgUnitForOrgUser(String company, UUID orgUserId, AGG_INTERVAL intervalType) {
        Map<String, Object> map = Maps.newHashMap();
        map.put("org_user_id", orgUserId.toString());
        String intervalCond = StringUtils.EMPTY;
        if (intervalType.equals(AGG_INTERVAL.month)) {
            intervalCond = "AND interval LIKE 'MONTH_%' ";
        } else if (intervalType.equals(AGG_INTERVAL.quarter)) {
            intervalCond = "AND interval = 'PAST_QUARTER_ONE' ";
        }
        String query = "SELECT DISTINCT(ou.id) from " + company + "." + OUS + " as ou " +
                "JOIN " + company + "." + ORG_AND_USER_DEV_PROD_REPORTS_MAPPINGS + " as m " +
                " on m.ou_id = ou.id WHERE ou.active = true and :org_user_id = ANY(m.org_user_ids) " + intervalCond;

        log.info("sql {}", query);
        log.info("params {}", map);

        List<UUID> list = template.queryForList(query, map, UUID.class);

        if (CollectionUtils.isEmpty(list))
            return null;

        return list;
    }

    private static final String SELECT_OU_FROM_ORG_USER_V2 = "SELECT distinct(ou.id) " +
            "FROM %s.ous AS ou " +
            "JOIN %s.ou_org_user_mappings_v2 AS m ON m.ou_ref_id = ou.ref_id " +
            "JOIN %s.org_users AS u ON u.ref_id = m.org_user_ref_id " +
            "WHERE ou.active = true AND u.id=:org_user_id";
    public List<UUID> getOrgUnitForOrgUserV2(String company, UUID orgUserId) {
        String sql = String.format(SELECT_OU_FROM_ORG_USER_V2, company, company, company);
        Map<String, Object> map = Maps.newHashMap();
        map.put("org_user_id", orgUserId);
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params {}", map);
        List<UUID> list = template.queryForList(sql, map, UUID.class);
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        return list;
    }

    public List<UUID> getOrgUserIdListFromIntegrationUserList(String company, List<UUID> integrationUserList) {
        log.info("fetching orgUserIds for integrationUserIds {} ", integrationUserList);
        return integrationUserList.stream().map(integrationUserId -> getOrgUserIdFromIntegrationUser(company, integrationUserId))
                .filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<UUID> getOrgUnitForOrgRef(String company, List<Integer> ouRefId) {
        Map<String, Object> map = Maps.newHashMap();
        map.put("ou_ref_ids", ouRefId);

        String query = "SELECT id FROM " + company + "." + OUS +
                " WHERE ref_id in (:ou_ref_ids) " +

                " AND active = true GROUP BY id ORDER BY versions DESC, name";

        log.info("sql {}", query);
        log.info("params {}", map);

        List<UUID> list = template.queryForList(query, map, UUID.class);

        if (CollectionUtils.isEmpty(list))
            return null;

        return list;
    }

    public List<OrgUnitInfo> getOrgUnitsForIntegration(String company, List<Integer> integrationList, Integer workspaceId) {

        Map<String, Object> param = Maps.newHashMap();
        param.put("integration_ids", integrationList);
        param.put("workspace_id", workspaceId);

        String query = "SELECT ou.id, ou.name, ouc.integration_id FROM " + company + "." + OUS +
                " as ou INNER JOIN " + company + "." + OU_CONTENT_SECTION_TABLE_NAME + " as ouc ON ou.id = ouc.ou_id " +
                "INNER JOIN  " + company + ".workspace_integrations wi on ouc.integration_id=wi.integration_id  " +
                "WHERE wi.workspace_id = :workspace_id AND ouc.integration_id IN (:integration_ids) AND ou.ou_category_id IN (select id from " + company + ".ou_categories where workspace_id = :workspace_id) AND ou.active=true " +
                " GROUP BY ou.id, ou.name, ouc.integration_id  ";

        List<OrgUnitInfo> orgUnitDetails = template.query(query, param, new ResultSetExtractor<List<OrgUnitInfo>>() {
            @Override
            public List<OrgUnitInfo> extractData(ResultSet rs) throws SQLException, DataAccessException {
                List<OrgUnitInfo> list = Lists.newArrayList();
                while (rs.next()) {
                    OrgUnitInfo details = OrgUnitInfo.builder()
                            .ouId(rs.getObject("id", UUID.class))
                            .ouName(rs.getString("name"))
                            .integrationId(rs.getInt("integration_id"))
                            .build();
                    list.add(details);
                }
                return list;
            }
        });

        return orgUnitDetails;
    }
}
