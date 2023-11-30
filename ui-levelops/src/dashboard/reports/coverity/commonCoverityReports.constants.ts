import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { WidgetFilterType } from "dashboard/constants/enums/WidgetFilterType.enum";
import { WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { unset } from "lodash";

export const issuesQuery = {
  [WIDGET_DATA_SORT_FILTER_KEY]: widgetDataSortingOptionsDefaultValue[widgetDataSortingOptionsNodeType.NON_TIME_BASED]
};

export const statDefaultQuery = {
  time_period: 1,
  agg_type: "average"
};

export const removeNoLongerSupportedFilters = (filter: any) => {
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

export const COVERITY_FILTER_KEY_MAPPING: Record<string, string> = {
  impact: "cov_defect_impacts",
  category: "cov_defect_categories",
  kind: "cov_defect_kinds",
  checker_name: "cov_defect_checker_names",
  component_name: "cov_defect_component_names",
  type: "cov_defect_types",
  domain: "cov_defect_domains",
  first_detected_stream: "cov_defect_first_detected_streams",
  last_detected_stream: "cov_defect_last_detected_streams",
  file: "cov_defect_file_paths",
  function: "cov_defect_function_names",
  first_detected_at: "cov_defect_first_detected_at",
  last_detected_at: "cov_defect_last_detected_at"
};

export const coverityReportsFeBasedFilter = {
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
};
