import { get, intersection, uniq } from "lodash";

import widgetConstants from "../dashboard/constants/widgetConstants";
import { WidgetType } from "../dashboard/helpers/helper";
import { ChartType } from "../shared-resources/containers/chart-container/ChartType";
import * as AppNames from "../dashboard/constants/applications/names";
import { RestIntegrations } from "../classes/RestIntegrations";
import Report from "../model/report/Report";
import { HIDE_REPORT } from "../dashboard/constants/filter-key.mapping";
import {
  jiraManagementTicketReport,
  LEAD_TIME_ISSUE_REPORT,
  leadTimeIssueReports,
  supportReports,
  JIRA_REPORTS_NOT_SUPPORTING_AZURE
} from "../dashboard/constants/applications/names";
import { REPORT_KEY_IS_ENABLED } from "../dashboard/reports/constants";

export const TABLE_REPORTS = ["levelops_table_report", "levelops_table_single_stat"];

export const REPORTS_SUPPORTED_BY_ALL_DASHBOARD = [...TABLE_REPORTS];

export const GITHUB_APPLICATIONS = [
  "helix_swarm",
  "helix_core",
  "gitlab",
  "helix",
  ...AppNames.BITBUCKET_APPLICATIONS,
  "gerrit"
];

export const getReportNameByKey = (key: string) => get(widgetConstants, [key, "name"], "");

export function getStatReports() {
  return getReportsByType(WidgetType.STATS);
}

export function getGraphReports() {
  return getReportsByType(WidgetType.GRAPH);
}

export function getCompositeGraphReports() {
  return getReportsByType(WidgetType.COMPOSITE_GRAPH);
}

export function getAllReports(): any[] {
  return (getReportsByType("all") || []).filter(report => {
    if (!(REPORT_KEY_IS_ENABLED in report) || report[REPORT_KEY_IS_ENABLED]) {
      return true;
    }
    return false;
  });
}

function getReportsByType(widgetType: WidgetType.STATS | WidgetType.GRAPH | WidgetType.COMPOSITE_GRAPH | "all") {
  const allReports: any = widgetConstants;
  let reportsByType: any[] = [];
  if (!widgetType) {
    reportsByType = Object.values(allReports);
    return reportsByType;
  }
  Object.keys(allReports)
    .filter(key => !allReports[key].deprecated)
    .map((reportType: string) => {
      const report = allReports[reportType];

      const chartType = get(report, ["chart_type"], "");
      const composite = get(report, ["composite"], "");
      const application = get(report, ["application"], "");
      const supportedWidgetTypes = get(report, ["supported_widget_types"], undefined);
      const hidden = get(report, [HIDE_REPORT], false);

      const isStat = [ChartType.STATS, ChartType.GRAPH_STAT].includes(chartType);
      const isGraph = !isStat;
      const isComposite = composite === true;

      const matchFound =
        (widgetType === WidgetType.STATS && isStat) ||
        (widgetType === WidgetType.GRAPH && isGraph) ||
        (widgetType === WidgetType.COMPOSITE_GRAPH && isComposite) ||
        widgetType === "all";

      if (!matchFound || hidden) {
        return;
      }

      report["report_type"] = reportType;
      report["applications"] = [application] || [];
      report["categories"] = report.category ? [report.category] : ["miscellaneous"];

      if (!supportedWidgetTypes) {
        const supportedWidgetType: string[] = [];

        if (isStat) {
          supportedWidgetType.push(WidgetType.STATS);
        }
        if (isGraph) {
          supportedWidgetType.push(WidgetType.GRAPH);
        }
        if (isComposite) {
          supportedWidgetType.push(WidgetType.COMPOSITE_GRAPH);
        }

        report["supported_widget_types"] = supportedWidgetType;
      }

      reportsByType.push(report);
    });
  return reportsByType;
}

