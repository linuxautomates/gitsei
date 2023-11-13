package io.levelops.commons.databases.services.organization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.api.client.util.Sets;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.levelops.commons.databases.models.database.dev_productivity.IdType;
import io.levelops.commons.databases.models.database.organization.DBOrgAccessUsers;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.OrgVersion;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.utils.DatabaseUtils;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.helper.organization.OrgUsersHelper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.utils.ListUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Log4j2
@Service
public class OrgAccessValidationService {

    private static final String ORG_USERS = "org_users";
    private static final String OU_MANAGERS = "ou_managers";
    private static final String OU_CONTENT_SECTIONS = "ou_content_sections";
    private static final String ORG_USER_CLOUD_ID_MAPPING = "org_user_cloud_id_mapping";
    private static final String ORG_VERSION_COUNTER = "org_version_counter";
    private static final String USER_MANAGED_OUS = "user_managed_ous";
    private static final String PROPELO_USERS = "users";

    private static final ResultSetExtractor<DBOrgAccessUsers> ORG_ACCESS_USERS_RESULT_SET_EXTRACTOR = rs -> {
        Set<UUID> authorizedList = Sets.newHashSet();
        Set<UUID> unAuthorizedList = Sets.newHashSet();

        while (rs.next()) {
            UUID id = rs.getObject("id", UUID.class);
            if (id == null) {
                continue;
            }
            authorizedList.add(id);
        }
        return DBOrgAccessUsers.builder()
                .authorizedUserList(authorizedList)
                .unAuthorizedUserList(unAuthorizedList)
                .build();
    };


    private final NamedParameterJdbcTemplate template;
    private final OrgVersionsDatabaseService versionsService;
    private final OrgUnitHelper orgUnitHelper;

