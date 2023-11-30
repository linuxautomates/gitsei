import { JiraZendeskNodeType } from "custom-hooks/helpers";
import { jiraZendeskSalesforceDrilldownTransformer } from "dashboard/helpers/drilldown-transformers";
import { ZendeskJiraTableConfig } from "dashboard/pages/dashboard-tickets/configs";
import { chartProps } from "dashboard/reports/commonReports.constants";

export const jiraZendeskReportDrilldown = {
  application: "jirazendesk",
  uriForNodeTypes: {
    [JiraZendeskNodeType.JIRA]: "jira_zendesk_aggs_list_jira",
    [JiraZendeskNodeType.ZENDESK]: "zendesk_tickets",
    [JiraZendeskNodeType.ZENDESK_LIST]: "jira_zendesk_aggs_list_zendesk",
    [JiraZendeskNodeType.COMMIT]: "jira_zendesk_aggs_list_commit"
  },
  columnsForNodeTypes: {
    [JiraZendeskNodeType.ZENDESK]: ZendeskJiraTableConfig.zendesk,
    [JiraZendeskNodeType.ZENDESK_LIST]: ZendeskJiraTableConfig.zendesk_list,
    [JiraZendeskNodeType.JIRA]: ZendeskJiraTableConfig.jira,
    [JiraZendeskNodeType.COMMIT]: ZendeskJiraTableConfig.commit
  },
  drilldownTransformFunction: (data: any) => jiraZendeskSalesforceDrilldownTransformer(data)
};

export const jiraZendeskReportChartTypes = {
  unit: "",
  chartProps: chartProps,
  areaProps: [],
  stackedArea: true
};
