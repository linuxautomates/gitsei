package io.levelops.integrations.snyk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SnykDepGraph.SnykDepGraphBuilder.class)
public class SnykDepGraph {
    @JsonProperty("org_id")
    private final String orgId;
    @JsonProperty("project_id")
    private final String projectId;

    @JsonProperty("schemaVersion")
    private final String schemaVersion;
    @JsonProperty("pkgManager")
    private final PkgManager pkgManager;
    @JsonProperty("pkgs")
    private final List<Pkg> pkgs;
    @JsonProperty("graph")
    private final Graph graph;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = PkgManager.PkgManagerBuilder.class)
    public static final class PkgManager{
        @JsonProperty("name")
        private final String name;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Pkg.PkgBuilder.class)
    public static final class Pkg{
        @JsonProperty("id")
        private final String id;
        @JsonProperty("info")
        private final PkgInfo info;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = PkgInfo.PkgInfoBuilder.class)
    public static final class PkgInfo{
        @JsonProperty("name")
        private final String name;
        @JsonProperty("version")
        private final String version;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Graph.GraphBuilder.class)
    public static final class Graph{
        @JsonProperty("rootNodeId")
        private final String rootNodeId;
        @JsonProperty("nodes")
        private final List<Node> nodes;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Node.NodeBuilder.class)
    public static final class Node{
        @JsonProperty("nodeId")
        private final String nodeId;
        @JsonProperty("pkgId")
        private final String pkgId;
        @JsonProperty("deps")
        private final List<NodeDeps> deps;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = NodeDeps.NodeDepsBuilder.class)
    public static final class NodeDeps{
        @JsonProperty("nodeId")
        private final String nodeId;
    }
}
