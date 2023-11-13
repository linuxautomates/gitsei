package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Bean describing a ticket from https://developer.zendesk.com/rest_api/docs/support/tickets#json-format
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Ticket.TicketBuilder.class)
public class Ticket {

    @JsonProperty("id")
    Long id;

    @JsonProperty("url")
    String url;

    @JsonProperty("external_id")
    String externalId;

    @JsonProperty("type")
    String type;

    @JsonProperty("subject")
    String subject;

    @JsonProperty("raw_subject")
    String rawSubject;

    @JsonProperty("description")
    String description;

    @JsonProperty("priority")
    String priority;

    @JsonProperty("status")
    String status;

    @JsonProperty("recipient")
    String recipient;

    @JsonProperty("requester_id")
    Long requesterId;

    @JsonProperty("brand_id")
    Long brandId;

    @JsonProperty
    Brand brand;

    @JsonProperty
    User requester;

    @JsonProperty("submitter_id")
    Long submitterId;

    @JsonProperty
    User submitter;

    @JsonProperty("assignee_id")
    Long assigneeId;

    @JsonProperty
    User assignee;

    @JsonProperty("organization_id")
    Long organizationId;

    @JsonProperty
    Organization organization;

    @JsonProperty("group_id")
    Long groupId;

    @JsonProperty
    Group group;

    @JsonProperty("collaborator_ids")
    List<Long> collaboratorIds;

    @JsonProperty
    List<User> collaborators;

    @JsonProperty("email_cc_ids")
    List<Long> emailCCIds;

    @JsonProperty
    List<User> emailCCs;

    @JsonProperty("follower_ids")
    List<Long> followerIds;

    @JsonProperty
    List<Map<String, String>> followers;

    @JsonProperty("ticket_followers")
    List<User> ticketFollowers;

    @JsonProperty("forum_topic_id")
    Long forumTopicId;

    @JsonProperty("problem_id")
    Long problemId;

    @JsonProperty("has_incidents")
    Boolean hasIncidents;

    @JsonProperty("due_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    Date dueAt;

    @JsonProperty("tags")
    List<String> tags;

    @JsonProperty("via")
    Via via;

    @JsonProperty("custom_fields")
    List<ZendeskField> customFields;

    @JsonProperty("fields")
    List<ZendeskField> fields;

    @JsonProperty("satisfaction_rating")
    SatisfactionRating satisfactionRating;

    @JsonProperty("sharing_agreement_ids")
    List<Long> sharingAgreementIds;

    @JsonProperty("follow_up_ids")
    List<Long> followUpIds;

    @JsonProperty("ticket_form_id")
    Long ticketFormId;

    @JsonProperty("allow_channelback")
    Boolean allowChannelBack;

    @JsonProperty("is_public")
    Boolean isPublic;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("updated_at")
    Date updatedAt;

    @JsonProperty("jira_links")
    List<JiraLink> jiraLinks; //enriched

    @JsonProperty("request_attributes")
    RequestAttributes requestAttributes; //enriched

    @JsonProperty("request_comments")
    List<RequestComment> requestComments; //enriched

    @JsonProperty("ticket_metric")
    TicketMetric ticketMetric;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = SatisfactionRating.SatisfactionRatingBuilder.class)
    public static class SatisfactionRating {

        @JsonProperty
        Long id;

        @JsonProperty
        String score;

        @JsonProperty
        String comment;

    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Via.ViaBuilder.class)
    public static class Via {

        @JsonProperty("channel")
        String channel;

        @JsonProperty("source")
        Source source;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = Source.SourceBuilder.class)
        static class Source {

            @JsonProperty
            To to;

            @JsonProperty
            From from;

            String rel;

            @Value
            @Builder(toBuilder = true)
            @JsonDeserialize(builder = To.ToBuilder.class)
            static class To {

                @JsonProperty
                String address;

                @JsonProperty
                String name;

                @JsonProperty("profile_url")
                String profileUrl;

                @JsonProperty("username")
                String userName;

                @JsonProperty
                String phone;

                @JsonProperty("formatted_phone")
                String formattedPhone;

                @JsonProperty("facebook_id")
                String facebookId;
            }

            @Value
            @Builder(toBuilder = true)
            @JsonDeserialize(builder = From.FromBuilder.class)
            static class From {

                @JsonProperty
                String address;

                @JsonProperty
                String name;

                @JsonProperty("original_recipients")
                String originalRecipients;

                @JsonProperty("ticket_id")
                Long ticketId;

                @JsonProperty
                String subject;

                @JsonProperty
                String id;

                @JsonProperty
                String title;

                @JsonProperty
                String deleted;

                @JsonProperty("revision_id")
                String revisionId;

                @JsonProperty("topic_id")
                String topicId;

                @JsonProperty("topic_name")
                String topicName;

                @JsonProperty("profile_url")
                String profileUrl;

                @JsonProperty("username")
                String userName;

                @JsonProperty
                String phone;

                @JsonProperty("formatted_phone")
                String formattedPhone;

                @JsonProperty("facebook_id")
                String facebookId;

                @JsonProperty("service_info")
                String serviceInfo;

                @JsonProperty("supports_channelback")
                String supportsChannelBack;

                @JsonProperty("supports_clickthrough")
                String supportsClickThrough;

                @JsonProperty("registered_integration_service_name")
                String registeredIntegrationServiceName;
            }
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = RequestAttributes.RequestAttributesBuilder.class)
    public static class RequestAttributes {

        @JsonProperty("can_be_solved_by_me")
        Boolean canBeSolvedByMe;

        @JsonProperty("followup_source_id")
        Long followUpSourceId;

    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ZendeskField.ZendeskFieldBuilder.class)
    public static class ZendeskField {
        @JsonProperty("id")
        Long id;

        @JsonProperty("value")
        Object value;
    }

}
