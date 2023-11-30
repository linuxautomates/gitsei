import { bullseyeTrendTransformer } from "custom-hooks/helpers";
import { HIDE_CUSTOM_FIELDS, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { bullseyeSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { CodeCoverageTrendReportTypes } from "model/report/bullseye/code-coverage-trend-report/codeCoverageTrendReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { BULLSEYE_APPEND_ACROSS_OPTIONS, BULLSEYE_APPLICATION } from "../commonBullseyeReports.constants";
import {
  bullseyeCodeCoverageTrendReportChartTypes,
  bullseyeCodeCoverageTrendReportDrilldown,
  bullseyeCodeCoverageTrendReportFilter
} from "./constants";
import { CodeCoverageTrendReportFiltersConfig } from "./filters.config";

const bullseyeCodeCoverageTrendReport: { bullseye_code_coverage_trend_report: CodeCoverageTrendReportTypes } = {
  bullseye_code_coverage_trend_report: {
    name: "Bullseye Code Coverage Score Trend Report",
    application: BULLSEYE_APPLICATION,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "bullseye_coverage_report",
    method: "list",
    xaxis: false,
    chart_props: bullseyeCodeCoverageTrendReportChartTypes,
    composite: true,
    composite_transform: {
      coverage_percentage: "Coverage Percentage"
    },
    tooltipMapping: { coverage_percentage: "code_coverage" },
    dataKey: "coverage_percentage",
    appendAcrossOptions: BULLSEYE_APPEND_ACROSS_OPTIONS,
    filters: bullseyeCodeCoverageTrendReportFilter,
    supported_filters: bullseyeSupportedFilters,
    drilldown: bullseyeCodeCoverageTrendReportDrilldown,
    transformFunction: (data: any) => bullseyeTrendTransformer(data),
    [REPORT_FILTERS_CONFIG]: CodeCoverageTrendReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  }
};
export default bullseyeCodeCoverageTrendReport;
