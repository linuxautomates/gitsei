package io.levelops.api.services;

import com.google.common.base.MoreObjects;
import io.levelops.api.model.slack.SlackInteractiveEvent;
import io.levelops.commons.databases.models.database.Question;
import io.levelops.commons.databases.models.database.Section;
import io.levelops.commons.databases.models.database.questionnaire.Answer;
import io.levelops.commons.databases.models.database.questionnaire.QuestionnaireDTO;
import io.levelops.commons.databases.models.database.questionnaire.SectionResponse;
import io.levelops.exceptions.InternalApiClientException;
import io.levelops.notification.services.QuestionnaireSlackMessageUtils;
import io.levelops.questionnaires.clients.QuestionnaireClient;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Service
@SuppressWarnings("unused")
public class SlackSubmitQuestionnaireService {
    private final QuestionnaireClient questionnaireClient;

    @Autowired
    public SlackSubmitQuestionnaireService(QuestionnaireClient questionnaireClient) {
        this.questionnaireClient = questionnaireClient;
    }

    private Answer.Response parseSelectedOption(Map<String, Map<String, Integer>> resultesponseScoreMap, String sectionId, String questionId, SlackInteractiveEvent.SelectedOption selectedOption) {
        String value = selectedOption.getValue();

        String key = sectionId + "_" + questionId;
        int score = resultesponseScoreMap.getOrDefault(key, Collections.emptyMap()).getOrDefault(value, 0);

        Answer.Response answerResonse = new Answer.Response(value, value, score, null, null, null, null);
        return answerResonse;
    }
    private List<Answer.Response> parseAnswerResponses(Map<String, Map<String, Integer>> resultesponseScoreMap, String sectionId, String questionId, SlackInteractiveEvent.ViewStateValue value){
        if(value.getCustomActionId() == null) {
            return Collections.emptyList();
        }
        List<Answer.Response> answerResponses = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(value.getCustomActionId().getSelectedOptions())) {
            for(SlackInteractiveEvent.SelectedOption option : value.getCustomActionId().getSelectedOptions()) {
                answerResponses.add(parseSelectedOption(resultesponseScoreMap, sectionId, questionId, option));
            }
        }
        if(value.getCustomActionId().getSelectedOption() != null) {
            answerResponses.add(parseSelectedOption(resultesponseScoreMap, sectionId, questionId, value.getCustomActionId().getSelectedOption()));
        }
        if(StringUtils.isNotBlank(value.getCustomActionId().getValue())) {
            String key = sectionId + "_" + questionId;
            String responseKey = IterableUtils.first(resultesponseScoreMap.get(key).keySet());
            int score = resultesponseScoreMap.getOrDefault(key, Collections.emptyMap()).getOrDefault(responseKey, 0);

            answerResponses.add(new Answer.Response(value.getCustomActionId().getValue(), null, score, null, null, null, null));
        }
        return answerResponses;
    }

    private List<Answer> parseChecklistAnswers(String slackUser, Long createdAt, Map<String, Map<String, Integer>> resultResponseScoreMap, SlackInteractiveEvent.ViewStateValue value){
        if(value.getCustomActionId() == null) {
            return Collections.emptyList();
        }
        List<Answer> answers = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(value.getCustomActionId().getSelectedOptions())) {
            for(SlackInteractiveEvent.SelectedOption selectedOption : value.getCustomActionId().getSelectedOptions()) {
                String combinedId = selectedOption.getValue();
                String[] split = combinedId.split("_");
                if((split == null) || (split.length != 2)) {
                    continue;
                }
                String sectionId = split[0];
                String questionId = split[1];

                String responseKey = IterableUtils.first(resultResponseScoreMap.get(combinedId).keySet());
                int score = resultResponseScoreMap.get(combinedId).getOrDefault(responseKey, 0);
                                                                                                                                                                                                                                                                                                                                                                                                                                                                log.debug("responseKey {}, score {}", responseKey, score);

                Answer answer = Answer.builder()
                        .questionId(questionId)
                        .userEmail(slackUser)
                        .answered(true).upload(false)
                        .responses(List.of(new Answer.Response("true", null, score, null, null, null, null)))
                        .createdAt(createdAt)
                        .build();
                answers.add(answer);
            }
        }
        return answers;
    }

    private Map<String, Map<String, Integer>> buildAnswerResponseScoreMap(QuestionnaireDTO questionnaire){
        if(CollectionUtils.isEmpty(questionnaire.getSections())) {
            return Collections.emptyMap();
        }
        Map<String, Map<String, Integer>> resultesponseScoreMap = new HashMap<>();
        for(Section section : questionnaire.getSections()) {
            if(CollectionUtils.isEmpty(section.getQuestions())) {
                continue;
            }
            for(Question q : section.getQuestions()) {
                if(CollectionUtils.isEmpty(q.getOptions())) {
                    continue;
                }
                String outerKey = section.getId() + "_" + q.getId();
                if(! resultesponseScoreMap.containsKey(outerKey)) {
                    resultesponseScoreMap.put(outerKey, new HashMap<>());
                }

                for(Question.Option option : q.getOptions()) {
                    resultesponseScoreMap.get(outerKey).put(option.getResponse(), option.getScore());
                }
            }
        }
        log.info("resultesponseScoreMap {}", resultesponseScoreMap);
        return resultesponseScoreMap;
    }

    private List<SectionResponse> convertEvent(String slackUser, Long createdAt, QuestionnaireDTO questionnaire, SlackInteractiveEvent slackInteractivityEvent) {
        Map<String, Map<String, Integer>> resultesponseScoreMap = buildAnswerResponseScoreMap(questionnaire);

        List<String> sectionIds = new ArrayList<>();
        Map<String, List<Answer>> sectionAnswersMap = new HashMap<>();
        Map<String, SlackInteractiveEvent.ViewStateValue> values = slackInteractivityEvent.getView().getState().getValues();
        for(String key :  values.keySet()){
            String[] split = key.split("_");
            if((split == null) || (split.length != 2)) {
                continue;
            }
            String sectionId = split[0];
            String questionId = split[1];

            if(!sectionAnswersMap.containsKey(sectionId)) {
                sectionIds.add(sectionId);
                sectionAnswersMap.put(sectionId, new ArrayList<>());
            }

            SlackInteractiveEvent.ViewStateValue value = values.get(key);

            if("CHECKLIST".equals(questionId)) {
                List<Answer> answers = parseChecklistAnswers(slackUser, createdAt, resultesponseScoreMap, value);
                if(CollectionUtils.isNotEmpty(answers)) {
                    sectionAnswersMap.get(sectionId).addAll(answers);
                }
            } else {
                List<Answer.Response> answerResponses = parseAnswerResponses(resultesponseScoreMap, sectionId, questionId, value);
                Answer answer = Answer.builder()
                        .questionId(questionId)
                        .userEmail(slackUser)
                        .answered(CollectionUtils.isNotEmpty(answerResponses)).upload(false).createdAt(createdAt)
                        .responses(answerResponses).build();
                sectionAnswersMap.get(sectionId).add(answer);
            }
        }
        List<SectionResponse> sectionResponses = new ArrayList<>();
        for(String sectionId :sectionIds) {
            SectionResponse.SectionResponseBuilder bldr = SectionResponse.builder().sectionId(sectionId);
            if(sectionAnswersMap.containsKey(sectionId)) {
                bldr.answers(sectionAnswersMap.get(sectionId));
            }
            sectionResponses.add(bldr.build());
        }
        return sectionResponses;
    }

    private boolean areAnswersSame(Answer existingAnswer, Answer newAnswer) {
        Set<String> existingResponse = CollectionUtils.isEmpty(existingAnswer.getResponses()) ? Collections.emptySet() : existingAnswer.getResponses().stream().map(Answer.Response::getValue).collect(Collectors.toSet());
        Set<String> newResponse = CollectionUtils.isEmpty(newAnswer.getResponses()) ? Collections.emptySet() : newAnswer.getResponses().stream().map(Answer.Response::getValue).collect(Collectors.toSet());
        return existingResponse.equals(newResponse);
    }

    private Answer mergeAnswer(Answer existingAnswer, Answer newAnswer) {
        log.debug("existingAnswer {}", existingAnswer);
        log.debug("newAnswer {}", newAnswer);
        if((existingAnswer == null) && (newAnswer == null)) {
            return null;
        } else if ((existingAnswer == null) || (newAnswer == null)) {
            return MoreObjects.firstNonNull(existingAnswer, newAnswer);
        }
        if(newAnswer.isAnswered()) {
            log.debug("newAnswer is answered");
            if(areAnswersSame(existingAnswer, newAnswer)) {
                log.debug("existingAnswer & newAnswer is same, so using existingAnswer");
                return existingAnswer;
            } else {
                log.debug("existingAnswer & newAnswer are not the same, so using newAnswer");
                return newAnswer;
            }
        } else if (existingAnswer.isAnswered()) {
            log.debug("existingAnswer is answered");
            return existingAnswer;
        } else {
            return newAnswer;
        }
    }

    private SectionResponse mergeSectionResponse(Section section, SectionResponse existingResponse, SectionResponse newResponse){
        if(existingResponse == null) {
            return newResponse;
        } else if(newResponse == null) {
            return existingResponse;
        }

        Map<String, Answer> existingAnswersMap = (CollectionUtils.isNotEmpty(existingResponse.getAnswers())) ? existingResponse.getAnswers().stream().collect(Collectors.toMap(Answer::getQuestionId, x -> x)) : Collections.emptyMap();
        Map<String, Answer> newAnswersMap = (CollectionUtils.isNotEmpty(newResponse.getAnswers())) ? newResponse.getAnswers().stream().collect(Collectors.toMap(Answer::getQuestionId, x -> x)) : Collections.emptyMap();
        List<Answer> mergedAnswers = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(section.getQuestions())) {
            for (Question q : section.getQuestions()) {
                String qId = q.getId();
                mergedAnswers.add(mergeAnswer(existingAnswersMap.get(qId), newAnswersMap.get(qId)));
            }
        }
        return SectionResponse.builder()
                .sectionId(section.getId())
                .answers(mergedAnswers)
                .build();
    }
    private List<SectionResponse> mergeResponses(List<Section> sections, List<SectionResponse> existingResponses, List<SectionResponse> newResponses) {
        if((CollectionUtils.isEmpty(existingResponses)) && (CollectionUtils.isEmpty(newResponses))) {
            return Collections.emptyList();
        }
        if(CollectionUtils.isEmpty(existingResponses)) {
            return newResponses;
        } else if (CollectionUtils.isEmpty(newResponses)) {
            return existingResponses;
        }

        Map<String, SectionResponse> existingMap = existingResponses.stream().collect(Collectors.toMap(SectionResponse::getSectionId, x -> x));
        Map<String, SectionResponse> newMap = newResponses.stream().collect(Collectors.toMap(SectionResponse::getSectionId, x -> x));

        List<SectionResponse> mergedResponses = new ArrayList<>();
        if(CollectionUtils.isNotEmpty(sections)) {
            for(Section s : sections) {
                String sectionId = s.getId();
                mergedResponses.add(mergeSectionResponse(s, existingMap.get(sectionId), newMap.get(sectionId)));
            }
        }
        return mergedResponses;
    }

    public void processSubmitQuestionnaireEvent(SlackInteractiveEvent slackInteractivityEvent) throws InternalApiClientException {
        log.info("Process Submit Questionnaire Event starting");
        if((slackInteractivityEvent.getView() == null) || (slackInteractivityEvent.getView().getState() == null)) {
            return;
        }
        Map<String, SlackInteractiveEvent.ViewStateValue> values = slackInteractivityEvent.getView().getState().getValues();
        if((values == null) || (values.size() == 0)) {
            return;
        }
        String privateMetadata = slackInteractivityEvent.getView().getPrivateMetadata();
        if(StringUtils.isBlank(privateMetadata)) {
            return;
        }

        Optional<ImmutablePair<String, String>> result = QuestionnaireSlackMessageUtils.parseQuestionnaireSlackCallbackId(privateMetadata);
        if(result.isEmpty()) {
            log.info("Slack Questionnaire Interactive Event could not parse privateMetadata {}", privateMetadata);
            return;
        }

        Long createdAt = Instant.now().getEpochSecond();
        String company = result.get().getLeft();
        String questionnaireId = result.get().getRight();
        log.debug("company {}, questionnaireId {}",company, questionnaireId);

        String usersFullName = (slackInteractivityEvent.getUser() != null) ? slackInteractivityEvent.getUser().getName() : "Unknown";
        String slackUser = "Slack User - " + usersFullName;
        log.debug("slackUser {}", slackUser);

        QuestionnaireDTO questionnaire = questionnaireClient.get(company, UUID.fromString(questionnaireId));
        log.debug("questionnaire {}", questionnaire);

        List<SectionResponse> newResponses = convertEvent(slackUser, createdAt, questionnaire, slackInteractivityEvent);
        log.debug("newResponses {}", newResponses);
        List<SectionResponse> existingResponses = questionnaire.getAnswers();
        log.debug("existingResponses {}", existingResponses);
        List<SectionResponse> mergedResponses = mergeResponses(questionnaire.getSections(), existingResponses, newResponses);
        log.debug("mergedResponses {}", mergedResponses);

        if(mergedResponses == null) {
            log.info("mergedResponses is null, will not update questionnaire, company {}, questionnaire id {}", company, questionnaireId);
            return;
        }

        long answered = mergedResponses.stream()
                .filter(x -> CollectionUtils.isNotEmpty(x.getAnswers()))
                .map(SectionResponse::getAnswers)
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .filter(Answer::isAnswered).count();
        log.debug("answered = {}", answered);

        QuestionnaireDTO mergedQuestionnaire = questionnaire.toBuilder().answers(mergedResponses).answeredQuestions((int)answered).build();
        log.debug("mergedQuestionnaire {}", mergedQuestionnaire);

        log.info("Trying to update company {} questionnaire id {}",company,questionnaireId);
        questionnaireClient.update(company, slackUser, UUID.fromString(questionnaireId), mergedQuestionnaire);
        log.info("Updated company {} questionnaire id {}",company,questionnaireId);
    }
}
