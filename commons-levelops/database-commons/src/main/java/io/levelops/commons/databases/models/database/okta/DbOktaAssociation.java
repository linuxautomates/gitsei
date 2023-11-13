package io.levelops.commons.databases.models.database.okta;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.okta.models.AssociatedMembers;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbOktaAssociation.DbOktaAssociationBuilder.class)
public class DbOktaAssociation {

    @JsonProperty
    String id;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("primary_id")
    String primaryId;

    @JsonProperty("primary_name")
    String primaryName;

    @JsonProperty("primary_title")
    String primaryTitle;

    @JsonProperty("primary_description")
    String primaryDescription;

    @JsonProperty("associated_id")
    String associatedId;

    @JsonProperty("associated_name")
    String associatedName;

    @JsonProperty("associated_title")
    String associatedTitle;

    @JsonProperty("associated_description")
    String associatedDescription;

    @JsonProperty
    Date lastUpdatedAt;

    public static DbOktaAssociation fromOktaAssociation(String primaryId,
                                                        String associatedId,
                                                        AssociatedMembers associatedMembers,
                                                        String integrationId,
                                                        Date currentTime) {
        return DbOktaAssociation.builder()
                .integrationId(integrationId)
                .primaryId(primaryId)
                .primaryName(associatedMembers.getPrimaryName())
                .primaryDescription(associatedMembers.getPrimaryDescription())
                .primaryTitle(associatedMembers.getPrimaryTitle())
                .associatedId(associatedId)
                .associatedDescription(associatedMembers.getAssociatedDescription())
                .associatedName(associatedMembers.getAssociatedName())
                .associatedTitle(associatedMembers.getAssociatedTitle())
                .lastUpdatedAt(currentTime)
                .build();
    }
}
