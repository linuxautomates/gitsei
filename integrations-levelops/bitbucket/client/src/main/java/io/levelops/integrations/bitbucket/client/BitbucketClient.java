package io.levelops.integrations.bitbucket.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.utils.ListUtils;
import io.levelops.integrations.bitbucket.models.BitbucketCommit;
import io.levelops.integrations.bitbucket.models.BitbucketCommitDiffStat;
import io.levelops.integrations.bitbucket.models.BitbucketPaginatedResponse;
import io.levelops.integrations.bitbucket.models.BitbucketPullRequest;
import io.levelops.integrations.bitbucket.models.BitbucketPullRequestActivity;
import io.levelops.integrations.bitbucket.models.BitbucketRepository;
import io.levelops.integrations.bitbucket.models.BitbucketTag;
import io.levelops.integrations.bitbucket.models.BitbucketWorkspace;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.logging.log4j.util.Strings;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class BitbucketClient {

    private final ClientHelper<BitbucketClientException> clientHelper;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter BITBUCKET_DATE_FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss", Locale.US);

    private static final String PAGE_LENGTH = "100"; // in general, max is 100 but some endpoints may have a smaller limit
    private static final String GET_PULL_REQUESTS_PAGE_LEN = "50"; // max of 50 for pullrequests
    private static final String PR_ACTIVITY_PAGE_LEN = "50";

    private static final String BB_PR_FIELDS = "values.description,values.title,values.close_source_branch,values.type,values.id,values.destination,values.created_on,values.summary,values.source,values.comment_count,values.state,values.task_count,values.reason,values.updated_on,values.author,values.merge_commit,values.closed_by,values.participants,page,size,pagelen";

    @Builder
    public BitbucketClient(OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        clientHelper = ClientHelper.<BitbucketClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(BitbucketClientException.class)
                .build();
    }

    private HttpUrl.Builder baseUrlBuilder() {
        return HttpUrl.parse(BitbucketConstants.BASE_URL).newBuilder();
    }

    /**
     * Paginates BitbucketPaginatedResponse optimally by checking the response "next" and "pagelen" fields.
     */
    public static <T> Stream<T> streamBitbucketPages(int startingPage, Function<Integer, BitbucketPaginatedResponse<T>> pageSupplier) {
        return PaginationUtils.stream(String.valueOf(startingPage), pageString -> {
            int pageNumber = Integer.parseInt(pageString);
            BitbucketPaginatedResponse<T> response = pageSupplier.apply(pageNumber);
            if (response == null) {
                return PaginationUtils.CursorPageData.<T>builder()
                        .data(List.of())
                        .cursor(null)
                        .build();
            }
            String nextCursor = String.valueOf(pageNumber + 1);
            if (StringUtils.isEmpty(response.getNext())
                    || (response.getPagelen() != null && CollectionUtils.size(response.getValues()) < response.getPagelen())) {
                // if we know there are no new pages, shot-circuit the pagination
                nextCursor = null;
            }
            return PaginationUtils.CursorPageData.<T>builder()
                    .data(ListUtils.emptyIfNull(response.getValues()))
                    .cursor(nextCursor)
                    .build();
        });
    }

    //region Generate Date Query
    public static String generateDateQuery(final String fieldName, final Instant from, final Instant to) {
        String query = String.format("%s > %s AND %s <= %s",
                fieldName, BITBUCKET_DATE_FORMATTER.format(from.atZone(ZoneOffset.UTC)),
                fieldName, BITBUCKET_DATE_FORMATTER.format(to.atZone(ZoneOffset.UTC)));
        return query;
    }
    //endregion

    public Stream<BitbucketWorkspace> streamWorkspaces() throws BitbucketClientException {
        try {
            // -- pulling all the workspaces in memory since the scale is low and rate limits seem strong
            List<BitbucketWorkspace> workspaces = streamBitbucketPages(1, page -> {
                try {
                    return getWorkspaces(page);
                } catch (BitbucketClientException e) {
                    log.debug("Failed to get workspaces after page {}", page, e);
                    throw new RuntimeStreamException("Failed to get workspaces after page=" + page, e);
                }
            }).collect(Collectors.toList());
            return workspaces.stream();
        } catch (RuntimeStreamException e) {
            throw new BitbucketClientException("Failed to list all Workspaces", e);
        }
    }

    public BitbucketPaginatedResponse<BitbucketWorkspace> getWorkspaces(int pageNumber) throws BitbucketClientException {
        log.info("pageNumber = {}", pageNumber);
        pageNumber = Math.max(1, pageNumber);
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("workspaces")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("pagelen", PAGE_LENGTH);
        HttpUrl url = urlBuilder.build();
        log.info("getWorkspaces, url = {}", url);
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                BitbucketPaginatedResponse.ofType(objectMapper, BitbucketWorkspace.class));
    }

    //region Bitbucket Repos
    private List<BitbucketRepository> addWorkspaceSlugToRepo(List<BitbucketRepository> repos, String workspaceSlug) {
        if (repos == null) {
            return null;
        }
        return repos.stream()
                .map(r -> r.toBuilder().workspaceSlug(workspaceSlug).build())
                .collect(Collectors.toList());
    }

    public Stream<BitbucketRepository> searchRepositories(String repo) {
        try {
            return streamWorkspaces().flatMap(ws -> streamRepositories(ws.getSlug())
                            .filter(repository -> repository.getFullName().toLowerCase().contains(repo)))
                    .distinct();
        } catch (BitbucketClientException e) {
            log.debug("Failed to get repositories repo={}", repo, e);
            throw new RuntimeStreamException("Failed to get repositories afterfor repo=" + repo, e);
        }
    }

    public Stream<BitbucketRepository> streamRepositories(String workspaceSlug) {
        return streamBitbucketPages(1, page -> {
            try {
                BitbucketPaginatedResponse<BitbucketRepository> repos = getRepositories(workspaceSlug, page);
                List<BitbucketRepository> reposSanitized = addWorkspaceSlugToRepo(repos.getValues(), workspaceSlug);
                return repos.toBuilder()
                        .values(reposSanitized)
                        .build();
            } catch (BitbucketClientException e) {
                log.debug("Failed to get repositories after page={} for ws={}", page, workspaceSlug, e);
                throw new RuntimeStreamException("Failed to get repositories after page=" + page + " for ws=" + workspaceSlug, e);
            }
        });
    }

    public BitbucketPaginatedResponse<BitbucketRepository> getRepositories(String workspaceSlug, int pageNumber) throws BitbucketClientException {
        Validate.notBlank(workspaceSlug, "workspace slug cannot be null or empty.");
        pageNumber = Math.max(1, pageNumber);
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("repositories")
                .addPathSegment(workspaceSlug)
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("pagelen", PAGE_LENGTH);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                BitbucketPaginatedResponse.ofType(objectMapper, BitbucketRepository.class));
    }


    public BitbucketPaginatedResponse<BitbucketRepository> getRepositories(int pageNumber, int pageSize) throws BitbucketClientException {
        pageNumber = Math.max(1, pageNumber);
        pageSize = pageSize == 0 ? Integer.valueOf(PAGE_LENGTH) : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("repositories")
                .addQueryParameter("sort", "-updated_on")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("pagelen", String.valueOf(pageSize));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                BitbucketPaginatedResponse.ofType(objectMapper, BitbucketRepository.class));
    }

    public BitbucketPaginatedResponse<BitbucketRepository> searchRepositories(String workspaceSlug, int pageNumber, int pageSize) throws BitbucketClientException {
        pageNumber = Math.max(1, pageNumber);
        pageSize = pageSize == 0 ? Integer.valueOf(PAGE_LENGTH) : pageSize;
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("repositories")
                .addPathSegment(StringUtils.isEmpty(workspaceSlug) ? "" : workspaceSlug)
                .addQueryParameter("sort", "-updated_on")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("pagelen", String.valueOf(pageSize));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                BitbucketPaginatedResponse.ofType(objectMapper, BitbucketRepository.class));
    }
    //endregion

    //region Bitbucket Commits
    private List<BitbucketCommit> sanitizeCommits(final List<BitbucketCommit> commits, String workspaceSlug, String repoId) {
        if (commits == null) {
            return null;
        }
        return commits.stream()
                .map(c -> c.toBuilder().workspaceSlug(workspaceSlug).repoUuid(repoId).build())
                .collect(Collectors.toList());
    }

    public Stream<BitbucketCommit> streamRepoCommits(String workspaceSlug, final String repoId) {
        return streamBitbucketPages(1, page -> {
            try {
                BitbucketPaginatedResponse<BitbucketCommit> commits = getRepoCommits(workspaceSlug, repoId, page);
                List<BitbucketCommit> sanitizedCommits = sanitizeCommits(commits.getValues(), workspaceSlug, repoId);
                return commits.toBuilder()
                        .values(sanitizedCommits)
                        .build();
            } catch (BitbucketClientException e) {
                log.debug("Failed to get commits after page={} for repo={}/{}", page, workspaceSlug, repoId, e);
                throw new RuntimeStreamException("Failed to get repositories after page=" + page + " for repo=" + workspaceSlug + "/" + repoId, e);
            }
        });
    }

    public BitbucketPaginatedResponse<BitbucketCommit> getRepoCommits(String workspaceSlug, final String repoId, int pageNumber) throws BitbucketClientException {
        Validate.notBlank(workspaceSlug, "accountId cannot be null or empty.");
        Validate.notBlank(repoId, "repoId cannot be null or empty.");
        pageNumber = Math.max(1, pageNumber);
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("repositories")
                .addPathSegment(workspaceSlug)
                .addPathSegment(repoId)
                .addPathSegment("commits")
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("pagelen", PAGE_LENGTH);

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                BitbucketPaginatedResponse.ofType(objectMapper, BitbucketCommit.class));
    }
    //endregion

    //region Bitbucket Commit DiffSet
    public Stream<BitbucketCommitDiffStat> streamRepoCommitDiffSets(String workspaceSlug, final String repoId, final String commitId) {
        return streamBitbucketPages(1, page -> {
            try {
                return getRepoCommitDiffSets(workspaceSlug, repoId, commitId, page);
            } catch (BitbucketClientException e) {
                log.debug("Failed to get commit diff sets after page {} for {}/{}, commitId={}", workspaceSlug, repoId, commitId, page, e);
                throw new RuntimeStreamException("Failed to get commit diff sets after page=" + page + " for " + workspaceSlug + "/" + repoId + ", commitId=" + commitId, e);
            }
        });
    }

    public BitbucketPaginatedResponse<BitbucketCommitDiffStat> getRepoCommitDiffSets(String workspaceSlug, final String repoId, final String commitId, int pageNumber) throws BitbucketClientException {
        Validate.notBlank(workspaceSlug, "workspaceSlug cannot be null or empty.");
        pageNumber = Math.max(1, pageNumber);
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("repositories")
                .addPathSegment(workspaceSlug)
                .addPathSegment(repoId)
                .addPathSegment("diffstat")
                .addPathSegment(commitId)
                .addQueryParameter("page", String.valueOf(pageNumber))
                .addQueryParameter("pagelen", PAGE_LENGTH);

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                BitbucketPaginatedResponse.ofType(objectMapper, BitbucketCommitDiffStat.class));
    }
    //endregion

    //region Bitbucket PRs
    public Stream<BitbucketPullRequest> streamPullRequests(String workspaceSlug, String repoId, Instant from, Instant to) {
        return streamBitbucketPages(1, pageNumber -> {
            try {
                return getPullRequests(workspaceSlug, repoId, from, to, pageNumber);
            } catch (BitbucketClientException e) {
                throw new RuntimeStreamException("Failed to stream pull requests after page " + pageNumber + " for " + workspaceSlug + "/" + repoId, e);
            }
        });
    }

    /**
     * Return PRs for given account and repo between 2 dates.
     */
    public BitbucketPaginatedResponse<BitbucketPullRequest> getPullRequests(String workspaceSlug, String repoId, Instant from, Instant to, int page) throws BitbucketClientException {
        Validate.notBlank(workspaceSlug, "workspaceSlug cannot be null or empty.");
        Validate.notBlank(repoId, "repoName cannot be null or empty.");
        Validate.notNull(from, "from cannot be null.");
        Validate.notNull(to, "to cannot be null.");
        String query = generateDateQuery("updated_on", from, to);
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("repositories")
                .addPathSegment(workspaceSlug)
                .addPathSegment(repoId)
                .addPathSegment("pullrequests")
                .addQueryParameter("state", "OPEN")
                .addQueryParameter("state", "MERGED")
                .addQueryParameter("state", "DECLINED")
                .addQueryParameter("q", query)
                .addQueryParameter("fields", BB_PR_FIELDS)
                .addQueryParameter("page", String.valueOf(page))
                .addQueryParameter("pagelen", GET_PULL_REQUESTS_PAGE_LEN);
        log.debug("Bitbucket PR page={} query={}", page, query);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                BitbucketPaginatedResponse.ofType(objectMapper, BitbucketPullRequest.class));
    }

    public BitbucketPullRequest getPullRequest(String workspaceSlug, String repoId, String prId) throws BitbucketClientException {
        Validate.notBlank(workspaceSlug, "workspaceSlug cannot be null or empty.");
        Validate.notBlank(repoId, "repoName cannot be null or empty.");
        Validate.notBlank(prId, "prId cannot be null or empty.");
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("repositories")
                .addPathSegment(workspaceSlug)
                .addPathSegment(repoId)
                .addPathSegment("pullrequests")
                .addPathSegment(prId);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request, BitbucketPullRequest.class);
    }
    //endregion

    //region PR Commits
    public Stream<BitbucketCommit> streamPullRequestCommits(String workspaceSlug, String repoUuid, Integer pullRequestId) {
        MutableInt page = new MutableInt(1);
        return PaginationUtils.stream("", cursor -> {
            int pageNb = page.getAndIncrement();
            try {
                log.debug("Bitbucket Commits repo={} pr={} cursor={}", repoUuid, pullRequestId, cursor);
                BitbucketPaginatedResponse<BitbucketCommit> commits = getPullRequestCommits(workspaceSlug, repoUuid, pullRequestId, cursor);
                return PaginationUtils.CursorPageData.<BitbucketCommit>builder()
                        .cursor(commits.extractNextPage().orElse(null))
                        .data(commits.getValues())
                        .build();
            } catch (BitbucketClientException e) {
                log.warn("Failed to get pull request commits after page={} for repo={}, pr={}", pageNb, repoUuid, pullRequestId, e);
                throw new RuntimeStreamException("Failed to get pull request  commits after page=" + pageNb + " for " + repoUuid + "/" + pullRequestId, e);
            }
        });
    }

    public BitbucketPaginatedResponse<BitbucketCommit> getPullRequestCommits(String workspaceSlug, String repoUuid, Integer pullRequestId, @Nullable String cursor) throws BitbucketClientException {
        Validate.notBlank(repoUuid, "repoId cannot be null or empty.");
        Validate.notNull(pullRequestId, "pullRequestId cannot be null.");
        // /2.0/repositories/{workspace}/{repo_slug}/pullrequests/{pull_request_id}/commits
        // /2.0/repositories/{}/{repo_id}/pullrequests/{pull_request_id}/commits
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("repositories")
                .addPathSegment(workspaceSlug)
                .addPathSegment(repoUuid)
                .addPathSegment("pullrequests")
                .addPathSegment(pullRequestId.toString())
                .addPathSegment("commits")
                .addQueryParameter("pagelen", PAGE_LENGTH);
        if (Strings.isNotEmpty(cursor)) {
            urlBuilder.addQueryParameter("page", cursor);
        }
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request,
                BitbucketPaginatedResponse.ofType(objectMapper, BitbucketCommit.class));
    }

    public Stream<BitbucketPullRequestActivity> streamPullRequestActivity(String workspaceSlug, String repoUuid, String pullRequestId) {
        MutableInt page = new MutableInt(1);
        return PaginationUtils.stream("", cursor -> {
            int pageNb = page.getAndIncrement();
            try {
                log.debug("Bitbucket PR activity for repo={} pr={} cursor={}", repoUuid, pullRequestId, cursor);
                BitbucketPaginatedResponse<BitbucketPullRequestActivity> response = getPullRequestActivity(workspaceSlug, repoUuid, pullRequestId, cursor);
                return PaginationUtils.CursorPageData.<BitbucketPullRequestActivity>builder()
                        .cursor(response.extractNextPage().orElse(null))
                        .data(response.getValues())
                        .build();
            } catch (BitbucketClientException e) {
                log.warn("Failed to get pull request activity after page={} for repo={}, pr={}", pageNb, repoUuid, pullRequestId, e);
                throw new RuntimeStreamException("Failed to get pull request activity after page=" + pageNb + " for " + repoUuid + "/" + pullRequestId, e);
            }
        });
    }


    public BitbucketPaginatedResponse<BitbucketPullRequestActivity> getPullRequestActivity(String workspaceSlug, String repoUuid, String prId, String cursor) throws BitbucketClientException {
        Validate.notBlank(repoUuid, "repoUuid cannot be null or empty.");
        Validate.notBlank(workspaceSlug, "workspaceSlug cannot be null.");
        Validate.notBlank(prId, "prId cannot be null or empty.");
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("repositories")
                .addPathSegment(workspaceSlug)
                .addPathSegment(repoUuid)
                .addPathSegment("pullrequests")
                .addPathSegment(prId)
                .addPathSegment("activity")
                .addQueryParameter("pagelen", PR_ACTIVITY_PAGE_LEN);
        if (Strings.isNotEmpty(cursor)) {
            urlBuilder.addQueryParameter("page", cursor);
        }
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request, BitbucketPaginatedResponse.ofType(objectMapper, BitbucketPullRequestActivity.class));
    }

    public BitbucketPaginatedResponse<BitbucketTag> getTags(String workspaceSlug, String repo, @Nullable String cursor) throws BitbucketClientException {
        Validate.notBlank(repo, "repo cannot be null or empty.");
        Validate.notBlank(workspaceSlug, "workspaceSlug cannot be null.");
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment("repositories")
                .addPathSegment(workspaceSlug)
                .addPathSegment(repo)
                .addPathSegment("refs")
                .addPathSegment("tags")
                .addQueryParameter("pagelen", PAGE_LENGTH);
        if (Strings.isNotEmpty(cursor)) {
            urlBuilder.addQueryParameter("page", cursor);
        }
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request, BitbucketPaginatedResponse.ofType(objectMapper, BitbucketTag.class));
    }

    public Stream<BitbucketTag> streamTags(String workspaceSlug, String repo, boolean ignoreErrors) {
        MutableInt page = new MutableInt(1);
        return PaginationUtils.stream("", cursor -> {
            int pageNb = page.getAndIncrement();
            try {
                log.debug("Bitbucket Tags repo={} cursor={}", repo, cursor);
                BitbucketPaginatedResponse<BitbucketTag> tags = getTags(workspaceSlug, repo, cursor);
                return PaginationUtils.CursorPageData.<BitbucketTag>builder()
                        .cursor(tags.extractNextPage().orElse(null))
                        .data(tags.getValues())
                        .build();
            } catch (BitbucketClientException e) {
                if (ignoreErrors) {
                    log.warn("Failed to get tags after page={} for repo={}. Ignoring.", pageNb, repo, e);
                    return PaginationUtils.CursorPageData.<BitbucketTag>builder()
                            .cursor(null)
                            .data(null)
                            .build();
                }
                throw new RuntimeStreamException("Failed to get tags after page=" + pageNb + " for " + repo, e);
            }
        });
    }
    //endregion
}
