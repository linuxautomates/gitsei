package io.levelops.integrations.bitbucket_server.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.client.exceptions.HttpException;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.integrations.bitbucket_server.models.*;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

@Log4j2
public class BitbucketServerClient {

    private final ClientHelper<BitbucketServerClientException> clientHelper;
    private final ObjectMapper objectMapper;
    public final String resourceUrl;
    private final int pageSize;

    private static final String PAGE_LENGTH = "250";
    private static final String PROJECTS = "projects";
    private static final String REPOSITORIES = "repos";
    private static final String COMMITS = "commits";
    private static final String COMMIT_DIFF = "diff";
    private static final String PULL_REQUESTS = "pull-requests";
    private static final String TAGS = "tags";
    private static final String PR_ACTIVITIES = "activities";

    @Builder
    public BitbucketServerClient(OkHttpClient okHttpClient, ObjectMapper objectMapper,
                                 String resourceUrl, Integer pageSize, Boolean allowUnsafeSSL) {
        this.objectMapper = objectMapper;
        this.resourceUrl = resourceUrl;
        this.pageSize = pageSize != 0 ? pageSize : BitbucketServerClientFactory.DEFAULT_PAGE_SIZE;
        if (BooleanUtils.isTrue(allowUnsafeSSL)) {
            try {
                okHttpClient = ClientHelper.configureToIgnoreCertificate(okHttpClient.newBuilder()).build();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                log.warn("Failed to configure BitbucketServer client to ignore SSL certificate validation", e);
            }
        }
        clientHelper = ClientHelper.<BitbucketServerClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(BitbucketServerClientException.class)
                .build();
    }

    /**
     * Paginates BitbucketServerPaginatedResponse optimally by checking the response "nextPageStart" and "start" fields.
     */
    public static <T> Stream<T> streamBitbucketServerPages(int startingPage, Function<Integer, BitbucketServerPaginatedResponse<T>> pageSupplier) {
        return PaginationUtils.stream(String.valueOf(startingPage), pageString -> {
            int pageNumber = Integer.parseInt(pageString);
            BitbucketServerPaginatedResponse<T> response = pageSupplier.apply(pageNumber);
            String nextPageStart = String.valueOf(response.getNextPageStart());
            if (response.getIsLastPage() || response.getNextPageStart() == null
                    || (response.getSize() != null && CollectionUtils.size(response.getValues()) < response.getSize())) {
                nextPageStart = null;
            }
            return PaginationUtils.CursorPageData.<T>builder()
                    .data(response.getValues())
                    .cursor(nextPageStart)
                    .build();
        });
    }

    public Stream<BitbucketServerProject> streamProjects() throws BitbucketServerClientException {
        Stream<BitbucketServerProject> projects;
        try {
            projects = streamBitbucketServerPages(0, page -> {
                try {
                    return getProjects(page);
                } catch (BitbucketServerClientException e) {
                    log.debug("Failed to get projects after page {}", page, e);
                    throw new RuntimeStreamException("Failed to get projects after page=" + page, e);
                }
            });
            return projects;
        } catch (RuntimeStreamException e) {
            throw new BitbucketServerClientException("Failed to list all projects", e);
        }
    }

