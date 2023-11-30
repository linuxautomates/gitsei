import { IntegrationTypes } from "constants/IntegrationTypes";

export enum integrationStringUtility {
  AZURE_DEVOPS = "azure_devops"
}

export type azureCustomHygienesUtilityConfigType = {
  defaultReport: string;
  moreFilters: any;
};

export enum OrgUnitType {
  DELETE = "DELETE",
  FILTER = "Filter",
  USER = "User"
}

export const orgUnitTypeOptions = Object.values(OrgUnitType).map(type => ({
  label: type.toString(),
  value: type.toString()
}));

export enum integrationParameters {
  EQUALS = "EQUALS",
  DOES_NOT_EQUAL = "DOES_NOT_EQUAL",
  STARTS_WITH = "STARTS_WITH",
  IS_BETWEEN = "IS_BETWEEN",
  CONTAINS = "CONTAINS"
}

export const ouIntegrationParameters = [
  {
    label: "Equals",
    value: integrationParameters.EQUALS
  },
  {
    label: "Does Not Equal",
    value: integrationParameters.DOES_NOT_EQUAL
  },
  {
    label: "Starts with",
    value: integrationParameters.STARTS_WITH
  },
  {
    label: "Contains",
    value: integrationParameters.CONTAINS
  }
];

export const oUIntegrationPartialMatchParameters = [integrationParameters.STARTS_WITH, integrationParameters.CONTAINS];
export const oUIntegrationEqualsMatchParameters = [integrationParameters.EQUALS];

