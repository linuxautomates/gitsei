package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

/**
 * Bean definition for zendesk brand (https://developer.zendesk.com/rest_api/docs/support/brands)
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Brand.BrandBuilder.class)
public class Brand {

    @JsonProperty
    String url;

    @JsonProperty
    Long id;

    @JsonProperty
    String name;

    @JsonProperty("brand_url")
    String brandUrl;

    @JsonProperty("has_help_center")
    Boolean hasHelpCenter;

    @JsonProperty("help_center_state")
    String helpCenterState;

    @JsonProperty
    Boolean active;

    @JsonProperty("default_brand")
    Boolean defaultBrand;

    @JsonProperty
    RequestComment.Attachment logo;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("updated_at")
    Date updatedAt;

    @JsonProperty
    String subdomain;

    @JsonProperty("host_mapping")
    String hostMapping;

    @JsonProperty("signature_template")
    String signatureTemplate;

}
