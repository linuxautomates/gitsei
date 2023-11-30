import { ChartType } from "../../../shared-resources/containers/chart-container/ChartType";
import { seriesDataTransformer, tableTransformer, trendReportTransformer } from "../../../custom-hooks/helpers";
import { zendeskSupportedFilters } from "../supported-filters.constant";
import { topCustomersReportDrilldownTransformer } from "dashboard/helpers/drilldown-transformers";
import { ChartContainerType } from "../../helpers/helper";
import { zendeskDrilldown } from "../drilldown.constants";
import { topCustomerTableConfig } from "../../pages/dashboard-tickets/configs/githubTableConfig";
import { ZendeskStacksReportsKey, hygieneDefaultSettings } from "../helper";
import { TIME_FILTER_RANGE_CHOICE_MAPPER, PREVIEW_DISABLED, REPORT_FILTERS_CONFIG, FE_BASED_FILTERS } from "./names";
import { SHOW_SETTINGS_TAB } from "../filter-key.mapping";
import { WIDGET_VALIDATION_FUNCTION } from "../filter-name.mapping";
import { hygieneWeightValidationHelper } from "../../helpers/widgetValidation.helper";
import { getXAxisLabel } from "shared-resources/charts/bar-chart/bar-chart.helper";
import { show_value_on_bar } from "./constant";
import { ZendeskBounceReportFiltersConfig } from "dashboard/reports/zendesk/bounce-report/filters.config";
import { ZendeskHopsReportFiltersConfig } from "dashboard/reports/zendesk/hops-report/filters.config";
import { ZendeskResponseTimeReportFiltersConfig } from "dashboard/reports/zendesk/response-time-report/filters.config";
import { ZendeskResolutionTimeReportFiltersConfig } from "dashboard/reports/zendesk/resolution-time-report/filters.config";
import { ZendeskTicketsReportFiltersConfig } from "dashboard/reports/zendesk/tickets-report/filters.config";
import { ZendeskHygieneReportFiltersConfig } from "dashboard/reports/zendesk/hygiene-report/filters.config";
import { ZendeskTicketsReopensReportFiltersConfig } from "dashboard/reports/zendesk/reopens-report/filters.config";
import { ZendeskRepliesReportFiltersConfig } from "dashboard/reports/zendesk/replies-report/filters.config";
import { ZendeskAgentWaitTimeReportFiltersConfig } from "dashboard/reports/zendesk/agent-wait-time-report/filters.config";
import { ZendeskRequesterWaitTimeReportFiltersConfig } from "dashboard/reports/zendesk/requester-wait-time-report/filters.config";
import { ZendeskBounceTrendsReportFiltersConfig } from "dashboard/reports/zendesk/bounce-report-trends/filters.config";
import { ZendeskHopsReportTrendsFiltersConfig } from "dashboard/reports/zendesk/hops-report-trends/filters.config";
import { ZendeskResponseTimeReportTrendsFiltersConfig } from "dashboard/reports/zendesk/response-time-report-trends/filters.config";
import { ZendeskResolutionTimeReportTrendsFiltersConfig } from "dashboard/reports/zendesk/resolution-time-report-trends/filters.config";
import { ZendeskTicketsReportTrendsFiltersConfig } from "dashboard/reports/zendesk/tickets-report-trends/filters.config";
import { ZendeskHygieneReportTrendsFiltersConfig } from "dashboard/reports/zendesk/hygiene-report-trends/filters.config";
import { ZendeskRequesterWaitTimeReportTrendsFiltersConfig } from "dashboard/reports/zendesk/requester-wait-time-report-trends/filters.config";
import { ZendeskAgentWaitTimeReportTrendsFiltersConfig } from "dashboard/reports/zendesk/agent-wait-time-report-trends/filters.config";
import { ZendeskRepliesReportTrendsFiltersConfig } from "dashboard/reports/zendesk/replies-report-trends/filters.config";
import { ZendeskTicketsReopensReportTrendsFiltersConfig } from "dashboard/reports/zendesk/reopens-report-trends/filters.config";
import { ZendeskTopCustomersReportFiltersConfig } from "dashboard/reports/zendesk/top-customers-report/filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const chartProps = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

