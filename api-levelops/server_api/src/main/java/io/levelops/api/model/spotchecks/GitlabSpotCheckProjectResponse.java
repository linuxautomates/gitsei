package io.levelops.api.model.spotchecks;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.*;
import io.levelops.commons.models.ExceptionPrintout;
import io.levelops.integrations.gitlab.models.GitlabProject;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

import static io.levelops.api.model.spotchecks.DateUtils.gitlabDateToString;

@Value
@Builder(toBuilder = true)
public class GitlabSpotCheckProjectResponse {
    @JsonProperty("projects")
    List<GitlabSpotCheckProject> projects;

    @JsonProperty("limit")
    Integer limit;

    @JsonProperty("project_not_found")
    Boolean projectNotFound;

    @JsonProperty("error")
    ExceptionPrintout error;


}
