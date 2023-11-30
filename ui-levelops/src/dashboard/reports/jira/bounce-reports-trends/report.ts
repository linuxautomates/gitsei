import { trendReportTransformer } from "custom-hooks/helpers";
import { REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { jiraDrilldown } from "dashboard/constants/drilldown.constants";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { JiraBounceReportTrendsType } from "model/report/jira/bounce-report-trends/bounceReportsTrends.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import { ALLOWED_WIDGET_DATA_SORTING } from "../../../constants/filter-name.mapping";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY
} from "../../../constants/filter-key.mapping";
import { JiraIssuesBounceReportTrendsFiltersConfig } from "./filters.config";
import {
  jiraBounceReportsTrendsCompositeTransform,
  jiraBounceReportsTrendsFilters,
  jiraBounceTrendsChartProps
} from "./constants";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "../constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

const jiraBounceReportTrends: { bounce_report_trends: JiraBounceReportTrendsType } = {
  bounce_report_trends: {
    name: "Issue Bounce Report Trends",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    chart_props: jiraBounceTrendsChartProps,
    uri: "bounce_report",
    composite: true,
    composite_transform: jiraBounceReportsTrendsCompositeTransform,
    method: "list",
    filters: jiraBounceReportsTrendsFilters,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    default_query: {
      sort_xaxis: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
    },
    supported_filters: jiraSupportedFilters,
    drilldown: jiraDrilldown,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraIssuesBounceReportTrendsFiltersConfig,
    transformFunction: data => trendReportTransformer(data),
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
  }
};

export default jiraBounceReportTrends;
