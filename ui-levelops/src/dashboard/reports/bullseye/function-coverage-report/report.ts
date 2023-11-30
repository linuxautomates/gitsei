import { bullseyeDataTransformer } from "custom-hooks/helpers";
import { REPORT_FILTERS_CONFIG, HIDE_CUSTOM_FIELDS } from "dashboard/constants/applications/names";
import { bullseyeDrilldown } from "dashboard/constants/drilldown.constants";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { bullseyeSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { FunctionCoverageReportTypes } from "model/report/bullseye/function-coverage-report/functionCoverageReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { BULLSEYE_APPEND_ACROSS_OPTIONS, BULLSEYE_APPLICATION, defaultSorts } from "../commonBullseyeReports.constants";
import {
  bullseyeFunctionCoverageReportChartTypes,
  bullseyeFunctionCoverageReportDrilldown,
  bullseyeFunctionCoverageReportFilter,
  bullseyeFunctionCoverageReportFilterNameMapping
} from "./constants";
import { FunctionCoverageReportFiltersConfig } from "./filters.config";

const bullseyeFunctionCoverageReport: { bullseye_function_coverage_report: FunctionCoverageReportTypes } = {
  bullseye_function_coverage_report: {
    name: "Bullseye Function Coverage Report",
    application: BULLSEYE_APPLICATION,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    uri: "bullseye_coverage_report",
    method: "list",
    chart_props: bullseyeFunctionCoverageReportChartTypes,
    dataKey: "function_percentage_coverage",
    defaultAcross: "project",
    appendAcrossOptions: BULLSEYE_APPEND_ACROSS_OPTIONS,
    filters: bullseyeFunctionCoverageReportFilter,
    supported_filters: bullseyeSupportedFilters,
    drilldown: bullseyeFunctionCoverageReportDrilldown,
    transformFunction: (data: any) => bullseyeDataTransformer(data),
    [FILTER_NAME_MAPPING]: bullseyeFunctionCoverageReportFilterNameMapping,
    [REPORT_FILTERS_CONFIG]: FunctionCoverageReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  }
};
export default bullseyeFunctionCoverageReport;
