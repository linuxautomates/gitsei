package io.levelops.commons.databases.services;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log4j2
@Service
public class CiCdMetadataConditionBuilder {

    public enum FieldTypes{
        ArrayType,
        StringType,
        BooleanType
    }
    @Getter
    public enum MetadataFields {

        service_ids(FieldTypes.ArrayType),
        env_ids(FieldTypes.ArrayType),
        infra_ids(FieldTypes.ArrayType),
        repo_url(FieldTypes.StringType),
        branch(FieldTypes.StringType),
        service_types(FieldTypes.ArrayType),
        rollback(FieldTypes.BooleanType),
        tags(FieldTypes.ArrayType);

        @Accessors(fluent = true)
        public final FieldTypes fieldType;

        MetadataFields(FieldTypes fieldType) {
            this.fieldType = fieldType;
        }

        @JsonCreator
        @Nullable
        public static MetadataFields fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(MetadataFields.class, value);
        }
    }

    @Autowired
    public CiCdMetadataConditionBuilder() {}

    public void prepareMetadataConditions(CiCdJobRunsFilter filter, Map<String, Object> params, String paramPrefix, List<String> criterias) {
        Map<String, Object> metadataFields = new HashMap<>();
        if(CollectionUtils.isNotEmpty(filter.getServices())) {
            metadataFields.put("service_ids", filter.getServices());
        }
        if(CollectionUtils.isNotEmpty(filter.getEnvironments())) {
            metadataFields.put("env_ids", filter.getEnvironments());
        }
        if(CollectionUtils.isNotEmpty(filter.getInfrastructures())) {
            metadataFields.put("infra_ids", filter.getInfrastructures());
        }
        if(CollectionUtils.isNotEmpty(filter.getRepositories())) {
            metadataFields.put("repo_url", filter.getRepositories());
        }
        if(filter.getRollback() != null) {
            metadataFields.put("rollback", filter.getRollback());
        }
        if(CollectionUtils.isNotEmpty(filter.getBranches())) {
            metadataFields.put("branch", filter.getBranches());
        }
        if(CollectionUtils.isNotEmpty(filter.getDeploymentTypes())) {
            metadataFields.put("service_types", filter.getDeploymentTypes());
        }
        if(CollectionUtils.isNotEmpty(filter.getTags())) {
            metadataFields.put("tags", filter.getTags());
        }
        if (!metadataFields.isEmpty()){
            createMetadataConditions(params, paramPrefix, metadataFields, true, criterias);
        }

        Map<String, Object> excludeMetadataFields = new HashMap<>();
        if (CollectionUtils.isNotEmpty(filter.getExcludeServices())){
            excludeMetadataFields.put("service_ids", filter.getExcludeServices());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeEnvironments())){
            excludeMetadataFields.put("env_ids", filter.getExcludeEnvironments());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeInfrastructures())){
            excludeMetadataFields.put("infra_ids", filter.getExcludeInfrastructures());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeRepositories())){
            excludeMetadataFields.put("repo_url", filter.getExcludeRepositories());
        }
        if(filter.getExcludeRollback() != null){
            excludeMetadataFields.put("rollback", filter.getExcludeRollback());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeBranches())){
            excludeMetadataFields.put("branch", filter.getExcludeBranches());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeDeploymentTypes())){
            excludeMetadataFields.put("service_types", filter.getExcludeDeploymentTypes());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeTags())){
            excludeMetadataFields.put("tags", filter.getExcludeTags());
        }
        if(!excludeMetadataFields.isEmpty()){
            createMetadataConditions(params, paramPrefix, excludeMetadataFields, false, criterias);
        }
    }

    @SuppressWarnings("unchecked")
    private void createMetadataConditions(Map<String, Object> params,
                                          String paramPrefix,
                                          Map<String, Object> json,
                                          boolean include,
                                          List<String> criteriaConditions) {
        int fieldNumber = 0;
        for (String key : json.keySet()) {
            if (StringUtils.isEmpty(key)) {
                continue;
            }
            String fieldRef = (paramPrefix == null ? "" : paramPrefix) +
                    (include ? "metadatafield" : "not_metadatafield") + fieldNumber;

            if (json.get(key) instanceof Boolean && MetadataFields.fromString(key).fieldType == FieldTypes.BooleanType) {
                Boolean booleanValue = Boolean.class.cast(json.get(key));
                fieldRef = fieldRef + "_boolean" + fieldNumber;
                StringBuilder conditionBuilder = new StringBuilder("(");
                String condition = (include ? " " : " ( NOT ") + "metadata @> :" + fieldRef + " :: jsonb";
                conditionBuilder.append(condition);
                if (!include) {
                    conditionBuilder.append(" OR NOT metadata ?? '").append(key).append("' ");
                    conditionBuilder.append(") ");
                }
                conditionBuilder.append(") ");
                criteriaConditions.add(conditionBuilder.toString());
                params.put(fieldRef, "{\"" + key + "\":" + booleanValue + "}");
            } else if (MetadataFields.fromString(key).fieldType == FieldTypes.ArrayType) {
                StringBuilder conditionBuilder = new StringBuilder("(");
                int valNum = 0;
                List<String> values = List.class.cast(json.get(key));
                if (CollectionUtils.isEmpty(values)) {
                    continue;
                }
                String condition = (include ? " " : " ( NOT ") + " metadata->'" + key + "' @> ANY(ARRAY[ :";
                fieldRef = fieldRef + "_val" + (valNum);
                conditionBuilder.append(condition).append(fieldRef).append(" ]::jsonb[])");
                if (!include) {
                    conditionBuilder.append(" OR NOT metadata ?? '").append(key).append("' ");
                    conditionBuilder.append(") ");
                }
                params.put(fieldRef, values.stream().map(val -> "[\"" + StringEscapeUtils.escapeJson(val) + "\"]")
                        .collect(Collectors.toList()));
                conditionBuilder.append(") ");
                criteriaConditions.add(conditionBuilder.toString());
            } else if(MetadataFields.fromString(key).fieldType == FieldTypes.StringType) {
                StringBuilder conditionBuilder = new StringBuilder("(");
                int valNum = 0;
                List<String> values = List.class.cast(json.get(key));
                if (CollectionUtils.isEmpty(values)) {
                    continue;
                }
                String condition = (include ? "" : "NOT ") + "metadata @> :";
                for (String value : values) {
                    fieldRef = fieldRef + "_val" + (valNum++);
                    if (valNum > 1) {
                        conditionBuilder.append(" OR ");
                    }
                    conditionBuilder.append(condition).append(fieldRef).append("::jsonb");
                    params.put(fieldRef, "{\"" + key + "\":\"" + StringEscapeUtils.escapeJson(value) + "\"}");
                }
                criteriaConditions.add(conditionBuilder.append(")").toString());
            }
            fieldNumber += 1;
        }
    }
}