const zendeskHygieneTypes = ["IDLE", "POOR_DESCRIPTION", "NO_CONTACT", "MISSED_RESOLUTION_TIME"];

export const ZendeskDashboards = {
  zendesk_bounce_report: {
    name: "Support Bounce Report",
    application: IntegrationTypes.ZENDESK,
    xaxis: true,
    defaultAcross: "brand",
    chart_type: ChartType?.SCATTER,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      yDataKey: "median",
      rangeY: ["min", "max"],
      unit: "Bounces"
    },
    uri: "zendesk_bounce_report",
    method: "list",
    filters: {},
    supported_filters: zendeskSupportedFilters,
    drilldown: zendeskDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      created_at: "zendesk_created_at"
    },
    [SHOW_SETTINGS_TAB]: true,
    [REPORT_FILTERS_CONFIG]: ZendeskBounceReportFiltersConfig
  },
  zendesk_hops_report: {
    name: "Support Hops report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    xaxis: true,
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
      unit: "Hops",
      sortBy: "median",
      chartProps: chartProps
    },
    uri: "zendesk_hops_report",
    method: "list",
    filters: {},
    defaultAcross: "brand",
    supported_filters: zendeskSupportedFilters,
    drilldown: zendeskDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      created_at: "zendesk_created_at"
    },
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    [SHOW_SETTINGS_TAB]: true,
    [REPORT_FILTERS_CONFIG]: ZendeskHopsReportFiltersConfig
  },
  zendesk_response_time_report: {
    name: "Support Response Time Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    xaxis: true,
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
    uri: "zendesk_response_time_report",
    method: "list",
    filters: {},
    defaultAcross: "brand",
    supported_filters: zendeskSupportedFilters,
    convertTo: "days",
    drilldown: zendeskDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      created_at: "zendesk_created_at"
    },
    [REPORT_FILTERS_CONFIG]: ZendeskResponseTimeReportFiltersConfig
  },
  zendesk_resolution_time_report: {
    name: "Support Resolution Time Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    xaxis: true,
    defaultAcross: "brand",
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
    uri: "zendesk_resolution_time_report",
    method: "list",
    filters: {},
    supported_filters: zendeskSupportedFilters,
    convertTo: "days",
    drilldown: zendeskDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      created_at: "zendesk_created_at"
    },
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    [SHOW_SETTINGS_TAB]: true,
    [REPORT_FILTERS_CONFIG]: ZendeskResolutionTimeReportFiltersConfig
  },
  zendesk_tickets_report: {
    name: "Support Tickets Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "brand",
    chart_props: {
      barProps: [
        {
          name: "total_tickets",
          dataKey: "total_tickets",
          unit: "Tickets"
        }
      ],
      stacked: false,
      unit: "Tickets",
      sortBy: "total_tickets",
      chartProps: chartProps
    },
    uri: "zendesk_tickets_report",
    method: "list",
    filters: {},
    supported_filters: zendeskSupportedFilters,
    drilldown: zendeskDrilldown,
    stack_filters: zendeskSupportedFilters.values,
    getTotalKey: () => "total_tickets",
    [ZendeskStacksReportsKey.ZENDESK_STACKED_KEY]: true,
    transformFunction: data => seriesDataTransformer(data),
    xAxisLabelTransform: params => getXAxisLabel(params),
    getSortKey: params => {
      const { across } = params;
      let key = undefined;
      if (["ticket_created"].includes(across)) {
        key = "key";
      }

      return key;
    },
    getSortOrder: params => {
      const { interval, across } = params;
      let key = undefined;
      if (["ticket_created"].includes(across)) {
        if (["month"].includes(interval)) {
          key = "asc";
        }
      }

      return key;
    },
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      created_at: "zendesk_created_at"
    },
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    [SHOW_SETTINGS_TAB]: true,
    [REPORT_FILTERS_CONFIG]: ZendeskTicketsReportFiltersConfig
  },
  zendesk_hygiene_report: {
    name: "Support Hygiene Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.SCORE,
    chart_container: ChartContainerType.HYGIENE_API_WRAPPER,
    xaxis: false,
    chart_props: {
      unit: "tickets",
      barProps: [
        {
          name: "total_tickets",
          dataKey: "total_tickets"
        }
      ],
      stacked: false,
      sortBy: "total_tickets",
      chartProps: chartProps
    },
    uri: "zendesk_hygiene_report",
    method: "list",
    filters: {
      across: "hygiene_type"
    },
    hygiene_uri: "zendesk_tickets",
    hygiene_trend_uri: "zendesk_tickets_report",
    hygiene_types: zendeskHygieneTypes,
    drilldown: zendeskDrilldown,
    [PREVIEW_DISABLED]: true,
    supported_filters: zendeskSupportedFilters,
    default_query: hygieneDefaultSettings,
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      created_at: "zendesk_created_at"
    },
    [SHOW_SETTINGS_TAB]: true,
    [WIDGET_VALIDATION_FUNCTION]: hygieneWeightValidationHelper,
    [REPORT_FILTERS_CONFIG]: ZendeskHygieneReportFiltersConfig
  },
  zendesk_reopens_report: {
    name: "Support Ticket Reopens Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    xaxis: true,
    defaultAcross: "brand",
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
      unit: "Reopens",
      sortBy: "median",
      chartProps: chartProps
    },
    uri: "zendesk_reopens_report",
    method: "list",
    filters: {},
    supported_filters: zendeskSupportedFilters,
    drilldown: zendeskDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      created_at: "zendesk_created_at"
    },
    [REPORT_FILTERS_CONFIG]: ZendeskTicketsReopensReportFiltersConfig
  },
  zendesk_replies_report: {
    name: "Support Replies Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    xaxis: true,
    defaultAcross: "brand",
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
      unit: "Replies",
      sortBy: "median",
      chartProps: chartProps
    },
    uri: "zendesk_replies_report",
    method: "list",
    filters: {},
    supported_filters: zendeskSupportedFilters,
    drilldown: zendeskDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      created_at: "zendesk_created_at"
    },
    [REPORT_FILTERS_CONFIG]: ZendeskRepliesReportFiltersConfig
  },
  zendesk_agent_wait_time_report: {
    name: "Support Agent Wait Time Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    xaxis: true,
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
    uri: "zendesk_agent_wait_time_report",
    method: "list",
    filters: {},
    defaultAcross: "brand",
    supported_filters: zendeskSupportedFilters,
    convertTo: "days",
    drilldown: zendeskDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      created_at: "zendesk_created_at"
    },
    [REPORT_FILTERS_CONFIG]: ZendeskAgentWaitTimeReportFiltersConfig
  },
  zendesk_requester_wait_time_report: {
    name: "Support Requester Wait Time Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    xaxis: true,
    defaultAcross: "brand",
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
    uri: "zendesk_requester_wait_time_report",
    method: "list",
    filters: {},
    supported_filters: zendeskSupportedFilters,
    convertTo: "days",
    drilldown: zendeskDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      created_at: "zendesk_created_at"
    },
    [REPORT_FILTERS_CONFIG]: ZendeskRequesterWaitTimeReportFiltersConfig
  },
  zendesk_bounce_report_trends: {
    name: "Support Bounce Trend Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    composite: true,
    composite_transform: {
      min: "zendesk_bounce_min",
      median: "zendesk_bounce_median",
      max: "zendesk_bounce_max"
    },
    chart_props: {
      unit: "Bounces",
      chartProps: chartProps
    },
    uri: "zendesk_bounce_report",
    method: "list",
    filters: {
      across: "trend"
    },
    supported_filters: zendeskSupportedFilters,
    drilldown: zendeskDrilldown,
    transformFunction: data => trendReportTransformer(data),
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      created_at: "zendesk_created_at"
    },
    [REPORT_FILTERS_CONFIG]: ZendeskBounceTrendsReportFiltersConfig
  },
  zendesk_hops_report_trends: {
    name: "Support Hops Trend Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    composite: true,
    composite_transform: {
      min: "zendesk_hops_min",
      median: "zendesk_hops_median",
      max: "zendesk_hops_max"
    },
    chart_props: {
      unit: "Hops",
      chartProps: {
        barGap: 0,
        margin: { top: 20, right: 10, left: 10, bottom: 50 }
      }
    },
    uri: "zendesk_hops_report",
    method: "list",
    filters: {
      across: "trend"
    },
    supported_filters: zendeskSupportedFilters,
    drilldown: zendeskDrilldown,
    transformFunction: data => trendReportTransformer(data),
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      created_at: "zendesk_created_at"
    },
    [REPORT_FILTERS_CONFIG]: ZendeskHopsReportTrendsFiltersConfig
  },
  zendesk_response_time_report_trends: {
    name: "Support Response Time Trend Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    composite: true,
    composite_transform: {
      min: "zendesk_response_time_min",
      median: "zendesk_response_time_median",
      max: "zendesk_response_time_max"
    },
    chart_props: {
      unit: "Days",
      chartProps: {
        barGap: 0,
        margin: { top: 20, right: 10, left: 10, bottom: 50 }
      }
    },
    uri: "zendesk_response_time_report",
    method: "list",
    filters: {
      across: "trend"
    },
    supported_filters: zendeskSupportedFilters,
    convertTo: "days",
    drilldown: zendeskDrilldown,
    transformFunction: data => trendReportTransformer(data),
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      created_at: "zendesk_created_at"
    },
    [REPORT_FILTERS_CONFIG]: ZendeskResponseTimeReportTrendsFiltersConfig
  },
  zendesk_resolution_time_report_trends: {
    name: "Support Resolution Time Trend Report",
    application: IntegrationTypes.ZENDESK,
    xaxis: false,
    composite: true,
    composite_transform: {
      min: "zendesk_resolution_time_min",
      median: "zendesk_resolution_time_median",
      max: "zendesk_resolution_time_max"
    },
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      unit: "Days",
      chartProps: chartProps
    },
    uri: "zendesk_resolution_time_report",
    method: "list",
    filters: {
      across: "trend"
    },
    supported_filters: zendeskSupportedFilters,
    convertTo: "days",
    drilldown: zendeskDrilldown,
    transformFunction: data => trendReportTransformer(data),
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      created_at: "zendesk_created_at"
    },
    [REPORT_FILTERS_CONFIG]: ZendeskResolutionTimeReportTrendsFiltersConfig
  },
  zendesk_tickets_report_trends: {
    name: "Support Tickets Trend Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    chart_props: {
      unit: "Tickets",
      chartProps: chartProps
    },
    composite: true,
    composite_transform: {
      total_tickets: "total_zendesk_tickets"
    },
    uri: "zendesk_tickets_report",
    method: "list",
    filters: {
      across: "trend"
    },
    supported_filters: zendeskSupportedFilters,
    drilldown: zendeskDrilldown,
    transformFunction: data => trendReportTransformer(data),
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      created_at: "zendesk_created_at"
    },
    [REPORT_FILTERS_CONFIG]: ZendeskTicketsReportTrendsFiltersConfig
  },
  zendesk_hygiene_report_trends: {
    name: "Support Hygiene Trend Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.HYGIENE_API_WRAPPER,
    xaxis: false,
    chart_props: {
      unit: "Score",
      chartProps: chartProps
    },
    uri: "zendesk_hygiene_report",
    method: "list",
    filters: {
      across: "trend"
    },
    hygiene_uri: "zendesk_tickets",
    hygiene_trend_uri: "zendesk_tickets_report",
    hygiene_types: zendeskHygieneTypes,
    drilldown: zendeskDrilldown,
    supported_filters: zendeskSupportedFilters,
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      created_at: "zendesk_created_at"
    },
    [WIDGET_VALIDATION_FUNCTION]: hygieneWeightValidationHelper,
    [REPORT_FILTERS_CONFIG]: ZendeskHygieneReportTrendsFiltersConfig
  },
  zendesk_requester_wait_time_report_trends: {
    name: "Support Requester Wait Time Trend Report",
    application: IntegrationTypes.ZENDESK,
    xaxis: false,
    composite: true,
    composite_transform: {
      min: "zendesk_requester_wait_time_min",
      median: "zendesk_requester_wait_time_median",
      max: "zendesk_requester_wait_time_max"
    },
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      unit: "Days",
      chartProps: chartProps
    },
    uri: "zendesk_requester_wait_time_report",
    method: "list",
    filters: {
      across: "trend"
    },
    supported_filters: zendeskSupportedFilters,
    convertTo: "days",
    drilldown: zendeskDrilldown,
    transformFunction: data => trendReportTransformer(data),
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      created_at: "zendesk_created_at"
    },
    [REPORT_FILTERS_CONFIG]: ZendeskRequesterWaitTimeReportTrendsFiltersConfig
  },
  zendesk_agent_wait_time_report_trends: {
    name: "Support Agent Wait Time Trend Report",
    application: IntegrationTypes.ZENDESK,
    xaxis: false,
    composite: true,
    composite_transform: {
      min: "zendesk_agent_wait_time_min",
      median: "zendesk_agent_wait_time_median",
      max: "zendesk_agent_wait_time_max"
    },
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      unit: "Days",
      chartProps: chartProps
    },
    uri: "zendesk_agent_wait_time_report",
    method: "list",
    filters: {
      across: "trend"
    },
    supported_filters: zendeskSupportedFilters,
    convertTo: "days",
    drilldown: zendeskDrilldown,
    transformFunction: data => trendReportTransformer(data),
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      created_at: "zendesk_created_at"
    },
    [REPORT_FILTERS_CONFIG]: ZendeskAgentWaitTimeReportTrendsFiltersConfig
  },
  zendesk_replies_report_trends: {
    name: "Support Replies Trend Report",
    application: IntegrationTypes.ZENDESK,
    xaxis: false,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      unit: "Days",
      chartProps: chartProps
    },
    composite: true,
    composite_transform: {
      min: "zendesk_replies_min",
      median: "zendesk_replies_median",
      max: "zendesk_replies_max"
    },
    uri: "zendesk_replies_report",
    method: "list",
    filters: {
      across: "trend"
    },
    supported_filters: zendeskSupportedFilters,
    drilldown: zendeskDrilldown,
    transformFunction: data => trendReportTransformer(data),
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      created_at: "zendesk_created_at"
    },
    [REPORT_FILTERS_CONFIG]: ZendeskRepliesReportTrendsFiltersConfig
  },
  zendesk_reopens_report_trends: {
    name: "Support Ticket Reopens Trend Report",
    application: IntegrationTypes.ZENDESK,
    xaxis: false,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      unit: "Days",
      chartProps: chartProps
    },
    composite: true,
    composite_transform: {
      min: "zendesk_reopens_min",
      median: "zendesk_reopens_median",
      max: "zendesk_reopens_max"
    },
    uri: "zendesk_reopens_report",
    method: "list",
    filters: {
      across: "trend"
    },
    supported_filters: zendeskSupportedFilters,
    drilldown: zendeskDrilldown,
    transformFunction: data => trendReportTransformer(data),
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      created_at: "zendesk_created_at"
    },
    [REPORT_FILTERS_CONFIG]: ZendeskTicketsReopensReportTrendsFiltersConfig
  },
  zendesk_top_customers_report: {
    name: "Support Top Customers Report",
    application: IntegrationTypes.ZENDESK,
    chart_type: ChartType?.TABLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    chart_props: {
      size: "small",
      columns: [
        {
          title: "Name",
          key: "key",
          dataIndex: "key",
          width: "25%"
        },
        {
          title: "Total Tickets",
          key: "total_tickets",
          dataIndex: "total_tickets",
          width: "25%"
        }
      ],
      chartProps: {}
    },
    uri: "zendesk_tickets_report",
    method: "list",
    filters: {},
    defaultFilters: {
      age: { $lt: 30 }
    },
    defaultAcross: "brand",
    supported_filters: zendeskSupportedFilters,
    drilldown: {
      title: "Zendesk Top Customer Report",
      uri: "zendesk_tickets_report",
      columns: topCustomerTableConfig,
      supported_filters: zendeskSupportedFilters,
      application: "top_customer",
      drilldownTransformFunction: data => topCustomersReportDrilldownTransformer(data)
    },
    transformFunction: data => tableTransformer(data),
    [TIME_FILTER_RANGE_CHOICE_MAPPER]: {
      created_at: "zendesk_created_at"
    },
    [SHOW_SETTINGS_TAB]: true,
    [REPORT_FILTERS_CONFIG]: ZendeskTopCustomersReportFiltersConfig
  }
};
