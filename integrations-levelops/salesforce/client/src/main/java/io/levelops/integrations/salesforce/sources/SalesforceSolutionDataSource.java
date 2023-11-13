package io.levelops.integrations.salesforce.sources;

import io.levelops.integrations.salesforce.client.SalesforceClientFactory;
import io.levelops.integrations.salesforce.models.SalesforceIngestionQuery;
import io.levelops.integrations.salesforce.models.Solution;
import io.levelops.integrations.salesforce.models.User;
import lombok.extern.log4j.Log4j2;

/**
 * SalesForce's implementation of the {@link io.levelops.ingestion.sources.DataSource} to fetch Data resources.
 */
@Log4j2
public class SalesforceSolutionDataSource extends SalesforceDataSource<Solution> {

    public SalesforceSolutionDataSource(SalesforceClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public String getSOQLStatement(SalesforceIngestionQuery query) {
        String soqlQuery = "SELECT " + SOQLHelper.constructSOQLFields("", Solution.SOQL_FIELDS) + ", " +
                SOQLHelper.constructSOQLFields("CreatedBy.", User.SOQL_FIELDS) + ", " +
                SOQLHelper.constructSOQLFields("LastModifiedBy.", User.SOQL_FIELDS) + " FROM Solution";
        String whereClause = SOQLHelper.constructWhereClauseLastModifiedDate(query);
        if (whereClause != null) {
            soqlQuery = soqlQuery + " WHERE " + whereClause;
        }
        return soqlQuery;
    }

    @Override
    public Class<Solution> getType() {
        return Solution.class;
    }
}
