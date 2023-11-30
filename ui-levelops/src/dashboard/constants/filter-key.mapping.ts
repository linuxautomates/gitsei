import { filterMapping, ignoreFilterKeysType } from "dashboard/dashboard-types/common-types";
import { SPRINT } from "./applications/names";

//key used for get the filter mapping from the report
export const PARTIAL_FILTER_MAPPING_KEY = "partialFilterKeyMapping";

//partial filter key
export const PARTIAL_FILTER_KEY = "partial_filter_key";

//Filter key mapping
export const FILTER_KEY_MAPPING = "filter_key_mapping";

// for handling those cases where key is needed in stacked data but should be excluded from chart props
export const ALLOW_KEY_FOR_STACKS = "allow_key_for_stacks";

//partial filter transform key
export const PARTIAL_FILTER_TRANSFORM_KEY = "partial_filter_transform_key";

//disable partial filter select mapping key
export const DISABLE_PARTIAL_FILTER_SELECT_MAPPING_KEY = "disable_partial_filter_select_mapping_key";

//disable partial filter mapping key
export const DISABLE_PARTIAL_FILTER_MAPPING_KEY = "disable_partial_filter_mapping_key";

export const DISABLE_EXCLUDE_FILTER_MAPPING_KEY = "disable_exclude_filter_mapping_key";

//jira,scm common
export const JIRA_SCM_COMMON_PARTIAL_FILTER_KEY = "partial_match";

//scm reports partial filter key
export const SCM_PARTIAL_FILTER_KEY = "partial";

//partial filter mappings
export const scmPartialFilterKeyMapping: filterMapping = {
  assignee: "assignees"
};

export const jenkinsPartialFilterKeyMapping: filterMapping = {
  jenkins_job_path: "job_normalized_full_name"
};

// TODO: Add filtering logic for ignoreCommonFilterKeys
export const jiraSprintIgnoreConfig: ignoreFilterKeysType = {
  ignoreCustomFilterKeys: [SPRINT]
};

export const SHOW_SETTINGS_TAB = "show_settings_tab";
export const SHOW_AGGREGATIONS_TAB = "show_aggregations_tab";
export const SHOW_FILTERS_TAB = "show_filters_tab";
export const SHOW_METRICS_TAB = "show_metrics_tab";
export const SHOW_WEIGHTS_TAB = "show_weights_tab";

// for showing stacks for field on tab other than metric
export const STACKS_SHOW_TAB = "stacks_show_tab";
export const BAR_CHART_REF_LINE_STROKE = "BAR_CHART_REF_LINE_STROKE";
export const CSV_DRILLDOWN_TRANSFORMER = "CSV_DRILLDOWN_TRANSFORMER";

/*
 This key is used to refer a config for removing filter keys from
 specific report.
 */
export const IGNORE_FILTER_KEYS_CONFIG = "IGNORE_FILTER_KEYS_CONFIG";

/* for storing all for which API call is to be made to display value in widget filter preview
   this happens when value sent to BE is "id" and value displayed is "name".
 */
export const API_BASED_FILTER = "API_BASED_FILTER";

// for storing singular values of filter field key which were converted to plural by valuesToFilters
export const FIELD_KEY_FOR_FILTERS = "FIELD_KEY_FOR_FILTERS";

export const MAX_DRILLDOWN_COLUMNS = "MAX_DRILLDOWN_COLUMNS";

export const HIDE_REPORT = "HIDE_REPORT";
export const COPY_DESTINATION_DASHBOARD_NODE = "copy_destination_dashboards";

export const RANGE_FILTER_CHOICE = "range_filter_choice";

export const DEFAULT_METADATA = "default_metadata";

export const STAT_TIME_BASED_FILTER = "stat_time_based_filter";

export const OU_EXCLUSION_CONFIG = "OU_EXCLUSION_CONFIG";

export const WIDGET_FILTER_PREVIEW_TRANSFORMER = "widgetFilterPreviewTransformer";

export const INCLUDE_ACROSS_OU_EXCLUSIONS = "INCLUDE_ACROSS_OU_EXCLUSIONS";

export const IS_FRONTEND_REPORT = "IS_FRONTEND_REPORT";
export const GET_CUSTOMIZE_TITLE = "get_customize_title";
export const CATEGORY = "category";

export const FILTERS_NOT_SUPPORTING_PARTIAL_FILTERS = "filters_not_supporting_partial_filter";

export const AVAILABLE_COLUMNS = "available_columns";
export const DEFAULT_COLUMNS = "default_columns";

export const REQUIRED_ONE_FILTER = "REQUIRED_ONE_FILTER";
export const REQUIRED_ONE_FILTER_KEYS = "REQUIRED_ONE_FILTER_KEYS";
