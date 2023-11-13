package io.levelops.integrations.salesforce.sources;

import io.levelops.integrations.salesforce.models.SalesforceIngestionQuery;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class SOQLHelper {

    public static String constructSOQLFields(String prefix, List<String> fields) {
        return fields.stream().map(val -> prefix + val).collect(Collectors.joining(", "));
    }

    public static String constructWhereClauseLastModifiedDate(SalesforceIngestionQuery query) {
        if (query.getPartial()) {
            return "LastModifiedDate > " + SOQLHelper.getSOQLFormatDate(query.getFrom()) + " AND " +
                    "LastModifiedDate < " + SOQLHelper.getSOQLFormatDate(query.getTo());
        }
        return null;
    }

    public static String constructWhereClauseCreatedDate(SalesforceIngestionQuery query) {
        if (query.getPartial()) {
            return "CreatedDate > " + SOQLHelper.getSOQLFormatDate(query.getFrom()) + " AND " +
                    "CreatedDate < " + SOQLHelper.getSOQLFormatDate(query.getTo());
        }
        return null;
    }

    public static String getSOQLFormatDate(Long timeInMillisecond) {
        if (timeInMillisecond != null) {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date(timeInMillisecond));
        }
        return null;
    }
}
