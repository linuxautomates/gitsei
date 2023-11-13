package io.levelops.commons.databases.models.database.scm;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class ScmPRPartial {
    private final UUID id;
    private final String mergeSha;
    private final List<String> commitShas;
}
