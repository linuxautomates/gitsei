import { REPORT_FILTERS_CONFIG } from "./../../../constants/applications/names";
import { TIME_FILTER_RANGE_CHOICE_MAPPER } from "dashboard/constants/applications/names";
import { SHOW_SETTINGS_TAB } from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { CustomFieldMappingKey, zendeskCustomFieldsMapping } from "dashboard/constants/helper";
import { jiraZenDeskSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { JiraZendeskReportTypes } from "model/report/jira/jira-zendesk-report/jiraZendeskReport.constants";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { jiraZendeskReportChartTypes, jiraZendeskReportDrilldown } from "./constants";
import { jiraZendeskReportBlockTimeFilterTransformation } from "./helper";
import { JiraZendeskSupportEscalationReportFiltersConfig } from "./filters.config";
import { JIRA_COMMON_FILTER_OPTION_MAPPING } from "../../commonReports.constants";

const jiraZendeskReport: { jira_zendesk_report: JiraZendeskReportTypes } = {
  jira_zendesk_report: {
    name: "Support Escalation Report",
    application: "jirazendesk",
    chart_type: ChartType?.SANKEY,
    chart_container: ChartContainerType.SANKEY_API_WRAPPER,
    xaxis: false,
    chart_props: jiraZendeskReportChartTypes,
    drilldown: jiraZendeskReportDrilldown,
    uri: "jira_zendesk",
    method: "list",
    blockTimeFilterTransformation: jiraZendeskReportBlockTimeFilterTransformation,
    supported_filters: jiraZenDeskSupportedFilters,
    [CustomFieldMappingKey.CUSTOM_FIELD_MAPPING_KEY]: zendeskCustomFieldsMapping,
    [FILTER_NAME_MAPPING]: JIRA_COMMON_FILTER_OPTION_MAPPING,
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      jira_issue_created_at: "jirazendesk_issue_created_at"
    },
    [SHOW_SETTINGS_TAB]: true,
    [REPORT_FILTERS_CONFIG]: JiraZendeskSupportEscalationReportFiltersConfig
  }
};

export default jiraZendeskReport;
