import { seriesDataTransformer } from "custom-hooks/helpers";
import { REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { jiraDrilldown } from "dashboard/constants/drilldown.constants";
import {
  ALLOWED_WIDGET_DATA_SORTING,
  FILTER_NAME_MAPPING,
  VALUE_SORT_KEY
} from "dashboard/constants/filter-name.mapping";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { HopsReportType } from "model/report/jira/hops-report/hopsReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { hopsReportChartProps } from "./constants";
import { jiraOnChartClickPayload, jiraXAxisLabelTransformForCustomFields } from "../commonJiraReports.helper";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY
} from "../../../constants/filter-key.mapping";
import { jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import { JiraHopsReportFiltersConfig } from "./filters.config";
import { JIRA_COMMON_FILTER_OPTION_MAPPING } from "../../commonReports.constants";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "../constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

const jiraHopsReport: { hops_report: HopsReportType } = {
  hops_report: {
    name: "Issue Hops Report",
    application: IntegrationTypes.JIRA,
    defaultAcross: "assignee",
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    showExtraInfoOnToolTip: ["total_tickets"],
    xaxis: true,
    default_query: {
      sort_xaxis: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
    },
    [ALLOWED_WIDGET_DATA_SORTING]: true,
    [VALUE_SORT_KEY]: "hops",
    defaultSort: [{ id: "hops", desc: true }],
    chart_props: hopsReportChartProps,
    uri: "hops_report",
    method: "list",
    filters: {},
    supported_filters: jiraSupportedFilters,
    drilldown: {
      ...jiraDrilldown,
      defaultSort: [{ id: "hops", desc: true }]
    },
    transformFunction: data => seriesDataTransformer(data),
    [FILTER_NAME_MAPPING]: JIRA_COMMON_FILTER_OPTION_MAPPING,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraHopsReportFiltersConfig,
    xAxisLabelTransform: jiraXAxisLabelTransformForCustomFields,
    onChartClickPayload: jiraOnChartClickPayload,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
  }
};

export default jiraHopsReport;
