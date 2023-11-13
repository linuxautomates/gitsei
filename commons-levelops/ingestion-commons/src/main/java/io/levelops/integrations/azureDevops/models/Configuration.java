package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Configuration.ConfigurationBuilder.class)
public class Configuration {

    @JsonProperty("designerJson")
    DesignerJson designerJson;

    @JsonProperty("variables")
    Map<String, Variable> variables;

    @JsonProperty("path")
    String path;

    @JsonProperty("repository")
    Repository repository;

    @JsonProperty("type")
    String type;

    @Value
    @Builder
    @JsonDeserialize(builder = Repository.RepositoryBuilder.class)
    public static class Repository {

        @JsonProperty("fullName")
        String fullName;

        @JsonProperty("connection")
        Connection connection;

        @JsonProperty("type")
        String type;

        @JsonProperty("id")
        String id;

        @Value
        @Builder
        @JsonDeserialize(builder = Connection.ConnectionBuilder.class)
        public static class Connection {
            @JsonProperty("id")
            String id;
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Variable.VariableBuilder.class)
    public static class Variable {

        @JsonProperty("isSecret")
        Boolean isSecret;

        @JsonProperty("value")
        String value;

    }

    @Value
    @Builder
    @JsonDeserialize(builder = DesignerJson.DesignerJsonBuilder.class)
    public static class DesignerJson {

        @JsonProperty("repository")
        Repository repository;

        @JsonProperty("variables")
        Map<String, Variable> variables;

    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = VariableGroup.VariableGroupBuilder.class)
    public static class VariableGroup {

        @JsonProperty("variables")
        Map<String, Configuration.Variable> variables;

        @JsonProperty("type")
        String type;

        @JsonProperty("id")
        Integer id;

        @JsonProperty("name")
        String name;

        @JsonProperty("description")
        String description;

        @JsonProperty("isShared")
        Boolean isShared;

    }

}