    @Autowired
    public OrgAccessValidationService(DataSource dataSource, OrgVersionsDatabaseService versionService, OrgUnitHelper orgUnitHelper) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.versionsService = versionService;
        this.orgUnitHelper = orgUnitHelper;
    }

    //ToDo: VA - Add Unit tests
    public List<DBOrgUnit> getOrgsManagedUsingManagersEmail(final String company, String managersEmail, Integer pageNumber, Integer pageSize) throws SQLException {
        Validate.notBlank(managersEmail, "Managers Email cannot be null or empty!");
        String sqlFormat = "select * from %s.ous where active = true and id in (\n" +
                "   select distinct(ou_id) \n" +
                "   from %s.org_users \n" +
                "   INNER JOIN %s.ou_managers ON ref_id = org_user_id AND email = :email\n" +
                ")\n" +
                " ORDER BY ref_id " + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("email", managersEmail);

        String sql = String.format(sqlFormat, company, company, company);
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DBOrgUnit> orgUnits = template.query(sql, params, (rs, row) -> {
            Set<Integer> tagIds = ParsingUtils.parseSet("tag_ids", Integer.class, rs.getArray("tag_ids"));
            return DBOrgUnit.builder()
                    .id((UUID) rs.getObject("id"))
                    .refId(rs.getInt("ref_id"))
                    .name(rs.getString("name"))
                    .description(rs.getString("description"))
                    .parentRefId(rs.getInt("parent_ref_id"))
                    .tags(null)
                    .tagIds(tagIds)
                    .versions(DatabaseUtils.fromSqlArray(rs.getArray("versions"), Integer.class).collect(Collectors.toSet()))
                    .active(rs.getBoolean("active"))
                    .createdAt(rs.getTimestamp("created_at").toInstant())
                    .updatedAt(rs.getTimestamp("updated_at").toInstant())
                    .build();
        });
        return orgUnits;
    }

    public List<DBOrgUnit> getOrgsManagedByAdminUsingAdminsEmail(final String company, String adminsEmail, Integer pageNumber, Integer pageSize) throws SQLException {
        Validate.notBlank(adminsEmail, "Managers Email cannot be null or empty!");
        String sqlFormat = "select * from {0}.ous where active = true and ref_id in (\n" +
                "   select distinct(ou_ref_id) from (\n" +
                "   select * from {0}.user_managed_ous umo \n" +
                "   INNER JOIN {0}.users u ON umo.user_id = u.id AND u.email = :email where u.usertype IN (:userTypes)\n" +
                ") a ) \n" +
                " ORDER BY ref_id " + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("email", adminsEmail);
        params.addValue("userTypes",List.of("ORG_ADMIN_USER", "ADMIN"));

        String sql = MessageFormat.format(sqlFormat, company);
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DBOrgUnit> orgUnits = template.query(sql, params, (rs, row) -> {
            Set<Integer> tagIds = ParsingUtils.parseSet("tag_ids", Integer.class, rs.getArray("tag_ids"));
            return DBOrgUnit.builder()
                    .id((UUID) rs.getObject("id"))
                    .refId(rs.getInt("ref_id"))
                    .name(rs.getString("name"))
                    .description(rs.getString("description"))
                    .parentRefId(rs.getInt("parent_ref_id"))
                    .tags(null)
                    .tagIds(tagIds)
                    .versions(DatabaseUtils.fromSqlArray(rs.getArray("versions"), Integer.class).collect(Collectors.toSet()))
                    .active(rs.getBoolean("active"))
                    .createdAt(rs.getTimestamp("created_at").toInstant())
                    .updatedAt(rs.getTimestamp("updated_at").toInstant())
                    .build();
        });
        return orgUnits;
    }

    public boolean validateAccess(String company, String email, String version, UUID ouId) throws SQLException {

        int currentVersion;

        if (StringUtils.isEmpty(version)) {
            currentVersion = versionsService.getActive(company, OrgVersion.OrgAssetType.USER).get().getVersion();
        } else {
            currentVersion = Integer.parseInt(version);
        }

        Map<String, Object> params = Maps.newHashMap();
        params.put("email", email);
        params.put("ou_id", ouId);
        params.put("versions", new int[]{currentVersion});

        String selectQuery = "SELECT EXISTS ( SELECT 1 FROM " + company + "." + ORG_USERS
                + " INNER JOIN " + company + "." + OU_MANAGERS
                + " ON ref_id = org_user_id "
                + " AND email = :email "
                + " AND ou_id = :ou_id"
                + " AND versions @> :versions"
                + " )";

        return template.queryForObject(selectQuery, params, Boolean.class);
    }

    public DBOrgAccessUsers getAllAccessUsers(String company, String email, String version, IdType idType, List<UUID> userIdList) throws SQLException, JsonProcessingException {

        int currentVersion;

        if (StringUtils.isEmpty(version)) {
            currentVersion = versionsService.getActive(company, OrgVersion.OrgAssetType.USER).get().getVersion();
        } else {
            currentVersion = Integer.parseInt(version);
        }

        Map<String, Object> params = Maps.newHashMap();
        params.put("email", email);
        params.put("userIdList", userIdList);
        params.put("versions", new int[]{currentVersion});

        String adminsQuery = "SELECT id FROM " + company + ".ous WHERE active = true and ref_id in ("
                + " SELECT DISTINCT(ou_ref_id) FROM ("
                + " SELECT * from " + company + ".user_managed_ous umo "
                + " INNER JOIN " + company + ".users u ON umo.user_id = u.id AND u.email = :email"
                + " ) a )";

        String managersOrAdminsQuery = " WITH managers as ( SELECT om.ou_id FROM " +
                company + "." + OU_MANAGERS + " om"
                + " INNER JOIN " + company + "." + ORG_USERS + " ou"
                + " ON  ou.ref_id = om.org_user_id "
                + " WHERE email = :email and versions @> :versions "
                + "  UNION " + adminsQuery + ") ";



        List<UUID> orgUnitIdList = getOrgUnitList(company, params, managersOrAdminsQuery);

        String dynamicUserSelectionQuery = getDynamicUserSelectionQueryFromOuIds(company, orgUnitIdList, params);
        String dynamicUserMatch = StringUtils.isNotEmpty(dynamicUserSelectionQuery) ? " OR ou.id IN (SELECT id from ( " + dynamicUserSelectionQuery + ") AS dy ) " : "";

        String orgIdQuery = "";

        if (idType.equals(IdType.OU_USER_IDS)) {
            orgIdQuery = " (:userIdList)  ";
        } else {
            orgIdQuery = " ( SELECT DISTINCT org_user_id FROM " +
                    company + "." + ORG_USER_CLOUD_ID_MAPPING + " ocm" +
                    " INNER JOIN " + company + "." + ORG_USERS + " ou" +
                    " ON ou.id = ocm.org_user_id" +
                    " WHERE integration_user_id in (:userIdList)" +
                    " AND ou.versions @> :versions ) ";
        }

        String ouIdSubQuery = " AND ocs.ou_id IN ( SELECT om.ou_id FROM managers om )";

        String authorizedSelect = "SELECT ou.id, 'authorized' as access FROM " + company + "." + ORG_USERS + " ou";
        String unAuthorizedSelect = "SELECT ou.id, 'unauthorized' as access FROM " + company + "." + ORG_USERS + " ou WHERE ou.id IN ";
        String groupBy = " GROUP BY ou.id, access ";

        String joinContestSections = " INNER JOIN " + company + "." + OU_CONTENT_SECTIONS + " ocs" +
                " ON ( ref_id = ANY (ocs.user_ref_ids ) " +
                dynamicUserMatch +
                ") AND ou.id in " + orgIdQuery;

        String authorizedQuery = authorizedSelect
                + joinContestSections
                + ouIdSubQuery
                + groupBy;

        String finalQuery = managersOrAdminsQuery + authorizedQuery
                + " UNION "
                + unAuthorizedSelect
                + orgIdQuery
                + " AND ou.id NOT IN ( SELECT id FROM (  "
                + authorizedQuery
                + ") X )"
                + groupBy;

        log.info("Query is {}", finalQuery);
        log.info("Params are {}", params);

        return template.query(finalQuery, params, (ResultSetExtractor<DBOrgAccessUsers>) rs -> {
            Set<UUID> authorizedList = Sets.newHashSet();
            Set<UUID> unAuthorizedList = Sets.newHashSet();

            while (rs.next()) {
                if ("authorized".equals(rs.getString("access"))) {
                    authorizedList.add(rs.getObject("id", UUID.class));
                } else {
                    unAuthorizedList.add(rs.getObject("id", UUID.class));
                }
            }
            return DBOrgAccessUsers.builder()
                    .authorizedUserList(authorizedList)
                    .unAuthorizedUserList(unAuthorizedList)
                    .build();
        });
    }

    public DBOrgAccessUsers getAllAccessUsersByOuId(String company, String version, List<UUID> ouIds) throws SQLException {
        Set<UUID> authorizedUserList = new HashSet<>();
        Set<UUID> unAuthorizedUserList = new HashSet<>();
        try {
            ListUtils.emptyIfNull(ouIds).stream()
                    .map(RuntimeStreamException.wrap(ouId -> getAllAccessUsersByOuId(company, version, ouId)))
                    .filter(Objects::nonNull)
                    .forEach(users -> {
                        authorizedUserList.addAll(SetUtils.emptyIfNull(users.getAuthorizedUserList()));
                        unAuthorizedUserList.addAll(SetUtils.emptyIfNull(users.getUnAuthorizedUserList()));
                    });
        } catch (RuntimeStreamException e) {
            if (e.getCause() instanceof SQLException) {
                throw (SQLException) e.getCause();
            }
            throw new SQLException("Failed to get all users by ou ids", e);
        }
        return DBOrgAccessUsers.builder()
                .authorizedUserList(authorizedUserList)
                .unAuthorizedUserList(unAuthorizedUserList)
                .build();
    }

    public DBOrgAccessUsers getAllAccessUsersByOuId(String company, String version, UUID ouId, Boolean strictlyExplicit) throws SQLException, JsonProcessingException {
        OUConfiguration ouConfig = orgUnitHelper.getOuConfiguration(company, ouId).get();
        DBOrgAccessUsers users = getAllExplicitlyDeclaredUsersByOuId(company, version, ouId);
        //PROP-403 : In case of trellis strictlyExplicit = true. Then, if there's an explicit condition defined on OU but no users are matching it, return empty rather than all users
        if (users.isNotEmpty() || (BooleanUtils.isTrue(strictlyExplicit) && ouConfig.hasUsersSelection())){
            return users;
        }
        log.info("Could not find explicitly declared users for OU id={}, company={}, version={}. Returning all users...", ouId, company, version);
        return getAllUsers(company, version);
    }

    public DBOrgAccessUsers getAllAccessUsersByOuId(String company, String version, UUID ouId) throws SQLException, JsonProcessingException {
        return getAllAccessUsersByOuId(company, version, ouId, null);
    }

    /**
     * This returns only explicitly declared users, i.e. static users or dynamic users.
     * If users are not explicitly defined, then the OU is implicitly supposed to contain any user.
     */
    public DBOrgAccessUsers getAllExplicitlyDeclaredUsersByOuId(String company, @Nullable String version, UUID ouId) throws SQLException, JsonProcessingException {

        int currentVersion = getCurrentVersion(company, version);

        Map<String, Object> params = Maps.newHashMap();
        params.put("versions", new int[]{currentVersion});

        String dynamicUserSelectionQuery = getDynamicUserSelectionQueryFromOuIds(company, List.of(ouId), params);
        boolean hasDynamicUsersCondition = StringUtils.isNotEmpty(dynamicUserSelectionQuery);

        String dynamicUserMatch = hasDynamicUsersCondition ? " OR ou.id IN (SELECT id from ( " + dynamicUserSelectionQuery + ") AS dy ) " : "";

        String orgUsersSelect = "SELECT ou.id  FROM " + company + "." + ORG_USERS + " ou";
        String groupBy = " GROUP BY ou.id";

        String joinContentSections = " INNER JOIN " + company + "." + OU_CONTENT_SECTIONS + " ocs" +
                " ON ( (ref_id = ANY (ocs.user_ref_ids ) AND ocs.ou_id = :outer_ou_ids) " +
                dynamicUserMatch + ")";
        params.put("outer_ou_ids", ouId);

        List<String> finalTableConditions = new ArrayList<>();

        //add version condition for static users
        if (!hasDynamicUsersCondition) {
            finalTableConditions.add(getVersionConditionSql(company, params));
        }
        String where = finalTableConditions.isEmpty()? "" : " WHERE " + String.join(" AND ", finalTableConditions);

        String finalQuery = orgUsersSelect
                + joinContentSections
                + where
                + groupBy;

        log.info("sql = " + finalQuery); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);

        return template.query(finalQuery, params, ORG_ACCESS_USERS_RESULT_SET_EXTRACTOR);
    }

    /**
     * This returns all users for latest version.
     */
    public DBOrgAccessUsers getAllUsers(String company, @Nullable String version) throws SQLException {

        int currentVersion = getCurrentVersion(company, version);

        Map<String, Object> params = Maps.newHashMap();
        params.put("versions", new int[]{currentVersion});

        List<String> finalTableConditions = new ArrayList<>();
        finalTableConditions.add(getVersionConditionSql(company, params));

        String where = finalTableConditions.isEmpty()? "" : " WHERE " + String.join(" AND ", finalTableConditions);

        String orgUsersSelect = "SELECT ou.id  FROM " + company + "." + ORG_USERS + " ou";
        String finalQuery = orgUsersSelect
                + where;

        log.info("sql = " + finalQuery); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);

        return template.query(finalQuery, params, ORG_ACCESS_USERS_RESULT_SET_EXTRACTOR);
    }

    private int getCurrentVersion(String company, @Nullable String version) throws SQLException {
        if (StringUtils.isNotEmpty(version)) {
            return Integer.parseInt(version);
        }
        return versionsService.getActive(company, OrgVersion.OrgAssetType.USER)
                .orElseThrow(() -> new SQLException("Could not find active user version"))
                .getVersion();
    }

    private String getVersionConditionSql(String company,Map<String, Object> params ) {
        String versionCondition = "ou.versions @> ARRAY(SELECT version FROM " + company + "." + ORG_VERSION_COUNTER + " WHERE type = :org_user_selection_version_type AND active = true)";
        params.put("org_user_selection_version_type", "USER");
        return versionCondition;
    }

    private String getDynamicUserSelectionQueryFromOuIds(String company, List<UUID> orgUnitIdList, Map<String, Object> params) throws SQLException, JsonProcessingException {
        List<OUConfiguration> ouConfigurationlist = orgUnitHelper.getOuConfigurationList(company, null, orgUnitIdList);

        final Map<String, Object> dynamicUserSelector = Maps.newHashMap();

        ouConfigurationlist.stream()
                .filter(ouConfiguration -> ouConfiguration.getDynamicUsers() && CollectionUtils.isNotEmpty(ouConfiguration.getSections()))
                .map(ouConfiguration -> ouConfiguration.getSections())
                .flatMap(section -> section.stream())
                .forEach(section -> {
                    if (MapUtils.isNotEmpty(section.getDynamicUsers())) {
                        for (Map.Entry<String, Object> entry : section.getDynamicUsers().entrySet()) {
                            String key = entry.getKey();
                            if(key.equals("partial_match")){
                                var map = (Map<String, Object>) entry.getValue();
                                for (Map.Entry<String, Object> entry1 : map.entrySet()){
                                    key = entry1.getKey();
                                    Object value = entry1.getValue();
                                    parseCondition(key, value, dynamicUserSelector);
                                }
                            }else if(key.equals("exclude")){
                                dynamicUserSelector.put(key, entry.getValue());
                            } else {
                                Object value = entry.getValue();
                                parseCondition(key, value, dynamicUserSelector);
                            }
                        }
                    }
                });
        String dynamicUserSelectorQuery = "";

        if (MapUtils.isNotEmpty(dynamicUserSelector)) {
            dynamicUserSelectorQuery = OrgUsersHelper.getOrgUsersSelectQuery(company,dynamicUserSelector,params);
        }
        return dynamicUserSelectorQuery;
    }

    private List<UUID> getOrgUnitList(String company, Map<String, Object> params, String mangersQuery) throws SQLException {

        String query = mangersQuery + "SELECT ou_id FROM managers";

        log.info("Query is {}", query);
        log.info("Params are {}", params);

        return template.queryForList(query, params, UUID.class);
    }

    public void parseCondition(final String key, final Object value, final Map<String, Object> params) {

        if (value instanceof String) {
            parseCondition(key, (String) value, params);
        }

        if (value instanceof Number) {
            parseCondition(key, (Number) value, params);
        }

        if (value instanceof Map) {
            parseCondition(key, (Map<String, Object>) value, params);
        }

        if (value instanceof Collection) {
            parseCondition(key, (Collection) value, params);
        }

    }

    public void parseCondition(final String key, final String value, final Map<String, Object> params) {
        List<String> list = (List<String>) params.getOrDefault(key, Lists.newArrayList());
        list.add(value);
        params.put(key, list);
    }

    public void parseCondition(final String key, final Number value, final Map<String, Object> params) {
        List<Object> list = (List<Object>) params.getOrDefault(key, Lists.newArrayList());
        list.add(value);
        params.put(key, value);
    }

    public void parseCondition(final String key, final Collection<? extends Object> value, final Map<String, Object> params) {

        List<Object> list = (List<Object>) params.getOrDefault(key, Lists.newArrayList());
        for (var v : value) {
            list.add(v);
        }
        params.put(key, list);
    }

    public static void parseCondition(final String key, final Map<String, Object> conditions, final Map<String, Object> params) {

        Map<String, Object> map = Maps.newHashMap();
        Map<String, Object> tempMap = Maps.newHashMap();
        var obj = params.get(key);

        if (obj instanceof Map) {
            tempMap = (Map<String, Object>) obj;
        }

        for (var entry : conditions.entrySet()) {
            var entryKey = entry.getKey();
            var value = entry.getValue();
            var tempValue = tempMap.get(entry.getKey());
            switch (entry.getKey()) {
                case "$gt":
                case "$gte":
                case "$age":
                    if (tempMap.containsKey(entryKey) && value != null && value instanceof Number && (long) tempValue > (long) value) {
                        value = tempValue;
                    }

                    break;
                case "$lt":
                case "$lte":
                    if (tempMap.containsKey(entryKey) && value != null && value instanceof Number && (long) tempValue < (long) value) {
                        value = tempValue;
                    }
                    break;
                case "$begin":
                case "$begins":
                case "$end":
                case "$ends":
                case "$contain":
                case "$contains":
                    if (tempMap.containsKey(entryKey) && value instanceof String) {
                        value = ((String) value).replace("(", "");
                        value = ((String) value).replace(")", "");
                        value = "(" + value + "|" + tempValue + ")";
                    }
                    break;
                default:
                    // pass it to other parsers and append results
                    log.warn("unsupported key: {}", entry.getKey());
                    continue;
            }
            map.put(entryKey, value);
        }
        ;
        params.put(key, map);
    }
}
