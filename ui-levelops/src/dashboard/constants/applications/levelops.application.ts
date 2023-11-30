import { getReportsPage, getWorkitemsPage } from "constants/routePaths";
import {
  levelopsAsssessmentCountReportTransformer,
  seriesDataTransformer,
  trendReportTransformer
} from "custom-hooks/helpers";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { levelOpsAssessmentDrillDown, levelOpsIssuesDrilldown } from "../drilldown.constants";
import { LevelOpsAssessmentResponseTimeTableConfig } from "dashboard/pages/dashboard-tickets/configs/levelOpsAssessmentTableConfig";
import { levelopsResponseTimeReportDrilldownTransformer } from "dashboard/helpers/drilldown-transformers/levelopsDrilldownTransformer";
import { levelopsAssessmentTimeResponseTableTransformer } from "custom-hooks/helpers/trendReport.helper";
import { CSV_DRILLDOWN_TRANSFORMER } from "../filter-key.mapping";
import { levelopsAssessmentTimeTableReportCSVTransformer } from "dashboard/helpers/csv-transformers/levelops-reports/assessment-time-report-table.transformer";
import { levelopsAssessmentCountReportCSVTransformer } from "dashboard/helpers/csv-transformers/levelops-reports/assessment-count-report.transformer";
import { widgetFilterPreviewTransformer } from "custom-hooks/helpers/helper";
import { show_value_on_bar } from "./constant";
import { HIDE_CUSTOM_FIELDS, REPORT_FILTERS_CONFIG, FE_BASED_FILTERS } from "./names";
import { LevelopsAssessmentCountReportFiltersConfig } from "dashboard/reports/levelops/levelops-assessment-count-report/filter.config";
import { convertToDays } from "utils/timeUtils";

const application: string = "levelops";

export type DynamicGraphFilter = {
  label: string;
  filterType: "apiMultiSelect" | "apiSelect" | "select" | "multiSelect" | "dateRange";
  filterField: string;
  position: "right" | "left";
  searchField?: string;
  uri?: string;
  options?: Array<{ label: string; value: any }>;
  selectedValue?: any;

  // Which keys in the apiData correspond to
  // the labels and values in the filter options?
  labelKey?: string;
  valueKey?: string;
};

const chartProps = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

const assessmentsSupportedFilters: Array<DynamicGraphFilter> = [
  {
    label: "Assessments",
    uri: "questionnaires",
    searchField: "name",
    filterType: "apiMultiSelect",
    position: "left",
    filterField: "questionnaire_template_id"
  },
  {
    label: "Progress",
    filterType: "select",
    filterField: "completed",
    position: "left",
    options: [
      { label: "COMPLETED", value: "true" },
      { label: "NOT COMPLETED", value: "false" }
    ]
  },
  { label: "Tags", filterType: "apiMultiSelect", position: "left", filterField: "tags", uri: "tags" },
  { label: "Updated Between", filterType: "dateRange", position: "right", filterField: "updated_at" },
  { label: "Created Between", filterType: "dateRange", position: "right", filterField: "created_at" }
];

const workItemSupportedFilters: Array<DynamicGraphFilter> = [
  {
    label: "Project",
    filterType: "apiMultiSelect",
    filterField: "products",
    uri: "products",
    position: "left"
  },
  {
    label: "Reporter",
    filterType: "apiMultiSelect",
    filterField: "reporters",
    uri: "users",
    searchField: "email",
    position: "left"
  },
  {
    label: "Assignee",
    filterType: "apiMultiSelect",
    filterField: "assignees",
    uri: "users",
    searchField: "email",
    position: "left",
    labelKey: "email",
    valueKey: "id"
  },
  {
    label: "States",
    filterType: "apiMultiSelect",
    filterField: "states",
    searchField: "name",
    uri: "states",
    position: "left"
  },
  { label: "Tags", filterType: "apiMultiSelect", position: "left", filterField: "tags", uri: "tags" },
  { label: "Updated Between", filterType: "dateRange", position: "right", filterField: "updated_at" }
];

