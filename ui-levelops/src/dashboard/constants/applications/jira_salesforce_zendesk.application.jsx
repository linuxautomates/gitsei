import {
  scmFilesTransform,
  seriesDataTransformer,
  tableTransformer,
  trendReportTransformer
} from "custom-hooks/helpers";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import React from "react";
import { tableCell } from "utils/tableUtils";
import { AvatarWithText } from "shared-resources/components";
import { get } from "lodash";
import { jiraSalesforceSupportedFilters, jiraZenDeskSupportedFilters } from "../supported-filters.constant";

import { JiraZendeskFilesTableConfig, JiraSalesforceFilesTableConfig } from "dashboard/pages/dashboard-tickets/configs";
import { zendeskTicketIdColumn } from "dashboard/pages/dashboard-tickets/configs/zendeskTableConfig";
import { jiraZendeskSalesforceDrilldownTransformer } from "dashboard/helpers/drilldown-transformers";
import { ChartContainerType } from "../../helpers/helper";
import { CustomFieldMappingKey, zendeskCustomFieldsMapping } from "../helper";
import { REPORT_FILTERS_CONFIG, TIME_FILTER_RANGE_CHOICE_MAPPER } from "./names";
import { jiraSalesforceZendeskCommonRangeChoiceMapping } from "../timeFilterRangeChoiceMapping";
import { HIDE_REPORT, SHOW_SETTINGS_TAB } from "../filter-key.mapping";
import { JiraZendeskSupportHotSpotReportFiltersConfig } from "dashboard/reports/jiraZendesk/jira-zendesk-files-report/filters.config";
import { JiraSalesforceSupportHotSpotReportFiltersConfig } from "dashboard/reports/jiraSalesforce/jira-salesforce-files-report/filters.config";
import { JiraZendeskEscalationTimeReportFiltersConfig } from "dashboard/reports/jiraZendesk/jira-zendesk-escalation-time-report/filters.config";
import { ZendeskTimeAcrossStagesReportFiltersConfig } from "dashboard/reports/jiraZendesk/zendesk-time-across-stages/filters.config";
import { ZendeskConfigToFixTrendsReportFiltersConfig } from "dashboard/reports/jiraZendesk/zendesk-c2f-trends-report/filters.config";
import { JiraSalesforceEscalationTimeReportFiltersConfig } from "dashboard/reports/jiraSalesforce/salesforce-escalation-time-report/filters.config";
import { JiraSalesforceConfigToFixTrendsReportFiltersConfig } from "dashboard/reports/jiraSalesforce/support-config to-fix-trends-report/filters.config";
import { JiraSalesforceTimeAcrossStageReportFiltersConfig } from "dashboard/reports/jiraSalesforce/time-across-stages-report/filters.config";
import { getDashboardsPage } from "constants/routePaths";
import { IntegrationTypes } from "constants/IntegrationTypes";

const chartProps = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

const JiraIDLink = ({ intId, index }) => {
  const projectParams = useParams();
  const _baseUrl = `${getDashboardsPage(projectParams)}/ticket_details?key=${i}&integration_id=${intId.toString()}`;
  return (
    <a target="_blank" href={_baseUrl} className="mr-5" key={index}>
      {index}
    </a>
  );
};

const acrossStagesColumns = [
  {
    title: "Subject",
    key: "subject",
    dataIndex: "subject",
    width: "10%",
    ellipsis: true
  },
  {
    title: "Jira Keys",
    key: "jira_ids",
    dataIndex: "jira_ids",
    width: "10%",
    render: (item, record, index) =>
      Array.isArray(item) &&
      item.map(i => {
        const integrationId = get(record, ["integration_ids"], []);
        return <JiraIDLink intId={integrationId} index={i} />;
      })
  },
  {
    title: "Escalation Time",
    key: "escalation_time",
    dataIndex: "escalation_time",
    width: "10%",
    ellipsis: true
  },
  {
    title: "Resolution Time",
    key: "resolution_time",
    dataIndex: "resolution_time",
    width: "10%",
    ellipsis: true
  }
];

