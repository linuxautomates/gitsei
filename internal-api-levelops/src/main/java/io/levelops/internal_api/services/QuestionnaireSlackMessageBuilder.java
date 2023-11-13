package io.levelops.internal_api.services;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.Question;
import io.levelops.commons.databases.models.database.Section;
import io.levelops.commons.databases.models.database.questionnaire.Answer;
import io.levelops.commons.databases.models.database.questionnaire.QuestionnaireDTO;
import io.levelops.commons.databases.models.database.questionnaire.SectionResponse;
import io.levelops.notification.services.QuestionnaireSlackMessageUtils;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Log4j2
@Service
public class QuestionnaireSlackMessageBuilder {
    private static final int MODAL_VIEW_TITLE_MAX_LENGTH = 22;
    private static final int MAX_DATA_BLOCKS_COUNT = 45;
    private static final String NOOP_BLOCK_ID_FORMAT = "NOOP_%s";

    private static final String EDIT_ASSESSMENT_BUTTON_FORMAT = "{\"block_id\": \"questionnaire_notification\",\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"%s\"},\"accessory\": {\"type\": \"button\",\"text\": {\"type\": \"plain_text\",\"text\": \"Answer Inline\",\"emoji\": true},\"value\": \"%s\",\"action_id\": \"edit_questionnaire\"}}";

    private static final String WI_LINK_QUESTIONNAIRE_LINK_TEXT_FORMAT = "<%s|%s>    <%s|View assessment.>";
    private static final String QUESTIONNAIRE_LINK_TEXT_FORMAT = "<%s|View assessment.>";
    private static final String WI_LINK_QUESTIONNAIRE_LINK_FORMAT = "{\"block_id\": \"%s\",\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"%s\"}}";

    private static final String DEFAULT_TITLE = "Answer Inline";
    private static final String TITLE_BLOCK_FORMAT = "{\"block_id\": \"NOOP_%s\",\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"*Assessment:* `%s`\"}}";

    private static final String SECTION_NAME_FORMAT = "{\"block_id\": \"NOOP_%s\",\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"*Section:* `%s`\"}}";
    private static final String QUESTION_RADIO_BUTTON_OPTION = "{\"text\": {\"type\": \"plain_text\",\"text\": \"%s\",\"emoji\": true},\"value\": \"%s\"}";

    private static final String QUESTION_RADIO_BUTTON_WITH_INITIAL = "{\"block_id\": \"%s_%s\",\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"*%s*\"},\"accessory\": {\"type\": \"radio_buttons\",\"options\": [%s],\"initial_option\": %s,\"action_id\": \"custom_action_id\"}}";
    private static final String QUESTION_RADIO_BUTTON_WITHOUT_INITIAL = "{\"block_id\": \"%s_%s\",\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"*%s*\"},\"accessory\": {\"type\": \"radio_buttons\",\"options\": [%s],\"action_id\": \"custom_action_id\"}}";

    private static final String QUESTION_TEXT_WITH_INITIAL = "{\"block_id\": \"%s_%s\",\"type\": \"input\",\"element\": {\"type\": \"plain_text_input\",\"multiline\": true,\"action_id\": \"custom_action_id\",\"initial_value\": \"%s\"},\"label\": {\"type\": \"plain_text\",\"text\": \"%s\",\"emoji\": true}}";
    private static final String QUESTION_TEXT_WITHOUT_INITIAL = "{\"block_id\": \"%s_%s\",\"type\": \"input\",\"element\": {\"type\": \"plain_text_input\",\"multiline\": true,\"action_id\": \"custom_action_id\"},\"label\": {\"type\": \"plain_text\",\"text\": \"%s\",\"emoji\": true}}";

    private static final String QUESTION_CHECK_BOX_OPTION = "{\"text\": {\"type\": \"mrkdwn\",\"text\": \"%s\"},\"value\": \"%s\"}";
    private static final String QUESTION_CHECK_BOX_WITH_INITIAL = "{\"block_id\": \"%s_%s\",\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"*%s*\"},\"accessory\": {\"type\": \"checkboxes\",\"options\": [%s],\"initial_options\": [%s],\"action_id\": \"custom_action_id\"}}";
    private static final String QUESTION_CHECK_BOX_WITHOUT_INITIAL = "{\"block_id\": \"%s_%s\",\"type\": \"section\",\"text\": {\"type\": \"mrkdwn\",\"text\": \"*%s*\"},\"accessory\": {\"type\": \"checkboxes\",\"options\": [%s],\"action_id\": \"custom_action_id\"}}";

    private static final String EDIT_QUESTIONNAIRE_MODAL = "{\"title\": {\"type\": \"plain_text\",\"text\": \"%s\"},\"submit\": {\"type\": \"plain_text\",\"text\": \"Submit\"},\"blocks\": [%s],\"type\": \"modal\",\"callback_id\": \"%s\",\"private_metadata\": \"%s\"}";

