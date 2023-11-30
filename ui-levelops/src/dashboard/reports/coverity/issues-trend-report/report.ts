import { trendReportTransformer } from "custom-hooks/helpers";
import {
  FE_BASED_FILTERS,
  NO_LONGER_SUPPORTED_FILTER,
  PREV_REPORT_TRANSFORMER
} from "dashboard/constants/applications/names";
import { coverityDrilldown } from "dashboard/constants/drilldown.constants";
import { coverityIssueSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType, transformCoverityPrevReportQuery } from "dashboard/helpers/helper";
import { IssuesTrendReportTypes } from "model/report/coverity/issues-trend-report/issuesTrendReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  coverityReportsFeBasedFilter,
  COVERITY_FILTER_KEY_MAPPING,
  issuesQuery,
  removeNoLongerSupportedFilters
} from "../commonCoverityReports.constants";
import { coverityReportXaxisLabelTransform } from "../commonCoverityReports.helper";
import { coverutyIssuesTrendReportChartTypes } from "./constants";

const coverityIssuesTrendReport: {
  coverity_issues_trend_report: IssuesTrendReportTypes;
} = {
  coverity_issues_trend_report: {
    name: "Coverity Issues Trend Report",
    application: "coverity",
    chart_type: ChartType?.LINE,
    defaultAcross: "snapshot_created",
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: coverutyIssuesTrendReportChartTypes,
    uri: "coverity_defects_report",
    method: "list",
    filters: {},
    default_query: issuesQuery,
    supportExcludeFilters: true,
    supported_filters: coverityIssueSupportedFilters,
    [PREV_REPORT_TRANSFORMER]: (data: any) => transformCoverityPrevReportQuery(data),
    drilldown: coverityDrilldown,
    xAxisLabelTransform: (params: any) => coverityReportXaxisLabelTransform,
    transformFunction: (data: any) => trendReportTransformer(data),
    [FE_BASED_FILTERS]: coverityReportsFeBasedFilter,
    COVERITY_FILTER_KEY_MAPPING,
    [NO_LONGER_SUPPORTED_FILTER]: (filters: any) => removeNoLongerSupportedFilters(filters)
  }
};

export default coverityIssuesTrendReport;
