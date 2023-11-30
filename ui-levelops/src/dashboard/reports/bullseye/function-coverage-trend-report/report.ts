import { bullseyeTrendTransformer } from "custom-hooks/helpers";
import { REPORT_FILTERS_CONFIG, HIDE_CUSTOM_FIELDS } from "dashboard/constants/applications/names";
import { bullseyeSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { FunctionCoverageTrendReportTypes } from "model/report/bullseye/function-coverage-trend-report/functionCoverageTrendReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { BULLSEYE_APPEND_ACROSS_OPTIONS, BULLSEYE_APPLICATION } from "../commonBullseyeReports.constants";
import {
  bullseyeFunctionCoverageTrendReportChartTypes,
  bullseyeFunctionCoverageTrendReportDrilldown,
  bullseyeFunctionCoverageTrendReportFilter
} from "./constants";
import { FunctionCoverageTrendReportFiltersConfig } from "./filters.config";

const bullseyeFunctionCoverageTrendReport: {
  bullseye_function_coverage_trend_report: FunctionCoverageTrendReportTypes;
} = {
  bullseye_function_coverage_trend_report: {
    name: "Bullseye Function Coverage Trend Report",
    application: BULLSEYE_APPLICATION,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "bullseye_coverage_report",
    method: "list",
    xaxis: false,
    chart_props: bullseyeFunctionCoverageTrendReportChartTypes,
    composite: true,
    composite_transform: {
      function_percentage_coverage: "Function Percentage Coverage"
    },
    tooltipMapping: { function_percentage_coverage: "function_coverage" },
    dataKey: "function_percentage_coverage",
    appendAcrossOptions: BULLSEYE_APPEND_ACROSS_OPTIONS,
    filters: bullseyeFunctionCoverageTrendReportFilter,
    supported_filters: bullseyeSupportedFilters,
    drilldown: bullseyeFunctionCoverageTrendReportDrilldown,
    transformFunction: (data: any) => bullseyeTrendTransformer(data),
    [REPORT_FILTERS_CONFIG]: FunctionCoverageTrendReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  }
};

export default bullseyeFunctionCoverageTrendReport;
