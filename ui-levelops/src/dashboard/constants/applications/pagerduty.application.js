import { ChartType } from "../../../shared-resources/containers/chart-container/ChartType";
import { pagerdutyServicesTransformer, trendReportTransformer } from "../../../custom-hooks/helpers";
import {
  pagerdutyFilters,
  pagerdutyIncidentSupportedFilters,
  pagerdutyServicesSupportedFilters
} from "../supported-filters.constant";
import { ChartContainerType } from "../../helpers/helper";
import { PagerdutyHotspotTableConfig } from "dashboard/pages/dashboard-tickets/configs";
import { genericDrilldownTransformer } from "dashboard/helpers/drilldown-transformers";
import moment from "moment";
import { pagerdutyWidgets } from "./pagerduty";
import { DEPRECATED_REPORT, HIDE_CUSTOM_FIELDS, REPORT_FILTERS_CONFIG } from "./names";
import { PagerdutyStacksReportFiltersConfig } from "dashboard/reports/pagerduty/stacks-report/filter.config";
import { PagerdutyReleaseincidentsReportFiltersConfig } from "dashboard/reports/pagerduty/release-incidents/filter.config";
import { PagerdutyAckTrendsReportFiltersConfig } from "dashboard/reports/pagerduty/ack-trend/filter.config";
import { PagerdutyAfterHoursReportFiltersConfig } from "dashboard/reports/pagerduty/after-hours/filter.config";
import { PagerdutyIncidentReportTrendsFiltersConfig } from "dashboard/reports/pagerduty/incident-report-trends/filter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

const chartProps = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

const jenkinsSupportedFilters = {
  uri: "jenkins_pipelines_jobs_filter_values",
  values: ["cicd_job_id"]
};

// data is in seconds.
const ackTrendTransform = data => {
  let result = data;
  const isValid = moment(data).isValid();
  if (isValid) {
    const seconds = data;
    const duration = moment.duration(seconds, "seconds");
    result = duration.format("y[yrs] M[mos] d[d] h[hrs] m[m] s[s]");
  }

  return result;
};

