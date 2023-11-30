import { REPORT_FILTERS_CONFIG, TIME_FILTER_RANGE_CHOICE_MAPPER } from "./names";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { snykIssuesReportTransform } from "custom-hooks/helpers";
import { snykSupportedFilters } from "../supported-filters.constant";
import { snykDrilldown } from "../drilldown.constants";
import { ChartContainerType } from "../../helpers/helper";
import { SnykVulnerabilityReportFiltersConfig } from "dashboard/reports/snyk/vulnerability-report/filter.config";

const SNYK_APPLICATION = "snyk";

const chartProps = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

export const SnykDashboards = {
  snyk_vulnerability_report: {
    name: "Snyk Vulnerability Report",
    application: SNYK_APPLICATION,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "snyk_issues_report",
    method: "list",
    stack_filters: ["severity", "type", "project"],
    defaultAcross: "trend",
    chart_props: {
      unit: "Issues",
      sortBy: "total",
      chartProps: chartProps,
      barProps: [
        {
          name: "Total Issues",
          dataKey: "total",
          unit: "Issues"
        }
      ],
      stacked: false
    },
    supported_filters: snykSupportedFilters,
    xaxis: true,
    drilldown: snykDrilldown,
    across: ["trend", "severity", "type", "project"],
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      publication_range: "snyk_publication_range",
      disclosure_range: "snyk_disclosure_range"
    },
    transformFunction: (data: any) => snykIssuesReportTransform(data),
    [REPORT_FILTERS_CONFIG]: SnykVulnerabilityReportFiltersConfig
  }
};
