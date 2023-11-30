import { ChartType } from "../../../shared-resources/containers/chart-container/ChartType";
import { testrailsTransformer } from "../../../custom-hooks/helpers/seriesData.helper";
import { trendReportTransformer } from "../../../custom-hooks/helpers/trendReport.helper";
import { testrailsSupportedFilters } from "../supported-filters.constant";
import { testRailsDrilldown } from "../drilldown.constants";
import { ChartContainerType } from "dashboard/helpers/helper";
import { show_value_on_bar } from "./constant";
import { FE_BASED_FILTERS, PREV_REPORT_TRANSFORMER } from "./names";
import { HIDE_CUSTOM_FIELDS, REPORT_FILTERS_CONFIG } from "./names";
import {
  TestrailsTestReportFiltersConfig,
  getDrillDownType
} from "dashboard/reports/testRails/tests-report/filter.config";
import { TestrailsTestTrendsReportFiltersConfig } from "dashboard/reports/testRails/tests-trend-report/filter.config";
import { TestrailsTestEstimateReportFiltersConfig } from "dashboard/reports/testRails/tests-estimate-report/filter.config";
import { TestrailsTestEstimateTrendsReportFiltersConfig } from "dashboard/reports/testRails/tests-estimate-trend-report/filter.config";
import { TestrailsTestEstimateForecastTrendsReportFiltersConfig } from "dashboard/reports/testRails/tests-estimate-forecast-trend-report/filter.config";
import { TestrailsTestEstimateForecastReportFiltersConfig } from "dashboard/reports/testRails/tests-estimate-forecast-report/filter.config";
import {
  TESTRAILS_FILTER_LABEL_MAPPING,
  convertChartType
} from "dashboard/reports/testRails/commonTestRailsReports.constants";
import { CSV_DRILLDOWN_TRANSFORMER } from "../filter-key.mapping";
import { testrailsCsvDrilldownDataTransformer } from "dashboard/helpers/csv-transformers/testrailsDataTransformer";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { transformSCMPRsReportPrevQuery } from "dashboard/reports/scm/pr-report/helper";

const testrailsStackFilters = [
  "milestone",
  "project",
  "test_plan",
  "test_run",
  "priority",
  "status",
  "assignee",
  "type"
];

const chartProps = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

export const TestrailsDashboard = {
  testrails_tests_report: {
    name: "TestRail Test Report",
    application: IntegrationTypes.TESTRAILS,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "testrails_tests_report",
    method: "list",
    [PREV_REPORT_TRANSFORMER]: data => transformSCMPRsReportPrevQuery(data),
    filters: {},
    xaxis: true,
    chart_props: {
      barProps: [
        {
          name: "Total Tests",
          dataKey: "total_tests"
        }
      ],
      unit: "Tests",
      chartProps: chartProps,
      stacked: false
    },
    default_query: {
      metric: "test_case_count"
    },
    defaultAcross: "milestone",
    stack_filters: testrailsStackFilters,
    supported_filters: testrailsSupportedFilters,
    transformFunction: data => testrailsTransformer(data),
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    drilldown: testRailsDrilldown,
    [REPORT_FILTERS_CONFIG]: TestrailsTestReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: false,
    getDrillDownType: getDrillDownType,
    acrossFilterLabelMapping: TESTRAILS_FILTER_LABEL_MAPPING,
    stackFilterLabelMapping: TESTRAILS_FILTER_LABEL_MAPPING,
    across: ["status"],
    [CSV_DRILLDOWN_TRANSFORMER]: testrailsCsvDrilldownDataTransformer,
    allow_key_for_stacks: true,
    convertChartType: convertChartType
  },
  testrails_tests_trend_report: {
    name: "TestRail Test Trend Report",
    application: IntegrationTypes.TESTRAILS,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "testrails_tests_report",
    composite: true,
    composite_transform: {
      total_tests: "total_testrails_tests"
    },
    method: "list",
    filters: {
      across: "trend"
    },
    xaxis: false,
    chart_props: {
      unit: "Tests",
      chartProps: chartProps
    },
    supported_filters: testrailsSupportedFilters,
    drilldown: testRailsDrilldown,
    transformFunction: data => trendReportTransformer(data),
    [REPORT_FILTERS_CONFIG]: TestrailsTestTrendsReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: false
  },
  testrails_tests_estimate_report: {
    name: "TestRail Test Estimate Report",
    application: IntegrationTypes.TESTRAILS,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "testrails_tests_estimate_report",
    method: "list",
    filters: {},
    defaultFilterKey: "median",
    defaultAcross: "milestone",
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
      unit: "Tests",
      sortBy: "median",
      chartProps: chartProps
    },
    stack_filters: testrailsStackFilters,
    supported_filters: testrailsSupportedFilters,
    drilldown: testRailsDrilldown,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    transformFunction: data => testrailsTransformer(data),
    [REPORT_FILTERS_CONFIG]: TestrailsTestEstimateReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  testrails_tests_estimate_trend_report: {
    name: "TestRail Test Estimate Trend Report",
    application: IntegrationTypes.TESTRAILS,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "testrails_tests_estimate_report",
    method: "list",
    filters: {
      across: "trend"
    },
    xaxis: false,
    composite: true,
    composite_transform: {
      min: "testrails_tests_estimate_min",
      median: "testrails_tests_estimate_median",
      max: "testrails_tests_estimate_max"
    },
    chart_props: {
      unit: "Tests",
      chartProps: chartProps
    },
    supported_filters: testrailsSupportedFilters,
    drilldown: testRailsDrilldown,
    transformFunction: data => trendReportTransformer(data),
    [REPORT_FILTERS_CONFIG]: TestrailsTestEstimateTrendsReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  testrails_tests_estimate_forecast_report: {
    name: "TestRail Test Estimate Forecast Report",
    application: IntegrationTypes.TESTRAILS,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "testrails_tests_estimate_forecast_report",
    method: "list",
    filters: {},
    defaultFilterKey: "median",
    defaultAcross: "milestone",
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
      unit: "Tests",
      sortBy: "median",
      chartProps: chartProps
    },
    stack_filters: testrailsStackFilters,
    supported_filters: testrailsSupportedFilters,
    drilldown: testRailsDrilldown,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    [REPORT_FILTERS_CONFIG]: TestrailsTestEstimateForecastReportFiltersConfig,
    transformFunction: data => testrailsTransformer(data),
    [HIDE_CUSTOM_FIELDS]: true
  },
  testrails_tests_estimate_forecast_trend_report: {
    name: "TestRail Test Estimate Forecast Trend Report",
    application: IntegrationTypes.TESTRAILS,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "testrails_tests_estimate_forecast_report",
    method: "list",
    filters: {
      across: "trend"
    },
    xaxis: false,
    composite: true,
    composite_transform: {
      min: "testrails_tests_estimate_forecast_min",
      median: "testrails_tests_estimate_forecast_median",
      max: "testrails_tests_estimate_forecast_max"
    },
    chart_props: {
      unit: "Tests",
      chartProps: chartProps
    },
    supported_filters: testrailsSupportedFilters,
    drilldown: testRailsDrilldown,
    transformFunction: data => trendReportTransformer(data),
    [REPORT_FILTERS_CONFIG]: TestrailsTestEstimateForecastTrendsReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  }
};
