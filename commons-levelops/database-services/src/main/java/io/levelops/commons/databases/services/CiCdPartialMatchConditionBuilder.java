package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Service;
import io.levelops.commons.databases.services.CiCdMetadataConditionBuilder.MetadataFields;
import io.levelops.commons.databases.services.CiCdMetadataConditionBuilder.FieldTypes;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Log4j2
@Service
public class CiCdPartialMatchConditionBuilder {

    @Autowired
    public CiCdPartialMatchConditionBuilder(){}

    public void preparePartialMatchConditions(CiCdJobRunsFilter filter, Map<String, Object> params, List<String> criterias, String paramSuffix, Set<String> partialMatchColumns) {
        if(MapUtils.isNotEmpty(filter.getPartialMatch())) {
            Map<String, Map<String, String>> partialMatch = filter.getPartialMatch();
            sanitizedPartialMatch(partialMatch);
            addPartialMatchClause(partialMatch, criterias, params, null, partialMatchColumns, Collections.emptySet(), paramSuffix);
        }
    }

    private void sanitizedPartialMatch(Map<String, Map<String, String>> partialMatch){
        if(MapUtils.isEmpty(partialMatch)){
            return;
        }
        if(!MapUtils.isEmpty(partialMatch.get("services"))){
            partialMatch.put("service_ids", partialMatch.get("services"));
            partialMatch.remove("services");
        }
        if(!MapUtils.isEmpty(partialMatch.get("environments"))){
            partialMatch.put("env_ids", partialMatch.get("environments"));
            partialMatch.remove("environments");
        }
        if(!MapUtils.isEmpty(partialMatch.get("infrastructures"))){
            partialMatch.put("infra_ids", partialMatch.get("infrastructures"));
            partialMatch.remove("infrastructures");
        }
        if(!MapUtils.isEmpty(partialMatch.get("repositories"))){
            partialMatch.put("repo_url", partialMatch.get("repositories"));
            partialMatch.remove("repositories");
        }
        if(!MapUtils.isEmpty(partialMatch.get("deployment_types"))){
            partialMatch.put("service_types", partialMatch.get("deployment_types"));
            partialMatch.remove("deployment_types");
        }
    }

    private void addPartialMatchClause(Map<String, Map<String, String>> partialMatchMap,
                                       List<String> criteria, Map<String, Object> params,
                                       MapSqlParameterSource sqlParams,
                                       Set<String> partialMatchColumns, Set<String> partialMatchArrayColumns, String suffix) {
        if (MapUtils.isEmpty(partialMatchMap)) {
            return;
        }
        for (Map.Entry<String, Map<String, String>> partialMatchEntry : partialMatchMap.entrySet()) {
            String key = partialMatchEntry.getKey();
            Map<String, String> value = partialMatchEntry.getValue();

            String begins = value.get("$begins");
            String ends = value.get("$ends");
            String contains = value.get("$contains");

            if (StringUtils.firstNonEmpty(begins, ends, contains) != null) {
                if(CiCdMetadataConditionBuilder.MetadataFields.fromString(key) != null) {
                    if (MetadataFields.fromString(key).fieldType == FieldTypes.ArrayType) {
                        key = "metadata->'" + key + "'";
                        createPartialMatchConditionJsonArray(criteria, params, key, begins, ends, contains, suffix);
                    } else if (MetadataFields.fromString(key).fieldType == FieldTypes.StringType) {
                        key = "metadata->>'" + key + "'";
                        createPartialMatchCondition(criteria, params, sqlParams, key, begins, ends, contains, suffix);
                    }
                }
                else {
                    if (partialMatchColumns.contains(key)) {
                        createPartialMatchCondition(criteria, params, sqlParams, key, begins, ends, contains, suffix);
                    } else if (partialMatchArrayColumns.contains(key)) {
                        createPartialMatchConditionArray(criteria, params, sqlParams, key, begins, ends, contains, suffix);
                    }
                }
            }
        }
    }

