import {
  COMPARE_X_AXIS_TIMESTAMP,
  INCLUDE_INTERVAL_IN_PAYLOAD,
  LABEL_TO_TIMESTAMP,
  REPORT_FILTERS_CONFIG
} from "dashboard/constants/applications/names";
import { jiraDrilldown } from "dashboard/constants/drilldown.constants";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING, WIDGET_VALIDATION_FUNCTION } from "dashboard/constants/filter-name.mapping";
import { hygieneTypes } from "dashboard/constants/hygiene.constants";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { hygieneWeightValidationHelper } from "dashboard/helpers/widgetValidation.helper";
import { HygieneReportsTrendTypes } from "model/report/jira/hygiene-report-trends/hygieneReportsTrend.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import {
  jiraHygieneReportsTrendChartTypes,
  jiraHygieneReportsTrendDefaultQueryr,
  jiraHygieneReportsTrendFilter
} from "./constants";
import { JiraIssueHygieneReportTrendsFiltersConfig } from "./filter.config";
import { hygieneTrendOnChartClickPayload } from "./helper";
import { JIRA_COMMON_FILTER_OPTION_MAPPING } from "../../commonReports.constants";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "../constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

const hygieneReportsTrend: { hygiene_report_trends: HygieneReportsTrendTypes } = {
  hygiene_report_trends: {
    name: "Issue Hygiene Trend Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.AREA,
    chart_container: ChartContainerType.HYGIENE_API_WRAPPER,
    xaxis: false,
    chart_props: jiraHygieneReportsTrendChartTypes,
    uri: "hygiene_report",
    method: "list",
    filters: jiraHygieneReportsTrendFilter,
    default_query: jiraHygieneReportsTrendDefaultQueryr,
    hygiene_uri: "jira_tickets",
    hygiene_trend_uri: "tickets_report",
    hygiene_types: hygieneTypes,
    drilldown: jiraDrilldown,
    supported_filters: jiraSupportedFilters,
    [FILTER_NAME_MAPPING]: JIRA_COMMON_FILTER_OPTION_MAPPING,
    [WIDGET_VALIDATION_FUNCTION]: hygieneWeightValidationHelper,
    [COMPARE_X_AXIS_TIMESTAMP]: true,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraIssueHygieneReportTrendsFiltersConfig,
    onChartClickPayload: hygieneTrendOnChartClickPayload,
    [LABEL_TO_TIMESTAMP]: false,
    [INCLUDE_INTERVAL_IN_PAYLOAD]: true,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY
  }
};
export default hygieneReportsTrend;
