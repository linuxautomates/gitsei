package io.levelops.commons.databases.services.parsers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdJobConfigChangesFilter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.models.filters.CiCdUtils.parseCiCdQualifiedJobNames;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOfObjectOrDefault;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;

@Log4j2
public class CiCdJobConfigChangesFilterParser {

    private final ObjectMapper objectMapper;

    public CiCdJobConfigChangesFilterParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @SuppressWarnings("unchecked")
    public CiCdJobConfigChangesFilter merge(Integer integrationId, CiCdJobConfigChangesFilter requestFilter, Map<String, Object> productFilter) {
        Map<String, Object> excludeFields = (Map<String, Object>) productFilter
                .getOrDefault("exclude", Map.of());
        return CiCdJobConfigChangesFilter.builder()
                .across(requestFilter.getAcross())
                .stacks(requestFilter.getStacks())
                .calculation(requestFilter.getCalculation())
                .cicdUserIds(getListOrDefault(productFilter, "cicd_user_ids"))
                .jobNames(getListOrDefault(productFilter, "job_names"))
                .instanceNames(getListOrDefault(productFilter, "instance_names"))
                .changeStartTime(requestFilter.getChangeStartTime())
                .changeEndTime(requestFilter.getChangeEndTime())
                .integrationIds(List.of(String.valueOf(integrationId)))
                .types(CICD_TYPE.parseFromFilter(productFilter))
                .projects(getListOrDefault(productFilter, "projects"))
                .excludeProjects(getListOrDefault(excludeFields, "projects"))
                .excludeJobNames(getListOrDefault(excludeFields, "job_names"))
                .excludeInstanceNames(getListOrDefault(excludeFields, "instance_names"))
                .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                .excludeCiCdUserIds(getListOrDefault(excludeFields, "cicd_user_ids"))
                .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(productFilter, "qualified_job_names")))
                .build();
    }

    public String getSqlStmt(String company, List<String> conditions,
                             String innerSelect, boolean isList) {
        String whereClause = CollectionUtils.isEmpty(conditions) ? "" : " WHERE " + String.join(" AND ", conditions);
        if (isList) {
            innerSelect = " c.cicd_job_id, j.job_name, c.change_time," +
                    " c.change_type, c.cicd_user_id, i.type, i.name as instance_name, i.id as instance_guid";
        }
        return " SELECT  c.id as id," + innerSelect
                + " FROM " + company + ".cicd_job_config_changes as c"
                + " JOIN " + company + ".cicd_jobs as j on c.cicd_job_id = j.id"
                + " LEFT OUTER JOIN " + company + ".cicd_instances as i on j.cicd_instance_id = i.id"
                + whereClause;
    }
}
