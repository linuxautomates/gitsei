package io.levelops.commons.databases.models.database.kudos;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
public class DBKudos {
    UUID id;
    UUID dashboardId;
    UUID screenshotId;
    String level;
    String author;
    String type;
    String icon;
    String breadcrumbs;
    Boolean anonymousLink;
    Instant expiration;
    String body;
    Boolean includeWidgetDetails;
    Instant createdAt;
    Instant updatedAt;
    Set<DBKudosWidget> widgets;
    Set<DBKudosSharing> sharings;
}