const salesforceColumns = [
  {
    title: "Type",
    key: "type",
    dataIndex: "type",
    width: "10%",
    ellipsis: true
  },
  {
    title: "Contact",
    key: "contact",
    dataIndex: "contact",
    width: "10%",
    ellipsis: true,
    render: item => item && <AvatarWithText text={item.includes("UNASSIGNED") ? "UNASSIGNED" : item} />
  },
  {
    title: "Creator",
    key: "creator",
    dataIndex: "creator",
    width: "10%",
    ellipsis: true,
    render: item => item && <AvatarWithText text={item.includes("UNASSIGNED") ? "UNASSIGNED" : item} />
  },
  {
    title: "Priority",
    key: "priority",
    dataIndex: "priority",
    width: "7%",
    ellipsis: true,
    render: props => tableCell("priority", props)
  },
  {
    title: "Status",
    key: "status",
    dataIndex: "status",
    width: "7%",
    ellipsis: true,
    render: props => tableCell("status", props)
  },
  {
    title: "Created At",
    key: "case_created_at",
    dataIndex: "case_created_at",
    width: "10%",
    ellipsis: true,
    render: value => tableCell("updated_on", value)
  },
  {
    title: "Updated At",
    key: "case_modified_at",
    dataIndex: "case_modified_at",
    width: "10%",
    ellipsis: true,
    render: value => tableCell("updated_on", value)
  }
];
const zendeskColumns = [
  {
    title: "Assignee",
    dataIndex: "assignee_email",
    key: "assignee_email",
    width: "10%",
    ellipsis: true,
    render: item => item && <AvatarWithText text={item.includes("UNASSIGNED") ? "UNASSIGNED" : item} />
  },
  {
    title: "Submitter",
    dataIndex: "submitter_email",
    key: "submitter_email",
    width: "10%",
    ellipsis: true,
    render: item => item && <AvatarWithText text={item.includes("UNASSIGNED") ? "UNASSIGNED" : item} />
  },
  {
    title: "Type",
    key: "type",
    dataIndex: "type",
    width: "10%",
    ellipsis: true
  },
  {
    title: "Priority",
    key: "priority",
    dataIndex: "priority",
    width: "7%",
    ellipsis: true,
    render: props => tableCell("priority", props)
  },
  {
    title: "Status",
    key: "status",
    dataIndex: "status",
    width: "7%",
    ellipsis: true,
    render: props => tableCell("status", props)
  },
  {
    title: "Created At",
    key: "ticket_created_at",
    dataIndex: "ticket_created_at",
    width: "10%",
    ellipsis: true,
    render: value => tableCell("updated_on", value)
  },
  {
    title: "Updated At",
    key: "ticket_updated_at",
    dataIndex: "ticket_updated_at",
    width: "10%",
    ellipsis: true,
    render: value => tableCell("updated_on", value)
  }
];

