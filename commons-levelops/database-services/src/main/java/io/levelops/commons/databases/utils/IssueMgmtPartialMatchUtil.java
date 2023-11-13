package io.levelops.commons.databases.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;

public class IssueMgmtPartialMatchUtil {

    public static void createPartialMatchFilter(Map<String, Map<String, String>> partialMatchMap,
                                                List<String> issueTblConditions,
                                                Map<String, Object> params,
                                                String tblQualifier,
                                                Map<String,String> partialMatchColumns,
                                                Map<String,String> partialMatchArrayColumns,
                                                Map<String,String> partialMatchArrayAttributeColumns,
                                                Map<String,String> partialMatchAttributeColumns,
                                                boolean enableCustomFieldPartialMatch) {
        for (Map.Entry<String, Map<String, String>> partialMatchEntry : partialMatchMap.entrySet()) {
            String key = partialMatchEntry.getKey();
            Map<String, String> value = partialMatchEntry.getValue();

            String begins = value.get("$begins");
            String ends = value.get("$ends");
            String contains = value.get("$contains");

            if (StringUtils.firstNonEmpty(begins, ends, contains) != null) {
                if (enableCustomFieldPartialMatch && IssueMgmtCustomFieldUtils.isCustomField(key)) {
                    key = "custom_fields->>'" + key + "'";
                    createPartialMatchCondition(issueTblConditions, params, key, begins, ends, contains, tblQualifier);
                } else if (partialMatchArrayColumns.containsKey(key)) {
                    createPartialMatchConditionArray(issueTblConditions, params, partialMatchArrayColumns.get(key), begins, ends, contains, tblQualifier);
                } else if (partialMatchColumns.containsKey(key)) {
                    createPartialMatchCondition(issueTblConditions, params, partialMatchColumns.get(key), begins, ends, contains, tblQualifier);
                } else if (partialMatchAttributeColumns.containsKey(key)) {
                    key = "attributes->>'" + partialMatchAttributeColumns.get(key) + "'";
                    createPartialMatchCondition(issueTblConditions, params, key, begins, ends, contains, tblQualifier);
                } else if (partialMatchArrayAttributeColumns.containsKey(key)) {
                    key = "attributes->'" + partialMatchArrayAttributeColumns.get(key) + "'";
                    createPartialMatchConditionJsonArray(issueTblConditions, params, key, begins, ends, contains, tblQualifier);
                }
            }
        }
    }

    private static void createPartialMatchConditionJsonArray(List<String> issueTblConditions, Map<String, Object> params,
                                                             String key, String begins, String ends, String contains, String tblQualifier) {
        key = tblQualifier + key;
        String keyName = key.replaceAll("[^A-Za-z0-9_]", "");
        if (begins != null) {
            String beingsCondition = "exists (select 1 from jsonb_array_elements_text(" + key + ") as k where k ILIKE :" + keyName + "_begins )";
            params.put(keyName + "_begins", begins + "%");
            issueTblConditions.add(beingsCondition);
        }
        if (ends != null) {
            String endsCondition = "exists (select 1 from jsonb_array_elements_text(" + key + ") as k where k ILIKE :" + keyName + "_ends )";
            params.put(keyName + "_ends", "%" + ends);
            issueTblConditions.add(endsCondition);
        }
        if (contains != null) {
            String containsCondition = "exists (select 1 from jsonb_array_elements_text(" + key + ") as k where k ILIKE :" + keyName + "_contains )";
            params.put(keyName + "_contains", "%" + contains + "%");
            issueTblConditions.add(containsCondition);
        }
    }

    private static void createPartialMatchConditionArray(List<String> issueTblConditions, Map<String, Object> params,
                                                         String key, String begins, String ends, String contains, String tblQualifier) {
        key = tblQualifier + key;
        String keyName = key.replaceAll("[^A-Za-z0-9_]", "");
        if (begins != null) {
            String beingsCondition = "exists (select 1 from unnest (" + key + ") as k where k ILIKE :" + keyName + "_begins )";
            params.put(keyName + "_begins", begins + "%");
            issueTblConditions.add(beingsCondition);
        }
        if (ends != null) {
            String endsCondition = "exists (select 1 from unnest (" + key + ") as k where k ILIKE :" + keyName + "_ends )";
            params.put(keyName + "_ends", "%" + ends);
            issueTblConditions.add(endsCondition);
        }
        if (contains != null) {
            String containsCondition = "exists (select 1 from unnest (" + key + ") as k where k ILIKE :" + keyName + "_contains )";
            params.put(keyName + "_contains", "%" + contains + "%");
            issueTblConditions.add(containsCondition);
        }
    }

    private static void createPartialMatchCondition(List<String> issueTblConditions, Map<String, Object> params, String key, String begins,
                                                    String ends, String contains, String tblQualifier) {
        key = key.equalsIgnoreCase("sprint_name") ? "name" : key;
        key = tblQualifier + key;
        String keyName = key.replaceAll("[^A-Za-z0-9_]", "");
        if (key.equalsIgnoreCase("milestone_full_name") || key.equalsIgnoreCase("sprint_full_name") || key.equalsIgnoreCase("sprint_full_names")) {
            keyName = key;
            key = "concat(parent_field_value, '\\',  name)";
        }
        if (begins != null) {
            String beingsCondition = key + " SIMILAR TO :" + keyName + "_begins ";
            params.put(keyName + "_begins", begins + "%");
            issueTblConditions.add(beingsCondition);
        }

        if (ends != null) {
            String endsCondition = key + " SIMILAR TO :" + keyName + "_ends ";
            params.put(keyName + "_ends", "%" + ends);
            issueTblConditions.add(endsCondition);
        }

        if (contains != null) {
            String containsCondition = key + " SIMILAR TO :" + keyName + "_contains ";
            params.put(keyName + "_contains", "%" + contains + "%");
            issueTblConditions.add(containsCondition);
        }
    }
}
