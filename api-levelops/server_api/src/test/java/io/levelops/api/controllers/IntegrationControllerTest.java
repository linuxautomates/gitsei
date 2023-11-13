package io.levelops.api.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;
import org.assertj.core.api.Assertions;
import java.time.Instant;

public class IntegrationControllerTest {

    private static String configStr = "{\n" +
            "  \"integration_id\": \"5\",\n" +
            "  \"custom_hygienes\": [\n" +
            "    {\n" +
            "      \"id\": \"3505ad50-f8e0-11ec-b2f8-651fb990fbb3\",\n" +
            "      \"name\": \"Missing Story Points\",\n" +
            "      \"missing_fields\": {\n" +
            "        \"customfield_10014\": true\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"43e533e0-f8e0-11ec-b2f8-651fb990fbb3\",\n" +
            "      \"name\": \"Missing Acceptance Criteria\",\n" +
            "      \"missing_fields\": {\n" +
            "        \"customfield_10070\": true\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"config\": {\n" +
            "    \"epic_field\": [\n" +
            "      {\n" +
            "        \"key\": \"customfield_10008\",\n" +
            "        \"name\": \"Epic Link\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"agg_custom_fields\": [\n" +
            "      {\n" +
            "        \"key\": \"customfield_10014\",\n" +
            "        \"name\": \"Story Points\"\n" +
            "      },\n" +
            "      {\n" +
            "        \"key\": \"customfield_10302\",\n" +
            "        \"name\": \"Category\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"salesforce_fields\": [],\n" +
            "    \"story_points_field\": [\n" +
            "      {\n" +
            "        \"key\": \"customfield_10014\",\n" +
            "        \"name\": \"story points field\"\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

    private static String existingConfigStr = "{\n" +
            "  \"integration_id\": \"5\",\n" +
            "  \"custom_hygienes\": [\n" +
            "    {\n" +
            "      \"id\": \"3505ad50-f8e0-11ec-b2f8-651fb990fbb3\",\n" +
            "      \"name\": \"Missing Story Points\",\n" +
            "      \"missing_fields\": {\n" +
            "        \"customfield_10014\": true\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"43e533e0-f8e0-11ec-b2f8-651fb990fbb3\",\n" +
            "      \"name\": \"Missing Acceptance Criteria\",\n" +
            "      \"missing_fields\": {\n" +
            "        \"customfield_10070\": true\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"config\": {\n" +
            "    \"epic_field\": [\n" +
            "      {\n" +
            "        \"key\": \"customfield_10008\",\n" +
            "        \"name\": \"Epic Link\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"agg_custom_fields\": [\n" +
            "      {\n" +
            "        \"key\": \"customfield_10014\",\n" +
            "        \"name\": \"Story Points\"\n" +
            "      }\n" +
            "    ],\n" +
            "    \"salesforce_fields\": [],\n" +
            "    \"story_points_field\": [\n" +
            "      {\n" +
            "        \"key\": \"customfield_10014\",\n" +
            "        \"name\": \"story points field\"\n" +
            "      }\n" +
            "    ]\n" +
            "  },\n" +
            "  \"metadata\": {\n" +
            "    \"config_updated_at\": 1689907043\n" +
            "  }\n" +
            "}";

    @Test
    public void test1() throws JsonProcessingException {
        ObjectMapper mapper = DefaultObjectMapper.get();
        IntegrationConfigsController integrationConfigsController = new IntegrationConfigsController(null);

        IntegrationConfig config = mapper.readValue(configStr, IntegrationConfig.class);
        IntegrationConfig existingConfig = mapper.readValue(existingConfigStr, IntegrationConfig.class);

        long now = Instant.now().getEpochSecond();

        Long customFieldsUpdatedAt = integrationConfigsController.getNewConfigUpdatedAt(now, existingConfig, config);
        Assertions.assertThat(customFieldsUpdatedAt).isNotNull();
        Assertions.assertThat(customFieldsUpdatedAt).isEqualTo(now);

        IntegrationConfig existingConfig1 = mapper.readValue(existingConfigStr, IntegrationConfig.class);

        customFieldsUpdatedAt = integrationConfigsController.getNewConfigUpdatedAt(now, existingConfig, existingConfig1);
        Assertions.assertThat(customFieldsUpdatedAt).isNotNull();
        Assertions.assertThat(customFieldsUpdatedAt).isEqualTo(1689907043);

        // if the old version is missing, it should get updated:
        IntegrationConfig existingConfig2 = existingConfig1.toBuilder()
                .metadata(null)
                .build();
        customFieldsUpdatedAt = integrationConfigsController.getNewConfigUpdatedAt(now, existingConfig2, existingConfig1);
        Assertions.assertThat(customFieldsUpdatedAt).isNotNull();
        Assertions.assertThat(customFieldsUpdatedAt).isEqualTo(now);
    }
}

