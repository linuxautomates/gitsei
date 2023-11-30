import { coverityDefectsReportTransform } from "custom-hooks/helpers/helper";
import {
  FE_BASED_FILTERS,
  NO_LONGER_SUPPORTED_FILTER,
  PREV_REPORT_TRANSFORMER,
  REPORT_FILTERS_CONFIG
} from "dashboard/constants/applications/names";
import { coverityDrilldown } from "dashboard/constants/drilldown.constants";
import { coverityIssueSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType, transformCoverityPrevReportQuery } from "dashboard/helpers/helper";
import { IssuesReportTypes } from "model/report/coverity/issues-report/issuesReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  coverityReportsFeBasedFilter,
  issuesQuery,
  removeNoLongerSupportedFilters,
  COVERITY_FILTER_KEY_MAPPING
} from "../commonCoverityReports.constants";
import { coverityReportXaxisLabelTransform } from "../commonCoverityReports.helper";
import { coverutyIssuesReportChartTypes } from "./constants";
import { IssuesReportFiltersConfig } from "./filters.config";

const coverityIssuesReport: {
  coverity_issues_report: IssuesReportTypes;
} = {
  coverity_issues_report: {
    name: "Coverity Issues Report",
    application: "coverity",
    chart_type: ChartType?.BAR,
    defaultAcross: "last_detected",
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    appendAcrossOptions: [
      { label: "First detected", value: "first_detected" },
      { label: "Last detected", value: "last_detected" }
    ],
    xaxis: true,
    chart_props: coverutyIssuesReportChartTypes,
    uri: "coverity_defects_report",
    method: "list",
    filters: {},
    default_query: issuesQuery,
    supportExcludeFilters: true,
    supported_filters: coverityIssueSupportedFilters,
    drilldown: coverityDrilldown,
    [PREV_REPORT_TRANSFORMER]: (data: any) => transformCoverityPrevReportQuery(data),
    xAxisLabelTransform: coverityReportXaxisLabelTransform,
    transformFunction: (data: any) => coverityDefectsReportTransform(data),
    [FE_BASED_FILTERS]: coverityReportsFeBasedFilter,
    [REPORT_FILTERS_CONFIG]: IssuesReportFiltersConfig,
    COVERITY_FILTER_KEY_MAPPING,
    [NO_LONGER_SUPPORTED_FILTER]: (filters: any) => removeNoLongerSupportedFilters(filters)
  }
};

export default coverityIssuesReport;
