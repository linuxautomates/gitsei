package io.levelops.integrations.tenable.sources;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder(toBuilder = true)
class ChunkPageData {
    public static final String FINISHED = "FINISHED";

    String uuid;
    Integer chunkId;
    Boolean done;
    String status;
    List<Integer> chunksAvailable;
    List<Integer> chunksProcessed;
}