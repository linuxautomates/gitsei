package io.levelops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PluginResult {

    @JsonProperty("id")
    private final String id; // UUID

    @JsonProperty("tool")
    private final String tool; // unique name of the tool or UUID if custom

    @JsonProperty("version")
    private final String version;

    @JsonProperty("product_id")
    private final String productId;

    @JsonProperty("successful")
    private final Boolean successful;

    @JsonProperty("metadata")
    private final Map<String, Object> metadata;

    @JsonProperty("gcs_path")
    private final String gcsPath;

    @JsonProperty("labels")
    private final Map<String, List<String>> labels;

    @JsonProperty("created_at")
    private final Date createdAt;

    public PluginResult(String id, String tool, String version, String productId, Boolean successful, Map<String, Object> metadata, String gcsPath, Map<String, List<String>> labels, Date createdAt) {
        this.id = id;
        this.tool = tool;
        this.version = version;
        this.productId = productId;
        this.successful = successful;
        this.metadata = metadata;
        this.gcsPath = gcsPath;
        this.labels = labels;
        this.createdAt = createdAt;
    }

    public static PluginResultBuilder builder() {
        return new PluginResultBuilder();
    }

    public String getId() {
        return id;
    }

    public String getTool() {
        return tool;
    }

    public String getVersion() {
        return version;
    }

    public String getProductId() {
        return productId;
    }

    public Boolean getSuccessful() {
        return successful;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public String getGcsPath() {
        return gcsPath;
    }

    public Map<String, List<String>> getLabels() {
        return labels;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PluginResult that = (PluginResult) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(tool, that.tool) &&
                Objects.equals(version, that.version) &&
                Objects.equals(productId, that.productId) &&
                Objects.equals(successful, that.successful) &&
                Objects.equals(metadata, that.metadata) &&
                Objects.equals(gcsPath, that.gcsPath) &&
                Objects.equals(labels, that.labels) &&
                Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tool, version, productId, successful, metadata, gcsPath, labels, createdAt);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("tool", tool)
                .append("version", version)
                .append("productId", productId)
                .append("successful", successful)
                .append("metadata", metadata)
                .append("gcsPath", gcsPath)
                .append("labels", labels)
                .append("createdAt", createdAt)
                .toString();
    }

    /**
     * Auto-Generated Builder with Lombok
     */
    public static class PluginResultBuilder {
        private String id;
        private String tool;
        private String version;
        private String productId;
        private Boolean successful;
        private Map<String, Object> metadata;
        private String gcsPath;
        private ArrayList<String> labels$key;
        private ArrayList<List<String>> labels$value;
        private Date createdAt;

        PluginResultBuilder() {
        }

        public PluginResultBuilder id(String id) {
            this.id = id;
            return this;
        }

        public PluginResultBuilder tool(String tool) {
            this.tool = tool;
            return this;
        }

        public PluginResultBuilder version(String version) {
            this.version = version;
            return this;
        }

        public PluginResultBuilder productId(String productId) {
            this.productId = productId;
            return this;
        }

        public PluginResultBuilder successful(Boolean successful) {
            this.successful = successful;
            return this;
        }

        public PluginResultBuilder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public PluginResultBuilder gcsPath(String gcsPath) {
            this.gcsPath = gcsPath;
            return this;
        }

        public PluginResultBuilder label(String labelKey, List<String> labelValue) {
            if (this.labels$key == null) {
                this.labels$key = new ArrayList<String>();
                this.labels$value = new ArrayList<List<String>>();
            }
            this.labels$key.add(labelKey);
            this.labels$value.add(labelValue);
            return this;
        }

        public PluginResultBuilder labels(Map<? extends String, ? extends List<String>> labels) {
            if (this.labels$key == null) {
                this.labels$key = new ArrayList<String>();
                this.labels$value = new ArrayList<List<String>>();
            }
            for (final Map.Entry<? extends String, ? extends List<String>> $lombokEntry : labels.entrySet()) {
                this.labels$key.add($lombokEntry.getKey());
                this.labels$value.add($lombokEntry.getValue());
            }
            return this;
        }

        public PluginResultBuilder clearLabels() {
            if (this.labels$key != null) {
                this.labels$key.clear();
                this.labels$value.clear();
            }
            return this;
        }

        public PluginResultBuilder createdAt(Date createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public PluginResult build() {
            Map<String, List<String>> labels;
            switch (this.labels$key == null ? 0 : this.labels$key.size()) {
                case 0:
                    labels = java.util.Collections.emptyMap();
                    break;
                case 1:
                    labels = java.util.Collections.singletonMap(this.labels$key.get(0), this.labels$value.get(0));
                    break;
                default:
                    labels = new java.util.LinkedHashMap<String, List<String>>(this.labels$key.size() < 1073741824 ? 1 + this.labels$key.size() + (this.labels$key.size() - 3) / 3 : Integer.MAX_VALUE);
                    for (int $i = 0; $i < this.labels$key.size(); $i++) {
                        labels.put(this.labels$key.get($i), (List<String>) this.labels$value.get($i));
                    }
                    labels = java.util.Collections.unmodifiableMap(labels);
            }

            return new PluginResult(id, tool, version, productId, successful, metadata, gcsPath, labels, createdAt);
        }

        public String toString() {
            return "PluginResult.PluginResultBuilder(id=" + this.id + ", tool=" + this.tool + ", version=" + this.version + ", productId=" + this.productId + ", successful=" + this.successful + ", metadata=" + this.metadata + ", gcsPath=" + this.gcsPath + ", labels$key=" + this.labels$key + ", labels$value=" + this.labels$value + ", createdAt=" + this.createdAt + ")";
        }
    }
}