    private static final String DIVIDER_BLOCK = "{\"block_id\": \"%s\",\"type\": \"divider\"}";

    private final RandomStringGenerator randomStringGenerator;

    @Autowired
    public QuestionnaireSlackMessageBuilder(RandomStringGenerator randomStringGenerator) {
        this.randomStringGenerator = randomStringGenerator;
    }

    private String getNoOpBlockId() {
        return String.format(NOOP_BLOCK_ID_FORMAT, randomStringGenerator.randomString());
    }

    private String buildDividerBlock() {
        return String.format(DIVIDER_BLOCK, getNoOpBlockId());
    }

    private String buildEditAssessmentButtonBlock(final String company, final String questionnaireId, final String templatedText) {
        String editAssessmentButtonValue = QuestionnaireSlackMessageUtils.buildQuestionnaireSlackCallbackId(company, questionnaireId);
        String editAssessmentButtonBlock = String.format(EDIT_ASSESSMENT_BUTTON_FORMAT, StringEscapeUtils.escapeJson(templatedText), StringEscapeUtils.escapeJson(editAssessmentButtonValue));
        log.debug("editAssessmentButtonBlock = {}", editAssessmentButtonBlock);
        return editAssessmentButtonBlock;
    }

    private String buildSectionName(String sectionId, String sectionName) {
        return String.format(SECTION_NAME_FORMAT, sectionId, StringEscapeUtils.escapeJson(sectionName));
    }

    private String buildRadioButton(String sectionId, Question question, Answer answer){
        List<String> optionBlocks = (CollectionUtils.isEmpty(question.getOptions())) ? Collections.emptyList() : question.getOptions().stream().map(o -> o.getResponse()).map(StringEscapeUtils::escapeJson).map(x -> String.format(QUESTION_RADIO_BUTTON_OPTION, x, x)).collect(Collectors.toList());
        String initialOptionsBlock = ((answer == null) || (CollectionUtils.isEmpty(answer.getResponses()))) ? null : answer.getResponses().stream().map(a -> a.getValue()).map(StringEscapeUtils::escapeJson).map(x -> String.format(QUESTION_RADIO_BUTTON_OPTION, x, x)).collect(Collectors.toList()).get(0);
        String questionBlock = null;
        if(StringUtils.isNotBlank(initialOptionsBlock)) {
            questionBlock = String.format(QUESTION_RADIO_BUTTON_WITH_INITIAL, sectionId, question.getId(), StringEscapeUtils.escapeJson(question.getName()), String.join(",", optionBlocks), initialOptionsBlock);
        } else {
            questionBlock = String.format(QUESTION_RADIO_BUTTON_WITHOUT_INITIAL, sectionId, question.getId(), StringEscapeUtils.escapeJson(question.getName()), String.join(",", optionBlocks));
        }
        return questionBlock;
    }

    private String buildText(String sectionId, Question question, Answer answer) {
        if((answer != null) && (CollectionUtils.isNotEmpty(answer.getResponses()))) {
            String initialValue = answer.getResponses().get(0).getValue();
            return String.format(QUESTION_TEXT_WITH_INITIAL, sectionId, question.getId(),StringEscapeUtils.escapeJson(initialValue),StringEscapeUtils.escapeJson(question.getName()));
        } else {
            return String.format(QUESTION_TEXT_WITHOUT_INITIAL, sectionId, question.getId(),StringEscapeUtils.escapeJson(question.getName()));
        }
    }

