import { ChartContainerType } from "dashboard/helpers/helper";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { MICROSOFT_ISSUES_REPORT } from "constants/restUri";
import { microsoftIssuesReportTransformer } from "custom-hooks/helpers/helper";
import { microsoftDrilldown } from "../drilldown.constants";
import {
  FE_BASED_FILTERS,
  HIDE_CUSTOM_FIELDS,
  MICROSOFT_APPLICATION_NAME,
  MICROSOFT_ISSUES_REPORT_NAME,
  REPORT_FILTERS_CONFIG
} from "./names";
import { microsoftIssueSupportedFilters } from "../supported-filters.constant";
import { ThreatModelingIssuesReportFiltersConfig } from "dashboard/reports/microsoft/vulnerability-report/filters.config";
import { show_value_on_bar } from "./constant";

const chartProps = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

export const MicrosoftDashboard = {
  [MICROSOFT_ISSUES_REPORT_NAME]: {
    name: "Microsoft Threat Modeling Issues Report",
    application: MICROSOFT_APPLICATION_NAME,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    show_max: true,
    stack_filters: ["category", "priority", "tag", "project"],
    across: ["category", "priority", "tag", "project"],
    defaultAcross: "category",
    chart_props: {
      // These barProps apply to non-stacked only
      barProps: [
        {
          name: "Total",
          dataKey: "count",
          unit: "Issues"
        }
      ],
      stacked: false,
      unit: "Issues",
      chartProps
    },
    uri: MICROSOFT_ISSUES_REPORT,
    method: "list",
    filters: {},
    supported_filters: microsoftIssueSupportedFilters,
    last_n_max_limit: 50,
    transformFunction: microsoftIssuesReportTransformer,
    drilldown: microsoftDrilldown,
    [REPORT_FILTERS_CONFIG]: ThreatModelingIssuesReportFiltersConfig,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    [HIDE_CUSTOM_FIELDS]: true
  }
};
