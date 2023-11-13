package io.levelops.etl.jobs.gitlab;

import io.levelops.commons.databases.models.database.scm.DbScmUser;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class GitlabState {
    private List<DbScmUser> users;
}
