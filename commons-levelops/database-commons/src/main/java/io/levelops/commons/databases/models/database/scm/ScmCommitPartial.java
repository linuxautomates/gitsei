package io.levelops.commons.databases.models.database.scm;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class ScmCommitPartial {
    private final UUID id;
    private final String commitSha;


}
