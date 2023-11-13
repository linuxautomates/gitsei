package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder.Default;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.EnumUtils;

import java.util.List;
import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
@SuperBuilder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
/**
 * Represents an instance of a Questionaire Template {@link QuestionnaireTemplate}
 */
public class Questionnaire {
    private String id;
    private String questionnaireTemplateId;
    private String workItemId;
    private String productId;

    @JsonProperty(value = "answered_questions")
    @Default
    private Integer answered = 0;
    @JsonProperty(value = "total_questions")
    @Default
    private Integer totalQuestions = 0;
    @JsonProperty(value = "total_score")
    @Default
    private Integer totalPossibleScore = 0;
    private String targetEmail; //which user the quiz notification is to be sent to.
    private String senderEmail;
    private String bucketName;
    private String bucketPath;
    @JsonProperty(value = "current_score")
    @Default
    private Integer score = 0;
    @Default
    private Severity priority = Severity.UNKNOWN;
    private State state;
    @JsonProperty("main")
    private Boolean main;
    private Long completedAt;
    private Long updatedAt;
    private Long createdAt;
    @Default
    private Boolean messageSent = false;

    @Singular
    @JsonProperty("kb_ids")
    private List<UUID> kbIds;

    public enum State {
        CREATED,
        INCOMPLETE,
        COMPLETED; // this state actually means submitted - regardless of completion %

        @JsonCreator
        public static State fromString(final String state){
            return EnumUtils.getEnumIgnoreCase(State.class, state);
        }
    }
}