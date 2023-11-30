import { IssueManagementOptions } from "constants/issueManagementOptions";

// Id's
export const NEW_CATEGORY_ID: string = "new_category";
export const NEW_SCHEME_ID: string = "new";
export const TICKET_CATEGORIZATION_SCHEMES_SEARCH_ID: string = "ticket_categorization_schemes_search_id";
export const TICKET_CATEGORIZATION_LIST_STATE_ID: string = "ticket_categorization_list_state_id";
export const TICKET_CATEGORIZATION_SCHEMES_CHECK_ID: string = "ticket_categorization_schemes_check_id";
export const TICKET_CATEGORIZATION_INTEGRATION_IDS = "TICKET_CATEGORIZATION_INTEGRATION_IDS";

// Messages
export const TICKET_CATEGORIZATION_PROFILE_LIST_FAIL_MESSAGE: string = "Failed to Fetch Data. Please try again";
export const DELETE_PROFILE_MESSAGE =
  "When this profile is deleted the widgets will use the default Ticket Categorization Profile. Are you Sure?";
export const PROFILE_CREATED_SUCCESSFULLY = "Profile Created Successfully";
export const PROFILE_UPDATED_SUCCESSFULLY = "Profile Updated Successfully";
export const CATEGORIES_CONFIGURATION_PAGE_DESC =
  "Tickets are categorized based on the first category rule that matches the ticket.";
export const CATEGORY_EDIT_CREATE_MODEL_DESC =
  "Categories are specific business initiatives, strategic objectives within a profile.";
export const ALLOCATION_GOALS_DESCRIPTION = "Define ideal resource allocation range for each category";
export const DEFAULT_CATEGORY_DESCRIPTION = "Tickets that don’t match any category.";
export const CATEGORY_DELETE_MESSAGE = "Are you sure want to delete this category?";
export const UNCATEGORIZED_ID_SUFFIX = "uncategorized_category";
export const PROFILE_NOT_FOUND = "Profile Not Found!";

// Titles and Labels
export const PROFILE = "Profile"; // for places where singular name is used
export const PROFILES = "Profiles"; // for places where plural name is used
export const ADD_PROFILE = "Add Profile";
export const EDIT_PROFILE = "Edit Profile";
export const CATEGORIES = "Categories";
export const ADD_CATEGORY = "Add Category";
export const EDIT_CATEGORY = "Edit Category";
export const DEFAULT_CATEGORY_NAME = "Uncategorized";
export const FILTER_FIELD_UNCATEGORIZED_NAME = "Other";

// Common strings
export const TICKET_CATEGORIZATION_SCHEME: string = "ticket_categorization_scheme";
export const ticketCategorizationFiltersIgnoreKeys = [
  "issue_created_at",
  "issue_updated_at",
  "workitem_created_at",
  "workitem_resolved_at",
  "workitem_updated_at"
];
export const EFFORT_INVESTMENT_CATEGORIES_VALUES_NODE = "categories_filter_values";
export const ISSUE_MANAGEMENT_INTEGRATION_BE_KEY = "integration_type";

export const profileIssueManagementOption = [
  { label: "Jira", value: IssueManagementOptions.JIRA },
  { label: "Azure", value: IssueManagementOptions.AZURE }
];

export type IssueManagementType = "jira" | "azure_devops";
export const CATEGORY_DEFAULT_BACKGORUND_COLOR = "#FC91AA";
export const WORKITEM_ATTRIBUTE_KEYS = ["code_area", "teams"];
export const colorPickerColors = [
  "#F2917A",
  "#EEA248",
  "#CAC035",
  "#84C67B",
  "#30C5E8",
  "#66AEEF",
  "#86A2F1",
  "#B8322E",
  "#9C4D0E",
  "#655E10",
  "#35672F",
  "#126176",
  "#2846A4",
  "#412F9D",
  "#E14331",
  "#C6670B",
  "#847B0D",
  "#3A8433",
  "#0B93A4",
  "#2959C3",
  "#6045E3",
  "#F1664B",
  "#D58A19",
  "#A99F15",
  "#4EA64B",
  "#28CED4",
  "#3886EB",
  "#5C5CEE",
  "#F4C0B2",
  "#F0D368",
  "#E4DE77",
  "#C0E0B1",
  "#87D5D7",
  "#9FD1F3",
  "#B2C9F4",
  "#FF6633",
  "#FFB399",
  "#FF33FF",
  "#FFFF99",
  "#00B3E6",
  "#E6B333",
  "#3366E6",
  "#999966",
  "#99FF99",
  "#B34D4D",
  "#80B300",
  "#809900",
  "#E6B3B3",
  "#6680B3",
  "#66991A",
  "#FF99E6",
  "#CCFF1A",
  "#FF1A66",
  "#E6331A",
  "#33FFCC",
  "#66994D",
  "#B366CC",
  "#4D8000",
  "#B33300",
  "#CC80CC",
  "#66664D",
  "#991AFF",
  "#FFFF00",
  "#4DB3FF",
  "#1AB399",
  "#E666B3",
  "#33991A",
  "#CC9999",
  "#B3B31A",
  "#00E680",
  "#4D8066",
  "#809980",
  "#E6FF80",
  "#1AFF33",
  "#999933",
  "#FF3380",
  "#CCCC00",
  "#C76114",
  "#4D80CC",
  "#9900B3",
  "#E64D66",
  "#4DB380",
  "#FF4D4D",
  "#99E6E6",
  "#16161D",
  "#7DF9FF",
  "#6F00FF",
  "#CCFF00",
  "#BF00FF",
  "#8F00FF",
  "#6C3082",
  "#1B4D3E",
  "#B48395",
  "#AB4B52",
  "#CC474B",
  "#563C5C",
  "#00FF40",
  "#96C8A2",
  "#c7b793",
  "#f5f5d5",
  "#dee094",
  "#d26676",
  "#1c4046",
  "#587b7f",
  "#84737b",
  "#994b68",
  "#99554b",
  "#3a4e48",
  "#7eb0cb",
  "#cbc07e",
  "#cb7e8a",
  "#cb997e",
  "#fff5ee",
  "#066052",
  "#9d604d",
  "#ac6f66",
  "#4a0a0a",
  "#5e877d",
  "#e0d5cc"
];

export const issueManagementToIntegrationLabelMapping: Record<IssueManagementType, string> = {
  jira: "Jira Filters",
  azure_devops: "Azure Filters"
};

export const INTEGRAION_LIST_ID = "effort_investment_integration_list";