package io.levelops.commons.databases.models.database.questionnaire;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class SectionResponse {
    @JsonProperty("id")
    private String id;
    @JsonProperty("user_email")
    private String userEmail;
    @JsonProperty("section_id")
    @JsonAlias("question_id")
    private String sectionId;
    @JsonProperty("answers")
    @JsonAlias("assertions")
    private List<Answer> answers;
}