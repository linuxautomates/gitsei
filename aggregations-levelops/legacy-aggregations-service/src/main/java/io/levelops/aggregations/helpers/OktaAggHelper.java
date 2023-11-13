package io.levelops.aggregations.helpers;

import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.okta.DbOktaAssociation;
import io.levelops.commons.databases.models.database.okta.DbOktaGroup;
import io.levelops.commons.databases.models.database.okta.DbOktaUser;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.OktaAggService;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.integrations.okta.models.OktaGroup;
import io.levelops.integrations.okta.models.OktaUser;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Helper class for performing Okta aggregations
 */
@Log4j2
@Service
public class OktaAggHelper {

    private static final String GROUPS_DATA_TYPE = "groups";
    private static final String USERS_DATA_TYPE = "users";

    private final JobDtoParser jobDtoParser;
    private final OktaAggService oktaAggService;
    private final IntegrationTrackingService trackingService;

    @Autowired
    public OktaAggHelper(JobDtoParser jobDtoParser,
                         OktaAggService oktaAggService,
                         IntegrationTrackingService trackingService) {
        this.jobDtoParser = jobDtoParser;
        this.oktaAggService = oktaAggService;
        this.trackingService = trackingService;
    }

    public boolean insertGroups(String customer, Date currentTime, String integrationId, MultipleTriggerResults results) {
        return jobDtoParser.applyToResults(customer,
                GROUPS_DATA_TYPE,
                OktaGroup.class,
                results.getTriggerResults().get(0),
                group -> {
                    DbOktaGroup dbOktaGroup = DbOktaGroup.fromOktaGroup(group, integrationId, currentTime);
                    oktaAggService.insert(customer, dbOktaGroup);
                },
                List.of());
    }

    public boolean insertUsers(String customer, Date currentTime, String integrationId, MultipleTriggerResults results) {
        boolean result = jobDtoParser.applyToResults(customer,
                USERS_DATA_TYPE,
                OktaUser.class,
                results.getTriggerResults().get(0),
                user -> {
                    DbOktaUser dbUser = DbOktaUser.fromOktaUSer(user, integrationId, currentTime);
                    oktaAggService.insert(customer, dbUser);
                    if (user.getAssociatedMembers() != null) {
                        user.getAssociatedMembers().forEach(associatedMembers -> {
                            associatedMembers.getAssociatedMembers().forEach(memberId -> {
                                oktaAggService.insert(customer,
                                        DbOktaAssociation.fromOktaAssociation(user.getId(), memberId, associatedMembers, integrationId, currentTime));
                            });
                        });
                    }
                },
                List.of());
        if (result)
            trackingService.upsert(customer,
                    IntegrationTracker.builder()
                            .integrationId(integrationId)
                            .latestIngestedAt(io.levelops.commons.dates.DateUtils.truncate(currentTime, Calendar.DATE))
                            .build());
        return result;
    }
}
