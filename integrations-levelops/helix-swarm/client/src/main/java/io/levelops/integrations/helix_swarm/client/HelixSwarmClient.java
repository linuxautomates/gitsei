package io.levelops.integrations.helix_swarm.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.integrations.helix_swarm.models.ActivityResponse;
import io.levelops.integrations.helix_swarm.models.CommentResponse;
import io.levelops.integrations.helix_swarm.models.GroupResponse;
import io.levelops.integrations.helix_swarm.models.HelixSwarmGroup;
import io.levelops.integrations.helix_swarm.models.HelixSwarmProject;
import io.levelops.integrations.helix_swarm.models.HelixSwarmReviewInfo;
import io.levelops.integrations.helix_swarm.models.HelixSwarmUser;
import io.levelops.integrations.helix_swarm.models.ProjectResponse;
import io.levelops.integrations.helix_swarm.models.ProjectsResponseV10;
import io.levelops.integrations.helix_swarm.models.ReviewFileResponse;
import io.levelops.integrations.helix_swarm.models.ReviewInfoResponse;
import io.levelops.integrations.helix_swarm.models.ReviewResponse;
import io.levelops.integrations.helix_swarm.models.ReviewResponseV10;
import io.levelops.integrations.helix_swarm.models.UserResponse;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class HelixSwarmClient {

    private static final String API_BASE_PATH = "api/v9";
    private static final String API_V_10_BASE_PATH = "api/v10";
    private static final String PROJECTS = "projects";
    private static final String USERS = "users";
    private static final String GROUPS = "groups";
    private static final String REVIEWS = "reviews";
    private static final String FILES = "files";
    private static final String COMMENTS = "comments";
    private static final String ACTIVITY = "activity";
    private static final String PARAM_MAX = "max";
    private static final String PARAM_AFTER = "after";
    private static final String PARAM_TOPIC = "topic";
    private static final String PARAM_TYPE = "type";
    private static final String PARAM_STREAM = "stream";
    private static final String REVIEWS_TOPIC = "reviews/";
    private static final String TYPE_REVIEW = "review";
    private static final String TYPE_JOB = "job";

    private final ClientHelper<HelixSwarmClientException> clientHelper;
    private final String resourceUrl;
    private final int pageSize;

    @Builder
    public HelixSwarmClient(final OkHttpClient okHttpClient, final ObjectMapper objectMapper, final String resourceUrl,
                            final int pageSize) {
        this.resourceUrl = resourceUrl;
        this.pageSize = pageSize;
        this.clientHelper = ClientHelper.<HelixSwarmClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(HelixSwarmClientException.class)
                .build();
    }

    public ReviewResponse getReviews(String after) throws HelixSwarmClientException {
        var builder = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(API_BASE_PATH)
                .addPathSegment(REVIEWS)
                .addQueryParameter(PARAM_MAX, String.valueOf(pageSize));
        if (StringUtils.isNotEmpty(after)) {
            builder.addQueryParameter(PARAM_AFTER, after);
        }
        Request request = buildRequest(builder.build());
        return clientHelper.executeAndParse(request, ReviewResponse.class);
    }

    public HelixSwarmReviewInfo getReviewInfo(Long id) throws HelixSwarmClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(API_BASE_PATH)
                .addPathSegment(REVIEWS)
                .addPathSegment(String.valueOf(id))
                .build();
        Request request = buildRequest(url);
        ReviewInfoResponse reviewInfoResponse = clientHelper.executeAndParse(request, ReviewInfoResponse.class);
        return reviewInfoResponse.getReview();
    }

    public ActivityResponse getReviewActivities(long reviewId, String after) throws HelixSwarmClientException {
        return getActivities(reviewId, after, TYPE_REVIEW);
    }

    private ActivityResponse getActivities(long reviewId, String after, String type) throws HelixSwarmClientException {
        var builder = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(API_BASE_PATH)
                .addPathSegment(ACTIVITY)
                .addQueryParameter(PARAM_TYPE, type)
                .addQueryParameter(PARAM_STREAM, "review-" + reviewId)
                .addQueryParameter(PARAM_MAX, String.valueOf(pageSize));;
        if (StringUtils.isNotEmpty(after)) {
            builder.addQueryParameter(PARAM_AFTER, after);
        }
        Request request = buildRequest(builder.build());
        return clientHelper.executeAndParse(request, ActivityResponse.class);
    }

    public ReviewResponseV10 getReviewsV10(String after) throws HelixSwarmClientException {
        var builder = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(API_V_10_BASE_PATH)
                .addPathSegment(REVIEWS)
                .addQueryParameter(PARAM_MAX, String.valueOf(pageSize));
        if (StringUtils.isNotEmpty(after)) {
            builder.addQueryParameter(PARAM_AFTER, after);
        }
        Request request = buildRequest(builder.build());
        return clientHelper.executeAndParse(request, ReviewResponseV10.class);
    }

    public ReviewFileResponse getReviewFiles(long reviewId, String after) throws HelixSwarmClientException {
        var builder = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(API_V_10_BASE_PATH)
                .addPathSegment(REVIEWS)
                .addPathSegment(String.valueOf(reviewId))
                .addPathSegment(FILES)
                .addQueryParameter(PARAM_MAX, String.valueOf(pageSize));
        if (StringUtils.isNotEmpty(after)) {
            builder.addQueryParameter(PARAM_AFTER, after);
        }
        Request request = buildRequest(builder.build());
        return clientHelper.executeAndParse(request, ReviewFileResponse.class);
    }

    public ProjectsResponseV10 getProjectsV10() throws HelixSwarmClientException {
        var builder = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(API_V_10_BASE_PATH)
                .addPathSegment(PROJECTS)
                .addQueryParameter(PARAM_MAX, String.valueOf(pageSize));;
        Request request = buildRequest(builder.build());
        return clientHelper.executeAndParse(request, ProjectsResponseV10.class);
    }

    public List<HelixSwarmProject> getProjects() throws HelixSwarmClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(API_BASE_PATH)
                .addPathSegment(PROJECTS)
                .build();
        Request request = buildRequest(url);
        ProjectResponse projectResponse = clientHelper.executeAndParse(request, ProjectResponse.class);
        return ListUtils.emptyIfNull(projectResponse.getProjects());
    }

    public List<HelixSwarmUser> getUsers() throws HelixSwarmClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(API_BASE_PATH)
                .addPathSegment(USERS)
                .build();
        Request request = buildRequest(url);
        UserResponse userResponse = clientHelper.executeAndParse(request, UserResponse.class);
        return ListUtils.emptyIfNull(userResponse.getUsers());
    }

    public List<HelixSwarmGroup> getGroups() throws HelixSwarmClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(API_BASE_PATH)
                .addPathSegment(GROUPS)
                .build();
        Request request = buildRequest(url);
        GroupResponse groupResponse = clientHelper.executeAndParse(request, GroupResponse.class);
        return ListUtils.emptyIfNull(groupResponse.getGroups());
    }

    public CommentResponse getComments(long reviewId, Long after) throws HelixSwarmClientException {
        var builder = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(API_BASE_PATH)
                .addPathSegment(COMMENTS)
                .addQueryParameter(PARAM_TOPIC, REVIEWS_TOPIC + reviewId)
                .addQueryParameter(PARAM_MAX, String.valueOf(pageSize));;
        if (after != null) {
            builder.addQueryParameter(PARAM_AFTER, String.valueOf(after));
        }
        Request request = buildRequest(builder.build());
        return clientHelper.executeAndParse(request, CommentResponse.class);
    }

    public ActivityResponse getJobActivities(long reviewId, String after) throws HelixSwarmClientException {
        return getActivities(reviewId, after, TYPE_JOB);
    }


    private Request buildRequest(HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
    }

    public static boolean isApiNotFoundExceptions(HelixSwarmClientException e) {
        if(e == null) {
            return false;
        }
        String errorMessage = e.getMessage();
        return (StringUtils.isNotBlank(errorMessage)) && (errorMessage.startsWith("Response not successful")) && (errorMessage.contains("404"));
    }
}
