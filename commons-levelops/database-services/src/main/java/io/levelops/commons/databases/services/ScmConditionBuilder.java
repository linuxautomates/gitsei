package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.web.exceptions.BadRequestException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.levelops.commons.databases.services.ScmDoraAggService.DIRECT_MERGE_TABLE;

public class ScmConditionBuilder {

    public static final Set<String> MATCH_ARRAY_COLUMNS = Set.of("labels", "tags");

    public static Map<String, List<String>> buildRegexPatternMap(Map<String, Object> params,
                                                                 Map<VelocityConfigDTO.ScmConfig.Field, Map<String, List<String>>> profileConfig,
                                                                 List<String> conditions,
                                                                 String tableName,
                                                                 String suffix) {
        List<String> conditionsForCommits = new ArrayList<>();
        for (Map.Entry<VelocityConfigDTO.ScmConfig.Field, Map<String, List<String>>> partialMatchEntry : profileConfig.entrySet()) {
            VelocityConfigDTO.ScmConfig.Field field = partialMatchEntry.getKey();
            Map<String, List<String>> matchEntryValue = partialMatchEntry.getValue();
            if (field == VelocityConfigDTO.ScmConfig.Field.commit_branch) {
                getRegexPattern(params, conditionsForCommits, suffix, field, matchEntryValue);
            } else {
                getRegexPattern(params, conditions, suffix, field, matchEntryValue);
            }
        }
        return Map.of(tableName, conditions, DIRECT_MERGE_TABLE, conditionsForCommits);
    }

