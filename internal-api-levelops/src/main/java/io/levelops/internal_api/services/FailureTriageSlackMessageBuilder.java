package io.levelops.internal_api.services;

import io.levelops.commons.databases.models.database.cicd.FailureTriageSlackMessage;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.internal_api.services.WorkItemService.WORKITEM_LINK_FORMAT;

@Log4j2
@Service
public class FailureTriageSlackMessageBuilder {
    private static final Integer MAX_DATA_BLOCKS_COUNT = 45;
    public static final String TRIAGE_RULE_LINK_FORMAT = "%s/#/admin/triage/rules/edit?rule=%s";
    public static final String TRIAGE_RESULT_LINK_FORMAT = "%s/#/admin/triage/view?id=%s";
    private static final String NOTIFICATION_HEADER = "{\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \":warning: A Jenkins Job has hit some triage rules.\"}}";
    private static final String SECTION_PLAIN_TEXT = "{\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"*%s:* %s\"}}";
    private static final String FORWARD_TICKET = "{\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"Forward this ticket\"},\"accessory\": {\"type\": \"multi_conversations_select\",\"placeholder\": {\"type\": \"plain_text\",\"text\": \"Select conversations\",\"emoji\": true}}}";
    private static final String ASSIGN_TICKET = "{\"block_id\":\"assign_snippet_ticket\",\"type\":\"section\",\"text\":{\"type\":\"mrkdwn\",\"text\":\"`Assign To :`\"},\"accessory\":{\"action_id\":\"assignee_select_snippet_ticket\",\"type\":\"users_select\",\"placeholder\":{\"type\":\"plain_text\",\"text\":\"Select assignee\",\"emoji\":true}}}";

    private static final String JOB_DETAILS = "{ \"type\": \"section\", \"text\": { \"type\": \"mrkdwn\", \"text\": \"<%s|%s #%d>    <%s|View Jenkins Logs>\" } }";
    private static final String RULE_HIT_WITH_STAGE = "{\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"*Stage:* <%s|%s>    *Rule:* <%s|%s>    *Matches:* `%d`\"}}";
    private static final String RULE_HIT_WITHOUT_STAGE = "{\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"*Rule:* <%s|%s>    *Matches:* `%d`\"}}";
    private static final String CHANGE_TICKET_STATUS_OPTION = "{ \"text\": { \"type\": \"plain_text\", \"text\": \"%s\", \"emoji\": true }, \"value\": \"%s\" }";
    private static final String CHANGE_TICKET_STATUS_COMPLETE = "{ \"type\": \"section\", \"text\": { \"type\": \"mrkdwn\", \"text\": \"Change ticket status\" }, \"accessory\": { \"type\": \"static_select\", \"placeholder\": { \"type\": \"plain_text\", \"text\": \"Select an item\", \"emoji\": true }, \"options\": [ %s ] } }";

    private String buildJobDetailsBlock(String jobName, Long jobRunNumber, String jenkinsUrl, String appBaseUrl, UUID jobRunId){
        String jobRunTriageResultLink = (jobRunId != null) ? String.format(TRIAGE_RESULT_LINK_FORMAT, appBaseUrl, jobRunId.toString()) : null;
        String jobDetailsSection = String.format(JOB_DETAILS, jobRunTriageResultLink, jobName, jobRunNumber, jenkinsUrl);
        return jobDetailsSection;
    }

    private String buildRuleHitBlock (String stageName, String stageJenkinsUrl, String appBaseUrl, UUID ruleId, String ruleName,Integer matchesCount) {
        String triageRuleLink = (ruleId != null) ? String.format(TRIAGE_RULE_LINK_FORMAT, appBaseUrl, ruleId.toString()) : null;
        String ruleHitsSection = null;
        if(StringUtils.isBlank(stageName)) {
            ruleHitsSection = String.format(RULE_HIT_WITHOUT_STAGE, triageRuleLink, ruleName, matchesCount);
        } else {
            ruleHitsSection = String.format(RULE_HIT_WITH_STAGE, stageJenkinsUrl, stageName, triageRuleLink, ruleName, matchesCount);
        }
        return ruleHitsSection;
    }

    private String buildDividerBlock() {
        return "{\"type\": \"divider\"}";
    }
    private String buildSectionPlainTextBlock(String title, String value){
        if((title == null) || (value == null)) {
            return null;
        }
        return String.format(SECTION_PLAIN_TEXT, title, value);
    }
    private String buildWorkItemLinkBlock(String appBaseUrl, String vanityId){
        String wiLink = String.format(WORKITEM_LINK_FORMAT, appBaseUrl, vanityId);
        return (StringUtils.isNotBlank(wiLink)) ? buildSectionPlainTextBlock("Work Item", "<"+ wiLink +"|" + vanityId + ">") : null;
    }

