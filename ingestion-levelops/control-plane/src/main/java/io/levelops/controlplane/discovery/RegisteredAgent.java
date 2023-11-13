package io.levelops.controlplane.discovery;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.levelops.ingestion.models.AgentHandle;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Duration;
import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
public class RegisteredAgent {
    @Setter
    @JsonUnwrapped
    private AgentHandle agentHandle;

    @Setter
    @JsonProperty("last_heartbeat")
    private Instant lastHeartbeat;

    @JsonProperty("last_heartbeat_since")
    long getLastHeartbeatSince() {
        if (lastHeartbeat == null) {
            return -1;
        }
        return Duration.between(lastHeartbeat, Instant.now()).toSeconds();
    }
}