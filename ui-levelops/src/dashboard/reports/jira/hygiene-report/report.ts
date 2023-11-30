import { PREV_REPORT_TRANSFORMER, REPORT_FILTERS_CONFIG } from "./../../../constants/applications/names";
import {
  API_BASED_FILTER,
  FIELD_KEY_FOR_FILTERS,
  JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_KEY,
  PARTIAL_FILTER_MAPPING_KEY
} from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING, WIDGET_VALIDATION_FUNCTION } from "dashboard/constants/filter-name.mapping";
import { hygieneDefaultSettings } from "dashboard/constants/helper";
import { hygieneTypes } from "dashboard/constants/hygiene.constants";
import { jiraSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType, transformIssueHygienePrevQuery } from "dashboard/helpers/helper";
import { hygieneWeightValidationHelper } from "dashboard/helpers/widgetValidation.helper";
import { HygieneReportTypes } from "model/report/jira/hygiene-report/hygieneReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { jiraApiBasedFilterKeyMapping } from "../commonJiraReports.constants";
import { jiraHygieneReportChartTypes, jiraHygieneReportDrilldown } from "./constants";
import { JiraIssueHygieneReportFiltersConfig } from "./filters.config";
import { JIRA_COMMON_FILTER_OPTION_MAPPING } from "../../commonReports.constants";
import { JIRA_PARTIAL_FILTER_KEY_MAPPING } from "../constant";
import { IntegrationTypes } from "constants/IntegrationTypes";

const hygieneReport: { hygiene_report: HygieneReportTypes } = {
  hygiene_report: {
    name: "Issue Hygiene Report",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.SCORE,
    chart_container: ChartContainerType.HYGIENE_API_WRAPPER,
    xaxis: true,
    chart_props: jiraHygieneReportChartTypes,
    uri: "hygiene_report",
    method: "list",
    filters: {},
    defaultAcross: "project",
    hygiene_uri: "tickets_report",
    hygiene_trend_uri: "tickets_report",
    hygiene_types: hygieneTypes,
    drilldown: jiraHygieneReportDrilldown,
    default_query: hygieneDefaultSettings,
    supported_filters: jiraSupportedFilters,
    [FILTER_NAME_MAPPING]: JIRA_COMMON_FILTER_OPTION_MAPPING,
    [WIDGET_VALIDATION_FUNCTION]: hygieneWeightValidationHelper,
    [API_BASED_FILTER]: ["reporters", "assignees"],
    [FIELD_KEY_FOR_FILTERS]: jiraApiBasedFilterKeyMapping,
    [REPORT_FILTERS_CONFIG]: JiraIssueHygieneReportFiltersConfig,
    [PARTIAL_FILTER_MAPPING_KEY]: JIRA_PARTIAL_FILTER_KEY_MAPPING,
    [PARTIAL_FILTER_KEY]: JIRA_SCM_COMMON_PARTIAL_FILTER_KEY,
    [PREV_REPORT_TRANSFORMER]: data => transformIssueHygienePrevQuery(data)
  }
};
export default hygieneReport;
