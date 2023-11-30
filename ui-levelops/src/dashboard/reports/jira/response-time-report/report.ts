import { seriesDataTransformer } from "custom-hooks/helpers";
import { ChartContainerType } from "dashboard/helpers/helper";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { jiraResponseTimeReportDrilldown } from "dashboard/constants/drilldown.constants";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ResponseTimeReportType } from "model/report/jira/response-time-report/responseTimeReport.constant";
import { responseTimeChartProps } from "./constants";
import {
  ALLOWED_WIDGET_DATA_SORTING,
  FILTER_NAME_MAPPING,
  VALUE_SORT_KEY,
  WIDGET_DATA_SORT_FILTER_KEY
} from "dashboard/constants/filter-name.mapping";
import { jiraXAxisLabelTransformForCustomFields } from "../commonJiraReports.helper";
import { responseTimeOnChartClickPayloadHandler } from "./helper";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY
} from "../../../constants/filter-key.mapping";
import { IMPLICITY_INCLUDE_DRILLDOWN_FILTER, REPORT_FILTERS_CONFIG } from "../../../constants/applications/names";
import { jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import { JiraResponseTimeReportFiltersConfig } from "./filters.config";
import { includeSolveTimeImplicitFilter, JIRA_COMMON_FILTER_OPTION_MAPPING } from "../../commonReports.constants";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "../constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

const jiraResponseTimeReport: { response_time_report: ResponseTimeReportType } = {
  response_time_report: {
    name: "Issue Response Time Report",
    application: IntegrationTypes.JIRA,
    defaultAcross: "assignee",
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    showExtraInfoOnToolTip: ["total_tickets"],
    xaxis: true,
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]:
        widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
    },
    chart_props: responseTimeChartProps,
    uri: "response_time_report",
    method: "list",
    filters: {},
    supported_filters: jiraSupportedFilters,
    convertTo: "days",
    drilldown: jiraResponseTimeReportDrilldown,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: "response_time",
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraResponseTimeReportFiltersConfig,
    transformFunction: data => seriesDataTransformer(data),
    xAxisLabelTransform: jiraXAxisLabelTransformForCustomFields,
    onChartClickPayload: responseTimeOnChartClickPayloadHandler,
    [FILTER_NAME_MAPPING]: JIRA_COMMON_FILTER_OPTION_MAPPING,
    [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: includeSolveTimeImplicitFilter,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
  }
};

export default jiraResponseTimeReport;
