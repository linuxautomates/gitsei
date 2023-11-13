package io.levelops.integrations.github.services;

import io.levelops.commons.client.oauth.OauthTokenInterceptor;
import io.levelops.commons.client.oauth.StaticOauthTokenProvider;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.github.model.GithubConverters;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.IssueEvent;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.EventService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class GithubIntegrationTest {

    private GitHubClient client;
    private RepositoryService repositoryService;
    private CommitService commitService;
    private EventService eventService;
    private PullRequestService pullRequestService;

    @Before
    public void setUp() throws Exception {
        client = new GitHubClient();
        client.setOAuth2Token(System.getenv("GITHUB_API_KEY"));
        repositoryService = new RepositoryService(client);
        commitService = new CommitService(client);
        eventService = new EventService(client);
        pullRequestService = new PullRequestService(client);
    }

    @Test
    public void test() throws IOException {
        System.out.println(HttpUrl.parse("http://test/").newBuilder().addPathSegments("a/b/c").toString());
        List<Repository> repositories = repositoryService.getRepositories();

        System.out.println("Repos found: " + repositories.size());

        repositories.forEach(r -> {
            System.out.println(" * * * ");
            System.out.println(r.getName());
            System.out.println(r.getId());
            System.out.println(r.generateId());
            System.out.println(r.getGitUrl());
            System.out.println(r.getCloneUrl());
            System.out.println(r.getMasterBranch());
            System.out.println(r.getOwner().getName());
            System.out.println(r.getOpenIssues());
            System.out.println(r.getUpdatedAt());

            try {
                repositoryService.getBranches(r).forEach(b -> {
                    System.out.println(" > " + b.getName() + ", " + b.getCommit().getSha());
                });

//                StreamSupport.stream(eventService.pageEvents(r).spliterator(), false)
//                        .flatMap(Collection::stream)
//                        .forEach(DefaultObjectMapper::prettyPrint);
//
                pullRequestService.getPullRequests(r, "").forEach(pr -> {
                    DefaultObjectMapper.prettyPrint("PR " + pr.getTitle());
                    if (pr.getTitle().equals("LEV-450 Json Diff")) {
//                       DefaultObjectMapper.prettyPrint(pr);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
    }

    @Test
    public void testIssues() {
        Map<String, String> params = Map.of(
                "since", Instant.now().minus(10, ChronoUnit.DAYS).toString(),
                "direction", "asc",
                "sort", "updated");
        var l = StreamSupport.stream(new IssueService(client).pageIssues(new RepositoryId("maxime-levelops", "tmp-private"), params).spliterator(), false)
                .peek(System.out::println)
                .flatMap(Collection::stream)
                .filter(issue -> issue.getUpdatedAt() != null && issue.getUpdatedAt().toInstant().isBefore(Instant.now()))
                .takeWhile(issue -> issue.getUpdatedAt() != null && issue.getUpdatedAt().toInstant().isAfter(Instant.now().minus(8, ChronoUnit.DAYS)))
                .collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(l);
    }

    @Test
    public void commits() throws IOException {
//        Repository r = new RepositoryService(client).getRepository("hashicorp", "vault");
//        DefaultObjectMapper.prettyPrint(r.getOwner());
//        if (false) {
//            PageIterator<IssueEvent> o = new IssueService(client).pageEvents("maxime-levelops", "tmp-private");
//            PageIterator<IssueEvent> o = new IssueService(client).pageEvents("hashicorp", "vault");
//            o.forEach(l -> l.forEach(x -> System.out.println(String.format("issue=%d, event_id=%s, type=%s, login=%s, ts=%s", x.getIssue().getId(), x.getId(), x.getEvent(), x.getActor().getLogin(), x.getCreatedAt()))));
//
//        }
        Issue i = new IssueService(client).getIssue("levelops", "commons-levelops", 757);
        DefaultObjectMapper.prettyPrint(i);
        DefaultObjectMapper.prettyPrint(i.getAssignee());

        if(true)return;


        RepositoryCommit commit = new CommitService(client)
                .getCommit(RepositoryId.create("levelops", "api-levelops"), "c72176cec4e85c05042b5d22604f643963b81a8b");
        DefaultObjectMapper.prettyPrint(GithubConverters.parseGithubCommit(commit));

        commit = new CommitService(client)
                .getCommit(RepositoryId.create("levelops", "api-levelops"), "c72176cec4e85c05042b5d22604f643963b81a8b");
        DefaultObjectMapper.prettyPrint(GithubConverters.parseGithubCommit(commit));
        DefaultObjectMapper.prettyPrint(commit);
    }

    @Test
    public void paged() {
        PageIterator<Repository> it = repositoryService.pageRepositories(0, 100);

        DefaultObjectMapper.prettyPrint(it);
    }

    @Test
    public void search() throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .addInterceptor(new OauthTokenInterceptor(StaticOauthTokenProvider.builder().token(System.getenv("GITHUB_API_KEY")).build()))
                .build();

        Call call = client.newCall(new Request.Builder()
                .url(HttpUrl.parse("https://api.github.com/search/code").newBuilder()
                        .addQueryParameter("q", "RequestMapping repo:levelops/api-levelops")
                        .build())
                .get()
                .build());
        Response response = call.execute();
        DefaultObjectMapper.prettyPrint(DefaultObjectMapper.get().readValue(response.body().string(), Map.class));
    }
}