export const LevelOpsDashboard = {
  levelops_assessment_count_report: {
    name: "Assessment Count Report",
    application,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    show_max: true,
    stack_filters: ["questionnaire_template_id", "assignee", "completed", "submitted", "tag"],
    across: ["questionnaire_template_id", "assignee", "completed", "submitted", "tag", "created", "updated"],
    defaultAcross: "questionnaire_template_id",
    chart_props: {
      barProps: [
        {
          name: "total",
          dataKey: "total",
          unit: "Assessments"
        }
      ],
      stacked: false,
      unit: "Assessments",
      sortBy: "total",
      chartProps: chartProps
    },
    uri: "quiz_aggs_count_report",
    method: "list",
    filters: {},
    supported_filters: assessmentsSupportedFilters,
    [CSV_DRILLDOWN_TRANSFORMER]: levelopsAssessmentCountReportCSVTransformer,
    transformFunction: (data: any) => levelopsAsssessmentCountReportTransformer(data),
    reportURL: () => `${getReportsPage()}?tab=assessments`,
    drilldown: levelOpsAssessmentDrillDown,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    widgetFilterPreviewTransformer: (filters: any) => widgetFilterPreviewTransformer(filters),
    [REPORT_FILTERS_CONFIG]: LevelopsAssessmentCountReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  levelops_assessment_response_time_report: {
    name: "Assessment Response Time Report",
    application,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    across: ["questionnaire_template_id", "assignee", "completed", "submitted", "tag", "created", "updated"],
    defaultAcross: "questionnaire_template_id",
    xaxis: true,
    show_max: true,
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
    uri: "quiz_aggs_response_time_report",
    method: "list",
    filters: {},
    supported_filters: assessmentsSupportedFilters,
    convertTo: "days",
    transformFunction: (data: any) => seriesDataTransformer(data),
    reportURL: () => `${getReportsPage()}?tab=assessments`,
    drilldown: levelOpsAssessmentDrillDown,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    widgetFilterPreviewTransformer: (filters: any) => widgetFilterPreviewTransformer(filters)
  },
  levelops_assessment_response_time__table_report: {
    name: "Assessment Response Time Report (Table)",
    application,
    chart_type: ChartType?.TABLE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    defaultFilterKey: "median",
    defaultAcross: "questionnaire_template_id",
    xaxis: true,
    across: ["questionnaire_template_id", "assignee", "completed", "submitted", "tag", "created", "updated"],
    chart_props: {
      size: "small",
      columns: [
        {
          title: "Key",
          key: "key",
          dataIndex: "key",
          width: "25%"
        },
        {
          title: "Min",
          key: "min",
          dataIndex: "min",
          width: "25%",
          render: (item: any, record: any, index: any) => (item ? `${convertToDays(item)} days` : "")
        },
        {
          title: "Median",
          key: "median",
          dataIndex: "median",
          width: "25%",
          render: (item: any, record: any, index: any) => (item ? `${convertToDays(item)} days` : "")
        },
        {
          title: "Max",
          key: "max",
          dataIndex: "max",
          width: "25%",
          render: (item: any, record: any, index: any) => (item ? `${convertToDays(item)} days` : "")
        }
      ],
      chartProps: {}
    },
    uri: "quiz_aggs_response_time_table_report",
    method: "list",
    filters: {},
    supported_filters: assessmentsSupportedFilters,
    convertTo: "days",
    transformFunction: (data: any) => levelopsAssessmentTimeResponseTableTransformer(data),
    reportURL: () => `${getReportsPage()}?tab=assessments`,
    [CSV_DRILLDOWN_TRANSFORMER]: levelopsAssessmentTimeTableReportCSVTransformer,
    drilldown: {
      title: "Assessments Response Time Report",
      uri: "quiz_aggs_response_time_table_report",
      application: "levelops_assessment_response_time",
      supported_filters: [],
      columns: LevelOpsAssessmentResponseTimeTableConfig,
      drilldownTransformFunction: (data: any) => levelopsResponseTimeReportDrilldownTransformer(data)
    },
    widgetFilterPreviewTransformer: (filters: any) => widgetFilterPreviewTransformer(filters)
  },
  levelops_assessment_count_report_trends: {
    name: "Assessment Count Report Trends",
    application,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    stacks: false,
    chart_props: {
      unit: "Assessments",
      chartProps: chartProps
    },
    uri: "quiz_aggs_count_report",
    method: "list",
    filters: {
      across: "trend"
    },
    supported_filters: assessmentsSupportedFilters,
    [CSV_DRILLDOWN_TRANSFORMER]: levelopsAssessmentCountReportCSVTransformer,
    transformFunction: (data: any) => trendReportTransformer(data),
    reportURL: () => `${getReportsPage()}?tab=assessments`,
    drilldown: levelOpsAssessmentDrillDown
  },
  levelops_workitem_count_report: {
    name: "Issues Count Report",
    application,
    chart_type: ChartType?.BAR,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    show_max: true,
    stack_filters: ["state", "assignee", "reporter", "tag", "product"],
    across: ["state", "assignee", "reporter", "tag", "product", "created", "updated"],
    defaultAcross: "state",
    chart_props: {
      barProps: [
        {
          name: "total",
          dataKey: "total",
          unit: "Issues"
        }
      ],
      stacked: false,
      unit: "Issues",
      sortBy: "total",
      chartProps: chartProps
    },
    uri: "work_item_aggs_count_report",
    method: "list",
    filters: {},
    supported_filters: workItemSupportedFilters,
    transformFunction: (data: any) => levelopsAsssessmentCountReportTransformer(data),
    reportURL: getWorkitemsPage,
    [FE_BASED_FILTERS]: {
      show_value_on_bar
    },
    drilldown: levelOpsIssuesDrilldown
  },
  levelops_workitem_count_report_trends: {
    name: "Issues Count Report Trends",
    application,
    chart_type: ChartType?.LINE,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: false,
    stacks: false,
    chart_props: {
      unit: "Issues",
      chartProps: chartProps
    },
    uri: "work_item_aggs_count_report",
    method: "list",
    filters: {
      across: "trend"
    },
    supported_filters: workItemSupportedFilters,
    transformFunction: (data: any) => trendReportTransformer(data),
    reportURL: getWorkitemsPage,
    drilldown: levelOpsIssuesDrilldown
  }
};
