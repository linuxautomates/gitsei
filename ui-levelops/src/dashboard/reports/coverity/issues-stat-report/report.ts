import { statReportTransformer } from "custom-hooks/helpers";
import {
  FE_BASED_FILTERS,
  NO_LONGER_SUPPORTED_FILTER,
  PREV_REPORT_TRANSFORMER
} from "dashboard/constants/applications/names";
import { coverityIssueSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType, transformCoverityPrevReportQuery } from "dashboard/helpers/helper";
import { IssuesStatReportTypes } from "model/report/coverity/issues-stat-report/issuesStatReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  coverityReportsFeBasedFilter,
  COVERITY_FILTER_KEY_MAPPING,
  removeNoLongerSupportedFilters,
  statDefaultQuery
} from "../commonCoverityReports.constants";

const coverityIssuesStatReport: {
  coverity_issues_stat_report: IssuesStatReportTypes;
} = {
  coverity_issues_stat_report: {
    name: "Coverity Issues Single Stat",
    application: "coverity",
    chart_type: ChartType?.STATS,
    defaultAcross: "snapshot_created",
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      unit: "Defects"
    },
    uri: "coverity_defects_report",
    method: "list",
    filters: {},
    compareField: "total_defects",
    default_query: statDefaultQuery,
    supportExcludeFilters: true,
    supported_filters: coverityIssueSupportedFilters,
    drilldown: {},
    transformFunction: (data: any) => statReportTransformer(data),
    supported_widget_types: ["stats"],
    [PREV_REPORT_TRANSFORMER]: (data: any) => transformCoverityPrevReportQuery(data),
    [FE_BASED_FILTERS]: coverityReportsFeBasedFilter,
    COVERITY_FILTER_KEY_MAPPING,
    [NO_LONGER_SUPPORTED_FILTER]: (filters: any) => removeNoLongerSupportedFilters(filters)
  }
};

export default coverityIssuesStatReport;