    public BitbucketServerPaginatedResponse<BitbucketServerProject> getProjects(int pageNumber) throws BitbucketServerClientException {
        pageNumber = Math.max(0, pageNumber);
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(PROJECTS)
                .addQueryParameter("start", String.valueOf(pageNumber))
                .addQueryParameter("limit", PAGE_LENGTH);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                BitbucketServerPaginatedResponse.ofType(objectMapper, BitbucketServerProject.class));
    }

    public Stream<BitbucketServerRepository> searchRepositories(String repo) {
        try {
            return streamProjects().flatMap(ws -> {
                try {
                    return streamRepositories(ws.getKey())
                            .filter(repository -> repository.getName().toLowerCase().contains(repo))
                            .distinct();
                } catch (BitbucketServerClientException e) {
                    log.debug("Failed to get repositories repo={}", repo, e);
                    throw new RuntimeStreamException("Failed to get repositories for repo=" + repo, e);
                }
            }).distinct();
        } catch (BitbucketServerClientException e) {
            log.debug("Failed while streaming projects to get repositories repo={}", repo, e);
            throw new RuntimeStreamException("Failed while streaming projects to get repositories for repo=" + repo, e);
        }
    }

    public Stream<BitbucketServerRepository> streamRepositories(String projectKey) throws BitbucketServerClientException {
        Stream<BitbucketServerRepository> repositories;
        try {
            repositories = streamBitbucketServerPages(0, page -> {
                try {
                    return getRepositories(projectKey, page);
                } catch (BitbucketServerClientException e) {
                    log.debug("Failed to get repos after page {}", page, e);
                    throw new RuntimeStreamException("Failed to get repos after page=" + page, e);
                }
            });
            return repositories;
        } catch (RuntimeStreamException e) {
            throw new BitbucketServerClientException("Failed to list all repos", e);
        }
    }

    public BitbucketServerPaginatedResponse<BitbucketServerRepository> getRepositories(String projectKey, int pageNumber) throws BitbucketServerClientException {
        pageNumber = Math.max(0, pageNumber);
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(PROJECTS)
                .addPathSegment(projectKey)
                .addPathSegment(REPOSITORIES)
                .addQueryParameter("start", String.valueOf(pageNumber))
                .addQueryParameter("limit", PAGE_LENGTH);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                BitbucketServerPaginatedResponse.ofType(objectMapper, BitbucketServerRepository.class));
    }

    public Stream<BitbucketServerRepository> streamRepositories() {
        try {
            return PaginationUtils.stream(0, 1, page -> {
                try {
                    return getRepositories(page, Integer.valueOf(PAGE_LENGTH)).getValues();
                } catch (BitbucketServerClientException e) {
                    log.warn("Failed to get projects after page " + page, e);
                    return List.of();
                }
            });
        } catch (RuntimeStreamException e) {
            log.warn("Failed to list all projects", e);
            return Stream.of();
        }
    }

    public BitbucketServerPaginatedResponse<BitbucketServerRepository> getRepositories(int pageNumber, int pageSize) throws BitbucketServerClientException {
        pageNumber = Math.max(0, pageNumber);
        pageSize = pageSize == 0 ? Integer.valueOf(PAGE_LENGTH) : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(REPOSITORIES)
                .addQueryParameter("sort", "-updated_on")
                .addQueryParameter("start", String.valueOf(pageNumber))
                .addQueryParameter("limit", String.valueOf(pageSize));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                BitbucketServerPaginatedResponse.ofType(objectMapper, BitbucketServerRepository.class));
    }

    public Stream<BitbucketServerCommit> streamCommits(String projectKey, String repositorySlug) throws BitbucketServerClientException {
        Stream<BitbucketServerCommit> commits;
        try {
            commits = streamBitbucketServerPages(0, page -> {
                try {
                    return getCommits(projectKey, repositorySlug, page);
                } catch (BitbucketServerClientException e) {
                    /*
                      See https://levelops.atlassian.net/browse/LEV-3628
                      Handling uninitialized repos i.e. repos without default branch or repos with non standard default branch
                    */
                    if (isHttpException(e, HttpStatus.NOT_FOUND)) {
                        log.warn("Ignoring exception due to uninitialized repo!", e);
                        return BitbucketServerPaginatedResponse.<BitbucketServerCommit>builder()
                                .currentPageStart(0)
                                .values(List.of())
                                .isLastPage(true)
                                .build();
                    }
                    log.debug("Failed to get commits after page {}", page, e);
                    throw new RuntimeStreamException("Failed to get commits after page=" + page, e);
                }
            });
            return commits;
        } catch (RuntimeStreamException e) {
            throw new BitbucketServerClientException("Failed to list all commits", e);
        }
    }

    private boolean isHttpException(Throwable exception, HttpStatus httpStatus) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof HttpException) {
                HttpException httpException = (HttpException) cause;
                if (httpException.getCode() == httpStatus.value()) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
    }


    public BitbucketServerPaginatedResponse<BitbucketServerCommit> getCommits(String projectKey, String repositorySlug,
                                                                              int pageNumber) throws BitbucketServerClientException {
        pageNumber = Math.max(0, pageNumber);
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(PROJECTS)
                .addPathSegment(projectKey)
                .addPathSegment(REPOSITORIES)
                .addPathSegment(repositorySlug)
                .addPathSegment(COMMITS)
                .addQueryParameter("merges", "include")
                .addQueryParameter("start", String.valueOf(pageNumber))
                .addQueryParameter("limit", PAGE_LENGTH);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                BitbucketServerPaginatedResponse.ofType(objectMapper, BitbucketServerCommit.class));
    }

    public BitbucketServerCommitDiffInfo getCommitDiff(String projectKey, String repositorySlug, String commitId) throws BitbucketServerClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(PROJECTS)
                .addPathSegment(projectKey)
                .addPathSegment(REPOSITORIES)
                .addPathSegment(repositorySlug)
                .addPathSegment(COMMITS)
                .addPathSegment(commitId)
                .addPathSegment(COMMIT_DIFF);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request, BitbucketServerCommitDiffInfo.class);
    }

    public Stream<BitbucketServerPullRequest> streamPullRequests(String projectKey, String repositorySlug) throws BitbucketServerClientException {
        Stream<BitbucketServerPullRequest> pullRequests;
        try {
            pullRequests = streamBitbucketServerPages(0, page -> {
                try {
                    return getPullRequests(projectKey, repositorySlug, page);
                } catch (BitbucketServerClientException e) {
                    log.debug("Failed to get pull requests after page {}", page, e);
                    throw new RuntimeStreamException("Failed to get pull requests after page=" + page, e);
                }
            });
            return pullRequests;
        } catch (RuntimeStreamException e) {
            throw new BitbucketServerClientException("Failed to list all pull requests", e);
        }
    }

    public BitbucketServerPaginatedResponse<BitbucketServerPullRequest> getPullRequests(String projectKey, String repositorySlug,
                                                                                        int pageNumber) throws BitbucketServerClientException {
        pageNumber = Math.max(0, pageNumber);
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(PROJECTS)
                .addPathSegment(projectKey)
                .addPathSegment(REPOSITORIES)
                .addPathSegment(repositorySlug)
                .addPathSegment(PULL_REQUESTS)
                .addQueryParameter("state", "ALL")
                .addQueryParameter("order", "NEWEST")
                .addQueryParameter("start", String.valueOf(pageNumber))
                .addQueryParameter("limit", PAGE_LENGTH);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                BitbucketServerPaginatedResponse.ofType(objectMapper, BitbucketServerPullRequest.class));
    }

    public Stream<BitbucketServerPRActivity> streamPrActivities(String projectKey, String repositorySlug, int prId) throws BitbucketServerClientException {
        Stream<BitbucketServerPRActivity> prActivities;
        try {
            prActivities = streamBitbucketServerPages(0, page -> {
                try {
                    return getPrActivities(projectKey, repositorySlug, prId, page);
                } catch (BitbucketServerClientException e) {
                    log.debug("Failed to get pr activities after page {}", page, e);
                    throw new RuntimeStreamException("Failed to get pr activities after page=" + page, e);
                }
            });
            return prActivities;
        } catch (RuntimeStreamException e) {
            throw new BitbucketServerClientException("Failed to list all pr activities", e);
        }
    }

    public BitbucketServerPaginatedResponse<BitbucketServerPRActivity> getPrActivities(String projectKey, String repositorySlug,
                                                                                       int prId, int pageNumber) throws BitbucketServerClientException {
        pageNumber = Math.max(0, pageNumber);
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(PROJECTS)
                .addPathSegment(projectKey)
                .addPathSegment(REPOSITORIES)
                .addPathSegment(repositorySlug)
                .addPathSegment(PULL_REQUESTS)
                .addPathSegment(String.valueOf(prId))
                .addPathSegment(PR_ACTIVITIES)
                .addQueryParameter("start", String.valueOf(pageNumber))
                .addQueryParameter("limit", PAGE_LENGTH);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                BitbucketServerPaginatedResponse.ofType(objectMapper, BitbucketServerPRActivity.class));
    }

    public BitbucketServerBranchInfo getDefaultBranch(String projectKey, String repositorySlug) throws BitbucketServerClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(PROJECTS)
                .addPathSegment(projectKey)
                .addPathSegment(REPOSITORIES)
                .addPathSegment(repositorySlug)
                .addPathSegment("branches")
                .addPathSegment("default");
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request, BitbucketServerBranchInfo.class);
    }

    public Stream<BitbucketServerCommit> streamPrCommits(String projectKey, String repositorySlug, Integer prId) throws BitbucketServerClientException {
        Stream<BitbucketServerCommit> prCommits;
        try {
            prCommits = streamBitbucketServerPages(0, page -> {
                try {
                    return getPrCommits(projectKey, repositorySlug, prId, page);
                } catch (BitbucketServerClientException e) {
                    log.debug("Failed to get pull requests after page {}", page, e);
                    if (isHttpException(e, HttpStatus.NOT_FOUND)) {
                        log.warn("Ignoring exception due to missing commits for project: {}, repo: {}, pr: {}", projectKey, repositorySlug, prId, e);
                        return BitbucketServerPaginatedResponse.<BitbucketServerCommit>builder()
                                .currentPageStart(0)
                                .values(List.of())
                                .isLastPage(true)
                                .build();
                    }
                    throw new RuntimeStreamException("Failed to get pull requests after page=" + page, e);
                }
            });
            return prCommits;
        } catch (RuntimeStreamException e) {
            throw new BitbucketServerClientException("Failed to list all pull requests", e);
        }
    }

    public BitbucketServerPaginatedResponse<BitbucketServerCommit> getPrCommits(String projectKey, String repositorySlug, int prId, int pageNumber) throws BitbucketServerClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(PROJECTS)
                .addPathSegment(projectKey)
                .addPathSegment(REPOSITORIES)
                .addPathSegment(repositorySlug)
                .addPathSegment(PULL_REQUESTS)
                .addPathSegment(String.valueOf(prId))
                .addPathSegment(COMMITS)
                .addQueryParameter("start", String.valueOf(pageNumber))
                .addQueryParameter("limit", PAGE_LENGTH);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                BitbucketServerPaginatedResponse.ofType(objectMapper, BitbucketServerCommit.class));
    }

    public BitbucketServerPaginatedResponse<BitbucketServerTag> getTags(String projectKey, String repositorySlug, int pageNumber) throws BitbucketServerClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(PROJECTS)
                .addPathSegment(projectKey)
                .addPathSegment(REPOSITORIES)
                .addPathSegment(repositorySlug)
                .addPathSegment(TAGS)
                .addQueryParameter("start", String.valueOf(pageNumber))
                .addQueryParameter("limit", PAGE_LENGTH);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                BitbucketServerPaginatedResponse.ofType(objectMapper, BitbucketServerTag.class));
    }

    public Stream<BitbucketServerTag> streamTags(String projectKey, String repositorySlug) throws BitbucketServerClientException {
        Stream<BitbucketServerTag> tags;
        try {
            tags = streamBitbucketServerPages(0, page -> {
                try {
                    return getTags(projectKey, repositorySlug, page);
                } catch (BitbucketServerClientException e) {
                    log.debug("Failed to get tags after page {}", page, e);
                    throw new RuntimeStreamException("Failed to get tags after page=" + page, e);
                }
            });
            return tags;
        } catch (RuntimeStreamException e) {
            throw new BitbucketServerClientException("Failed to list all tags", e);
        }
    }

    private HttpUrl.Builder baseUrlBuilder() {
        return Objects.requireNonNull(HttpUrl.parse(resourceUrl + BitbucketServerConstants.BASE_PATH).newBuilder());
    }

    public BitbucketServerPaginatedResponse<BitbucketServerRepository> getRepositories(String projectKey, String repoName, int pageNumber) throws BitbucketServerClientException {
        pageNumber = Math.max(0, pageNumber);
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(PROJECTS)
                .addPathSegment(projectKey)
                .addPathSegment(REPOSITORIES)
                .addPathSegment(repoName == null ? "" : repoName)
                .addQueryParameter("start", String.valueOf(pageNumber))
                .addQueryParameter("limit", PAGE_LENGTH);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                BitbucketServerPaginatedResponse.ofType(objectMapper, BitbucketServerRepository.class));
    }

    public BitbucketServerPaginatedResponse<BitbucketServerRepository> getRepositories(String projectKey, String repoName, int pageNumber, int pageSize) throws BitbucketServerClientException {
        pageNumber = Math.max(0, pageNumber);
        pageSize = pageSize == 0 ? Integer.valueOf(PAGE_LENGTH) : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(PROJECTS)
                .addPathSegment(projectKey)
                .addPathSegment(REPOSITORIES)
                .addPathSegment(repoName == null ? "" : repoName)
                .addQueryParameter("start", String.valueOf(pageNumber))
                .addQueryParameter("limit", String.valueOf(pageSize));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                BitbucketServerPaginatedResponse.ofType(objectMapper, BitbucketServerRepository.class));
    }
}
