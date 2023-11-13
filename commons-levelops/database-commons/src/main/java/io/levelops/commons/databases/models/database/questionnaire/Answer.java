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
import lombok.Builder.Default;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Answer {
    @JsonProperty("question_id")
    @JsonAlias("assertion_id")
    private String questionId;
    @JsonProperty("user_email")
    private String userEmail;
    @JsonProperty("answered")
    private boolean answered;
    @JsonProperty("upload")
    private boolean upload;
    @JsonProperty("responses")
    @JsonAlias("response")
    private List<Response> responses;
    @Default
    @JsonAlias("comments")
    private List<Comment> comments = List.of();
    @JsonProperty("created_at")
    private Long createdAt;

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Response {
        @JsonProperty("value")
        private String value;
        @JsonProperty("original_value")
        private String originalValue;
        @JsonProperty("score")
        private Integer score;
        @JsonProperty("type")
        private String type;
        @JsonProperty(value = "file_name")
        private String fileName;
        @JsonProperty("created_at")
        private Long createdAt;
        @JsonProperty("user")
        private String user;
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Comment {
        @JsonProperty("user")
        private String user;
        @JsonProperty("message")
        private String message;
        @JsonProperty("created_at")
        private Long createdAt;
    }
}