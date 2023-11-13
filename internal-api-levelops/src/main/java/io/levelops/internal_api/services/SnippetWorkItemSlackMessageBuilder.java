package io.levelops.internal_api.services;

import com.google.common.base.Splitter;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.internal_api.services.handlers.LevelOpsLinkUtils;
import io.levelops.notification.services.WorkItemSlackMessageUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
public class SnippetWorkItemSlackMessageBuilder {
    private static final int MODAL_VIEW_TITLE_MAX_LENGTH = 22;
    private static final int SNIPPET_INLINE_PREVIEW_MAX_LENGTH = 555;
    private static final String REQUESTOR_BLOCK_FORMAT = "{\"block_id\": \"NOOP_REQUESTOR\",\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"From %s,\"}}";
    private static final String MESSAGE_BLOCK_FORMAT = "{\"block_id\": \"NOOP_MESSAGE\",\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"%s\"}}";

    private static final String SNIPPET_TITLE_BLOCK_FORMAT = "{\"block_id\": \"NOOP_TITLE\",\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"%s\"}}";
    private static final String SNIPPET_TITLE_LEVELOPS_WI_URL_BLOCK_FORMAT = "{\"block_id\": \"NOOP_TITLELEVELOPSWIURL\",\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"<%s|%s>\"}}";

    private static final String URL_BLOCK_FORMAT = "{\"block_id\": \"NOOP_URL\",\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"<%s|%s>\"}}";
    private static final String SNIPPET_INLINE_PREVIEW_BLOCK_FORMAT = "{\"block_id\": \"NOOP_SNIPPETPREVIEW\",\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"```%s```\"}}";
    private static final String SNIPPET_INLINE_PREVIEW_RICH_TEXT_BLOCK_FORMAT = "{\"block_id\": \"NOOP_SNIPPETPREVIEWRT\",\"type\": \"rich_text\",\"elements\": [{\"type\": \"rich_text_preformatted\",\"elements\": [{\"type\": \"text\",\"text\": \"%s\"}]}]}";
    private static final String VIEW_ENTIRE_SNIPPET_BUTTON_FORMAT = "{\"block_id\": \"workitem_snippet\",\"type\": \"actions\",\"elements\": [{\"type\": \"button\",\"text\": {\"type\": \"plain_text\",\"text\": \"View Entire Snippet\",\"emoji\": true},\"style\": \"primary\",\"value\": \"%s\",\"action_id\": \"view_wi_text_attachment\"}]}";

    private static final String CHANGE_TICKET_STATUS_OPTION = "{ \"text\": { \"type\": \"plain_text\", \"text\": \"%s\", \"emoji\": true }, \"value\": \"%s\" }";
    private static final String CHANGE_TICKET_STATUS_COMPLETE = "{\"block_id\": \"change_status_snippet_ticket\", \"type\": \"section\", \"text\": { \"type\": \"mrkdwn\", \"text\": \"`Approval Decision:`\" }, \"accessory\": {\"action_id\": \"change_status_snippet_ticket\",\"type\": \"static_select\", \"placeholder\": { \"type\": \"plain_text\", \"text\": \"Select an item\", \"emoji\": true }, \"options\": [ %s ] } }";

    private static final String FORWARD_TICKET = "{\"block_id\": \"forward_snippet_ticket\",\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"`Forward To:`\"},\"accessory\": {\"action_id\": \"forward_snippet_ticket\",\"type\": \"multi_conversations_select\",\"placeholder\": {\"type\": \"plain_text\",\"text\": \"Select conversations\",\"emoji\": true}}}";

    private static final String ASSIGN_TICKET = "{\"block_id\":\"assign_snippet_ticket\",\"type\":\"section\",\"text\":{\"type\":\"mrkdwn\",\"text\":\"`Assign To :`\"},\"accessory\":{\"action_id\":\"assignee_select_snippet_ticket\",\"type\":\"users_select\",\"placeholder\":{\"type\":\"plain_text\",\"text\":\"Select assignee\",\"emoji\":true}}}";

    private static final String NOOP_BLOCK_ID_FORMAT = "NOOP_%s";
    private static final String DIVIDER_BLOCK = "{\"block_id\": \"%s\",\"type\": \"divider\"}";
    private static final String TITLE_BLOCK_FORMAT = "{\"block_id\": \"%s\",\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"*Snippet:* `%s`\"}}";
    private static final String ATTCHMENT_TEXT_BLOCK_FORMAT = "{\"block_id\": \"NOOP_%s\",\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"```%s```\"}}";
    private static final int MAX_DATA_BLOCKS_COUNT = 45;
    private static final String VIEW_WORKITEM_ATTACHMENT_TEXT_MODAL = "{\"title\": {\"type\": \"plain_text\",\"text\": \"%s\"},\"blocks\": [%s],\"type\": \"modal\",\"callback_id\": \"view_wi_text_attachment\",\"private_metadata\": \"%s\"}";

