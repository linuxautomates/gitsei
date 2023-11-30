import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { WidgetFilterType } from "dashboard/constants/enums/WidgetFilterType.enum";
import { RANGE_FILTER_CHOICE } from "dashboard/constants/filter-key.mapping";
import { WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import { filterWithInfoType } from "dashboard/constants/filterWithInfo.mapping";
import {
  widgetDataSortingOptionsDefaultValue,
  widgetDataSortingOptionsNodeType
} from "dashboard/constants/WidgetDataSortingFilter.constant";
import { buildTimeFiltersOptions, TimeConfigTypes } from "dashboard/helpers/buildTimeFilters";
import { Dict } from "types/dict";
import { makeObjectKeyAsValue } from "utils/commonUtils";

export const AZURE_FILTER_KEY_MAPPING: Record<string, string> = {
  workitem_project: "workitem_projects",
  workitem_status: "workitem_statuses",
  workitem_priority: "workitem_priorities",
  workitem_status_category: "workitem_status_categories",
  workitem_parent_workitem_id: "workitem_parent_workitem_ids",
  workitem_epic: "workitem_epics",
  workitem_assignee: "workitem_assignees",
  workitem_ticket_category: "workitem_ticket_categories",
  workitem_version: "workitem_versions",
  workitem_fix_version: "workitem_fix_versions",
  workitem_reporter: "workitem_reporters",
  workitem_label: "workitem_labels",
  workitem_type: "workitem_types",
  workitem_stage: "workitem_stages"
};

export const AZURE_ID_FILTER_KEY_MAPPING: Record<string, string> = {
  workitem_assignee: "workitem_assignees",
  workitem_reporter: "workitem_reporters"
};

export const AZURE_PRIORITY_FILTER_KEY_MAPPING: Record<string, string> = {
  workitem_priority: "workitem_priorities"
};

export const AZURE_PARTIAL_FILTER_KEY_MAPPING: Record<string, string> = {
  workitem_label: "workitem_labels"
};

export const AZURE_REVERSE_FILTER_KEY_MAPPING: Record<string, string> = makeObjectKeyAsValue(AZURE_FILTER_KEY_MAPPING);

export const AZURE_ID_REVERSE_FILTER_KEY_MAPPING: Record<string, string> =
  makeObjectKeyAsValue(AZURE_ID_FILTER_KEY_MAPPING);

export const AZURE_REVERSE_PRIORITY_FILTER_KEY_MAPPING: Record<string, string> = makeObjectKeyAsValue(
  AZURE_PRIORITY_FILTER_KEY_MAPPING
);

export const chartProps = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

export const ADO_APPLICATION = "azure_devops";

export const ISSUE_TICKET_ACROSS_OPTION = [
  "project",
  "status",
  "priority",
  "assignee",
  "reporter",
  "workitem_type",
  "trend"
];

export const AZURE_APPEND_ACROSS_OPTIONS = [
  { label: "Azure Teams", value: "teams" },
  { label: "Azure Areas", value: "code_area" }
];

export const AZURE_STACKS_FILTERS = [
  "project",
  "status",
  "priority",
  "workitem_type",
  "status_category",
  "parent_workitem_id",
  "epic",
  "assignee",
  "ticket_category",
  "version",
  "fix_version",
  "reporter",
  "label",
  "story_points",
  "teams",
  "code_area"
];

export const AZURE_API_BASED_FILTERS = [
  "workitem_assignees",
  "workitem_reporters",
  "reporters",
  "assignees",
  "authors",
  "committers"
];

export const AZURE_API_BASED_FILTER_KEY_MAPPING = {
  assignees: "assignee",
  reporters: "reporter",
  workitem_assignees: "workitem_assignee",
  workitem_reporters: "workitem_reporter",
  authors: "author",
  committers: "committer"
};

export const AZURE_DRILL_DOWN_VALUES_TO_FILTER_KEYS = {
  status: "workitem_statuses",
  project: "workitem_projects",
  priority: "workitem_priorities",
  assignee: "workitem_assignees",
  reporter: "workitem_reporters",
  workitem_type: "workitem_types",
  story_points: "workitem_story_points",
  parent_workitem_id: "workitem_parent_workitem_ids",
  epic: "epics",
  ticket_category: "ticket_categories"
};

export const ID_FILTERS = ["assignee", "reporter"];

export const workitem_created_at = {
  type: WidgetFilterType.TIME_BASED_FILTERS,
  label: "WorkItem Created In",
  BE_key: "workitem_created_at",
  slicing_value_support: true,
  configTab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  options: buildTimeFiltersOptions({ [TimeConfigTypes.DAYS]: { options: [7, 30, 90, 180, 365] } })
};

export const workitem_updated_at = {
  type: WidgetFilterType.TIME_BASED_FILTERS,
  label: "WorkItem Updated In",
  BE_key: "workitem_updated_at",
  slicing_value_support: true,
  configTab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  options: buildTimeFiltersOptions({ [TimeConfigTypes.DAYS]: { options: [7, 30, 90, 180, 365] } })
};

export const workitem_resolved_at = {
  type: WidgetFilterType.TIME_BASED_FILTERS,
  label: "WorkItem resolved In",
  BE_key: "workitem_resolved_at",
  slicing_value_support: true,
  configTab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  options: buildTimeFiltersOptions({ [TimeConfigTypes.DAYS]: { options: [7, 30, 90, 180, 365] } })
};

export const drillDownValuesToFiltersKeys = {
  status: "workitem_statuses",
  project: "workitem_projects",
  priority: "workitem_priorities",
  assignee: "workitem_assignees",
  reporter: "workitem_reporters",
  workitem_type: "workitem_types",
  story_points: "workitem_story_points"
};

export const AZURE_FILTER_KEYS = {
  CODE_AREA: "code_area"
};

export const AZURE_TIME_PERIOD_OPTIONS = [
  {
    label: "Last day",
    value: 1
  },
  {
    label: "Last 7 days",
    value: 7
  },
  {
    label: "Last 2 Weeks",
    value: 14
  },
  {
    label: "Last 30 days",
    value: 30
  }
];

export const AZURE_EI_TIME_RANGE_DEF_META = {
  [RANGE_FILTER_CHOICE]: {
    workitem_resolved_at: {
      type: "relative",
      relative: {
        last: {
          num: 4,
          unit: "months"
        },
        next: {
          unit: "today"
        }
      }
    },
    committed_at: {
      type: "relative",
      relative: {
        last: {
          num: 4,
          unit: "months"
        },
        next: {
          unit: "today"
        }
      }
    }
  }
};

export const ATTRIBUTE_FILTERS = ["code_area", "teams"];

export const AZURE_ID_FILTERS = ["assignee", "reporter"];

export const AZURE_IMPLICIT_FILTER = {
  include_solve_time: true
};

export const REPORT_LIST_METHOD = "list";

export const COMMON_FILTER_OPTIONS_MAPPING: Dict<string, string> = {
  version: "Affects Version",
  workitem_parent_workitem_id: "Parent Workitem id"
};

export const LEAD_TIME_EXCLUDE_STAGE_FILTER: filterWithInfoType = {
  id: "exclude-stages",
  label: "Exclude Stages",
  filterKey: "jira_stages",
  description: "Exclude selected stages from Lead Time computation"
};

export const WORKITEM_PARENT_KEY = "parent_workitem_id";
export const WORKITEM_PARENT_TYPE_KEY = "workitem_parent_workitem_types";
export const WORKITEM_PARENT_TYPES_KEY_VALUE_MAPPING: Record<string, any> = {
  Features: ["Feature"]
};
export const xAxisMappingKeys = {
  [WORKITEM_PARENT_KEY]: WORKITEM_PARENT_TYPE_KEY
};
