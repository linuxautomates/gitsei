import { ButtonType } from "antd/lib/button";
import { IssueManagementType } from "../constants/ticket-categorization.constants";

export type ProfileStatusType = {
  loading: boolean | undefined;
  error: boolean | undefined;
};
export type ProfileBasicInfoType =
  | "name"
  | "description"
  | "defaultScheme"
  | "issue_management_integration"
  | "current_priorities_mapping"
  | "uncategorized_color";

export enum CategoryBasicInfoTypes {
  NAME = "name",
  DESCRIPTION = "description",
  BACKGROUND_COLOR = "background_color"
}

export type categoryBasicInfo = "name" | "description" | "background_color";

export type QueryStringType = string | string[] | null;

type HeaderActionConfigType = {
  type: ButtonType;
  label: string;
  hasClicked: boolean;
};

type BreadCrumbType = {
  label: string;
  path: string;
};

export type ConfigureProfileHeaderType = {
  title: string;
  action_buttons: {
    [x: string]: HeaderActionConfigType;
  };
  bread_crumbs: BreadCrumbType[];
};

export type CategoryCreatEditStepsType = {
  title: string;
  content: JSX.Element;
};

export type DashboardFiltersReportType = {
  application: string;
  filter: { uri: string; values: string[] };
  name: string;
  report: string;
  uri: string;
};

export type RestTicketCategorizationProfileJSONType = {
  id?: string;
  name?: string;
  default_scheme?: boolean;
  config?: {
    integration_type: IssueManagementType;
    description?: string;
    categories?: { [x: string]: any };
  };
};

// Effort Investment Settings BA2 types and interfaces
export enum EIConfigurationTabs {
  BASIC_INFO = "basic_info",
  CATEGORIES = "categories",
  ALLOCATION_GOALS = "allocation_goals"
}

export enum currentPrioritiesType {
  ISSUE_IN_ACTIVE_SPRINT = "active_sprints",
  // ISSUE_IN_ACTIVE_RELEASE = "active_releases",  ---> Hiding for now
  ISSUE_IN_IN_PROGRESS = "in_progress",
  ISSUE_IS_ASSIGNED = "assigned"
}

export const currentPriorityLabelMapping = {
  // [currentPrioritiesType.ISSUE_IN_ACTIVE_RELEASE]: "Issue is in an active release", ---> Hiding for now
  [currentPrioritiesType.ISSUE_IN_ACTIVE_SPRINT]: "Issue is in an active sprint",
  [currentPrioritiesType.ISSUE_IN_IN_PROGRESS]: "Issue is in In-Progress state",
  [currentPrioritiesType.ISSUE_IS_ASSIGNED]: "Issue is assigned"
};

export enum allocationGoalsParameters {
  IDEAL = "Ideal",
  ACCEPTABLE = "Acceptable",
  POOR = "Poor"
}

export const profilesCurrentPriorities = Object.values(currentPrioritiesType);
