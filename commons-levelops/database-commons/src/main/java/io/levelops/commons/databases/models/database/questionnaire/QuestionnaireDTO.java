package io.levelops.commons.databases.models.database.questionnaire;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.databases.models.database.NotificationItem;
import io.levelops.commons.databases.models.database.Question;
import io.levelops.commons.databases.models.database.Questionnaire;
import io.levelops.commons.databases.models.database.Section;
import io.levelops.commons.databases.models.database.Severity;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@SuperBuilder(toBuilder = true)
public class QuestionnaireDTO extends NotificationItem {
    @JsonProperty("id")
    private String id;
    @JsonProperty("qtemplate_id")
    private String questionnaireTemplateId;
    @JsonProperty("product_id")
    private String productId;
    @JsonProperty("qtemplate_name")
    private String questionnaireTemplateName;
    @JsonProperty("generation")
    private String generation;
    @JsonProperty("completed")
    @Default
    private Boolean completed = false;
    @JsonProperty("current_score")
    @Default
    private Integer currentScore = 0;
    @JsonProperty("priority")
    @Default
    private Severity priority = Severity.UNKNOWN;
    @JsonProperty("total_questions")
    @Default
    private Integer totalQuestions = 0;
    @JsonProperty("answered_questions")
    @Default
    private Integer answeredQuestions = 0;
    @JsonProperty("total_score")
    @Default
    private Integer totalScore = 0;
    @JsonProperty("updated_at")
    private Long updatedAt;

    @JsonProperty("sections")
    @Default
    private List<Section> sections = Collections.emptyList();
    @JsonProperty("section_responses")
    @Default
    private List<SectionResponse> answers = Collections.emptyList();
    @JsonProperty("comments")
    private String comments;

    @JsonProperty("message_sent")
    Boolean messageSent; // output

    @JsonProperty("ticket_id")
    private String ticketId;

    @Singular
    @JsonProperty("tag_ids")
    private List<String> tagIds;

    @JsonProperty("state")
    private Questionnaire.State state;
    @JsonProperty("main")
    private Boolean main;

    @Singular
    @JsonProperty("kb_ids")
    private List<UUID> kbIds;

    @JsonIgnore
    public static final int calculateScore(List<SectionResponse> sectionResponses){
        return (CollectionUtils.isEmpty(sectionResponses)) ? 0 : sectionResponses.stream()
                .filter(x -> CollectionUtils.isNotEmpty(x.getAnswers()))
                .map(SectionResponse::getAnswers)
                .flatMap(Collection::stream)
                .filter(Answer::isAnswered)
                .filter(x -> CollectionUtils.isNotEmpty(x.getResponses()))
                .map(Answer::getResponses)
                .flatMap(Collection::stream)
                .filter(x -> (x.getScore() != null))
                .mapToInt(Answer.Response::getScore)
                .sum();
    }

    @JsonIgnore
    public static final int calculateMaxScore(List<Section> sections){
        return (CollectionUtils.isEmpty(sections)) ? 0 : sections.stream()
                .filter(x -> CollectionUtils.isNotEmpty(x.getQuestions()))
                .map(Section::getQuestions)
                .flatMap(Collection::stream)
                .filter(x -> CollectionUtils.isNotEmpty(x.getOptions()))
                .map(Question::getOptions)
                .flatMap(Collection::stream)
                .filter(x -> (x.getScore() != null))
                .mapToInt(Question.Option::getScore)
                .sum();
    }

    @JsonIgnore
    public static final Severity calculatePriority(int lowBoundaryPercentage, int midBoundaryPercentage, int total, int current){
        if(total <= 0){
            return Severity.LOW;
        }
        int score = (current * 100) / total;
        return score <= lowBoundaryPercentage ? Severity.LOW : score <= midBoundaryPercentage ? Severity.MEDIUM : Severity.HIGH;
    }
}