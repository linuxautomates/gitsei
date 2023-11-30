import { jiraZendeskSalesforceDrilldownTransformer } from "dashboard/helpers/drilldown-transformers";
import { chartProps } from "dashboard/reports/commonReports.constants";
import { JiraSalesforceNodeType } from "../../../../custom-hooks/helpers";
import { SalesforceJiraTableConfig } from "../../../pages/dashboard-tickets/configs";

export const jiraSalesforceReportDrilldown = {
  application: "jirasalesforce",
  uriForNodeTypes: {
    [JiraSalesforceNodeType.JIRA]: "jira_salesforce_aggs_list_jira",
    [JiraSalesforceNodeType.SALESFORCE]: "salesforce_tickets",
    [JiraSalesforceNodeType.SALESFORCE_LIST]: "jira_salesforce_aggs_list_salesforce",
    [JiraSalesforceNodeType.COMMIT]: "jira_salesforce_aggs_list_commit"
  },
  columnsForNodeTypes: {
    [JiraSalesforceNodeType.SALESFORCE]: SalesforceJiraTableConfig.salesforce,
    [JiraSalesforceNodeType.SALESFORCE_LIST]: SalesforceJiraTableConfig.salesforce_list,
    [JiraSalesforceNodeType.JIRA]: SalesforceJiraTableConfig.jira,
    [JiraSalesforceNodeType.COMMIT]: SalesforceJiraTableConfig.commit
  },
  drilldownTransformFunction: (data: any) => jiraZendeskSalesforceDrilldownTransformer(data)
};

export const jiraSalesforceReportChartTypes = {
  unit: "",
  chartProps: chartProps,
  areaProps: [],
  stackedArea: true
};
