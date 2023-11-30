export enum WORKFLOW_PROFILE_MENU {
  CONFIGURATION = "configuration",
  LEAD_TIME_FOR_CHANGES = "Lead Time for Changes",
  DEPLOYMENT_FREQUENCY = "Deployment Frequency",
  MEAN_TIME_TO_RESTORE = "Mean Time To Restore",
  CHANGE_FAILURE_RATE = "Change Failure Rate",
  ASSOCIATIONS = "Associations"
}

export const WORKFLOW_PROFILE_SUB_MENU_SECTION = [
  "Lead Time for Changes",
  "Deployment Frequency",
  "Mean Time To Restore",
  "Change Failure Rate"
];

export const WORKSPACE_SELECTION_DISABLED_MESSAGE = "This Project is not applicable for the selected integration.";

export const OU_SELECTION_DISABLED_MESSAGE = `The current workflow profile cannot be associated with this collection 
1. Due to a mismatch in the integrations or 
2. This collection may already be linked to a different workflow profile.`;

export const ORG_UNIT_IMPACTED =
  "Some of the collection associated have been impacted due to a change in the integration that you just made.";

export const ORG_UNIT_IMPACTED_BUTTON =
  "You can view the impacted collection's list using the “Click here” link below the current collection list.";

export const CHANGE_FAILURE_RATE_COMMON_NOTE =
  "Failed Deployment can be a hotfix or a deployment that led to failure. ";
export const CHANGE_FAILURE_DESCRIPTION =
  "The change failure rate is calculated by dividing the total number of deployments causing failure by the total number of deployments. ";
export const CHANGE_FAILURE_CHECKBOX_DESCRIPTION =
  "Please check this if you want to calculate the change failure rate using only deployments causing failure. ";
export const SCM_COMMON_NOTE = "When using multiple filters, they will be combined with an 'OR' operation.";
export const IM_COMMON_NOTE = "When using multiple filters, they will be combined with an 'AND' operation.";
export const IM_FILTER_NOTE = "Please add more filters below in order to calculate the ";
export const SCM_PARAM_NOTE = "Please use any of the below parameters in order to calculate the ";
export const CICD_CHECKBOX_DESCRIPTION = "In addition, select the category of pipelines for the jobs mentioned above ";
export const TOTAL_DEPLOYMENT_INFO = "Total Deployment can be a release or a change set pushed to production.";
export const CICD_EXECUTION_FITER_NOTE =
  "Note: When using multiple job run parameters, they will be combined with an 'OR' operation.";
export const CICD_EXECUTION_HARNESS_FITER_NOTE =
  "Note: When using multiple pipeline execution filters, they will be combined with an 'OR' operation.";
export const CICD_FITER_NOTE = "Note: When using multiple filters, they will be combined with an 'AND' operation.";

export enum WORKFLOW_PROFILE_TABS {
  DEPLOYMENT_FREQUENCY_TAB = "Deployment Frequency",
  CHANGE_FAILURE_RATE_TAB = "Change Failure Rate",
  CHANGE_FAILURE_RATE_FAILED_DEPLOYMENT_TAB = "Failed Deployment",
  CHANGE_FAILURE_RATE_TOTAL_DEPLOYMENT_TAB = "Total Deployment",
  MEAN_TIME_TO_RESTORE_TAB = "Mean Time To Restore",
  LEAD_TIME_FOR_CHANGES_TAB = "Lead Time for Changes"
}

export const CHANGE_FAILURE_RATE_FAILED_DEPLOYMENT = "failed_deployment";
export const CHANGE_FAILURE_RATE_TOTAL_DEPLOYMENT = "total_deployment";

export const CICD_FOOTER_INFO =
  "The additional filters being used to define the deployments will be applicable to all the integrations that you have selected.";

export const EVENT_JOB_SELECTION_ERROR_MESSAGE = `Kindly resolve the conflict in job selection criteria for deployments 
causing failure and total deployments. Options:
1. Choose Include all jobs for both or
2. Choose Select jobs manually for both.`;

export const CALCULATION_RELEASED_IN_KEY = "released_in";
export const CALCULATION_RELEASED_IN_LABLE = "released date";
