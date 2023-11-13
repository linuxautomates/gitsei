package io.levelops.integrations.salesforce.sources;

import io.levelops.integrations.salesforce.client.SalesforceClientFactory;
import io.levelops.integrations.salesforce.models.Case;
import io.levelops.integrations.salesforce.models.SalesforceIngestionQuery;
import io.levelops.integrations.salesforce.models.User;

/**
 * SalesForce's implementation of the {@link io.levelops.ingestion.sources.DataSource} to fetch Case resources.
 */
public class SalesforceCaseDataSource extends SalesforceDataSource<Case> {

    public SalesforceCaseDataSource(SalesforceClientFactory clientFactory) {
        super(clientFactory);
    }

    public String getSOQLStatement(SalesforceIngestionQuery query) {
        String soqlQuery = "SELECT " + SOQLHelper.constructSOQLFields("", Case.SOQL_FIELDS) + ", " +
//                SOQLHelper.constructSOQLFields("Contact.", Contact.SOQL_FIELDS) + ", " +
                SOQLHelper.constructSOQLFields("CreatedBy.", User.SOQL_FIELDS) + ", " +
                SOQLHelper.constructSOQLFields("LastModifiedBy.", User.SOQL_FIELDS) +
                " FROM Case";
        String whereClause = SOQLHelper.constructWhereClauseLastModifiedDate(query);
        if (whereClause != null) {
            soqlQuery = soqlQuery + " WHERE " + whereClause;
        }
        return soqlQuery;
    }

    @Override
    public Class<Case> getType() {
        return Case.class;
    }
}
