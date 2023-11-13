package io.levelops.integrations.bitbucket.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BitbucketTeam.BitbucketTeamBuilder.class)
public class BitbucketTeam {

    @JsonProperty("username")
    String username;
    @JsonProperty("display_name")
    String displayName;
    @JsonProperty("uuid")
    String uuid;
    @JsonProperty("created_on")
    String createdOn;
    @JsonProperty("type")
    String type;
    @JsonProperty("has_2fa_enabled")
    Boolean has2faEnabled;

    /*
        "links": {
            "hooks": {
                "href": "https://api.bitbucket.org/2.0/teams/%7B64713cde-a3e0-4413-95ef-22a43584c9b2%7D/hooks"
            },
            "self": {
                "href": "https://api.bitbucket.org/2.0/teams/%7B64713cde-a3e0-4413-95ef-22a43584c9b2%7D"
            },
            "repositories": {
                "href": "https://api.bitbucket.org/2.0/repositories/%7B64713cde-a3e0-4413-95ef-22a43584c9b2%7D"
            },
            "html": {
                "href": "https://bitbucket.org/%7B64713cde-a3e0-4413-95ef-22a43584c9b2%7D/"
            },
            "avatar": {
                "href": "https://bitbucket.org/account/levelopsteam/avatar/"
            },
            "members": {
                "href": "https://api.bitbucket.org/2.0/teams/%7B64713cde-a3e0-4413-95ef-22a43584c9b2%7D/members"
            },
            "projects": {
                "href": "https://api.bitbucket.org/2.0/teams/%7B64713cde-a3e0-4413-95ef-22a43584c9b2%7D/projects/"
            },
            "snippets": {
                "href": "https://api.bitbucket.org/2.0/snippets/%7B64713cde-a3e0-4413-95ef-22a43584c9b2%7D"
            }
        },
     */
}
