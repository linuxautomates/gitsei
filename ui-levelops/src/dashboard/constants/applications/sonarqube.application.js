import {
  seriesDataTransformer,
  sonarqubeIssuesReportTransformer,
  sonarqubeTrendReportTransformer,
  trendReportTransformer,
  sonarQubeDuplicatiionBubbleChartTransformer
} from "custom-hooks/helpers";
import { ChartType } from "../../../shared-resources/containers/chart-container/ChartType";
import { sonarqubeSupportedFilters, sonarqubemetricsSupportedFilters } from "../supported-filters.constant";
import { ChartContainerType } from "dashboard/helpers/helper";
import { sonarQubeDrilldown, sonarQubeMetricDrilldown } from "../drilldown.constants";
import { show_value_on_bar } from "./constant";
import { FE_BASED_FILTERS, HIDE_TOTAL_TOOLTIP } from "./names";
import { HIDE_CUSTOM_FIELDS, REPORT_FILTERS_CONFIG } from "./names";
import { SonarqubeIssuesReportFiltersConfig } from "dashboard/reports/sonarqube/issues-report/filter.config";
import { SonarqubeEffortReportFiltersConfig } from "dashboard/reports/sonarqube/sonarqube-effort-report/filter.config";
import { SonarqubeIssuesReportTrendsFiltersConfig } from "dashboard/reports/sonarqube/sonarqube-issues-report-trends/filter.config";
import { SonarqubeEffortReportTrendsFiltersConfig } from "dashboard/reports/sonarqube/sonarqube-effort-report-trends/filter.config";
import { SonarqubeMetricsReportFiltersConfig } from "dashboard/reports/sonarqube/sonarqube-metrics-report/filter.config";
import { SonarqubeMetricsTrendsReportFiltersConfig } from "dashboard/reports/sonarqube/sonarqube-metrics-trend-report/filter.config";
import { SonarqubeCodeComplexityReportFiltersConfig } from "dashboard/reports/sonarqube/sonarqube-code-complexity-report/filter.config";
import { SonarqubeCodeComplexityTrendsReportFiltersConfig } from "dashboard/reports/sonarqube/sonarqube-code-complexity-trend-report/filter.config";
import { SonarqubeCodeDuplicationReportFiltersConfig } from "dashboard/reports/sonarqube/sonarqube-code-duplication-report/filter.config";
import { SonarqubeCodeDuplicationTrendsReportFiltersConfig } from "dashboard/reports/sonarqube/sonarqube-code-duplication-trend-report/filter.config";

const application = "sonarqube";

const chartProps = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

