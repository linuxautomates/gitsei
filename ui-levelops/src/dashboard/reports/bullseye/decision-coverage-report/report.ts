import { bullseyeDataTransformer } from "custom-hooks/helpers";
import { REPORT_FILTERS_CONFIG, HIDE_CUSTOM_FIELDS } from "dashboard/constants/applications/names";
import { bullseyeSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { DecisionCoverageReportTypes } from "model/report/bullseye/decision-coverage-report/decisionCoverageReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { BULLSEYE_APPEND_ACROSS_OPTIONS, BULLSEYE_APPLICATION } from "../commonBullseyeReports.constants";
import {
  bullseyeDecisionCoverageReportChartTypes,
  bullseyeDecisionCoverageReportDrilldown,
  bullseyeDecisionCoverageReportFilter
} from "./constants";
import { DecisionCoverageReportFiltersConfig } from "./filters.config";

const bullseyeDecisionCoverageReport: { bullseye_decision_coverage_report: DecisionCoverageReportTypes } = {
  bullseye_decision_coverage_report: {
    name: "Bullseye Decision Coverage Report",
    application: BULLSEYE_APPLICATION,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "bullseye_coverage_report",
    method: "list",
    xaxis: true,
    chart_props: bullseyeDecisionCoverageReportChartTypes,
    dataKey: "decision_percentage_coverage",
    defaultAcross: "project",
    supported_filters: bullseyeSupportedFilters,
    appendAcrossOptions: BULLSEYE_APPEND_ACROSS_OPTIONS,
    filters: bullseyeDecisionCoverageReportFilter,
    drilldown: bullseyeDecisionCoverageReportDrilldown,
    transformFunction: (data: any) => bullseyeDataTransformer(data),
    [REPORT_FILTERS_CONFIG]: DecisionCoverageReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  }
};
export default bullseyeDecisionCoverageReport;
