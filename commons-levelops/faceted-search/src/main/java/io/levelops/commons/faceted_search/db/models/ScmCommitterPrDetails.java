package io.levelops.commons.faceted_search.db.models;

import lombok.Builder;
import lombok.Value;

import java.util.List;
@Value
@Builder(toBuilder = true)
public class ScmCommitterPrDetails {

    private String commitId;

    private List<String> prIds;

    private List<String> jiraIssues;

    private List<String> workItems;
}