export const JiraSalesforceZendeskDashboard = {
  jira_zendesk_files_report: {
    name: "Support Hotspots Report",
    application: IntegrationTypes.JIRAZENDESK,
    chart_type: ChartType?.GRID_VIEW,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    uri: "jira_zendesk_files",
    rootFolderURI: "jira_zendesk_files_report",
    method: "list",
    filters: {},
    defaultFilters: {
      module: ""
    },
    sort_options: ["num_commits", "changes", "deletions", "additions"],
    supported_filters: jiraZenDeskSupportedFilters,
    drilldown: {
      title: "Jira/Zendesk Files",
      uri: "jira_zendesk_files",
      application: "jira_zendesk_files",
      columns: JiraZendeskFilesTableConfig,
      supported_filters: jiraZenDeskSupportedFilters,
      drilldownTransformFunction: data => jiraZendeskSalesforceDrilldownTransformer(data)
    },
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: jiraSalesforceZendeskCommonRangeChoiceMapping(IntegrationTypes.JIRAZENDESK),
    [CustomFieldMappingKey.CUSTOM_FIELD_MAPPING_KEY]: zendeskCustomFieldsMapping,
    transformFunction: data => scmFilesTransform(data),
    [REPORT_FILTERS_CONFIG]: JiraZendeskSupportHotSpotReportFiltersConfig,
    [SHOW_SETTINGS_TAB]: true
  },
  jira_salesforce_files_report: {
    name: "Support Hotspots Report",
    application: IntegrationTypes.JIRA_SALES_FORCE,
    chart_type: ChartType?.GRID_VIEW,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    uri: "jira_salesforce_files",
    rootFolderURI: "jira_salesforce_files_report",
    method: "list",
    filters: {},
    defaultFilters: {
      module: ""
    },
    sort_options: ["num_commits", "changes", "deletions", "additions"],
    supported_filters: jiraSalesforceSupportedFilters,
    drilldown: {
      title: "Jira/Salesforce Files",
      uri: "jira_salesforce_files",
      application: "jira_salesforce_files",
      columns: JiraSalesforceFilesTableConfig,
      supported_filters: jiraSalesforceSupportedFilters,
      drilldownTransformFunction: data => jiraZendeskSalesforceDrilldownTransformer(data)
    },
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: jiraSalesforceZendeskCommonRangeChoiceMapping(IntegrationTypes.JIRA_SALES_FORCE),
    transformFunction: data => scmFilesTransform(data),
    [SHOW_SETTINGS_TAB]: true,
    [HIDE_REPORT]: true,
    [REPORT_FILTERS_CONFIG]: JiraSalesforceSupportHotSpotReportFiltersConfig
  },
  jira_salesforce_escalation_time_report: {
    name: "Support Themes Report",
    application: IntegrationTypes.JIRA_SALES_FORCE,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      barProps: [
        {
          name: "min",
          dataKey: "min"
        },
        {
          name: "median",
          dataKey: "median"
        },
        {
          name: "max",
          dataKey: "max"
        }
      ],
      stacked: false,
      unit: "Days",
      sortBy: "median",
      chartProps: chartProps
    },
    xaxis: false,
    defaultFilterKey: "median",
    uri: "jira_salesforce_escalation_time_report",
    method: "list",
    filters: {
      across: "trend"
    },
    convertTo: "days",
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: jiraSalesforceZendeskCommonRangeChoiceMapping(IntegrationTypes.JIRA_SALES_FORCE),
    supported_filters: jiraSalesforceSupportedFilters,
    transformFunction: data => seriesDataTransformer(data),
    [SHOW_SETTINGS_TAB]: true,
    [HIDE_REPORT]: true,
    [REPORT_FILTERS_CONFIG]: JiraSalesforceEscalationTimeReportFiltersConfig
  },
  jira_zendesk_escalation_time_report: {
    name: "Support Themes Report",
    application: IntegrationTypes.JIRAZENDESK,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    chart_props: {
      barProps: [
        {
          name: "min",
          dataKey: "min"
        },
        {
          name: "median",
          dataKey: "median"
        },
        {
          name: "max",
          dataKey: "max"
        }
      ],
      stacked: false,
      unit: "Days",
      sortBy: "median",
      chartProps: chartProps
    },
    xaxis: false,
    uri: "jira_zendesk_escalation_time_report",
    method: "list",
    filters: {
      across: "trend"
    },
    convertTo: "days",
    supported_filters: jiraZenDeskSupportedFilters,
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: jiraSalesforceZendeskCommonRangeChoiceMapping(IntegrationTypes.JIRAZENDESK),
    [CustomFieldMappingKey.CUSTOM_FIELD_MAPPING_KEY]: zendeskCustomFieldsMapping,
    transformFunction: data => seriesDataTransformer(data),
    [SHOW_SETTINGS_TAB]: true,
    [REPORT_FILTERS_CONFIG]: JiraZendeskEscalationTimeReportFiltersConfig
  },
  salesforce_time_across_stages: {
    name: "Support Time Across Stages",
    application: IntegrationTypes.JIRA_SALES_FORCE,
    chart_type: ChartType?.TABLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    columns: [...acrossStagesColumns, ...salesforceColumns],
    xaxis: false,
    chart_props: {
      size: "small",
      columns: acrossStagesColumns,
      chartProps: {}
    },
    uri: "jira_salesforce_aggs_list_salesforce",
    method: "list",
    filters: {
      sort: [{ id: "escalation_time", desc: true }]
    },
    defaultFilters: {
      state_transition: { from_state: "", to_state: "" }
    },
    supported_filters: jiraSalesforceSupportedFilters,
    drilldown: {
      uri: "jira_salesforce_aggs_list_salesforce",
      application: "salesforce_time_across_stages",
      supported_filters: jiraSalesforceSupportedFilters,
      columnsWithInfo: ["escalation_time", "resolution_time"],
      drilldownTransformFunction: data => jiraZendeskSalesforceDrilldownTransformer(data)
    },
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: jiraSalesforceZendeskCommonRangeChoiceMapping(IntegrationTypes.JIRA_SALES_FORCE),
    transformFunction: data => tableTransformer(data),
    [SHOW_SETTINGS_TAB]: true,
    [HIDE_REPORT]: true,
    [REPORT_FILTERS_CONFIG]: JiraSalesforceTimeAcrossStageReportFiltersConfig
  },
  zendesk_time_across_stages: {
    name: "Support Time Across Stages",
    application: IntegrationTypes.JIRAZENDESK,
    chart_type: ChartType?.TABLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    columns: [zendeskTicketIdColumn, ...acrossStagesColumns, ...zendeskColumns],
    xaxis: false,
    chart_props: {
      size: "small",
      columns: acrossStagesColumns,
      chartProps: {}
    },
    uri: "jira_zendesk_aggs_list_zendesk",
    method: "list",
    filters: {
      sort: [{ id: "escalation_time", desc: true }]
    },
    defaultFilters: {
      state_transition: { from_state: "", to_state: "" }
    },
    supported_filters: jiraZenDeskSupportedFilters,
    drilldown: {
      uri: "jira_zendesk_aggs_list_zendesk",
      application: "zendesk_time_across_stages",
      supported_filters: jiraZenDeskSupportedFilters,
      columnsWithInfo: ["escalation_time", "resolution_time"],
      drilldownTransformFunction: data => jiraZendeskSalesforceDrilldownTransformer(data)
    },
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: jiraSalesforceZendeskCommonRangeChoiceMapping(IntegrationTypes.JIRAZENDESK),
    [CustomFieldMappingKey.CUSTOM_FIELD_MAPPING_KEY]: zendeskCustomFieldsMapping,
    transformFunction: data => tableTransformer(data),
    [SHOW_SETTINGS_TAB]: true,
    [REPORT_FILTERS_CONFIG]: ZendeskTimeAcrossStagesReportFiltersConfig
  },
  salesforce_c2f_trends: {
    name: "Support Config To Fix Trends",
    application: IntegrationTypes.JIRA_SALES_FORCE,
    chart_type: ChartType?.AREA,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    composite: true,
    composite_transform: {
      total_tickets: "salesforce_c2f_total_tickets"
    },
    chart_props: {
      unit: "Tickets",
      chartProps: chartProps,
      areaProps: [],
      stackedArea: true
    },
    uri: "jira_salesforce_resolved_tickets_trend",
    method: "list",
    filters: {},
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: jiraSalesforceZendeskCommonRangeChoiceMapping(IntegrationTypes.JIRA_SALES_FORCE),
    supported_filters: jiraSalesforceSupportedFilters,
    transformFunction: data => trendReportTransformer(data),
    [SHOW_SETTINGS_TAB]: true,
    [HIDE_REPORT]: true,
    [REPORT_FILTERS_CONFIG]: JiraSalesforceConfigToFixTrendsReportFiltersConfig
  },
  zendesk_c2f_trends: {
    name: "Support Config To Fix Trends",
    application: IntegrationTypes.JIRAZENDESK,
    chart_type: ChartType?.AREA,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    composite: true,
    composite_transform: {
      total_tickets: "zendesk_c2f_total_tickets"
    },
    chart_props: {
      unit: "Tickets",
      chartProps: chartProps,
      areaProps: [],
      stackedArea: true
    },
    uri: "jira_zendesk_resolved_tickets_trend",
    method: "list",
    filters: {},
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: jiraSalesforceZendeskCommonRangeChoiceMapping(IntegrationTypes.JIRAZENDESK),
    supported_filters: jiraZenDeskSupportedFilters,
    [CustomFieldMappingKey.CUSTOM_FIELD_MAPPING_KEY]: zendeskCustomFieldsMapping,
    transformFunction: data => trendReportTransformer(data),
    [SHOW_SETTINGS_TAB]: true,
    [REPORT_FILTERS_CONFIG]: ZendeskConfigToFixTrendsReportFiltersConfig
  }
};
