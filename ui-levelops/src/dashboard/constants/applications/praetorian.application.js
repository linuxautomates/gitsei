import { ChartType } from "../../../shared-resources/containers/chart-container/ChartType";
import { ChartContainerType } from "../../helpers/helper";
import { praetorianIssuesSupportedFilters } from "../supported-filters.constant";
import { praetorianDrilldown } from "../drilldown.constants";
import { praetorianIssuesReportTransform } from "../../../custom-hooks/helpers/helper";
import { HIDE_CUSTOM_FIELDS, REPORT_FILTERS_CONFIG, TIME_FILTER_RANGE_CHOICE_MAPPER, FE_BASED_FILTERS } from "./names";
import { IssuesReportFiltersConfig } from "dashboard/reports/praetorian/issues-report/filters.config";
import { show_value_on_bar } from "./constant";

const chartProps = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

// TODO: Fix drilldown
export const PraetorianDashboards = {
  praetorian_issues_report: {
    name: "Praetorian Issues Report",
    application: "praetorian",
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "praetorian_issues_aggs",
    method: "list",
    filters: {},
    stack_filters: ["category", "priority", "tag", "project"],
    defaultAcross: "category",
    chart_props: {
      unit: "Issues",
      sortBy: "count",
      chartProps: chartProps,
      barProps: [
        {
          name: "count",
          dataKey: "count"
        }
      ],
      stacked: false
    },
    supported_filters: praetorianIssuesSupportedFilters,
    xaxis: true,
    drilldown: praetorianDrilldown,
    across: ["category", "priority", "tag", "project"],
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      ingested_at: "praetorian_ingested_at"
    },
    transformFunction: data => praetorianIssuesReportTransform(data),
    [REPORT_FILTERS_CONFIG]: IssuesReportFiltersConfig,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    [HIDE_CUSTOM_FIELDS]: true
  }
};
