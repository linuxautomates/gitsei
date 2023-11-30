import { seriesDataTransformer } from "custom-hooks/helpers";
import { IMPLICITY_INCLUDE_DRILLDOWN_FILTER, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { jiraBounceReportDrilldown } from "dashboard/constants/drilldown.constants";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { JiraFirstAssigneeReportType } from "model/report/jira/jira-first-assignee-report/jiraFirstAssigneeReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { jiraXAxisLabelTransformForCustomFields } from "../commonJiraReports.helper";
import { issueFirstAssigneeReportImplicitFilter, issuesFirstAssigneeChartProps } from "./constants";
import { ALLOWED_WIDGET_DATA_SORTING, VALUE_SORT_KEY } from "dashboard/constants/filter-name.mapping";
import { onChartClickPayload } from "./helper";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY
} from "../../../constants/filter-key.mapping";
import { jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import { JiraFirstAssigneeReportFiltersConfig } from "./filters.config";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "../constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

const issuesFirstAssigneeReport: { first_assignee_report: JiraFirstAssigneeReportType } = {
  first_assignee_report: {
    name: "Issue First Assignee Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.BAR,
    defaultAcross: "assignee",
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    showExtraInfoOnToolTip: ["total_tickets"],
    chart_props: issuesFirstAssigneeChartProps,
    uri: "first_assignee_report",
    method: "list",
    filters: {},
    xaxis: true,
    default_query: {
      sort_xaxis: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
    },
    supported_filters: jiraSupportedFilters,
    convertTo: "days",
    drilldown: jiraBounceReportDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    xAxisLabelTransform: jiraXAxisLabelTransformForCustomFields,
    onChartClickPayload: onChartClickPayload,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [IMPLICITY_INCLUDE_DRILLDOWN_FILTER]: issueFirstAssigneeReportImplicitFilter,
    [REPORT_FILTERS_CONFIG]: JiraFirstAssigneeReportFiltersConfig,
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: "assign_to_resolve",
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
  }
};

export default issuesFirstAssigneeReport;