    private final RandomStringGenerator randomStringGenerator;

    @Autowired
    public SnippetWorkItemSlackMessageBuilder(RandomStringGenerator randomStringGenerator) {
        this.randomStringGenerator = randomStringGenerator;
    }

    private String getHostName(final String uri) {
        if(StringUtils.isBlank(uri)) {
            return uri;
        }
        try {
            URL url = new URL(uri);
            return url.getHost();
        } catch (MalformedURLException e) {
            log.warn("Error extraction hostname from url {}", uri, e);
            return null;
        }
    }

    private String buildRequestorBlock(final String requestor){
        return String.format(REQUESTOR_BLOCK_FORMAT, requestor);
    }

    private String buildTitleLevelOpsWIUrlBlock(final String appBaseUrl, String vanityId, String title) {
        String wiLink = LevelOpsLinkUtils.buildWorkItemLink(appBaseUrl, vanityId);
        if(StringUtils.isBlank(wiLink)) {
            return String.format(SNIPPET_TITLE_BLOCK_FORMAT, title);
        } else {
            return String.format(SNIPPET_TITLE_LEVELOPS_WI_URL_BLOCK_FORMAT, wiLink, title);
        }
    }

    private String buildMessageBlock(final String message){
        if (StringUtils.isBlank(message)) {
            return null;
        }
        return String.format(MESSAGE_BLOCK_FORMAT, message);
    }

    private String buildUrlBlock(final WorkItem workItem){
        return String.format(URL_BLOCK_FORMAT, workItem.getReason(), StringUtils.abbreviate(workItem.getReason(), 80));
    }

    private String buildSnippetInlinePreviewBlock(final WorkItem workItem, final Map<UUID, String> uploadIdTextAttachmentsMap) {
        String uploadId = CollectionUtils.emptyIfNull(workItem.getAttachments()).stream()
                .limit(1).map(WorkItem.Attachment::getUploadId).collect(Collectors.joining());
        if(StringUtils.isBlank(uploadId)) {
            return null;
        }
        String snippetText = uploadIdTextAttachmentsMap.getOrDefault(UUID.fromString(uploadId), null);
        if(StringUtils.isBlank(snippetText)) {
            return null;
        }

        final MutableInt totalCount = new MutableInt();
        List<String> snippetInlinePreviewLines = new ArrayList<>();
        snippetText.lines()
                .filter(Objects::nonNull)
                .limit(10)
                .forEach(s -> {
                    if(totalCount.getValue() >= SNIPPET_INLINE_PREVIEW_MAX_LENGTH) {
                        return;
                    }
                    if(totalCount.getValue() + s.length() <= SNIPPET_INLINE_PREVIEW_MAX_LENGTH) {
                        snippetInlinePreviewLines.add(s);
                        totalCount.add(s.length());
                    } else {
                        int capacity = SNIPPET_INLINE_PREVIEW_MAX_LENGTH - totalCount.getValue();
                        String abbrString = StringUtils.abbreviate(s, capacity);
                        snippetInlinePreviewLines.add(abbrString);
                        totalCount.add(abbrString.length());
                    }
                });

        String snippetInlinePreview = String.join("\n", snippetInlinePreviewLines);
        log.debug("snippetInlinePreview {}", snippetInlinePreview);
        return String.format(SNIPPET_INLINE_PREVIEW_BLOCK_FORMAT, StringEscapeUtils.escapeJson(snippetInlinePreview));
    }

