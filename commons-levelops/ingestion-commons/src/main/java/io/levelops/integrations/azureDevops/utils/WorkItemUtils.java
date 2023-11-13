package io.levelops.integrations.azureDevops.utils;

import io.levelops.commons.dates.DateUtils;
import io.levelops.integrations.azureDevops.models.FieldUpdate;
import io.levelops.integrations.azureDevops.models.WorkItemHistory;

import java.sql.Timestamp;

public class WorkItemUtils {

    public static final String FUTURE = "9999-01-01T00:00:00Z";

    public static String getChangedDateFromHistory(WorkItemHistory workItemHistory) {
        FieldUpdate fieldUpdate = workItemHistory.getFields();
        if (fieldUpdate != null) {
            if (fieldUpdate.getChangedDate() != null) {
                return fieldUpdate.getChangedDate().getNewValue();
            }
        }
        return workItemHistory.getRevisedDate();
    }

    public static Timestamp getChangedDateFromHistoryAsTimestamp(WorkItemHistory workItemHistory) {
        String dateFromHistory = getChangedDateFromHistory(workItemHistory);
        return FUTURE.equals(dateFromHistory) ? null : Timestamp.from(DateUtils.parseDateTime(dateFromHistory));
    }
}
