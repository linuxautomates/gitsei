import { ChartType } from "../../../shared-resources/containers/chart-container/ChartType";
import { seriesDataTransformer, tableTransformer } from "../../../custom-hooks/helpers/seriesData.helper";
import { trendReportTransformer } from "../../../custom-hooks/helpers/trendReport.helper";
import { salesForceSupportedFilters } from "../supported-filters.constant";
import { topCustomersReportDrilldownTransformer } from "dashboard/helpers/drilldown-transformers";
import { ChartContainerType } from "../../helpers/helper";
import { salesforceDrilldown } from "../drilldown.constants";
import { SalesForceTopCustomerTableConfig } from "dashboard/pages/dashboard-tickets/configs/salesforceTableConfig";
import { HIDE_CUSTOM_FIELDS, PREVIEW_DISABLED, REPORT_FILTERS_CONFIG, FE_BASED_FILTERS } from "./names";
import { hygieneDefaultSettings } from "../helper";
import { HIDE_REPORT, SHOW_SETTINGS_TAB } from "../filter-key.mapping";
import { WIDGET_VALIDATION_FUNCTION } from "../filter-name.mapping";
import { hygieneWeightValidationHelper } from "../../helpers/widgetValidation.helper";
import { show_value_on_bar } from "./constant";
import { SalesforceBounceReportFiltersConfig } from "dashboard/reports/salesforce/bounce-report/filter.config";
import { SalesforceHopsReportFiltersConfig } from "dashboard/reports/salesforce/hops-report/filter.config";
import { SalesforceResolutionTimeReportFiltersConfig } from "dashboard/reports/salesforce/resolution-time-report/filter.config";
import { SalesforceTicketsReportFiltersConfig } from "dashboard/reports/salesforce/tickets-report/filter.config";
import { SalesforceHygieneReportFiltersConfig } from "dashboard/reports/salesforce/hygiene-report/filter.config";
import { SalesforceTicketsTrendReportFiltersConfig } from "dashboard/reports/salesforce/ticket-trends-report/filter.config";
import { SalesforceTopCustomersReportFiltersConfig } from "dashboard/reports/salesforce/top-customers-report/filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const chartProps = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

const salesForceHygieneTypes = ["IDLE", "POOR_DESCRIPTION", "NO_CONTACT", "MISSED_RESOLUTION_TIME"];