    private String buildViewEntireSnippetButton(final String company, final WorkItem workItem) {
        if(CollectionUtils.isEmpty(workItem.getAttachments())) {
            return null;
        }
        WorkItem.Attachment attachment = workItem.getAttachments().get(0);
        String viewEntireSnippetButtonValue = WorkItemSlackMessageUtils.buildWorkItemAttachmentSlackCallbackId(company, workItem.getId(), attachment.getUploadId());
        String viewEntireSnippetButtonBlock = String.format(VIEW_ENTIRE_SNIPPET_BUTTON_FORMAT, StringEscapeUtils.escapeJson(viewEntireSnippetButtonValue));
        log.debug("viewEntireSnippetButtonBlock = {}", viewEntireSnippetButtonBlock);
        return viewEntireSnippetButtonBlock;
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

    private String getNoOpBlockId() {
        return String.format(NOOP_BLOCK_ID_FORMAT, randomStringGenerator.randomString());
    }
    private String buildDividerBlock() {
        return String.format(DIVIDER_BLOCK, getNoOpBlockId());
    }

    private List<String> buildTitleBlocks(String fileName) {
        return List.of(
                buildDividerBlock(),
                String.format(TITLE_BLOCK_FORMAT, getNoOpBlockId(), StringEscapeUtils.escapeJson(fileName)));
    }

    private String buildAttachmentTextModalMessage(final String company, final String workItemId, final String uploadId, final String fileName, final String text) {
        List<String> modalBlocks = new ArrayList<>();
        if (StringUtils.length(fileName) >= MODAL_VIEW_TITLE_MAX_LENGTH) {
            modalBlocks.addAll(buildTitleBlocks(fileName));
        }
        List<String> splitTexts = Splitter.fixedLength(2900).splitToList(text);
        for(String splitText : splitTexts) {
            if(modalBlocks.size() < MAX_DATA_BLOCKS_COUNT) {
                modalBlocks.add(String.format(ATTCHMENT_TEXT_BLOCK_FORMAT, randomStringGenerator.randomString(), StringEscapeUtils.escapeJson(splitText)));
            }
        }

        String modalBlocksMessage = modalBlocks.stream().filter(Objects::nonNull).collect(Collectors.joining(","));
        log.debug("modalBlocksMessage = {}", modalBlocksMessage);

        String viewWorkItemAttachmentTextId = WorkItemSlackMessageUtils.buildWorkItemAttachmentSlackCallbackId(company, workItemId, uploadId);
        log.debug("viewWorkItemAttachmentTextId = {}", viewWorkItemAttachmentTextId);

        String modalMessage = String.format(VIEW_WORKITEM_ATTACHMENT_TEXT_MODAL, StringEscapeUtils.escapeJson(StringUtils.abbreviate(fileName, MODAL_VIEW_TITLE_MAX_LENGTH)), modalBlocksMessage, viewWorkItemAttachmentTextId);
        log.debug("modalMessage = {}", modalMessage);

        return modalMessage;
    }

    private List<ImmutablePair<UUID,String>> buildAttachmentTextModalMessages(final String company, final WorkItem workItem, final Map<UUID, String> uploadIdTextAttachmentsMap) {
        List<ImmutablePair<UUID,String>> uploadIdModalMessageList = CollectionUtils.emptyIfNull(workItem.getAttachments()).stream()
                .filter(x -> StringUtils.isNotBlank(x.getUploadId()))
                .map(a -> {
                    UUID uploadId = UUID.fromString(a.getUploadId());
                    String attachmentText = uploadIdTextAttachmentsMap.getOrDefault(uploadId, null);
                    if(StringUtils.isBlank(attachmentText)) {
                        return null;
                    }
                    String attachmentTextModalMessage = buildAttachmentTextModalMessage(company, workItem.getId(),uploadId.toString(), a.getFileName(),attachmentText);
                    log.debug("attachmentTextModalMessage = {}", attachmentTextModalMessage);
                    return ImmutablePair.of(uploadId, attachmentTextModalMessage);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return uploadIdModalMessageList;
    }

    public WorkItemSlackMessageBuilder.WorkItemSlackMessages buildInteractiveMessage(final String company, final String appBaseUrl, final WorkItem workItem, final List<String> states, final Map<UUID, String> uploadIdTextAttachmentsMap, final String requestor, final String customMessage) {
        List<String> blocks = new ArrayList<>();
        //Add Divider
        blocks.add(buildDividerBlock());
        //Add Requestor
        blocks.add(buildRequestorBlock(requestor));
        //Add Title & LevelOps WI Url
        blocks.add(buildTitleLevelOpsWIUrlBlock(appBaseUrl, workItem.getVanityId(), workItem.getTitle()));
        //Add Message
        blocks.add(buildMessageBlock(customMessage));
        //Add Url
        blocks.add(buildUrlBlock(workItem));
        //Add Snippet Inline Preview
        blocks.add(buildSnippetInlinePreviewBlock(workItem, uploadIdTextAttachmentsMap));
        //Add View Snippet Button
        blocks.add(buildViewEntireSnippetButton(company, workItem));

        //Add Change Ticket Status Action
        blocks.add(buildChangeTicketStatusBlock(states));

        //Add Forward Ticket Action
        blocks.add(FORWARD_TICKET);

        //Add Assign Ticket Action
        blocks.add(ASSIGN_TICKET);

        String message = "[" + blocks.stream().filter(Objects::nonNull).collect(Collectors.joining(",")) + "]";

        List<ImmutablePair<UUID,String>> modalMessages = buildAttachmentTextModalMessages(company, workItem, uploadIdTextAttachmentsMap);

        WorkItemSlackMessageBuilder.WorkItemSlackMessages.WorkItemSlackMessagesBuilder bldr = WorkItemSlackMessageBuilder.WorkItemSlackMessages.builder().message(message);
        if(CollectionUtils.isNotEmpty(modalMessages)) {
            bldr.modalMessages(modalMessages);
        }
        return bldr.build();
    }
}
