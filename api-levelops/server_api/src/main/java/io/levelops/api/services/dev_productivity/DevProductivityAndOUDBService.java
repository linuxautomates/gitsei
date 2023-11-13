package io.levelops.api.services.dev_productivity;

import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfileInfo;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUnitInfo;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Log4j2
@Service
public class DevProductivityAndOUDBService {
    private final NamedParameterJdbcTemplate template;

    @Autowired
    public DevProductivityAndOUDBService(DataSource dataSource) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
    }

    private static final String SELECT_DEV_PROD_PROFILE_INFO_SQL = "select p.id as profile_id, p.name as profile_name, o.id as ou_id, o.ref_id as ou_ref_id, o.name as ou_name\n" +
            "from %s.dev_productivity_profiles as p\n" +
            "join %s.dev_productivity_profile_ou_mappings as m on m.dev_productivity_profile_id = p.id\n" +
            "join %s.ous as o on o.ref_id=m.ou_ref_id\n" +
            "where o.active=true AND p.id in (:profile_ids) \n" +
            "order by p.name, p.id, o.name, o.ref_id;";

    private static final String SELECT_DEV_PROD_PROFILE_INFO_SQL_V2 = "select p.id as profile_id, p.name as profile_name, o.id as ou_id, o.ref_id as ou_ref_id, o.name as ou_name\n" +
            "from %s.dev_productivity_profiles as p\n" +
            "join %s.dev_productivity_parent_profiles as pp on p.parent_profile_id = pp.id\n" +
            "join %s.dev_productivity_parent_profile_ou_mappings as m on m.dev_productivity_parent_profile_id = pp.id\n" +
            "join %s.ous as o on o.ref_id=m.ou_ref_id\n" +
            "where o.active=true AND p.id in (:profile_ids) \n" +
            "order by p.name, p.id, o.name, o.ref_id";

    public List<DevProductivityProfileInfo> getDevProductivityProfileInfo(final String company, final List<UUID> devProductivityProfileIds) {
        return getDevProductivityProfileInfo(company, devProductivityProfileIds, false);
    }

    public List<DevProductivityProfileInfo> getDevProductivityProfileInfo(final String company, final List<UUID> devProductivityProfileIds, final Boolean useParentProfiles) {
        if(CollectionUtils.isEmpty(devProductivityProfileIds)) {
            return Collections.EMPTY_LIST;
        }
        String selectSql = BooleanUtils.isTrue(useParentProfiles) ? String.format(SELECT_DEV_PROD_PROFILE_INFO_SQL_V2, company, company, company, company)
                            : String.format(SELECT_DEV_PROD_PROFILE_INFO_SQL, company, company, company);
        HashMap<String, Object> params = new HashMap<>();
        params.put("profile_ids", devProductivityProfileIds);
        log.debug("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<DevProductivityProfileInfo> dbResults = template.query(selectSql, params, (rs, rowNumber) -> {
            UUID profileId = (UUID) rs.getObject("profile_id");
            String profileName = rs.getString("profile_name");

            UUID ouId = (UUID) rs.getObject("ou_id");
            Integer ouRefId = rs.getInt("ou_ref_id");
            String ouName = rs.getString("ou_name");
            return DevProductivityProfileInfo.builder()
                    .id(profileId).name(profileName)
                    .associatedOUs(List.of(
                            OrgUnitInfo.builder().ouId(ouId).ouRefId(ouRefId).ouName(ouName).build()
                    ))
                    .build();
        });
        if(CollectionUtils.isEmpty(dbResults)) {
            return Collections.EMPTY_LIST;
        }
        Map<UUID, List<OrgUnitInfo>> profileIdOrgInfoMap = new HashMap<>();
        for(DevProductivityProfileInfo i : dbResults) {
            if(CollectionUtils.isEmpty(i.getAssociatedOUs())) {
                continue;
            }
            profileIdOrgInfoMap.computeIfAbsent(i.getId(), k -> new ArrayList<>()).add(i.getAssociatedOUs().get(0));
        }

        Set<UUID> processedProfileIds = new HashSet<>();
        List<DevProductivityProfileInfo> results = new ArrayList<>();
        for(DevProductivityProfileInfo i : dbResults) {
            if(processedProfileIds.contains(i.getId())) {
                continue;
            }
            processedProfileIds.add(i.getId());
            DevProductivityProfileInfo.DevProductivityProfileInfoBuilder bldr = DevProductivityProfileInfo.builder()
                    .id(i.getId()).name(i.getName());
            List<OrgUnitInfo> orgUnitInfos = profileIdOrgInfoMap.getOrDefault(i.getId(), new ArrayList<>());
            if(CollectionUtils.isNotEmpty(orgUnitInfos)) {
                bldr.associatedOUs(orgUnitInfos);
            }
            results.add(bldr.build());
        }
        return results;
    }
}
