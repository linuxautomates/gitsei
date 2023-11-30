import { ChartContainerType } from "dashboard/helpers/helper";
import { get } from "lodash";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { JiraReports } from "./../enums/jira-reports.enum";
import { ISSUE_MANAGEMENT_REPORTS, PREV_COMPOSITE_REPORT_TRANSFORMER, PREV_REPORT_TRANSFORMER } from "./names";

export const LEVELOPS_MULTITIME_SERIES_REPORT = "levelops_multitime_series_report";

export enum MultiTimeSeriesReports {
  ISSUES_REPORT = "tickets_report",
  BACKLOG_TREND_REPORT = "jira_backlog_trend_report",
  RESOLUTION_TIME_REPORT = "resolution_time_report"
}

export const LEVELOPS_MULTITIME_SERIES_SUPPORTED_REPORTS = [
  MultiTimeSeriesReports.ISSUES_REPORT,
  MultiTimeSeriesReports.BACKLOG_TREND_REPORT,
  MultiTimeSeriesReports.RESOLUTION_TIME_REPORT
];

export const aggregationMappingsForMultiTimeSeriesReport = (report: string) => {
  switch (report) {
    case JiraReports.JIRA_TICKETS_REPORT: {
      return [
        { label: "Issue Created", value: "issue_created" },
        { label: "Issue Updated", value: "issue_updated" },
        { label: "Issue Resolved", value: "issue_resolved" },
        { label: "Issue Due Date", value: "issue_due" }
      ];
    }
    case JiraReports.RESOLUTION_TIME_REPORT: {
      return [
        { label: "Issue Created", value: "issue_created" },
        { label: "Issue Updated", value: "issue_updated" },
        { label: "Issue Last Closed", value: "issue_resolved" }
      ];
    }
    case ISSUE_MANAGEMENT_REPORTS.TICKETS_REPORT:
    case ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_REPORT:
      return [
        { label: "Workitem Created", value: "workitem_created_at" },
        { label: "Workitem Updated", value: "workitem_updated_at" },
        { label: "Workitem Resolved", value: "workitem_resolved_at" }
      ];
    default:
      return [];
  }
};

const transformPrevQuery = (widget: any, data: any) => {
  const keysMapping = {
    workitem_created: "workitem_created_at",
    workitem_resolved: "workitem_resolved_at",
    workitem_updated: "workitem_updated_at"
  };
  const children = get(widget, ["metadata", "children"], []);
  if (children.length) {
    const updatedWidgets = (data || [])?.map((report: any) => {
      if (
        children.includes(report?.id) &&
        ["azure_tickets_report", "azure_resolution_time_report"].includes(report?.type)
      ) {
        const newQuery = {
          ...report?.query,
          across: get(keysMapping, [report?.query?.across], report?.query?.across),
          sort: [{ ...report?.query?.sort?.[0], id: get(keysMapping, [report?.query?.across], report?.query?.across) }]
        };
        return {
          ...report,
          query: newQuery
        };
      }
      return report;
    });
    return updatedWidgets;
  }
  return data;
};

export default {
  [LEVELOPS_MULTITIME_SERIES_REPORT]: {
    name: "Multi-Time Series Report",
    application: "levelops",
    chart_type: ChartType?.COMPOSITE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    composite: true,
    xaxis: true,
    show_max: true,
    chart_props: {
      unit: "Tickets"
    },
    tooltipMapping: { number_of_tickets_closed: "Number of Tickets" },
    [PREV_COMPOSITE_REPORT_TRANSFORMER]: transformPrevQuery
  }
};