    public static Map<String, List<String>> buildNewRegexPatternMap(Map<String, Object> params,
                                                                    Map<String, Map<String, List<String>>> profileConfig,
                                                                    List<String> conditions,
                                                                    String tableName,
                                                                    String suffix) {
        List<String> conditionsForCommits = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<String>>> partialMatchEntry : profileConfig.entrySet()) {
            String field = partialMatchEntry.getKey();
            Map<String, List<String>> matchEntryValue = partialMatchEntry.getValue();
            if ("commit_branch".equals(field) || "tags".equals(field)) {
                getnewRegexPattern(params, conditionsForCommits, suffix, field, matchEntryValue);
            } else {
                getnewRegexPattern(params, conditions, suffix, field, matchEntryValue);
            }
        }
        return Map.of(tableName, conditions, DIRECT_MERGE_TABLE, conditionsForCommits);
    }

    private static void getRegexPattern(Map<String, Object> params, List<String> conditions, String suffix, VelocityConfigDTO.ScmConfig.Field field, Map<String, List<String>> matchEntryValue) {
        List<String> begins = matchEntryValue.get("$begins");
        List<String> ends = matchEntryValue.get("$ends");
        List<String> contains = matchEntryValue.get("$contains");
        List<String> regex = matchEntryValue.get("$regex");
        if (begins != null || ends != null || contains != null) {
            buildPartialMatchCondition(conditions,
                    params, field.name(), begins, ends, contains, regex, suffix);
        }
    }

    private static void getnewRegexPattern(Map<String, Object> params, List<String> conditions, String suffix, String field, Map<String, List<String>> matchEntryValue) {
        List<String> begins = matchEntryValue.get("$begins");
        List<String> ends = matchEntryValue.get("$ends");
        List<String> contains = matchEntryValue.get("$contains");
        List<String> regex = matchEntryValue.get("$regex");
        if (begins != null || ends != null || contains != null) {
            buildPartialMatchCondition(conditions,
                    params, field, begins, ends, contains, regex, suffix);
        }
    }

    public static void buildPartialMatchCondition(List<String> conditions,
                                                  Map<String, Object> params,
                                                  String field,
                                                  List<String> begins,
                                                  List<String> ends,
                                                  List<String> contains,
                                                  List<String> regex,
                                                  String suffix) {
        if (begins != null) {
            List<String> newBegins = addPartialMatchOperator(begins, true, false, false);
            String beginsConditionStr = field + " LIKE ANY( ARRAY [  :" + field + "_begins_" + suffix + " ] ) ";
            beginsConditionStr = MATCH_ARRAY_COLUMNS.contains(field) ? getArrayConditionStr(field, suffix, "_begins_")
                    : beginsConditionStr;
            params.put(field + "_begins_" + suffix, newBegins);
            conditions.add(beginsConditionStr);
        }
        if (ends != null) {
            List<String> newEnds = addPartialMatchOperator(ends, false, true, false);
            String endsConditionStr = field + " LIKE ANY( ARRAY [  :" + field + "_ends_" + suffix + " ] ) ";
            endsConditionStr = MATCH_ARRAY_COLUMNS.contains(field) ? getArrayConditionStr(field, suffix, "_ends_")
                    : endsConditionStr;
            params.put(field + "_ends_" + suffix, newEnds);
            conditions.add(endsConditionStr);
        }
        if (contains != null) {
            List<String> newContains = addPartialMatchOperator(contains, false, false, true);
            String containsConditionStr = field + " LIKE ANY( ARRAY [  :" + field + "_contains_" + suffix + " ] ) ";
            containsConditionStr = MATCH_ARRAY_COLUMNS.contains(field) ?
                    getArrayConditionStr(field, suffix, "_contains_") : containsConditionStr;
            params.put(field + "_contains_" + suffix, newContains);
            conditions.add(containsConditionStr);
        }
        if (regex != null) {
            String containsConditionStr = field + " ~* ANY( ARRAY [  :" + field + "_regex_" + suffix + " ] ) ";
            containsConditionStr = MATCH_ARRAY_COLUMNS.contains(field) ?
                    getArrayConditionStr(field, suffix, "_regex_") : containsConditionStr;
            params.put(field + "_contains_" + suffix, regex);
            conditions.add(containsConditionStr);
        }
    }

    private static String getArrayConditionStr(String field, String suffix, String prefix) {
        return "exists (select 1 from unnest (" + field + ") as k where k LIKE ANY( ARRAY [  :" +
                field + prefix + suffix + " ] ))";
    }

    public static List<String> addPartialMatchOperator(List<String> regexPatternList,
                                                       boolean begins,
                                                       boolean ends,
                                                       boolean contains) {
        List<String> finalPatternList = new ArrayList<>();
        regexPatternList.forEach(regexPattern -> {
            StringBuilder builder = new StringBuilder(regexPattern);
            if (begins) {
                finalPatternList.add(builder.append('%').toString());
            } else if (ends) {
                finalPatternList.add(builder.insert(0, '%').toString());
            } else if (contains) {
                finalPatternList.add(builder.insert(0, '%').append('%').toString());
            }
        });
        return finalPatternList;
    }

    public static void checkFilterConfigValidity(ScmPrFilter filter, VelocityConfigDTO velocityConfigDTO) throws BadRequestException {
        if(velocityConfigDTO == null) {
            throw new BadRequestException("Error computing DORA metric, please provide velocity config..");
        }
        if(velocityConfigDTO.getScmConfig() == null) {
            throw new BadRequestException("Error computing DORA metric, please configure SCM config for velocity id : " + velocityConfigDTO.getId());
        }
        if (filter.getCalculation().equals(ScmPrFilter.CALCULATION.mean_time_to_recover)) {
            if (velocityConfigDTO.getScmConfig().getDefect() == null || (velocityConfigDTO.getScmConfig().getDefect()
                    != null && velocityConfigDTO.getScmConfig().getDefect().size() == 0))
                throw new BadRequestException("Error computing mean time to recover DORA metric, please provide Defect configuration for velocity profile id : " + velocityConfigDTO.getId());
        }
        if (filter.getCalculation().equals(ScmPrFilter.CALCULATION.lead_time_for_changes)) {
            if (velocityConfigDTO.getScmConfig().getRelease() == null || (velocityConfigDTO.getScmConfig().getRelease()
                    != null && velocityConfigDTO.getScmConfig().getRelease().size() == 0))
                throw new BadRequestException("Error computing Lead Time For Changes DORA metric, please provide Release configuration for velocity profile id : " + velocityConfigDTO.getId());
        }
        if (filter.getCalculation().equals(ScmPrFilter.CALCULATION.failure_rate)) {
            if (velocityConfigDTO.getScmConfig().getHotfix() == null || (velocityConfigDTO.getScmConfig().getHotfix()
                    != null && velocityConfigDTO.getScmConfig().getHotfix().size() == 0))
                throw new BadRequestException("Error computing Failure Rate DORA metric, please provide hotfix and release configurations for velocity profile id : " + velocityConfigDTO.getId());
            if (velocityConfigDTO.getScmConfig().getRelease() == null || (velocityConfigDTO.getScmConfig().getRelease()
                    != null && velocityConfigDTO.getScmConfig().getRelease().size() == 0))
                throw new BadRequestException("Error computing Failure Rate DORA metric, please provide hotfix and release configurations for velocity profile id : " + velocityConfigDTO.getId());
        }
        if (filter.getCalculation().equals(ScmPrFilter.CALCULATION.deployment_frequency)) {
            if (velocityConfigDTO.getScmConfig().getDeployment() == null || (velocityConfigDTO.getScmConfig().getDeployment()
                    != null && velocityConfigDTO.getScmConfig().getDeployment().size() == 0))
                throw new BadRequestException("Error computing Deployment Frequency DORA metric, please provide deployment configurations..");
        }
    }

    public static void checkFilterNewConfigValidity(ScmPrFilter filter, VelocityConfigDTO velocityConfigDTO) throws BadRequestException {
        if(velocityConfigDTO == null) {
            throw new BadRequestException("Error computing DORA metric, please provide velocity config..");
        }

        if((ScmPrFilter.CALCULATION.deployment_frequency).equals(filter.getCalculation())
                && (velocityConfigDTO.getDeploymentFrequency()== null
                || velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters() == null
                || velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency() == null)) {
            throw new BadRequestException("Error computing DORA metric, please configure Deployment Frequency for velocity id : " + velocityConfigDTO.getId());
        }

        if((ScmPrFilter.CALCULATION.failure_rate).equals(filter.getCalculation())
         && (velocityConfigDTO.getChangeFailureRate()== null
                || velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters() == null
                || velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment() == null
                || (!velocityConfigDTO.getChangeFailureRate().getIsAbsoulte()
                        && velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment() == null))) {
            throw new BadRequestException("Error computing DORA metric, please configure Change Failure Rate for velocity id : " + velocityConfigDTO.getId());
        }

        if((ScmPrFilter.CALCULATION.lead_time_for_changes).equals(filter.getCalculation())
                && velocityConfigDTO.getLeadTimeForChanges()== null ) {
            throw new BadRequestException("Error computing DORA metric, please configure Lead Time For Changes for velocity id : " + velocityConfigDTO.getId());
        }
        if((ScmPrFilter.CALCULATION.mean_time_to_recover).equals(filter.getCalculation())
                && velocityConfigDTO.getMeanTimeToRestore()== null) {
            throw new BadRequestException("Error computing DORA metric, please configure Mean Time To Restore for velocity id : " + velocityConfigDTO.getId());
        }

    }

}

