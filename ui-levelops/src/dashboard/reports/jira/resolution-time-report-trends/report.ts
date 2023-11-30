import { ResolutionTimeReportTrendsType } from "model/report/jira/resolution-time-report-trends/resolutionTimeReportTrends.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { jiraDrilldown } from "dashboard/constants/drilldown.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { trendReportTransformer } from "custom-hooks/helpers";
import { jiraApiBasedFilterKeyMapping, jiraChartProps } from "../commonJiraReports.constants";
import {
  ALLOWED_WIDGET_DATA_SORTING,
  FILTER_NAME_MAPPING,
  WIDGET_DATA_SORT_FILTER_KEY
} from "../../../constants/filter-name.mapping";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY
} from "../../../constants/filter-key.mapping";
import { IMPLICITY_INCLUDE_DRILLDOWN_FILTER, REPORT_FILTERS_CONFIG } from "../../../constants/applications/names";
import { JiraResolutionTimeTrendReportFiltersConfig } from "./filters.config";
import { FILTER_WITH_INFO_MAPPING, jiraExcludeStatusFilter } from "../../../constants/filterWithInfo.mapping";
import { includeSolveTimeImplicitFilter } from "../../commonReports.constants";
import { JIRA_TIME_ACROSS_FILTER_MAPPING } from "./constants";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "../constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

const resolutionTimeReportTrends: { resolution_time_report_trends: ResolutionTimeReportTrendsType } = {
  resolution_time_report_trends: {
    name: "Issues Resolution Time Trend Report",
    xaxis: false,
    application: IntegrationTypes.JIRA,
    composite: true,
    composite_transform: {
      min: "resolution_time_min",
      median: "resolution_time_median",
      max: "resolution_time_max"
    },
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      unit: "Days",
      chartProps: jiraChartProps
    },
    uri: "resolution_time_report",
    method: "list",
    filters: {
      across: "trend"
    },
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
    },
    supported_filters: jiraSupportedFilters,
    convertTo: "days",
    drilldown: jiraDrilldown,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraResolutionTimeTrendReportFiltersConfig,
    transformFunction: data => trendReportTransformer(data),
    [FILTER_NAME_MAPPING]: JIRA_TIME_ACROSS_FILTER_MAPPING,
    [FILTER_WITH_INFO_MAPPING]: [jiraExcludeStatusFilter],
    [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: includeSolveTimeImplicitFilter,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
  }
};

export default resolutionTimeReportTrends;