    private void createPartialMatchConditionJsonArray(List<String> criterias, Map<String, Object> params,
                                                      String key, String begins, String ends, String contains, String paramSuffix) {
        String keyName = key.replaceAll("[^A-Za-z0-9_]", "");
        if (begins != null) {
            String beingsCondition = "exists (select 1 from jsonb_array_elements_text(" + key + ") as k where k ILIKE :" + keyName + "_begins" + paramSuffix + " )";
            params.put(keyName + "_begins" + paramSuffix, begins + "%");
            criterias.add(beingsCondition);
        }

        if (ends != null) {
            String endsCondition = "exists (select 1 from jsonb_array_elements_text(" + key + ") as k where k ILIKE :" + keyName + "_ends" + paramSuffix + " )";
            params.put(keyName + "_ends" + paramSuffix, "%" + ends);
            criterias.add(endsCondition);
        }

        if (contains != null) {
            String containsCondition = "exists (select 1 from jsonb_array_elements_text(" + key + ") as k where k ILIKE :" + keyName + "_contains" + paramSuffix + " )";
            params.put(keyName + "_contains" + paramSuffix, "%" + contains + "%");
            criterias.add(containsCondition);
        }
    }

    private void createPartialMatchCondition(List<String> fileTableConditions,
                                             Map<String, Object> params,
                                             MapSqlParameterSource sqlParameterSourcearams,
                                             String key, String begins, String ends, String contains, String suffix) {
        String keyName = key.replaceAll("[^A-Za-z0-9_]", "");

        String partialString;
        if (begins != null) {
            String beingsCondition = key + " SIMILAR TO " + ":" + keyName + "_begins" + suffix;
            partialString = begins + "%";
            fileTableConditions.add(beingsCondition);
            if (sqlParameterSourcearams != null)
                sqlParameterSourcearams.addValue(keyName + "_begins" + suffix, partialString);
            else
                params.put(keyName + "_begins" + suffix, partialString);
        }

        if (ends != null) {
            String endsCondition = key + " SIMILAR TO " + ":" + keyName + "_ends" + suffix;
            partialString = "%" + ends;
            fileTableConditions.add(endsCondition);
            if (sqlParameterSourcearams != null)
                sqlParameterSourcearams.addValue(keyName + "_ends" + suffix, partialString);
            else
                params.put(keyName + "_ends" + suffix, partialString);
        }

        if (contains != null) {
            String containsCondition = key + " SIMILAR TO " + ":" + keyName + "_contains" + suffix;
            partialString = "%" + contains + "%";
            fileTableConditions.add(containsCondition);
            if (sqlParameterSourcearams != null)
                sqlParameterSourcearams.addValue(keyName + "_contains" + suffix, partialString);
            else
                params.put(keyName + "_contains" + suffix, partialString);
        }
    }

    private void createPartialMatchConditionArray(List<String> conditions, Map<String, Object> params,
                                                  MapSqlParameterSource sqlParameterSourcearams,
                                                  String key, String begins, String ends, String contains, String suffix) {
        String partialString;
        String keyName = key.replaceAll("[^A-Za-z0-9_]", "");
        if (begins != null) {
            String beingsCondition = "exists (select 1 from unnest (" + key + ") as k where k SIMILAR TO " + ":" + keyName + "_begins" + suffix;
            partialString = begins + "%";
            conditions.add(beingsCondition);
            if (sqlParameterSourcearams != null)
                sqlParameterSourcearams.addValue(keyName + "_begins" + suffix, partialString);
            else
                params.put(keyName + "_begins" + suffix, partialString);
        }

        if (ends != null) {
            String endsCondition = "exists (select 1 from unnest (" + key + ") as k where k SIMILAR TO " + ":" + keyName + "_ends" + suffix;
            partialString = "%" + ends;
            conditions.add(endsCondition);
            if (sqlParameterSourcearams != null)
                sqlParameterSourcearams.addValue(keyName + "_ends" + suffix, partialString);
            else
                params.put(keyName + "_ends" + suffix, partialString);
        }

        if (contains != null) {
            String containsCondition = "exists (select 1 from unnest (" + key + ") as k where k SIMILAR TO " + ":" + keyName + "_contains" + suffix;
            partialString = "%" + contains + "%";
            conditions.add(containsCondition);
            if (sqlParameterSourcearams != null)
                sqlParameterSourcearams.addValue(keyName + "_contains" + suffix, partialString);
            else
                params.put(keyName + "_contains" + suffix, partialString);
        }
    }
}