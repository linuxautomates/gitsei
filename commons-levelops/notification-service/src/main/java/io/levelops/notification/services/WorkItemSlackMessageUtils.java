package io.levelops.notification.services;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class WorkItemSlackMessageUtils {
    private static final String DELIMETER = "~@@@_";
    private static final String WI_VIEW_ATTACHMENT_SLACK_CALLBACK_ID_FORMAT =  "view_wi_text_attachment" + DELIMETER +  "%s"  + DELIMETER + "%s"  + DELIMETER + "%s";
    private static final Pattern WI_VIEW_ATTACHMENT_SLACK_CALLBACK_ID_PATTERN = Pattern.compile("^view_wi_text_attachment" + DELIMETER + "(?<company>.*)" + DELIMETER + "(?<wiid>.*)" + DELIMETER + "(?<uploadid>.*)$");

    public static final String buildWorkItemAttachmentSlackCallbackId(final String company, final String workItemId, final String uploadId) {
        Validate.notBlank(company, "company cannot be null or empty");
        Validate.notBlank(workItemId, "workItemId cannot be null or empty");
        Validate.notBlank(uploadId, "uploadId cannot be null or empty");
        return String.format(WI_VIEW_ATTACHMENT_SLACK_CALLBACK_ID_FORMAT, company, workItemId, uploadId);
    }

    public static Optional<WorkItemAttchmentMetadata> parseQuestionnaireSlackCallbackId(String callbackId) {
        Validate.notBlank(callbackId, "callbackId cannot be null or empty");
        Matcher matcher = WI_VIEW_ATTACHMENT_SLACK_CALLBACK_ID_PATTERN.matcher(callbackId);
        if(!matcher.matches()) {
            log.info("Pattern does not match for questionnaire slack callback id {}", callbackId);
            return Optional.empty();
        }
        String company = matcher.group("company");
        String workItemId = matcher.group("wiid");
        String uploadId = matcher.group("uploadid");
        log.info("callbackId {}, company {}, workItemId {}, uploadId {}", callbackId, company, workItemId, uploadId);
        return Optional.ofNullable(WorkItemAttchmentMetadata.builder().company(company).workItemId(workItemId).uploadId(uploadId).build());
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = WorkItemAttchmentMetadata.WorkItemAttchmentMetadataBuilder.class)
    public static class WorkItemAttchmentMetadata {
        private final String company;
        private final String workItemId;
        private final String uploadId;
    }
}
