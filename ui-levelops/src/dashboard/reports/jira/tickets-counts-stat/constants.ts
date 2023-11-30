import { issueSingleDefualtCreatedAt } from "../commonJiraReports.constants";

export const jiraTicketsCountsStatReportChartTypes = {
  unit: "Tickets"
};

// export const jiraTicketsCountsStatReportFilters =

export const jiraTicketsCountsStatReportDefaultQuery = {
  issue_created_at: issueSingleDefualtCreatedAt
};

export const jiraTicketsCountsStatReportStatTimeBasedFilters = {
  options: [
    { value: "issue_created", label: "Issue Created" },
    { value: "issue_resolved", label: "Issue Resolved" },
    { value: "issue_due", label: "Issue Due" },
    { value: "issue_updated", label: "Issue Updated" }
  ],
  getFilterLabel: (data: { filters: any }) => {
    const { filters } = data;
    return filters.across ? `${filters.across.replaceAll("_", " ")} in` : "";
  },
  getFilterKey: (data: { filters: any }) => {
    const { filters } = data;
    return filters.across ? `${filters.across}_at` : "";
  },
  defaultValue: "issue_created"
};
