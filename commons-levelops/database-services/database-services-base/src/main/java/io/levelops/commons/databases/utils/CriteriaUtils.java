package io.levelops.commons.databases.utils;

import org.apache.commons.collections4.MapUtils;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CriteriaUtils {
    public static void addPartialMatchClause(Map<String, Map<String, String>> partialMatchMap,
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

            if (begins != null || ends != null || contains != null) {
                if (partialMatchColumns.contains(key)) {
                    createPartialMatchCondition(criteria, params, sqlParams, key, begins, ends, contains, suffix);
                } else if (partialMatchArrayColumns.contains(key)) {
                    createPartialMatchConditionArray(criteria, params, sqlParams, key, begins, ends, contains, suffix);
                }
            }
        }
    }

    private static void createPartialMatchCondition(List<String> fileTableConditions,
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

    private static void createPartialMatchConditionArray(List<String> conditions, Map<String, Object> params,
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