export const SalesForceDashboards = {
  salesforce_bounce_report: {
    name: "Support Bounce Report",
    application: IntegrationTypes.SALESFORCE,
    xaxis: true,
    defaultAcross: "status",
    chart_type: ChartType?.SCATTER,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      yDataKey: "median",
      rangeY: ["min", "max"],
      unit: "Bounces"
    },
    uri: "salesforce_bounce_report",
    method: "list",
    filters: {},
    supported_filters: salesForceSupportedFilters,
    drilldown: salesforceDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    [HIDE_REPORT]: true,
    [SHOW_SETTINGS_TAB]: true,
    [REPORT_FILTERS_CONFIG]: SalesforceBounceReportFiltersConfig
  },
  salesforce_hops_report: {
    name: "Support Hops Report",
    application: IntegrationTypes.SALESFORCE,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    defaultAcross: "status",
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
    uri: "salesforce_hops_report",
    method: "list",
    filters: {},
    supported_filters: salesForceSupportedFilters,
    drilldown: salesforceDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    [HIDE_REPORT]: true,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    [SHOW_SETTINGS_TAB]: true,
    [REPORT_FILTERS_CONFIG]: SalesforceHopsReportFiltersConfig
  },
  // salesforce_response_time_report: {
  //   name: "Salesforce Response Time Report",
  //   application: IntegrationTypes.SALESFORCE,
  //   chart_type: ChartType.BAR,
  //   xaxis: true,
  //   chart_props: {
  //     barProps: [
  //       {
  //         name: "min",
  //         dataKey: "min"
  //       },
  //       {
  //         name: "median",
  //         dataKey: "median"
  //       },
  //       {
  //         name: "max",
  //         dataKey: "max"
  //       }
  //     ],
  //     stacked: false,
  //     unit: "Days",
  //     sortBy: "median",
  //     chartProps: chartProps
  //   },
  //   uri: "salesforce_response_time_report",
  //   method: "list",
  //   filters: {},
  //   supported_filters: salesForceSupportedFilters,
  //   convertTo: "days",
  //   transformFunction: data => seriesDataTransformer(data)
  // },
  salesforce_resolution_time_report: {
    name: "Support Resolution Time Report",
    application: IntegrationTypes.SALESFORCE,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    xaxis: true,
    defaultAcross: "status",
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
    uri: "salesforce_resolution_time_report", //BACKEND CHANGES
    method: "list",
    filters: {},
    supported_filters: salesForceSupportedFilters,
    convertTo: "days",
    drilldown: salesforceDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    [SHOW_SETTINGS_TAB]: true,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    [HIDE_REPORT]: true,
    [REPORT_FILTERS_CONFIG]: SalesforceResolutionTimeReportFiltersConfig
  },
  salesforce_tickets_report: {
    name: "Support Tickets Report",
    application: IntegrationTypes.SALESFORCE,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "status",
    chart_props: {
      barProps: [
        {
          name: "total_cases",
          dataKey: "total_cases",
          unit: "Tickets"
        }
      ],
      stacked: false,
      unit: "Tickets",
      sortBy: "total_tickets",
      chartProps: chartProps
    },
    uri: "salesforce_hygiene_report", //BACKEND CHANGES
    method: "list",
    filters: {},
    supported_filters: salesForceSupportedFilters,
    drilldown: salesforceDrilldown,
    transformFunction: data => seriesDataTransformer(data),
    [SHOW_SETTINGS_TAB]: true,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    [HIDE_REPORT]: true,
    [REPORT_FILTERS_CONFIG]: SalesforceTicketsReportFiltersConfig
  },
  salesforce_hygiene_report: {
    name: "Support Hygiene Report",
    application: IntegrationTypes.SALESFORCE,
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
      chartProps: {
        barGap: 0,
        margin: { top: 20, right: 5, left: 5, bottom: 50 }
      }
    },
    uri: "salesforce_hygiene_report",
    method: "list",
    filters: {
      across: "hygiene_type"
    },
    hygiene_uri: "salesforce_tickets",
    hygiene_trend_uri: "salesforce_tickets",
    hygiene_types: salesForceHygieneTypes,
    default_query: hygieneDefaultSettings,
    drilldown: salesforceDrilldown,
    [PREVIEW_DISABLED]: true,
    supported_filters: salesForceSupportedFilters,
    [SHOW_SETTINGS_TAB]: true,
    [HIDE_REPORT]: true,
    [WIDGET_VALIDATION_FUNCTION]: hygieneWeightValidationHelper,
    [REPORT_FILTERS_CONFIG]: SalesforceHygieneReportFiltersConfig
  },
  salesforce_tickets_report_trends: {
    name: "Support Ticket Trends Report",
    application: IntegrationTypes.SALESFORCE,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    composite: true,
    composite_transform: {
      total_tickets: "total_salesforce_tickets"
    },
    chart_props: {
      unit: "Cases",
      chartProps: chartProps
    },
    uri: "salesforce_hygiene_report",
    method: "list",
    filters: {
      across: "trend"
    },
    supported_filters: salesForceSupportedFilters,
    drilldown: salesforceDrilldown,
    transformFunction: data => trendReportTransformer(data),
    [REPORT_FILTERS_CONFIG]: SalesforceTicketsTrendReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  salesforce_top_customers_report: {
    name: "Support Top Customers Report",
    application: IntegrationTypes.SALESFORCE,
    chart_type: ChartType?.TABLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    defaultAcross: "status",
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
          title: "Total Cases",
          key: "total_cases",
          dataIndex: "total_cases",
          width: "25%"
        }
      ],
      chartProps: {}
    },
    uri: "salesforce_hygiene_report", //BACKEND CHANGES
    method: "list",
    filters: {},
    defaultFilters: {
      age: { $lt: 30 }
    },
    supported_filters: salesForceSupportedFilters,
    drilldown: {
      title: "Salesforce Top Customer Report",
      uri: "salesforce_hygiene_report",
      columns: SalesForceTopCustomerTableConfig,
      supported_filters: salesForceSupportedFilters,
      application: "top_customer",
      drilldownTransformFunction: data => topCustomersReportDrilldownTransformer(data)
    },
    transformFunction: data => tableTransformer(data),
    [SHOW_SETTINGS_TAB]: true,
    [HIDE_REPORT]: true,
    [REPORT_FILTERS_CONFIG]: SalesforceTopCustomersReportFiltersConfig
  }
};
