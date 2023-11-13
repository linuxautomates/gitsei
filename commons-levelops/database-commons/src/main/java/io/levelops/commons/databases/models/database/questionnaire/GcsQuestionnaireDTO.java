package io.levelops.commons.databases.models.database.questionnaire;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.databases.models.database.Section;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GcsQuestionnaireDTO {
    @JsonProperty("section_responses")
    @JsonAlias("answer_dtos")
    private List<SectionResponse> sectionResponses;
    @JsonProperty("assignment_msg")
    private String assignmentMsg;
    @JsonProperty("sections")
    @JsonAlias("questions")
    private List<Section> sections;
    @JsonProperty("comments")
    private String comments;
}