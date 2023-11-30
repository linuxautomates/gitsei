import { bullseyeTrendTransformer } from "custom-hooks/helpers";
import { REPORT_FILTERS_CONFIG, HIDE_CUSTOM_FIELDS } from "dashboard/constants/applications/names";
import { bullseyeSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { BranchCoverageTrendReportTypes } from "model/report/bullseye/branch-coverage-trend-report/branchCoverageTrendReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { BULLSEYE_APPEND_ACROSS_OPTIONS, BULLSEYE_APPLICATION } from "../commonBullseyeReports.constants";
import {
  bullseyeBranchCoverageTrendReportChartTypes,
  bullseyeBranchCoverageTrendReportDrilldown,
  bullseyeBranchCoverageTrendReportFilter
} from "./constants";
import { BranchCoverageTrendReportFiltersConfig } from "./filters.config";

const bullseyeBranchCoverageTrendReport: { bullseye_branch_coverage_trend_report: BranchCoverageTrendReportTypes } = {
  bullseye_branch_coverage_trend_report: {
    name: "Bullseye Branch Coverage Trend Report",
    application: BULLSEYE_APPLICATION,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "bullseye_coverage_report",
    method: "list",
    xaxis: false,
    chart_props: bullseyeBranchCoverageTrendReportChartTypes,
    composite: true,
    composite_transform: {
      condition_percentage_coverage: "Condition Percentage Coverage"
    },
    tooltipMapping: { condition_percentage_coverage: "condition_coverage" },
    dataKey: "condition_percentage_coverage",
    appendAcrossOptions: BULLSEYE_APPEND_ACROSS_OPTIONS,
    filters: bullseyeBranchCoverageTrendReportFilter,
    supported_filters: bullseyeSupportedFilters,
    drilldown: bullseyeBranchCoverageTrendReportDrilldown,
    transformFunction: (data: any) => bullseyeTrendTransformer(data),
    [REPORT_FILTERS_CONFIG]: BranchCoverageTrendReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  }
};

export default bullseyeBranchCoverageTrendReport;
