import { bullseyeDataTransformer } from "custom-hooks/helpers";
import { REPORT_FILTERS_CONFIG, HIDE_CUSTOM_FIELDS } from "dashboard/constants/applications/names";
import { bullseyeSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { CodeCoverageReportTypes } from "model/report/bullseye/code-coverage-report/codeCoverageReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { BULLSEYE_APPEND_ACROSS_OPTIONS, BULLSEYE_APPLICATION } from "../commonBullseyeReports.constants";
import {
  bullseyeCodeCoverageReportChartTypes,
  bullseyeCodeCoverageReportDrilldown,
  bullseyeCodeCoverageReportFilter
} from "./constants";
import { CodeCoverageReportFiltersConfig } from "./filters.config";

const bullseyeCodeCoverageReport: { bullseye_code_coverage_report: CodeCoverageReportTypes } = {
  bullseye_code_coverage_report: {
    name: "Bullseye Code Coverage Report",
    application: BULLSEYE_APPLICATION,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "bullseye_coverage_report",
    method: "list",
    xaxis: true,
    chart_props: bullseyeCodeCoverageReportChartTypes,
    dataKey: "coverage_percentage",
    defaultAcross: "project",
    supported_filters: bullseyeSupportedFilters,
    appendAcrossOptions: BULLSEYE_APPEND_ACROSS_OPTIONS,
    filters: bullseyeCodeCoverageReportFilter,
    drilldown: bullseyeCodeCoverageReportDrilldown,
    transformFunction: (data: any) => bullseyeDataTransformer(data),
    [REPORT_FILTERS_CONFIG]: CodeCoverageReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  }
};
export default bullseyeCodeCoverageReport;
