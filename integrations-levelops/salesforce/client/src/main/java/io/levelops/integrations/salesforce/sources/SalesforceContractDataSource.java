package io.levelops.integrations.salesforce.sources;

import io.levelops.integrations.salesforce.client.SalesforceClientFactory;
import io.levelops.integrations.salesforce.models.Contract;
import io.levelops.integrations.salesforce.models.SalesforceIngestionQuery;
import io.levelops.integrations.salesforce.models.User;

/**
 * SalesForce's implementation of the {@link io.levelops.ingestion.sources.DataSource} to fetch Service Contract resources.
 */
public class SalesforceContractDataSource extends SalesforceDataSource<Contract> {

    public SalesforceContractDataSource(SalesforceClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public String getSOQLStatement(SalesforceIngestionQuery query) {
        String soqlQuery = "SELECT " + SOQLHelper.constructSOQLFields("", Contract.SOQL_FIELDS) + ", " +
                SOQLHelper.constructSOQLFields("ActivatedBy.", User.SOQL_FIELDS) + ", " +
                SOQLHelper.constructSOQLFields("CreatedBy.", User.SOQL_FIELDS) + ", " +
                SOQLHelper.constructSOQLFields("LastModifiedBy.", User.SOQL_FIELDS) + " FROM Contract";
        String whereClause = SOQLHelper.constructWhereClauseLastModifiedDate(query);
        if (whereClause != null) {
            soqlQuery = soqlQuery + " WHERE " + whereClause;
        }
        return soqlQuery;
    }

    @Override
    public Class<Contract> getType() {
        return Contract.class;
    }
}
