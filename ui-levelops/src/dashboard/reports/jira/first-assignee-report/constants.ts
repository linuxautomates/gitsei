import { ImplicityIncludeDrilldownFilter } from "model/report/jira/jira-first-assignee-report/jiraFirstAssigneeReport.constants";
import { jiraChartProps } from "../commonJiraReports.constants";

export const issuesFirstAssigneeChartProps = {
  barProps: [
    {
      name: "min",
      dataKey: "min"
    },
    {
      name: "median",
      dataKey: "median"
    },
    {
      name: "max",
      dataKey: "max"
    }
  ],
  stacked: false,
  unit: "Days",
  chartProps: jiraChartProps,
  xAxisIgnoreSortKeys: ["priority"]
};

export const issueFirstAssigneeReportImplicitFilter: ImplicityIncludeDrilldownFilter = {
  missing_fields: {
    first_assignee: false
  }
};
