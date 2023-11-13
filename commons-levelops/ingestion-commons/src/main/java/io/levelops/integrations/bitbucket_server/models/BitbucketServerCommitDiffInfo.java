package io.levelops.integrations.bitbucket_server.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketServerCommitDiffInfo.BitbucketServerCommitDiffInfoBuilder.class)
public class BitbucketServerCommitDiffInfo {

    @JsonProperty("fromHash")
    String fromHash;

    @JsonProperty("toHash")
    String toHash;

    @JsonProperty("contextLines")
    Integer contextLines;

    @JsonProperty("whiteSpace")
    String whiteSpace;

    @JsonProperty("diffs")
    List<Diff> diffs;

    @JsonProperty("truncated")
    Boolean truncated;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = BitbucketServerCommitDiffInfo.Diff.DiffBuilder.class)
    public static class Diff {

        @JsonProperty("source")
        FileReference source;

        @JsonProperty("destination")
        FileReference destination;

        @JsonProperty("hunks")
        List<Hunk> hunks;

        @JsonProperty("fromHash")
        String fromHash;

        @JsonProperty("toHash")
        String toHash;

        @JsonProperty("contextLines")
        Integer contextLines;

        @JsonProperty("whiteSpace")
        String whiteSpace;

        @JsonProperty("truncated")
        Boolean truncated;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = FileReference.FileReferenceBuilder.class)
        public static class FileReference {

            @JsonProperty("components")
            List<String> components;

            @JsonProperty("parent")
            String parent;

            @JsonProperty("name")
            String name;

            @JsonProperty("toString")
            String toString;

            @JsonProperty("extension")
            String extension;
        }

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = BitbucketServerCommitDiffInfo.Diff.Hunk.HunkBuilder.class)
        public static class Hunk {

            @JsonProperty("sourceLine")
            Integer sourceLine;

            @JsonProperty("sourceSpan")
            Integer sourceSpan;

            @JsonProperty("destinationLine")
            Integer destinationLine;

            @JsonProperty("destinationSpan")
            Integer destinationSpan;

            @JsonProperty("segments")
            List<Segment> segments;

            @JsonProperty("truncated")
            Boolean truncated;

            @Value
            @Builder(toBuilder = true)
            @JsonDeserialize(builder = BitbucketServerCommitDiffInfo.Diff.Hunk.Segment.SegmentBuilder.class)
            public static class Segment {

                @JsonProperty("type")
                String type;

                @JsonProperty("lines")
                List<Line> lines;

                @JsonProperty("truncated")
                Boolean truncated;

                @Value
                @Builder(toBuilder = true)
                @JsonDeserialize(builder = BitbucketServerCommitDiffInfo.Diff.Hunk.Segment.Line.LineBuilder.class)
                public static class Line { // We do not parse the fields that contain source code

                    @JsonProperty("destination")
                    Integer destination;

                    @JsonProperty("source")
                    Integer source;

                    @JsonProperty("truncated")
                    Boolean truncated;
                }
            }
        }
    }
}