export const SonarQubeDashboard = {
  sonarqube_issues_report: {
    name: "SonarQube Issues Report",
    application,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultAcross: "type",
    xaxis: true,
    stack_filters: sonarqubeSupportedFilters.values,
    chart_props: {
      barProps: [
        {
          name: "total_issues",
          dataKey: "total_issues",
          unit: "Issues"
        }
      ],
      stacked: false,
      unit: "Issues",
      sortBy: "total_issues",
      chartProps: chartProps
    },
    uri: "sonarqube_issues_report",
    method: "list",
    filters: {},
    supported_filters: sonarqubeSupportedFilters,
    drilldown: sonarQubeDrilldown,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    transformFunction: data => sonarqubeIssuesReportTransformer(data),
    [REPORT_FILTERS_CONFIG]: SonarqubeIssuesReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  sonarqube_effort_report: {
    name: "SonarQube Effort Report",
    application,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultAcross: "type",
    defaultFilterKey: "sum",
    xaxis: true,
    chart_props: {
      barProps: [
        {
          name: "min",
          dataKey: "min"
        },
        {
          name: "sum",
          dataKey: "sum"
        },
        {
          name: "max",
          dataKey: "max"
        }
      ],
      stacked: false,
      unit: "Days",
      sortBy: "sum",
      chartProps: chartProps
    },
    uri: "sonarqube_effort_report",
    method: "list",
    filters: {},
    //convertTo: "mins",
    supported_filters: sonarqubeSupportedFilters,
    drilldown: sonarQubeDrilldown,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    transformFunction: data => seriesDataTransformer(data),
    [REPORT_FILTERS_CONFIG]: SonarqubeEffortReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  sonarqube_issues_report_trends: {
    name: "SonarQube Issues Report Trends",
    application,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    composite: true,
    composite_transform: {
      total_issues: "total_sonarQube_issues"
    },
    chart_props: {
      unit: "Issues",
      chartProps: chartProps
    },
    uri: "sonarqube_issues_report",
    method: "list",
    filters: {
      across: "trend"
    },
    supported_filters: sonarqubeSupportedFilters,
    drilldown: sonarQubeDrilldown,
    transformFunction: data => trendReportTransformer(data),
    [REPORT_FILTERS_CONFIG]: SonarqubeIssuesReportTrendsFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  sonarqube_effort_report_trends: {
    name: "SonarQube Effort Report Trends",
    application,
    xaxis: false,
    composite: true,
    composite_transform: {
      // min: "sonarQube_effort_min",
      sum: "sonarQube_effort_sum"
      // max: "sonarQube_effort_max"
    },
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      unit: "Days",
      chartProps: chartProps
    },
    uri: "sonarqube_effort_report",
    method: "list",
    filters: {
      across: "trend"
    },
    supported_filters: sonarqubeSupportedFilters,
    drilldown: sonarQubeDrilldown,
    transformFunction: data => sonarqubeTrendReportTransformer(data),
    [REPORT_FILTERS_CONFIG]: SonarqubeEffortReportTrendsFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  sonarqube_metrics_report: {
    name: "SonarQube Metrics Report",
    application,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    across: ["project", "visibility"],
    defaultAcross: "project",
    chart_props: {
      barProps: [
        {
          name: "sum",
          dataKey: "sum"
        }
      ],
      stacked: false,
      unit: "Coverage %",
      sortBy: "median",
      chartProps: chartProps
    },
    uri: "sonarqube_metrics_report",
    method: "list",
    filters: {
      metrics: ["coverage"]
    },
    chart_click_enable: false,
    supported_filters: sonarqubemetricsSupportedFilters,
    drilldown: sonarQubeMetricDrilldown,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    transformFunction: data => seriesDataTransformer(data),
    [REPORT_FILTERS_CONFIG]: SonarqubeMetricsReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  sonarqube_metrics_trend_report: {
    name: "SonarQube Metrics Trend Report",
    application,
    xaxis: false,
    composite: true,
    composite_transform: {
      min: "sonarQube_metrics_min",
      median: "sonarQube_metrics_median",
      max: "sonarQube_metrics_max"
    },
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      unit: "Coverage %",
      chartProps: chartProps
    },
    uri: "sonarqube_metrics_report",
    method: "list",
    filters: {
      across: "trend",
      metrics: ["coverage"]
    },
    chart_click_enable: false,
    supported_filters: sonarqubemetricsSupportedFilters,
    drilldown: sonarQubeMetricDrilldown,
    transformFunction: data => sonarqubeTrendReportTransformer(data),
    [REPORT_FILTERS_CONFIG]: SonarqubeMetricsTrendsReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  sonarqube_code_complexity_report: {
    name: "SonarQube Code Complexity Report",
    application,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    defaultAcross: "project",
    chart_props: {
      barProps: [
        {
          name: "sum",
          dataKey: "sum"
        }
      ],
      stacked: false,
      unit: "Complexity",
      sortBy: "median",
      chartProps: chartProps
    },
    uri: "sonarqube_metrics_report",
    method: "list",
    filters: {},
    default_query: {
      metrics: ["cognitive_complexity"]
    },
    chart_click_enable: false,
    supported_filters: sonarqubemetricsSupportedFilters,
    drilldown: sonarQubeMetricDrilldown,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    transformFunction: data => seriesDataTransformer(data),
    [REPORT_FILTERS_CONFIG]: SonarqubeCodeComplexityReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  sonarqube_code_complexity_trend_report: {
    name: "SonarQube Code Complexity Trend Report",
    application,
    xaxis: false,
    composite: true,
    defaultAcross: "trend",
    composite_transform: {
      min: "sonarQube_code_complexity_min",
      median: "sonarQube_code_complexity_median",
      max: "sonarQube_code_complexity_max"
    },
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      unit: "Complexity",
      chartProps: chartProps
    },
    uri: "sonarqube_metrics_report",
    method: "list",
    filters: {},
    default_query: {
      metrics: ["cognitive_complexity"]
    },
    chart_click_enable: false,
    supported_filters: sonarqubemetricsSupportedFilters,
    drilldown: sonarQubeMetricDrilldown,
    transformFunction: data => sonarqubeTrendReportTransformer(data),
    [REPORT_FILTERS_CONFIG]: SonarqubeCodeComplexityTrendsReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true,
    [HIDE_TOTAL_TOOLTIP]: true
  },

  sonarqube_code_duplication_report: {
    name: "SonarQube Code Duplication Report",
    application,
    chart_type: ChartType?.BUBBLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    chart_props: {
      bubbleProps: [
        {
          dataKey: "xAxis"
        },
        {
          dataKey: "yAxis"
        },
        {
          dataKey: "zAxis"
        }
      ],
      stacked: false,
      yunit: "Percent Duplicate lines",
      xunit: "Total lines of code",
      zunit: "Total Duplicate Lines",
      xunitLabel: "Lines Of Code",
      yunitLabel: "Percent Duplicated Lines",
      chartProps: chartProps
    },
    uri: "sonarqube_metrics_report",
    method: "list",
    filters: {
      across: "project"
    },
    default_query: {
      metrics: ["duplicated_lines_density"]
    },
    chart_click_enable: false,
    supported_filters: sonarqubemetricsSupportedFilters,
    drilldown: sonarQubeMetricDrilldown,
    transformFunction: data => sonarQubeDuplicatiionBubbleChartTransformer(data),
    [REPORT_FILTERS_CONFIG]: SonarqubeCodeDuplicationReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },

  sonarqube_code_duplication_trend_report: {
    name: "SonarQube Code Duplication Trend Report",
    application,
    xaxis: false,
    composite: true,
    composite_transform: {
      duplicated_density: "duplicated_density"
    },
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      unit: "Percent Duplicated Lines",
      chartProps: chartProps
    },
    uri: "sonarqube_metrics_report",
    method: "list",
    filters: {
      across: "trend",
      metrics: ["duplicated_lines_density"]
    },
    chart_click_enable: false,
    supported_filters: sonarqubemetricsSupportedFilters,
    drilldown: sonarQubeMetricDrilldown,
    transformFunction: data => sonarqubeTrendReportTransformer(data),
    [REPORT_FILTERS_CONFIG]: SonarqubeCodeDuplicationTrendsReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  }
};
