import { ChartType } from "../../../shared-resources/containers/chart-container/ChartType";
import { ChartContainerType } from "../../helpers/helper";
import { nccGroupReportSupportedFilters } from "../supported-filters.constant";
import { nccGroupDrilldown } from "../drilldown.constants";
import { nccGroupIssuesReportTransform } from "../../../custom-hooks/helpers/helper";
import { FE_BASED_FILTERS, HIDE_CUSTOM_FIELDS, REPORT_FILTERS_CONFIG, TIME_FILTER_RANGE_CHOICE_MAPPER } from "./names";

import { VulnerabilityReportFiltersConfig } from "dashboard/reports/ncc-group/vulnerability-report/filters.config";
import { show_value_on_bar } from "./constant";

const chartProps = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

export const NccGroupDashboards = {
  ncc_group_vulnerability_report: {
    name: "NCC Group Vulnerability Report",
    application: "nccgroup",
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "ncc_group_issues_aggs",
    method: "list",
    filters: {},
    stack_filters: ["project", "risk", "category", "component", "tag"],
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
    supported_filters: nccGroupReportSupportedFilters,
    xaxis: true,
    drilldown: nccGroupDrilldown,
    across: ["project", "risk", "category", "component", "tag"],
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      ingested_at: "ncc_ingested_at",
      created_at: "ncc_created_at"
    },
    transformFunction: data => nccGroupIssuesReportTransform(data),
    [REPORT_FILTERS_CONFIG]: VulnerabilityReportFiltersConfig,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    [HIDE_CUSTOM_FIELDS]: true
  }
};