    private List<String> buildRuleHitBlocks(final String stageName, final String stageJenkinsUrl, final String appBaseUrl, FailureTriageSlackMessage.RuleHit ruleHit, List<ImmutablePair<String,String>> fileUploads) {
        if(ruleHit == null) {
            return Collections.emptyList();
        }
        List<String> ruleHitBlocks = new ArrayList<>();
        ruleHitBlocks.add(buildRuleHitBlock(stageName, stageJenkinsUrl, appBaseUrl, ruleHit.getRuleId(), ruleHit.getRule(), ruleHit.getMatchesCount()));

        //ruleHitBlocks.add(buildRuleHitSnippetBlock(ruleHit.getSnippet()));
        String snippetFileName = null;
        if(StringUtils.isNotBlank(stageName)) {
            snippetFileName = stageName + " " + ruleHit.getRule() + ".txt";
        } else {
            snippetFileName = ruleHit.getRule() + ".txt";
        }

        fileUploads.add(ImmutablePair.of(snippetFileName, ruleHit.getSnippet()));
        return ruleHitBlocks;
    }

    private String formattedStateName(String state) {
        if(StringUtils.isBlank(state)) {
            return state;
        }
        if("CLOSED".equals(state)) {
            return "Closed";
        } else if("OPEN".equals(state)) {
            return "Open";
        } else if("IN_REVIEW".equals(state)) {
            return "In Review";
        } else if("NEW".equals(state)) {
            return "New";
        } else {
            return state;
        }
    }
    private String buildChangeTicketStatusBlock(List<String> states) {
        List<String> options = states.stream().map(x -> String.format(CHANGE_TICKET_STATUS_OPTION, formattedStateName(x), x)).collect(Collectors.toList());
        String changeTicketStatusBlock = String.format(CHANGE_TICKET_STATUS_COMPLETE, String.join(",", options));
        log.debug("changeTicketStatusBlock = {}", changeTicketStatusBlock);
        return changeTicketStatusBlock;
    }

    public String buildInteractiveMessage(final String appBaseUrl, final String vanityId, FailureTriageSlackMessage failureTriageSlackMessage, List<String> states, List<ImmutablePair<String,String>> fileUploads) {
        if(failureTriageSlackMessage == null) {
            return "";
        }
        if(CollectionUtils.isEmpty(failureTriageSlackMessage.getJobRuns())) {
            return "";
        }
        List<String> blocks = new ArrayList<>();
        blocks.add(NOTIFICATION_HEADER);
        blocks.add(buildWorkItemLinkBlock(appBaseUrl, vanityId));
        for(FailureTriageSlackMessage.JobRun jobRun : failureTriageSlackMessage.getJobRuns()) {
            List<String> jobBlocks = new ArrayList<>();
            jobBlocks.add(buildDividerBlock());
            jobBlocks.add(buildJobDetailsBlock(jobRun.getJobName(), jobRun.getJobRunNumber(), jobRun.getJenkinsUrl(), appBaseUrl, jobRun.getJobRunId()));
            //jobBlocks.add(buildJenkinsInstanceNameBlock(jobRun.getJenkinsInstanceName()));
            if((blocks.size() + jobBlocks.size()) <= MAX_DATA_BLOCKS_COUNT ) {
                blocks.addAll(jobBlocks);
            }

            if(CollectionUtils.isNotEmpty(jobRun.getRuleHits())) {
                for(FailureTriageSlackMessage.RuleHit ruleHit : jobRun.getRuleHits()) {
                    List<ImmutablePair<String,String>> fileUploadsForRuleHits = new ArrayList<>();
                    List<String> blocksForRuleHits = buildRuleHitBlocks(null, null, appBaseUrl, ruleHit, fileUploadsForRuleHits);
                    if((blocks.size() + blocksForRuleHits.size()) <= MAX_DATA_BLOCKS_COUNT ) {
                        blocks.addAll(blocksForRuleHits);
                        fileUploads.addAll(fileUploadsForRuleHits);
                    }
                }
            }

            if(CollectionUtils.isNotEmpty(jobRun.getStages())) {
                for(FailureTriageSlackMessage.Stage stage : jobRun.getStages()) {
                    if(CollectionUtils.isNotEmpty(stage.getRuleHits())) {
                        for(FailureTriageSlackMessage.RuleHit ruleHit : stage.getRuleHits()) {
                            List<ImmutablePair<String,String>> fileUploadsForRuleHits = new ArrayList<>();
                            List<String> blocksForRuleHits = buildRuleHitBlocks(stage.getStageName(), stage.getStageJenkinsUrl(), appBaseUrl, ruleHit, fileUploadsForRuleHits);
                            if((blocks.size() + blocksForRuleHits.size()) <= MAX_DATA_BLOCKS_COUNT ) {
                                blocks.addAll(blocksForRuleHits);
                                fileUploads.addAll(fileUploadsForRuleHits);
                            }
                        }
                    }
                }
            }
        }

        if(!Boolean.TRUE.equals(failureTriageSlackMessage.getHideChangeTicketStatus())) {
            blocks.add(buildChangeTicketStatusBlock(states));
        }

        if(!Boolean.TRUE.equals(failureTriageSlackMessage.getHideChangeTicketAssignee())) {
            blocks.add(FORWARD_TICKET);
            blocks.add(ASSIGN_TICKET);
        }
        String interactiveMessage = "[" + blocks.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.joining(",")) + "]";
        log.debug("interactiveMessage = {}", interactiveMessage);
        return interactiveMessage;
    }
}
