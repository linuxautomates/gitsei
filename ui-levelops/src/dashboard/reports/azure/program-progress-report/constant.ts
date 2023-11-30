export const TOTAL_SUM_OF_STORY_POINTS = "Total sum of story points";

export const TOTAL_NUMBER_OF_TICKETS = "Total number of tickets";

export const REPORT_NAME = "Program Progress Report";
export const DEFAULT_ACROSS = "parent_workitem_id";
export const URI = "issue_management_tickets_report";
export const STORY_POINT_URI = "issue_management_story_point_report";

export const FILTERS_KEY_MAPPING = {
  ticket_categories: "workitem_ticket_categories",
  ticket_categorization_scheme: "workitem_ticket_categorization_scheme"
};

export const WORKITEM_PARENT_KEY = "parent_workitem_id";
export const WORKITEM_PARENT_TYPE_KEY = "workitem_parent_workitem_types";
export const WORKITEM_PARENT_TYPES_KEY_VALUE_MAPPING: Record<string, any> = {
  Features: ["Feature"]
};
export const xAxisMappingKeys = {
  [WORKITEM_PARENT_KEY]: WORKITEM_PARENT_TYPE_KEY
};
export const REPORT_HEADER_INFO = "REPORT_HEADER_INFO";

export const DEFAULT_QUERY = {
  [WORKITEM_PARENT_TYPE_KEY]: ["Feature"]
};

export const DRILLDOWN_SUPPORTED_FILTERS_KEYS = [
  "integration_ids",
  "ticket_categorization_scheme",
  "workitem_parent_workitem_ids",
  "workitem_status_categories"
];

export const NOT_SUPPORTED_FILTERS = ["ticket_categorization_scheme"];
export const PROGRAM_PROGRESS_REPORT_DEFUALT_COLUMNS = [
  "workitem_id",
  "summary",
  "sprint_full_names",
  "workitems",
  "workitem_effort",
  "workitems_ratio",
  "due_date",
  "fe_status"
];
