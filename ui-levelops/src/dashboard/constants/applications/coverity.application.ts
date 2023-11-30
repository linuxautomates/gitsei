import { HIDE_CUSTOM_FIELDS, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import { ChartContainerType, transformCoverityPrevReportQuery } from "../../helpers/helper";
import { coverityIssueSupportedFilters } from "../supported-filters.constant";
import { coverityDrilldown } from "../drilldown.constants";
import { WIDGET_DATA_SORT_FILTER_KEY } from "../filter-name.mapping";
import { FE_BASED_FILTERS, NO_LONGER_SUPPORTED_FILTER, PREV_REPORT_TRANSFORMER } from "./names";
import { WidgetFilterType } from "../enums/WidgetFilterType.enum";
import { WIDGET_CONFIGURATION_KEYS } from "../../../constants/widgets";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "../WidgetDataSortingFilter.constant";
import { coverityDefectsReportTransform } from "../../../custom-hooks/helpers/helper";
import { trendReportTransformer, statReportTransformer } from "custom-hooks/helpers";
import { unset } from "lodash";
import { COVERITY_FILTER_KEY_MAPPING } from "dashboard/reports/coverity/commonCoverityReports.constants";
import { IssuesReportFiltersConfig } from "dashboard/reports/coverity/issues-report/filters.config";
import { IssueSingleStatFiltersConfig } from "dashboard/reports/coverity/issues-stat-report/filters.config";
import { IssuesTrendReportFiltersConfig } from "dashboard/reports/coverity/issues-trend-report/filters.config";
import { show_value_on_bar } from "./constant";

const chartProps = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

const issuesQuery = {
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
};

export const statDefaultQuery = {
  time_period: 1,
  agg_type: "average"
};

const removeNoLongerSupportedFilters = (filter: any) => {
  let newfilter = { ...(filter || {}) };
  const notSupportedFilter = [
    "first_detected",
    "last_detected",
    "cov_defect_first_detected",
    "cov_defect_last_detected"
  ];
  notSupportedFilter.forEach((item: string) => {
    unset(newfilter, [item]);
  });
  return newfilter;
};

export const CoverityReports = {
  coverity_issues_report: {
    name: "Coverity Issues Report",
    application: "coverity",
    chart_type: ChartType?.BAR,
    defaultAcross: "last_detected",
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    xaxis: true,
    appendAcrossOptions: [
      { label: "First detected", value: "first_detected" },
      { label: "Last detected", value: "last_detected" }
    ],
    chart_props: {
      barProps: [
        {
          name: "total_defects",
          dataKey: "total_defects",
          unit: "Defects"
        }
      ],
      stacked: false,
      unit: "Defects",
      sortBy: "total_defects",
      chartProps: chartProps
    },
    uri: "coverity_defects_report",
    method: "list",
    filters: {},
    default_query: issuesQuery,
    supportExcludeFilters: true,
    supported_filters: coverityIssueSupportedFilters,
    drilldown: coverityDrilldown,
    [PREV_REPORT_TRANSFORMER]: (data: any) => transformCoverityPrevReportQuery(data),
    xAxisLabelTransform: (params: any) => {
      const { across, item = {} } = params;
      const { key, additional_key } = item;

      let newLabel = key;

      if (["first_detected", "last_detected", "first_detected_stream", "last_detected_stream"].includes(across)) {
        newLabel = additional_key;
      }

      return newLabel;
    },
    transformFunction: (data: any) => coverityDefectsReportTransform(data),
    [FE_BASED_FILTERS]: {
      first_detected_at: {
        type: WidgetFilterType.TIME_BASED_FILTERS,
        label: "First Detected At",
        BE_key: "cov_defect_first_detected_at",
        configTab: WIDGET_CONFIGURATION_KEYS.FILTERS
      },
      last_detected_at: {
        type: WidgetFilterType.TIME_BASED_FILTERS,
        label: "Last Detected At",
        BE_key: "cov_defect_last_detected_at",
        configTab: WIDGET_CONFIGURATION_KEYS.FILTERS
      },
      show_value_on_bar
    },
    valuesToFilters: COVERITY_FILTER_KEY_MAPPING,
    [HIDE_CUSTOM_FIELDS]: true,
    [NO_LONGER_SUPPORTED_FILTER]: (filters: any) => removeNoLongerSupportedFilters(filters),
    [REPORT_FILTERS_CONFIG]: IssuesReportFiltersConfig
  },
  coverity_issues_trend_report: {
    name: "Coverity Issues Trend Report",
    application: "coverity",
    chart_type: ChartType?.LINE,
    defaultAcross: "snapshot_created",
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      barProps: [
        {
          name: "total_defects",
          dataKey: "total_defects",
          unit: "Defects"
        }
      ],
      stacked: false,
      unit: "Defects",
      sortBy: "total_defects",
      chartProps: chartProps
    },
    uri: "coverity_defects_report",
    method: "list",
    filters: {},
    default_query: issuesQuery,
    supportExcludeFilters: true,
    supported_filters: coverityIssueSupportedFilters,
    [PREV_REPORT_TRANSFORMER]: (data: any) => transformCoverityPrevReportQuery(data),
    drilldown: coverityDrilldown,
    xAxisLabelTransform: (params: any) => {
      const { across, item = {} } = params;
      const { key, additional_key } = item;

      let newLabel = key;

      if (["first_detected", "last_detected", "last_detected_stream", "first_detected_stream"].includes(across)) {
        newLabel = additional_key;
      }

      return newLabel;
    },
    transformFunction: (data: any) => trendReportTransformer(data),
    [FE_BASED_FILTERS]: {
      first_detected_at: {
        type: WidgetFilterType.TIME_BASED_FILTERS,
        label: "First Detected At",
        BE_key: "cov_defect_first_detected_at",
        configTab: WIDGET_CONFIGURATION_KEYS.FILTERS
      },
      last_detected_at: {
        type: WidgetFilterType.TIME_BASED_FILTERS,
        label: "Last Detected At",
        BE_key: "cov_defect_last_detected_at",
        configTab: WIDGET_CONFIGURATION_KEYS.FILTERS
      }
    },
    valuesToFilters: COVERITY_FILTER_KEY_MAPPING,
    [NO_LONGER_SUPPORTED_FILTER]: (filters: any) => removeNoLongerSupportedFilters(filters),
    [REPORT_FILTERS_CONFIG]: IssuesTrendReportFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  },
  coverity_issues_stat_report: {
    name: "Coverity Issues Single Stat",
    application: "coverity",
    chart_type: ChartType?.STATS,
    defaultAcross: "snapshot_created",
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    chart_props: {
      unit: "Defects"
    },
    uri: "coverity_defects_report",
    method: "list",
    filters: {},
    compareField: "total_defects",
    default_query: statDefaultQuery,
    supportExcludeFilters: true,
    supported_filters: coverityIssueSupportedFilters,
    drilldown: {},
    transformFunction: (data: any) => statReportTransformer(data),
    supported_widget_types: ["stats"],
    [PREV_REPORT_TRANSFORMER]: (data: any) => transformCoverityPrevReportQuery(data),
    [FE_BASED_FILTERS]: {
      first_detected_at: {
        type: WidgetFilterType.TIME_BASED_FILTERS,
        label: "First Detected At",
        BE_key: "cov_defect_first_detected_at",
        configTab: WIDGET_CONFIGURATION_KEYS.FILTERS
      },
      last_detected_at: {
        type: WidgetFilterType.TIME_BASED_FILTERS,
        label: "Last Detected At",
        BE_key: "cov_defect_last_detected_at",
        configTab: WIDGET_CONFIGURATION_KEYS.FILTERS
      }
    },
    valuesToFilters: COVERITY_FILTER_KEY_MAPPING,
    [NO_LONGER_SUPPORTED_FILTER]: (filters: any) => removeNoLongerSupportedFilters(filters),
    [REPORT_FILTERS_CONFIG]: IssueSingleStatFiltersConfig,
    [HIDE_CUSTOM_FIELDS]: true
  }
};
