package io.levelops.integrations.github.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.github.models.GithubUser;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import org.eclipse.egit.github.core.event.CreatePayload;
import org.eclipse.egit.github.core.event.DeletePayload;
import org.eclipse.egit.github.core.event.DownloadPayload;
import org.eclipse.egit.github.core.event.FollowPayload;
import org.eclipse.egit.github.core.event.ForkApplyPayload;
import org.eclipse.egit.github.core.event.ForkPayload;
import org.eclipse.egit.github.core.event.GistPayload;
import org.eclipse.egit.github.core.event.GollumPayload;
import org.eclipse.egit.github.core.event.IssueCommentPayload;
import org.eclipse.egit.github.core.event.IssuesPayload;
import org.eclipse.egit.github.core.event.MemberPayload;
import org.eclipse.egit.github.core.event.PublicPayload;
import org.eclipse.egit.github.core.event.PullRequestPayload;
import org.eclipse.egit.github.core.event.PullRequestReviewCommentPayload;
import org.eclipse.egit.github.core.event.PushPayload;
import org.eclipse.egit.github.core.event.TeamAddPayload;
import org.eclipse.egit.github.core.event.WatchPayload;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubApiRepoEvent.GithubApiRepoEventBuilder.class)
public class GithubApiRepoEvent {

    @JsonProperty("id")
    String id;

    @JsonProperty("type")
    String type;

    @JsonProperty("actor")
    GithubUser actor;

    @JsonProperty("repo")
    GithubRepository repo;

    @JsonProperty("payload")
    Map<String, Object> payload; // NOTE: payload schema is type-dependant

    @JsonProperty("public")
    Boolean isPublic;

    @JsonProperty("created_at")
    Date createdAt;
    
    public enum EventType {
        COMMIT_COMMENT("CommitCommentEvent"),
        CREATE("CreateEvent"),
        DELETE("DeleteEvent"),
        DOWNLOAD("DownloadEvent"),
        FOLLOW("FollowEvent"),
        FORK("ForkEvent"),
        FORK_APPLY("ForkApplyEvent"),
        GIST("GistEvent"),
        GOLLUM("GollumEvent"),
        ISSUE_COMMENT("IssueCommentEvent"),
        ISSUES("IssuesEvent"),
        MEMBER("MemberEvent"),
        PUBLIC("PublicEvent"),
        PULL_REQUEST("PullRequestEvent"),
        PULL_REQUEST_REVIEW_COMMENT("PullRequestReviewCommentEvent"),
        PUSH("PushEvent"),
        TEAM_ADD("TeamAddEvent"),
        WATCH("WatchEvent");

        @Getter
        private final String value;

        EventType(String value) {
            this.value = value;
        }

    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = PushPayload.PushPayloadBuilder.class)
    public static class PushPayload {
        @JsonProperty("before")
        String before;

        @JsonProperty("head")
        String head;

        @JsonProperty("ref")
        String ref;

        @JsonProperty("size")
        Integer size;

        @JsonProperty("commits")
        List<GithubApiCommit> commits;
    }
}
