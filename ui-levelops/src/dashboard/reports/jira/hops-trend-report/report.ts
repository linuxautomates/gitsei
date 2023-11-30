import { trendReportTransformer } from "custom-hooks/helpers";
import { jiraDrilldown } from "dashboard/constants/drilldown.constants";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { HopsReportTrendType } from "model/report/jira/hops-report-trend/hopsReportTrend.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { hopsTrendReportChartProps, hopsTrendReportCompositeTransform } from "./constants";
import {
  ALLOWED_WIDGET_DATA_SORTING,
  FILTER_NAME_MAPPING,
  WIDGET_DATA_SORT_FILTER_KEY
} from "dashboard/constants/filter-name.mapping";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY
} from "../../../constants/filter-key.mapping";
import { REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { JiraHopsReportTrendsFiltersConfig } from "./filters.config";
import { JIRA_COMMON_FILTER_OPTION_MAPPING } from "../../commonReports.constants";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "../constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

const jiraHopsReportTrends: { hops_report_trends: HopsReportTrendType } = {
  hops_report_trends: {
    name: "Issue Hops Report Trends",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    composite: true,
    composite_transform: hopsTrendReportCompositeTransform,
    xaxis: false,
    chart_props: hopsTrendReportChartProps,
    uri: "hops_report",
    method: "list",
    filters: {
      across: "trend"
    },
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.TIME_BASED]
    },
    [FILTER_NAME_MAPPING]: JIRA_COMMON_FILTER_OPTION_MAPPING,
    supported_filters: jiraSupportedFilters,
    drilldown: {
      ...jiraDrilldown,
      defaultSort: [{ id: "hops", desc: true }]
    },
    transformFunction: data => trendReportTransformer(data),
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraHopsReportTrendsFiltersConfig,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
  }
};

export default jiraHopsReportTrends;