export const PagerDutyDashboards = {
  pagerduty_incident_report: {
    name: "PagerDuty Incident Report",
    application: IntegrationTypes.PAGERDUTY,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.PRODUCTS_AGGS_API_WRAPPER,
    uri: "product_aggs",
    method: "list",
    filters: {
      integration_type: "PAGERDUTY"
    },
    xaxis: false,
    chart_props: {
      unit: "Incidents/Alerts",
      sortBy: "from",
      chartProps: chartProps
    },
    [DEPRECATED_REPORT]: true
  },
  pagerduty_alert_report: {
    name: "PagerDuty Alerts Report",
    application: IntegrationTypes.PAGERDUTY,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.PRODUCTS_AGGS_API_WRAPPER,
    uri: "product_aggs",
    method: "list",
    filters: {
      integration_type: "PAGERDUTY"
    },
    xaxis: false,
    chart_props: {
      unit: "Incidents/Alerts",
      sortBy: "from",
      chartProps: chartProps
    },
    [DEPRECATED_REPORT]: true
  },
  pagerduty_response_report: {
    name: "PagerDuty Response Times Report",
    application: IntegrationTypes.PAGERDUTY,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.PRODUCTS_AGGS_API_WRAPPER,
    uri: "product_aggs",
    method: "list",
    filters: {
      integration_type: "PAGERDUTY"
    },
    xaxis: false,
    chart_props: {
      unit: "Hours",
      sortBy: "from",
      chartProps: chartProps
    },
    [DEPRECATED_REPORT]: true
  },
  pagerduty_hotspot_report: {
    name: "PagerDuty Stacks Report",
    application: IntegrationTypes.PAGERDUTY,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "services_report_aggregate",
    method: "list",
    filters: {
      type: ["pagerduty"]
    },
    defaultAcross: "incident_priority",
    chart_props: {
      unit: "Jobs",
      chartProps: chartProps,
      barProps: [
        {
          name: "count",
          dataKey: "count"
        }
      ],
      stacked: false
    },
    defaultStacks: ["incident_priority"],
    xaxis: true,
    across: ["incident_priority", "incident_urgency", "alert_severity"],
    stack_filters: ["incident_priority", "incident_urgency", "alert_severity"],
    drilldown: {
      title: "Pagerduty Report",
      uri: "services_report_list",
      application: "pagerduty_hotspot_report",
      columns: PagerdutyHotspotTableConfig,
      supported_filters: pagerdutyServicesSupportedFilters,
      drilldownTransformFunction: data => genericDrilldownTransformer(data)
    },
    supported_filters: pagerdutyServicesSupportedFilters,
    transformFunction: data => pagerdutyServicesTransformer(data),
    [DEPRECATED_REPORT]: true,
    [REPORT_FILTERS_CONFIG]: PagerdutyStacksReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  pagerduty_release_incidents: {
    name: "PagerDuty Release Incidents",
    application: IntegrationTypes.PAGERDUTY,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "pagerduty_release_incidents",
    method: "list",
    defaultAcross: "incident_priority",
    chart_props: {
      unit: "Count",
      chartProps: chartProps,
      barProps: [
        {
          name: "count",
          dataKey: "count"
        }
      ],
      stacked: true
    },
    xaxis: true,
    across: ["incident_priority", "incident_urgency", "alert_severity"],
    supported_filters: [pagerdutyServicesSupportedFilters, jenkinsSupportedFilters],
    defaultFilters: {
      cicd_job_ids: []
    },
    transformFunction: data => pagerdutyServicesTransformer(data),
    requiredFilters: ["cicd_job_id"],
    [DEPRECATED_REPORT]: true,
    [REPORT_FILTERS_CONFIG]: PagerdutyReleaseincidentsReportFiltersConfig
  },
  pagerduty_ack_trend: {
    name: "PagerDuty Ack Trend",
    application: IntegrationTypes.PAGERDUTY,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "pagerduty_ack_trend",
    method: "list",
    defaultAcross: "incident_priority",
    chart_props: {
      transformFn: ackTrendTransform,
      totalCountTransformFn: ackTrendTransform,
      unit: "Seconds",
      chartProps: chartProps,
      barProps: [
        {
          name: "count",
          dataKey: "count"
        }
      ],
      stacked: true
    },
    xaxis: false,
    across: ["incident_priority", "incident_urgency", "alert_severity"],
    supported_filters: pagerdutyFilters,
    transformFunction: data => pagerdutyServicesTransformer(data),
    [DEPRECATED_REPORT]: true,
    [REPORT_FILTERS_CONFIG]: PagerdutyAckTrendsReportFiltersConfig
  },
  pagerduty_after_hours: {
    name: "PagerDuty After Hours",
    application: IntegrationTypes.PAGERDUTY,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "pagerduty_after_hours",
    method: "list",
    defaultAcross: "incident_priority",
    chart_props: {
      unit: "Minutes",
      chartProps: chartProps,
      barProps: [
        {
          name: "value",
          dataKey: "value"
        }
      ],
      stacked: false
    },
    xaxis: false,
    supported_filters: pagerdutyServicesSupportedFilters,
    transformFunction: data => pagerdutyServicesTransformer(data),
    [DEPRECATED_REPORT]: true,
    [REPORT_FILTERS_CONFIG]: PagerdutyAfterHoursReportFiltersConfig
  },
  pagerduty_incident_report_trends: {
    name: "PagerDuty Incident Report Trends",
    application: IntegrationTypes.PAGERDUTY,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "pagerduty_incident_rates",
    method: "list",
    chart_props: {
      unit: "Count",
      chartProps: chartProps
    },
    filters: {
      across: "trend"
    },
    xaxis: false,
    composite: true,
    composite_transform: {
      count: "pagerduty_incident_count"
    },
    supported_filters: pagerdutyIncidentSupportedFilters,
    transformFunction: data => trendReportTransformer(data),
    requiredFilters: ["pd_service"],
    singleSelectFilters: ["pd_service"],
    [DEPRECATED_REPORT]: true,
    [REPORT_FILTERS_CONFIG]: PagerdutyIncidentReportTrendsFiltersConfig
  },
  ...pagerdutyWidgets
};
