package io.levelops.integrations.github.services;

import io.levelops.integrations.github.client.GithubClient;
import io.levelops.integrations.github.models.GithubTag;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.stream.Collectors;

@Log4j2
public class GithubTagService {

    public List<GithubTag> getTags(GithubClient client, String owner, String repos, int perPage) {
            return client.streamTags(owner, repos, perPage).collect(Collectors.toList());
    }
}