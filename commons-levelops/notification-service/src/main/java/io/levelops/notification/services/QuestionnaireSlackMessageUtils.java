package io.levelops.notification.services;

import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
public class QuestionnaireSlackMessageUtils {
    private static final String DELIMETER = "~@@@_";
    private static final String QUESTIONNAIRE_SLACK_CALLBACK_ID_FORMAT =  "edit_questionnaire" + DELIMETER +  "%s"  + DELIMETER + "%s";
    private static final Pattern QUESTIONNAIRE_SLACK_CALLBACK_ID_PATTERN = Pattern.compile("^edit_questionnaire" + DELIMETER + "(?<company>.*)" + DELIMETER + "(?<qid>.*)$");

    public static final String buildQuestionnaireSlackCallbackId(final String company, final String questionnaireId) {
        Validate.notBlank(company, "company cannot be null or empty");
        Validate.notBlank(questionnaireId, "questionnaireId cannot be null or empty");
        return String.format(QUESTIONNAIRE_SLACK_CALLBACK_ID_FORMAT, company, questionnaireId);
    }

    public static Optional<ImmutablePair<String, String>> parseQuestionnaireSlackCallbackId(String callbackId) {
        Validate.notBlank(callbackId, "callbackId cannot be null or empty");
        Matcher matcher = QUESTIONNAIRE_SLACK_CALLBACK_ID_PATTERN.matcher(callbackId);
        if(!matcher.matches()) {
            log.info("Pattern does not match for questionnaire slack callback id {}", callbackId);
            return Optional.empty();
        }
        String company = matcher.group("company");
        String questionnaireId = matcher.group("qid");
        log.info("callbackId {}, company {}, questionnaireId {}", callbackId, company, questionnaireId);
        return Optional.ofNullable(ImmutablePair.of(company, questionnaireId));
    }

}
