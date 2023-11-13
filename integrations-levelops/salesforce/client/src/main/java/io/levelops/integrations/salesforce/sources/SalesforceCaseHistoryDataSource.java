package io.levelops.integrations.salesforce.sources;

import io.levelops.integrations.salesforce.client.SalesforceClientFactory;
import io.levelops.integrations.salesforce.models.CaseHistory;
import io.levelops.integrations.salesforce.models.SalesforceIngestionQuery;

/**
 * SalesForce's implementation of the {@link io.levelops.ingestion.sources.DataSource} to fetch Case History resources.
 */
public class SalesforceCaseHistoryDataSource extends SalesforceDataSource<CaseHistory> {

    public SalesforceCaseHistoryDataSource(SalesforceClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public String getSOQLStatement(SalesforceIngestionQuery query) {
        String soqlQuery =  "SELECT " + SOQLHelper.constructSOQLFields("", CaseHistory.SOQL_FIELDS) + " FROM CaseHistory";
        String whereClause = SOQLHelper.constructWhereClauseCreatedDate(query);
        if (whereClause != null) {
            soqlQuery = soqlQuery + " WHERE " + whereClause;
        }
        return soqlQuery;
    }

    @Override
    public Class<CaseHistory> getType() {
        return CaseHistory.class;
    }
}
