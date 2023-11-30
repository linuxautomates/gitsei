import { bullseyeDataTransformer } from "custom-hooks/helpers";
import { REPORT_FILTERS_CONFIG, HIDE_CUSTOM_FIELDS } from "dashboard/constants/applications/names";
import { bullseyeSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { BranchCoverageReportTypes } from "model/report/bullseye/branch-coverage-report/branchCoverageReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { BULLSEYE_APPEND_ACROSS_OPTIONS, BULLSEYE_APPLICATION } from "../commonBullseyeReports.constants";
import {
  bullseyeBranchCoverageReportChartTypes,
  bullseyeBranchCoverageReportDrilldown,
  bullseyeBranchCoverageReportFilter
} from "./constants";
import { BranchCoverageReportFiltersConfig } from "./filters.config";

const bullseyeBranchCoverageReport: { bullseye_branch_coverage_report: BranchCoverageReportTypes } = {
  bullseye_branch_coverage_report: {
    name: "Bullseye Branch Coverage Report",
    application: BULLSEYE_APPLICATION,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "bullseye_coverage_report",
    method: "list",
    xaxis: true,
    chart_props: bullseyeBranchCoverageReportChartTypes,
    dataKey: "condition_percentage_coverage",
    defaultAcross: "project",
    supported_filters: bullseyeSupportedFilters,
    appendAcrossOptions: BULLSEYE_APPEND_ACROSS_OPTIONS,
    filters: bullseyeBranchCoverageReportFilter,
    drilldown: bullseyeBranchCoverageReportDrilldown,
    transformFunction: (data: any) => bullseyeDataTransformer(data),
    [REPORT_FILTERS_CONFIG]: BranchCoverageReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  }
};

export default bullseyeBranchCoverageReport;
