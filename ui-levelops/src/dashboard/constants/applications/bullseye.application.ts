import { bullseyeDataTransformer, bullseyeTrendTransformer } from "custom-hooks/helpers";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";

import { bullseyeDrilldown } from "../drilldown.constants";
import { bullseyeSupportedFilters } from "../supported-filters.constant";
import { FILTER_NAME_MAPPING } from "../filter-name.mapping";
import { HIDE_CUSTOM_FIELDS, REPORT_FILTERS_CONFIG, FE_BASED_FILTERS } from "./names";
import { BranchCoverageReportFiltersConfig } from "dashboard/reports/bullseye/branch-coverage-report/filters.config";
import { FunctionCoverageReportFiltersConfig } from "dashboard/reports/bullseye/function-coverage-report/filters.config";
import { DecisionCoverageReportFiltersConfig } from "dashboard/reports/bullseye/decision-coverage-report/filters.config";
import { CodeCoverageReportFiltersConfig } from "dashboard/reports/bullseye/code-coverage-report/filters.config";
import { FunctionCoverageTrendReportFiltersConfig } from "dashboard/reports/bullseye/function-coverage-trend-report/filters.config";
import { BranchCoverageTrendReportFiltersConfig } from "dashboard/reports/bullseye/branch-coverage-trend-report/filters.config";
import { DecisionCoverageTrendReportFiltersConfig } from "dashboard/reports/bullseye/decision-coverage-trend-report/filters.config";
import { CodeCoverageTrendReportFiltersConfig } from "dashboard/reports/bullseye/code-coverage-trend-report/filters.config";
import { show_value_on_bar } from "./constant";

const BULLSEYE_APPLICATION = "bullseye";
const BULLSEYE_APPEND_ACROSS_OPTIONS = [
  { label: "Jenkins Job Name", value: "job_name" },
  { label: "Jenkins Job Path", value: "job_normalized_full_name" }
];
const chartProps = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

const defaultSorts: { [x: string]: any[] } = {
  function: [
    {
      id: "functions_covered",
      desc: true
    }
  ],
  branch: [
    {
      id: "conditions_covered",
      desc: true
    }
  ],
  decision: [
    {
      id: "decisions_covered",
      desc: true
    }
  ],
  code: [
    {
      id: "total_functions",
      desc: true
    },
    {
      id: "total_decisions",
      desc: true
    },
    {
      id: "total_conditions",
      desc: true
    }
  ]
};

