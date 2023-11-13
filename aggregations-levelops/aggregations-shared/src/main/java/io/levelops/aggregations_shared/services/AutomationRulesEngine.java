package io.levelops.aggregations_shared.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.automation_rules.AutomationRule;
import io.levelops.commons.databases.models.database.automation_rules.AutomationRuleHit;
import io.levelops.commons.databases.models.database.automation_rules.Criterea;
import io.levelops.commons.databases.models.database.automation_rules.ObjectType;
import io.levelops.commons.databases.services.automation_rules.AutomationRuleHitsDatabaseService;
import io.levelops.commons.databases.services.automation_rules.AutomationRulesDatabaseService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.RegexResult;
import io.levelops.commons.regex.RegexService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
public class AutomationRulesEngine {
    private final AutomationRulesDatabaseService automationRulesDatabaseService;
    private final AutomationRuleHitsDatabaseService automationRuleHitsDatabaseService;
    private final RegexService regexService;
    private final ObjectMapper mapper;

    @Autowired
    public AutomationRulesEngine(AutomationRulesDatabaseService automationRulesDatabaseService, AutomationRuleHitsDatabaseService automationRuleHitsDatabaseService, RegexService regexService, ObjectMapper mapper) {
        this.automationRulesDatabaseService = automationRulesDatabaseService;
        this.automationRuleHitsDatabaseService = automationRuleHitsDatabaseService;
        this.regexService = regexService;
        this.mapper = mapper;
    }

    private List<AutomationRule> getRulesFromDb(String company, ObjectType objectType) {
        List<AutomationRule> automationRules = new ArrayList<>();
        boolean keepFetching = true;
        int pageNumber = 0;
        while (keepFetching) {
            try {
                DbListResponse<AutomationRule> dbListResponse = automationRulesDatabaseService.listByFilter(company, pageNumber, 100, null, List.of(objectType), null);
                if(CollectionUtils.isNotEmpty(dbListResponse.getRecords())) {
                    automationRules.addAll(dbListResponse.getRecords());
                }
                keepFetching = (automationRules.size() < dbListResponse.getTotalCount());
                pageNumber++;
            } catch (SQLException e) {
                log.error("Error fetching automation rules from db!! company {}, objectType {}", company, objectType, e);
                keepFetching = false;
            }
        }
        return automationRules;
    }

    private String getTextBlob(Criterea criterea, Map<String, Object> data, String fullText) {
        String fieldName = criterea.getFieldName();
        if (StringUtils.isBlank(fieldName)) {
            return fullText;
        }
        if(!data.containsKey(fieldName)) {
            return null;
        }
        Object value = data.get(fieldName);
        if(value instanceof String) {
            return (String) value;
        } else {
            try {
                return mapper.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                log.error("Cannot generate full text blob!", e);
                return null;
            }
        }
    }

    private List<RegexResult> runRuleCriterias(String company, ObjectType objectType, String objectId, Map<String, Object> data, AutomationRule automationRule, String fullText) {
        List<RegexResult> criteriaMatches = new ArrayList<>();
        for(Criterea criterea : automationRule.getCritereas()) {
            if (CollectionUtils.isEmpty(criterea.getRegexes())) {
                continue;
            }
            String textBlob = getTextBlob(criterea, data, fullText);
            if (StringUtils.isEmpty(textBlob)) {
                continue;
            }
            log.debug("company {}, objectType {}, objectId {}, automationRule Id {}, criterea {} textBlob {}", company, objectType, objectId, automationRule.getId(), criterea, textBlob);

            RegexResult critereaMatch = null;
            try {
                critereaMatch = regexService.findRegexHits(new HashSet<>(criterea.getRegexes()), textBlob);
            } catch (Exception e) {
                log.error("automationRule Id Exception!", e);
            }

            if (critereaMatch == null || critereaMatch.getTotalMatches() == null || critereaMatch.getTotalMatches() < 1) {
                log.debug("company {}, objectType {}, objectId {}, automationRule Id {}, criterea {} no match", company, objectType, objectId, automationRule.getId(), criterea);
                return null;
            }
            log.debug("company {}, objectType {}, objectId {}, automationRule Id {}, criterea {} matches", company, objectType, objectId, automationRule.getId(), criterea);
            log.debug("critereaMatch {}", critereaMatch);
            criteriaMatches.add(critereaMatch);
        }
        return criteriaMatches;
    }

    public void scanWithRules(String company, ObjectType objectType, String objectId, Map<String, Object> data) {
        List<AutomationRule> automationRules = getRulesFromDb(company, objectType);
        String fullText = null;
        try {
            fullText = mapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            log.error("Cannot generate full text blob!", e);
            return;
        }
        for(AutomationRule automationRule : automationRules) {
            log.debug("company {}, objectType {}, objectId {}, automationRule Id {}", company, objectType, objectId, automationRule.getId());
            if(CollectionUtils.isEmpty(automationRule.getCritereas())) {
                log.debug("company {}, objectType {}, objectId {}, automationRule Id {} has no criterias", company, objectType, objectId, automationRule.getId());
                continue;
            }
            List<RegexResult> criteriaMatches = runRuleCriterias(company, objectType, objectId, data, automationRule, fullText);
            if(CollectionUtils.isEmpty(criteriaMatches)) {
                log.debug("company {}, objectType {}, objectId {}, automationRule Id {} not all criterias match", company, objectType, objectId, automationRule.getId());
                continue;
            }
            log.debug("company {}, objectType {}, objectId {}, automationRule Id {} all criterias match", company, objectType, objectId, automationRule.getId());

            AutomationRuleHit automationRuleHit = null;
            try {
                log.debug("Inserting match results...");
                automationRuleHit = AutomationRuleHit.builder()
                        .objectId(objectId).objectType(objectType)
                        .ruleId(automationRule.getId())
                        .count(criteriaMatches.get(0).getTotalMatches())
                        .hitContent(criteriaMatches.get(0).getFirstHitContext())
                        .context(Map.of(
                                "line", criteriaMatches.get(0).getFirstHitLineNumber()
                        ))
                        .build();
                log.info("company {}, objectType {}, objectId {}, automationRule Id {} automationRuleHit {}", company, objectType, objectId, automationRule.getId(), automationRuleHit.getId());
                automationRuleHitsDatabaseService.upsert(company, automationRuleHit);
            } catch (SQLException e) {
                log.error("Unable to persist automation rule hit: {}", automationRuleHit, e);
            }
        }
    }
}

