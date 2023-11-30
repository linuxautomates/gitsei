import { REPORT_FILTERS_CONFIG } from "./../../../constants/applications/names";
import { HIDE_REPORT, SHOW_SETTINGS_TAB } from "dashboard/constants/filter-key.mapping";
import { FILTER_NAME_MAPPING } from "dashboard/constants/filter-name.mapping";
import { jiraSalesforceSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { jiraSalesforceReportChartTypes, jiraSalesforceReportDrilldown } from "./constants";
import { JiraSalesforceReportTypes } from "model/report/jira/jira-salesforce-report/jiraSalesforceReport.constants";
import { JiraSalesforceSupportEscalationReportFiltersConfig } from "./filters.config";
import { JIRA_COMMON_FILTER_OPTION_MAPPING } from "../../commonReports.constants";

const jiraSalesforceReport: { jira_salesforce_report: JiraSalesforceReportTypes } = {
  jira_salesforce_report: {
    name: "Support Escalation Report",
    application: "jirasalesforce",
    chart_type: ChartType?.SANKEY,
    chart_container: ChartContainerType.SANKEY_API_WRAPPER,
    xaxis: false,
    chart_props: jiraSalesforceReportChartTypes,
    drilldown: jiraSalesforceReportDrilldown,
    uri: "jira_salesforce",
    method: "list",
    supported_filters: jiraSalesforceSupportedFilters,
    [FILTER_NAME_MAPPING]: JIRA_COMMON_FILTER_OPTION_MAPPING,
    [SHOW_SETTINGS_TAB]: true,
    [HIDE_REPORT]: true,
    [REPORT_FILTERS_CONFIG]: JiraSalesforceSupportEscalationReportFiltersConfig
  }
};

export default jiraSalesforceReport;
