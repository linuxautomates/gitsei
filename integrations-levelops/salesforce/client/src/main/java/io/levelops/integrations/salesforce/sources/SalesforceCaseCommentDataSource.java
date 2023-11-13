package io.levelops.integrations.salesforce.sources;

import io.levelops.integrations.salesforce.client.SalesforceClientFactory;
import io.levelops.integrations.salesforce.models.CaseComment;
import io.levelops.integrations.salesforce.models.SalesforceIngestionQuery;

/**
 * SalesForce's implementation of the {@link io.levelops.ingestion.sources.DataSource} to fetch Case comment resources.
 */
public class SalesforceCaseCommentDataSource  extends SalesforceDataSource<CaseComment> {

    public SalesforceCaseCommentDataSource(SalesforceClientFactory clientFactory) {
        super(clientFactory);
    }

    @Override
    public String getSOQLStatement(SalesforceIngestionQuery query) {
        String soqlQuery = "SELECT " + SOQLHelper.constructSOQLFields("", CaseComment.SOQL_FIELDS) + " FROM CaseComment";
        String whereClause = SOQLHelper.constructWhereClauseLastModifiedDate(query);
        if (whereClause != null) {
            soqlQuery = soqlQuery + " WHERE " + whereClause;
        }
        return soqlQuery;
    }

    @Override
    public Class<CaseComment> getType() {
        return CaseComment.class;
    }
}