export const BullseyeDashboard = {
  bullseye_function_coverage_report: {
    name: "Bullseye Function Coverage Report",
    application: BULLSEYE_APPLICATION,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    uri: "bullseye_coverage_report",
    method: "list",
    chart_props: {
      barProps: [
        {
          name: "Function Coverage",
          dataKey: "function_percentage_coverage"
        }
      ],
      unit: "Percentage (%)",
      stacked: false,
      transformFn: (data: any, dataKey: string) => (dataKey.includes("percentage") ? data + "%" : data),
      chartProps: chartProps
    },
    dataKey: "function_percentage_coverage",
    defaultAcross: "project",
    appendAcrossOptions: BULLSEYE_APPEND_ACROSS_OPTIONS,
    filters: {
      sort: defaultSorts.function
    },
    supported_filters: bullseyeSupportedFilters,
    drilldown: { ...bullseyeDrilldown, defaultSort: defaultSorts.function },
    transformFunction: (data: any) => bullseyeDataTransformer(data),
    [FILTER_NAME_MAPPING]: {
      job_normalized_full_names: "jenkins job path" // used for global filter label
    },
    [REPORT_FILTERS_CONFIG]: FunctionCoverageReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    }
  },
  bullseye_branch_coverage_report: {
    name: "Bullseye Branch Coverage Report",
    application: BULLSEYE_APPLICATION,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "bullseye_coverage_report",
    method: "list",
    xaxis: true,
    chart_props: {
      barProps: [
        {
          name: "Branch Coverage",
          dataKey: "condition_percentage_coverage"
        }
      ],
      unit: "Percentage (%)",
      stacked: false,
      transformFn: (data: any, dataKey: string) => (dataKey.includes("percentage") ? data + "%" : data),
      chartProps: chartProps
    },
    dataKey: "condition_percentage_coverage",
    defaultAcross: "project",
    supported_filters: bullseyeSupportedFilters,
    appendAcrossOptions: BULLSEYE_APPEND_ACROSS_OPTIONS,
    filters: {
      sort: defaultSorts.branch
    },
    drilldown: { ...bullseyeDrilldown, defaultSort: defaultSorts.branch },
    transformFunction: (data: any) => bullseyeDataTransformer(data),
    [REPORT_FILTERS_CONFIG]: BranchCoverageReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    }
  },
  bullseye_decision_coverage_report: {
    name: "Bullseye Decision Coverage Report",
    application: BULLSEYE_APPLICATION,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "bullseye_coverage_report",
    method: "list",
    xaxis: true,
    chart_props: {
      barProps: [
        {
          name: "Decision Coverage",
          dataKey: "decision_percentage_coverage"
        }
      ],
      unit: "Percentage (%)",
      stacked: false,
      transformFn: (data: any, dataKey: string) => (dataKey.includes("percentage") ? data + "%" : data),
      chartProps: chartProps
    },
    dataKey: "decision_percentage_coverage",
    defaultAcross: "project",
    supported_filters: bullseyeSupportedFilters,
    appendAcrossOptions: BULLSEYE_APPEND_ACROSS_OPTIONS,
    filters: {
      sort: defaultSorts.decision
    },
    drilldown: { ...bullseyeDrilldown, defaultSort: defaultSorts.decision },
    transformFunction: (data: any) => bullseyeDataTransformer(data),
    [REPORT_FILTERS_CONFIG]: DecisionCoverageReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    }
  },
  bullseye_code_coverage_report: {
    name: "Bullseye Code Coverage Report",
    application: BULLSEYE_APPLICATION,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "bullseye_coverage_report",
    method: "list",
    xaxis: true,
    chart_props: {
      barProps: [
        {
          name: "Code Coverage",
          dataKey: "coverage_percentage"
        }
      ],
      unit: "Percentage (%)",
      transformFn: (data: any) => data + "%",
      stacked: false,
      chartProps: chartProps
    },
    dataKey: "coverage_percentage",
    defaultAcross: "project",
    supported_filters: bullseyeSupportedFilters,
    appendAcrossOptions: BULLSEYE_APPEND_ACROSS_OPTIONS,
    filters: {
      sort: defaultSorts.code
    },
    drilldown: { ...bullseyeDrilldown, defaultSort: defaultSorts.code },
    transformFunction: (data: any) => bullseyeDataTransformer(data),
    [REPORT_FILTERS_CONFIG]: CodeCoverageReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    }
  },
  bullseye_function_coverage_trend_report: {
    name: "Bullseye Function Coverage Trend Report",
    application: BULLSEYE_APPLICATION,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "bullseye_coverage_report",
    method: "list",
    xaxis: false,
    chart_props: {
      unit: "Percentage (%)",
      chartProps: chartProps,
      lineProps: [{ dataKey: "function_percentage_coverage", transformer: (data: any) => data + "%" }]
    },
    composite: true,
    composite_transform: {
      function_percentage_coverage: "Function Percentage Coverage"
    },
    tooltipMapping: { function_percentage_coverage: "function_coverage" },
    dataKey: "function_percentage_coverage",
    appendAcrossOptions: BULLSEYE_APPEND_ACROSS_OPTIONS,
    filters: {
      across: "trend",
      sort: defaultSorts.function
    },
    supported_filters: bullseyeSupportedFilters,
    drilldown: { ...bullseyeDrilldown, defaultSort: defaultSorts.function },
    transformFunction: (data: any) => bullseyeTrendTransformer(data),
    [REPORT_FILTERS_CONFIG]: FunctionCoverageTrendReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  bullseye_branch_coverage_trend_report: {
    name: "Bullseye Branch Coverage Trend Report",
    application: BULLSEYE_APPLICATION,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "bullseye_coverage_report",
    method: "list",
    xaxis: false,
    chart_props: {
      unit: "Percentage (%)",
      chartProps: chartProps,
      lineProps: [{ dataKey: "condition_percentage_coverage", transformer: (data: any) => data + "%" }]
    },
    composite: true,
    composite_transform: {
      condition_percentage_coverage: "Condition Percentage Coverage"
    },
    tooltipMapping: { condition_percentage_coverage: "condition_coverage" },
    dataKey: "condition_percentage_coverage",
    appendAcrossOptions: BULLSEYE_APPEND_ACROSS_OPTIONS,
    filters: {
      across: "trend",
      sort: defaultSorts.branch
    },
    supported_filters: bullseyeSupportedFilters,
    drilldown: { ...bullseyeDrilldown, defaultSort: defaultSorts.branch },
    transformFunction: (data: any) => bullseyeTrendTransformer(data),
    [REPORT_FILTERS_CONFIG]: BranchCoverageTrendReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  bullseye_decision_coverage_trend_report: {
    name: "Bullseye Decision Coverage Trend Report",
    application: BULLSEYE_APPLICATION,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "bullseye_coverage_report",
    method: "list",
    xaxis: false,
    chart_props: {
      unit: "Percentage (%)",
      chartProps: chartProps,
      lineProps: [{ dataKey: "decision_percentage_coverage", transformer: (data: any) => data + "%" }]
    },
    composite: true,
    composite_transform: {
      decision_percentage_coverage: "Decision Percentage Coverage"
    },
    tooltipMapping: { decision_percentage_coverage: "decision_coverage" },
    dataKey: "decision_percentage_coverage",
    appendAcrossOptions: BULLSEYE_APPEND_ACROSS_OPTIONS,
    filters: {
      across: "trend",
      sort: defaultSorts.decision
    },
    supported_filters: bullseyeSupportedFilters,
    drilldown: { ...bullseyeDrilldown, defaultSort: defaultSorts.decision },
    transformFunction: (data: any) => bullseyeTrendTransformer(data),
    [REPORT_FILTERS_CONFIG]: DecisionCoverageTrendReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  bullseye_code_coverage_trend_report: {
    name: "Bullseye Code Coverage Score Trend Report",
    application: BULLSEYE_APPLICATION,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "bullseye_coverage_report",
    method: "list",
    xaxis: false,
    chart_props: {
      unit: "Percentage (%)",
      chartProps: chartProps,
      lineProps: [{ dataKey: "coverage_percentage", transformer: (data: any) => data + "%" }]
    },
    composite: true,
    composite_transform: {
      coverage_percentage: "Coverage Percentage"
    },
    tooltipMapping: { coverage_percentage: "code_coverage" },
    dataKey: "coverage_percentage",
    appendAcrossOptions: BULLSEYE_APPEND_ACROSS_OPTIONS,
    filters: {
      across: "trend",
      sort: defaultSorts.code
    },
    supported_filters: bullseyeSupportedFilters,
    drilldown: { ...bullseyeDrilldown, defaultSort: defaultSorts.code },
    transformFunction: (data: any) => bullseyeTrendTransformer(data),
    [REPORT_FILTERS_CONFIG]: CodeCoverageTrendReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  }
};
