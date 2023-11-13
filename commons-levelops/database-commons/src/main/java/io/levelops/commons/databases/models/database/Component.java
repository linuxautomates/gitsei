package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.levelops.commons.models.ComponentType;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

/**
 * Represents a LevelOps component which correspond to an specific type.<br />
 * <br />
 * Examples: <br /><br />
 * <ul>
 *  <li>JIRA - type: integration, name: jira</li>
 *  <li>PRAETORIENA REPORT - type: plugin_result, name: praetorian_report</li>
 * </ul>
 */
@Value
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = Component.ComponentBuilderImpl.class)
public class Component {
    private UUID id;
    private String name;
    private ComponentType type;
    private List<Component> subComponents;
    
    @JsonPOJOBuilder(withPrefix = "")
    static final class ComponentBuilderImpl extends Component.ComponentBuilder {

    }
}