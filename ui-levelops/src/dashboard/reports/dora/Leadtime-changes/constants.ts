import { newLeadTimeMetricOptions } from "dashboard/graph-filters/components/Constants";
import { LEAD_TIME_BY_STAGE_REPORT_CHART_PROPS } from "dashboard/reports/jira/lead-time-by-stage-report/constants";
import { WidgetActionFilterType } from "model/filters/levelopsFilters";
export const CHART_TITLE = "Breakdown by Stages";

export const chartProps = {
  ...LEAD_TIME_BY_STAGE_REPORT_CHART_PROPS,
  chartTitle: CHART_TITLE,
  statProps: {
    unit: "Days",
    unitSymbol: "",
    simplifyValue: true
  }
};

export const WIDGET_CONFIG_FILTERS: WidgetActionFilterType = {
  metrics: {
    datakey: "metrics",
    options: newLeadTimeMetricOptions,
    showArrow: true,
    prefixLabel: "Show:",
    defaultValue: "mean"
  }
};

export const CHECKBOX_TITLE = "Include items with missing stages";
