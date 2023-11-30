import { SalesforceJiraTableConfig, ZendeskJiraTableConfig } from "../pages/dashboard-tickets/table-config";

import { jiraSupportedFilters } from "./supported-filters.constant";

export const dashboardTicketsGlobalConfig = {
  commit_zendesk: {
    title: "Commit Tickets",
    uri: "jira_zendesk_aggs_list_commit",
    columns: ZendeskJiraTableConfig.commit,
    supported_filters: jiraSupportedFilters
  },
  commit_salesforce: {
    title: "Commit Tickets",
    uri: "jira_salesforce_aggs_list_commit",
    columns: SalesforceJiraTableConfig.commit,
    supported_filters: jiraSupportedFilters
  }
};