    private String buildCheckBox(String sectionId, String questionId, String text, List<ImmutablePair<String, String>> options, List<ImmutablePair<String, String>> initialOptions){
        List<String> optionBlocks = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(options)) {
            for(ImmutablePair<String,String> option : options) {
                String optionText = StringEscapeUtils.escapeJson(option.getLeft());
                String optionValue = StringEscapeUtils.escapeJson(option.getRight());
                optionBlocks.add(String.format(QUESTION_CHECK_BOX_OPTION, optionText, optionValue));
            }
        }
        List<String> initialOptionBlocks = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(initialOptions)) {
            for(ImmutablePair<String,String> initialOption : initialOptions) {
                String initialOptionText = StringEscapeUtils.escapeJson(initialOption.getLeft());
                String initialOptionValue = StringEscapeUtils.escapeJson(initialOption.getRight());
                initialOptionBlocks.add(String.format(QUESTION_CHECK_BOX_OPTION, initialOptionText, initialOptionValue));
            }
        }
        /*
        for(int i=0; i< optionTexts.size(); i++) {
            optionBlocks.add(String.format(QUESTION_CHECK_BOX_OPTION, StringEscapeUtils.escapeJson(optionTexts.get(i)), StringEscapeUtils.escapeJson(optionValues.get(i))));
        }
         */
        String questionBlock = null;
        if(CollectionUtils.isNotEmpty(initialOptionBlocks)) {
            questionBlock = String.format(QUESTION_CHECK_BOX_WITH_INITIAL, sectionId, questionId, StringEscapeUtils.escapeJson(text), String.join(",", optionBlocks), String.join(",", initialOptionBlocks));
        } else {
            questionBlock = String.format(QUESTION_CHECK_BOX_WITHOUT_INITIAL, sectionId, questionId, StringEscapeUtils.escapeJson(text), String.join(",", optionBlocks));
        }
        return questionBlock;
    }

    private List<String> buildDefaultSectionQuestionsBlocks(String sectionId, List<Question> questions, Map<String, Answer> answersMap) {
        List<Question> updatedQuestions = new ArrayList<>();
        updatedQuestions.addAll(questions);

        log.debug("before {}", updatedQuestions.stream().map(Question::getNumber).collect(Collectors.toList()));
        Collections.sort(updatedQuestions, new Comparator<Question>() {
            @Override
            public int compare(Question o1, Question o2) {
                return o1.getNumber().compareTo(o2.getNumber());
            }
        });
        log.debug("after {}", updatedQuestions.stream().map(Question::getNumber).collect(Collectors.toList()));

        List<String> blocks = new ArrayList<>();
        for(Question question : updatedQuestions) {
            String key = sectionId + "_" + question.getId();
            Answer answer = answersMap.get(key);

            if("single-select".equals(question.getType())) {
                blocks.add(buildRadioButton(sectionId, question, answer));
            } else if("boolean".equals(question.getType())) {
                blocks.add(buildRadioButton(sectionId, question, answer));
            } else if("text".equals(question.getType())) {
                blocks.add(buildText(sectionId, question, answer));
            } else if("multi-select".equals(question.getType())) {
                List<ImmutablePair<String, String>> options1 = (CollectionUtils.isEmpty(question.getOptions())) ? Collections.emptyList() : question.getOptions().stream().map(Question.Option::getResponse).map(x -> ImmutablePair.of(x,x)).collect(Collectors.toList());
                //List<String> options = (CollectionUtils.isEmpty(question.getOptions())) ? Collections.emptyList() : question.getOptions().stream().map(Question.Option::getResponse).collect(Collectors.toList());
                List<ImmutablePair<String, String>> initialOptions = ((answer == null) || (CollectionUtils.isEmpty(answer.getResponses()))) ? Collections.emptyList() :
                        answer.getResponses().stream().map(x -> x.getValue()).map(x -> ImmutablePair.of(x,x)).collect(Collectors.toList());
                blocks.add(buildCheckBox(sectionId, question.getId(), question.getName(), options1, initialOptions));
            } else if("checklist".equals(question.getType())) {
                log.warn("Default Section, Question Id {} has type {} which is not supported!", question.getId(), question.getType());
            } else {
                log.warn("Default Section, Question Id {} has type {} which is not supported!", question.getId(), question.getType());
            }
        }
        return blocks;
    }

    private List<String> buildChecklistSectionQuestionsBlocks(final String sectionId, List<Question> questions, Map<String, Answer> answersMap) {
        /*
        List<String> optionTexts = new ArrayList<>();
        List<String> optionValues = new ArrayList<>();
        for(Question question : questions) {
            optionTexts.add(question.getName());
            optionValues.add(String.format("%s_%s", sectionId, question.getId()));
        }
        */
        List<ImmutablePair<String,String>> options = (CollectionUtils.isEmpty(questions)) ? Collections.emptyList() : questions.stream().map(q -> ImmutablePair.of(q.getName(), String.format("%s_%s", sectionId, q.getId()))).collect(Collectors.toList());
        List<ImmutablePair<String,String>> initialOptions = (CollectionUtils.isEmpty(questions)) ? Collections.emptyList() : questions.stream()
                .map(q -> ImmutablePair.of(q, answersMap.get(sectionId + "_" + q.getId())))
                .filter(p -> Objects.nonNull(p.getRight()))
                .filter(p -> CollectionUtils.isNotEmpty(p.getRight().getResponses()))
                .map(p -> ImmutablePair.of(p.getLeft().getName(), String.format("%s_%s", sectionId, p.getLeft().getId()))).collect(Collectors.toList());

        List<String> blocks = new ArrayList<>();
        blocks.add(buildCheckBox(sectionId, "CHECKLIST" , "Checklist", options, initialOptions));
        return blocks;
    }

    private List<String> buildTitleBlocks(String questionnaireTemplateId, String questionnaireTemplateName) {
        return List.of(
                buildDividerBlock(),
                String.format(TITLE_BLOCK_FORMAT, questionnaireTemplateId, StringEscapeUtils.escapeJson(StringUtils.defaultIfBlank(questionnaireTemplateName, DEFAULT_TITLE))));
    }

    private List<String> buildSectionBlocks(Section section, Map<String, Answer> answersMap) {
        List<String> blocks = new ArrayList<>();
        blocks.add(buildDividerBlock());
        blocks.add(buildSectionName(section.getId(), section.getName()));
        if(CollectionUtils.isNotEmpty(section.getQuestions())) {
            if("DEFAULT".equals(section.getType().toString())) {
                blocks.addAll(buildDefaultSectionQuestionsBlocks(section.getId(), section.getQuestions(), answersMap));
            } else if ("CHECKLIST".equals(section.getType().toString())) {
                blocks.addAll(buildChecklistSectionQuestionsBlocks(section.getId(), section.getQuestions(), answersMap));
            } else {
                log.warn("Section with id {} has type {} which is not supported!", section.getId(), section.getType());
            }
        }
        return blocks;
    }

    private String buildEditQuestionnaireModalView(final String company, final String questionnaireId, final String questionnaireTemplateName, final String modalBlocks){
        String editAssessmentId = QuestionnaireSlackMessageUtils.buildQuestionnaireSlackCallbackId(company, questionnaireId);
        String modalViewTitle = StringUtils.abbreviate(StringUtils.defaultIfBlank(questionnaireTemplateName, DEFAULT_TITLE), MODAL_VIEW_TITLE_MAX_LENGTH);
        String editQuestionnaireModalView = String.format(EDIT_QUESTIONNAIRE_MODAL, StringEscapeUtils.escapeJson(modalViewTitle), modalBlocks, "edit_questionnaire", editAssessmentId);
        log.debug("editQuestionnaireModalView = {}", editQuestionnaireModalView);
        return editQuestionnaireModalView;
    }

    public QuestionnaireSlackMessages buildInteractiveMessages(final String company, final String templatedText, final QuestionnaireDTO questionnaireDetail) {
        List<String> blocks = new ArrayList<>();
        blocks.add(buildEditAssessmentButtonBlock(company, questionnaireDetail.getId(), templatedText));
        String message = "[" + blocks.stream().filter(Objects::nonNull).collect(Collectors.joining(",")) + "]";
        log.debug("message = {}", message);

        Map<String, Answer> answersMap = new HashMap<>();
        if(CollectionUtils.isNotEmpty(questionnaireDetail.getAnswers())) {
            for(SectionResponse sectionResponse : questionnaireDetail.getAnswers()) {
                String sectionId = sectionResponse.getSectionId();
                if(CollectionUtils.isNotEmpty(sectionResponse.getAnswers()));
                for(Answer answer : sectionResponse.getAnswers()) {
                    String questionId = answer.getQuestionId();
                    if(! answer.isAnswered()) {
                        continue;
                    }
                    String key = sectionId + "_" + questionId;
                    answersMap.put(key, answer);
                }
            }
        }

        List<String> modalBlocks = new ArrayList<>();
        if (StringUtils.length(questionnaireDetail.getQuestionnaireTemplateName()) >= MODAL_VIEW_TITLE_MAX_LENGTH) {
            modalBlocks.addAll(buildTitleBlocks(questionnaireDetail.getQuestionnaireTemplateId(), questionnaireDetail.getQuestionnaireTemplateName()));
        }
        if(CollectionUtils.isNotEmpty(questionnaireDetail.getSections())) {
            for(Section section : questionnaireDetail.getSections()) {
                List<String> sectionBlocks = buildSectionBlocks(section, answersMap);
                if(CollectionUtils.isNotEmpty(sectionBlocks)) {
                    if(modalBlocks.size() + sectionBlocks.size() <= MAX_DATA_BLOCKS_COUNT) {
                        modalBlocks.addAll(sectionBlocks);
                    }
                }
            }
        } else {
            log.debug("Sections is null or empty!");
        }
        String modalBlocksMessage = modalBlocks.stream().filter(Objects::nonNull).collect(Collectors.joining(","));
        log.debug("modalBlocksMessage = {}", modalBlocksMessage);

        String modalMessage = buildEditQuestionnaireModalView(company, questionnaireDetail.getId(), questionnaireDetail.getQuestionnaireTemplateName(), modalBlocksMessage);
        log.debug("modalMessage = {}", modalMessage);

        return QuestionnaireSlackMessages.builder().message(message).modalMessage(modalMessage).build();
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = QuestionnaireSlackMessages.QuestionnaireSlackMessagesBuilder.class)
    public static class QuestionnaireSlackMessages {
        private final String message;
        private final String modalMessage;
    }
}
