import { bullseyeTrendTransformer } from "custom-hooks/helpers";
import { REPORT_FILTERS_CONFIG, HIDE_CUSTOM_FIELDS } from "dashboard/constants/applications/names";
import { bullseyeSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { DecisionCoverageTrendReportTypes } from "model/report/bullseye/decision-coverage-trend-report/decisionCoverageTrendReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { BULLSEYE_APPEND_ACROSS_OPTIONS, BULLSEYE_APPLICATION } from "../commonBullseyeReports.constants";
import {
  bullseyeDecisionCoverageTrendReportChartTypes,
  bullseyeDecisionCoverageTrendReportDrilldown,
  bullseyeDecisionCoverageTrendReportFilter
} from "./constants";
import { DecisionCoverageTrendReportFiltersConfig } from "./filters.config";

const bullseyeDecisionCoverageTrendReport: {
  bullseye_decision_coverage_trend_report: DecisionCoverageTrendReportTypes;
} = {
  bullseye_decision_coverage_trend_report: {
    name: "Bullseye Decision Coverage Trend Report",
    application: BULLSEYE_APPLICATION,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "bullseye_coverage_report",
    method: "list",
    xaxis: false,
    chart_props: bullseyeDecisionCoverageTrendReportChartTypes,
    composite: true,
    composite_transform: {
      decision_percentage_coverage: "Decision Percentage Coverage"
    },
    tooltipMapping: { decision_percentage_coverage: "decision_coverage" },
    dataKey: "decision_percentage_coverage",
    appendAcrossOptions: BULLSEYE_APPEND_ACROSS_OPTIONS,
    filters: bullseyeDecisionCoverageTrendReportFilter,
    supported_filters: bullseyeSupportedFilters,
    drilldown: bullseyeDecisionCoverageTrendReportDrilldown,
    transformFunction: (data: any) => bullseyeTrendTransformer(data),
    [REPORT_FILTERS_CONFIG]: DecisionCoverageTrendReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  }
};
export default bullseyeDecisionCoverageTrendReport;