export const isSupportedByIntegration = (report: Report, hasIntegrations: boolean, supportedApplications: string[]) => {
  if (REPORTS_SUPPORTED_BY_ALL_DASHBOARD.includes(report.report_type)) {
    return true;
  }

  let reportApplications = [...report.applications];

  // If application is set as any, always return true
  if (reportApplications.includes("any")) {
    return true;
  }

  if (!hasIntegrations) {
    return false;
  }

  if (supportReports.includes(report.report_type as any)) {
    if (["jirazendesk", "jirasalesforce"].includes(report.application)) {
      reportApplications = uniq([...reportApplications, "salesforce", "zendesk", "jirazendesk", "jirasalesforce"]);
    } else {
      reportApplications = uniq([...reportApplications, "salesforce", "zendesk"]);
    }
  }

  if ([...leadTimeIssueReports, ...jiraManagementTicketReport].includes(report.report_type as any)) {
    if (report.report_type === LEAD_TIME_ISSUE_REPORT.LEAD_TIME_SINGLE_STAT) {
      reportApplications = uniq([...reportApplications, "githubjira"]);
    } else {
      reportApplications = uniq([...reportApplications, "jira"]);
    }

    if (!JIRA_REPORTS_NOT_SUPPORTING_AZURE.includes(report.report_type as any)) {
      reportApplications = uniq([...reportApplications, "azure_devops"]);
    }
  }

  // For the application dev_productivity, it requires issue management + scm
  if (report.applications.includes("dev_productivity")) {
    reportApplications = [...reportApplications, "jira", "azure_devops", "github"];
  }

  return (
    supportedApplications &&
    (!!intersection(supportedApplications, reportApplications).length ||
      reportApplications.some((application: string) =>
        [
          "jenkinsgithub",
          "jenkins",
          "githubjira",
          "jirazendesk",
          "jirasalesforce",
          "levelops",
          "praetorian",
          "bullseye",
          "nccgroup",
          "snyk",
          AppNames.MICROSOFT_APPLICATION_NAME
        ].includes(application)
      ))
  );
};

export const reportOptionList = (
  widgetType: WidgetType.STATS | WidgetType.GRAPH | WidgetType.COMPOSITE_GRAPH,
  integrations: RestIntegrations[] = []
) => {
  const applications = getIntegrationApplications(integrations);
  return Object.keys(widgetConstants)
    .map(reportType => {
      const application = get(widgetConstants, [reportType, "application"], "");
      const chartType = get(widgetConstants, [reportType, "chart_type"], "");
      const composite = get(widgetConstants, [reportType, "composite"], "");
      const hidden = get(widgetConstants, [reportType, HIDE_REPORT], false);
      const option = {
        label: get(widgetConstants, [reportType, "name"], ""),
        value: reportType
      };

      const supportedWidgetType =
        (widgetType === WidgetType.STATS && [ChartType.STATS, ChartType.GRAPH_STAT].includes(chartType)) ||
        (widgetType === WidgetType.GRAPH && ![ChartType.STATS, ChartType.GRAPH_STAT].includes(chartType)) ||
        widgetType === WidgetType.COMPOSITE_GRAPH;

      const allowedApplication =
        applications &&
        (applications.includes(application) ||
          [
            "jenkinsgithub",
            "jenkins",
            "githubjira",
            "jirazendesk",
            "jirasalesforce",
            "levelops",
            "praetorian",
            "bullseye",
            "nccgroup",
            "snyk",
            AppNames.MICROSOFT_APPLICATION_NAME
          ].includes(application));

      if (!supportedWidgetType || !allowedApplication || hidden) {
        return undefined;
      }

      if (widgetType === WidgetType.COMPOSITE_GRAPH) {
        if (composite === true) {
          return option;
        }
      } else {
        // check for no integration ids, removing unsupported report types
        if (
          integrations.length === 0 &&
          ["jira_zendesk_report", "scm_jira_files_report", "jira_salesforce_report"].includes(reportType)
        ) {
          return null;
        }
        return option;
      }
    })
    .filter((item: any) => item !== undefined);
};

export function getIntegrationApplications(integrationList: RestIntegrations[]) {
  let applications: any[] = [];
  (integrationList || []).forEach((integration: RestIntegrations) => {
    if (integration?.id !== undefined) {
      applications.push(integration.application);

      /**
       * additional case that we have include
       */
      if (GITHUB_APPLICATIONS.includes(integration.application) && !applications.includes("github")) {
        applications.push("github");
      }

      /**
       * Azure integration should also include github integration
       */
      if (applications.includes("azure_devops") && !applications.includes("github")) {
        applications.push("github");
      }
    }
  });
  return applications;
}
