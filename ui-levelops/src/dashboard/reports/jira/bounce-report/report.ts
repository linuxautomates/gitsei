import { REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { jiraBounceReportDrilldown } from "dashboard/constants/drilldown.constants";
import {
  WIDGET_DATA_SORT_FILTER_KEY,
  ALLOWED_WIDGET_DATA_SORTING,
  VALUE_SORT_KEY,
  FILTER_NAME_MAPPING
} from "dashboard/constants/filter-name.mapping";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { JiraBounceReportType } from "model/report/jira/bounce_report/bounceReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { jiraXAxisLabelTransformForCustomFields } from "../commonJiraReports.helper";
import { bounceReportOnChartClickPayloadHandler, bounceReportTransformer } from "./helper";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY
} from "../../../constants/filter-key.mapping";
import { jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import { JiraBounceReportFiltersConfig } from "./filters.config";
import { JIRA_COMMON_FILTER_OPTION_MAPPING } from "../../commonReports.constants";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "../constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

const jiraBounceReport: { bounce_report: JiraBounceReportType } = {
  bounce_report: {
    name: "Issue Bounce Report",
    application: IntegrationTypes.JIRA,
    xaxis: true,
    defaultAcross: "assignee",
    chart_type: ChartType?.SCATTER,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      yDataKey: "median",
      rangeY: ["min", "max"],
      unit: "Bounces",
      // When we do not want to sort the data for particular across value add across value in the array
      xAxisIgnoreSortKeys: ["priority"],
      xAxisLabelKey: "additional_key"
    },
    defaultSort: [{ id: "bounces", desc: true }],
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: "bounces",
    default_query: {
      [WIDGET_DATA_SORT_FILTER_KEY]:
        widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
    },
    uri: "bounce_report",
    method: "list",
    filters: {},
    supported_filters: jiraSupportedFilters,
    drilldown: {
      ...jiraBounceReportDrilldown,
      drilldownVisibleColumn: ["key", "summary", "component_list", "bounces", "hops", "assignee"]
    },
    transformFunction: data => bounceReportTransformer(data),
    [FILTER_NAME_MAPPING]: JIRA_COMMON_FILTER_OPTION_MAPPING,
    xAxisLabelTransform: jiraXAxisLabelTransformForCustomFields,
    onChartClickPayload: bounceReportOnChartClickPayloadHandler,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraBounceReportFiltersConfig,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
  }
};

export default jiraBounceReport;