export const partialMatchBlockedFiltersDroneCI = [
  `${IntegrationTypes.DRONECI}_cicd_user_ids`,
  `${IntegrationTypes.DRONECI}_job_statuses`,
  `${IntegrationTypes.DRONECI}_job_names`,
  `${IntegrationTypes.DRONECI}_projects`,
  `${IntegrationTypes.DRONECI}_stage_name`,
  `${IntegrationTypes.DRONECI}_step_name`,
  `${IntegrationTypes.DRONECI}_stage_status`,
  `${IntegrationTypes.DRONECI}_step_status`
];
export const partialMatchBlockedFiltersCircleCI = [
  `${IntegrationTypes.CIRCLECI}_cicd_user_ids`,
  `${IntegrationTypes.CIRCLECI}_job_statuses`,
  `${IntegrationTypes.CIRCLECI}_job_names`,
  `${IntegrationTypes.CIRCLECI}_projects`,
  `${IntegrationTypes.CIRCLECI}_stage_name`,
  `${IntegrationTypes.CIRCLECI}_step_name`,
  `${IntegrationTypes.CIRCLECI}_stage_status`,
  `${IntegrationTypes.CIRCLECI}_step_status`
];
export const partialMatchBlockedFiltersJenkins = [
  `${IntegrationTypes.JENKINS}_cicd_user_ids`,
  `${IntegrationTypes.JENKINS}_job_statuses`,
  `${IntegrationTypes.JENKINS}_job_names`,
  `${IntegrationTypes.JENKINS}_projects`,
  `${IntegrationTypes.JENKINS}_stage_name`,
  `${IntegrationTypes.JENKINS}_step_name`,
  `${IntegrationTypes.JENKINS}_stage_status`,
  `${IntegrationTypes.JENKINS}_step_status`
];
export const partialMatchBlockedFiltersAzureDevops = [
  `${IntegrationTypes.AZURE}_instance_names`,
  `${IntegrationTypes.AZURE}_cicd_user_ids`,
  `${IntegrationTypes.AZURE}_job_statuses`,
  `${IntegrationTypes.AZURE}_job_names`,
  `${IntegrationTypes.AZURE}_instance_names`,
  `${IntegrationTypes.AZURE}_code_area`,
  `${IntegrationTypes.AZURE}_teams`,
  `${IntegrationTypes.AZURE}_stage_name`,
  `${IntegrationTypes.AZURE}_step_name`,
  `${IntegrationTypes.AZURE}_stage_status`,
  `${IntegrationTypes.AZURE}_step_status`
];
export const partialMatchBlockedFiltersCoverityCov = [
  "coverity_cov_defect_impacts",
  "coverity_cov_defect_categories",
  "coverity_cov_defect_kinds",
  "coverity_cov_defect_checker_names",
  "coverity_cov_defect_component_names",
  "coverity_cov_defect_types",
  "coverity_cov_defect_domains",
  "coverity_cov_defect_file_paths",
  "coverity_cov_defect_function_names",
  "coverity_cov_defect_first_detected_at",
  "coverity_cov_defect_last_detected_at",
  "coverity_cov_defect_first_detected_streams",
  "coverity_cov_defect_last_detected_streams"
];
export const partialMatchBlockedFiltersHarnessNG = [
  `${IntegrationTypes.HARNESSNG}_rollback`,
  `${IntegrationTypes.HARNESSNG}_projects`,
  `${IntegrationTypes.HARNESSNG}_stage_name`,
  `${IntegrationTypes.HARNESSNG}_step_name`,
  `${IntegrationTypes.HARNESSNG}_stage_status`,
  `${IntegrationTypes.HARNESSNG}_step_status`
];
export const partialMatchBlockedFiltersGitlab = [
  `${IntegrationTypes.GITLAB}_stage_name`,
  `${IntegrationTypes.GITLAB}_step_name`,
  `${IntegrationTypes.GITLAB}_stage_status`,
  `${IntegrationTypes.GITLAB}_step_status`
];
export const partialMatchBlockedFiltersGithubAction = [
  `${IntegrationTypes.GITHUB_ACTIONS}_stage_name`,
  `${IntegrationTypes.GITHUB_ACTIONS}_step_name`,
  `${IntegrationTypes.GITHUB_ACTIONS}_stage_status`,
  `${IntegrationTypes.GITHUB_ACTIONS}_step_status`,
  `${IntegrationTypes.GITHUB_ACTIONS}_cicd_user_ids`,
  `${IntegrationTypes.GITHUB_ACTIONS}_job_statuses`,
  `${IntegrationTypes.GITHUB_ACTIONS}_job_names`,
  `${IntegrationTypes.GITHUB_ACTIONS}_projects`,
  `${IntegrationTypes.GITHUB_ACTIONS}_stage_name`,
  `${IntegrationTypes.GITHUB_ACTIONS}_step_name`,
  `${IntegrationTypes.GITHUB_ACTIONS}_stage_status`,
  `${IntegrationTypes.GITHUB_ACTIONS}_step_status`
];
// the values of this array contain application_filterKey
export const partialMatchBlockedFilters = [
  ...partialMatchBlockedFiltersDroneCI,
  ...partialMatchBlockedFiltersCircleCI,
  ...partialMatchBlockedFiltersJenkins,
  ...partialMatchBlockedFiltersAzureDevops,
  ...partialMatchBlockedFiltersCoverityCov,
  ...partialMatchBlockedFiltersHarnessNG,
  ...partialMatchBlockedFiltersGitlab,
  ...partialMatchBlockedFiltersGithubAction
];

export const partialMatchBlockedKeys = ["azure_devops_projects_cicd"];

export const workflowProfilePartialMatchBlocker = [
  `${IntegrationTypes.DRONECI}_instance_names`,
  `${IntegrationTypes.CIRCLECI}_instance_names`,
  `${IntegrationTypes.JENKINS}_instance_names`,
  `${IntegrationTypes.GITHUB_ACTIONS}_instance_names`,
];
// THE VALUES OF THIS ARRAY CONTAIN APPLICATION_FILTERKEY FOR MATCH ONLY EQUAL CONDTION
export const equalMatchBlockedFilters = [
  `${IntegrationTypes.TESTRAILS}_types`,
  `${IntegrationTypes.TESTRAILS}_projects`,
  `${IntegrationTypes.TESTRAILS}_statuses`,
  `${IntegrationTypes.TESTRAILS}_milestones`,
  `${IntegrationTypes.TESTRAILS}_test_plans`,
  `${IntegrationTypes.TESTRAILS}_test_runs`,
  `${IntegrationTypes.TESTRAILS}_priorities`
];
