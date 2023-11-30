import { basicMappingType } from "dashboard/dashboard-types/common-types";

export const JENKINS_JUNIT_FILTER_KEY_MAPPING: basicMappingType<string> = {
  cicd_user_id: "cicd_user_ids",
  job_status: "job_statuses",
  job_name: "job_names",
  test_status: "test_statuses",
  test_suite: "test_suites"
};

export const JENKINS_CICD_FILTER_KEY_MAPPING: basicMappingType<string> = {
  cicd_user_id: "cicd_user_ids",
  author: "authors",
  job_status: "job_statuses",
  job_name: "job_names",
  repo: "repos",
  project_name: "projects",
  instance_name: "instance_names",
  job_normalized_full_name: "job_normalized_full_names"
};

export const JENKINS_CICD_FILTER_LABEL_MAPPING: basicMappingType<string> = {
  repo: "SCM Repos",
  project_name: "Project",
  instance_name: "Instance Name",
  trend: "Trend",
  job_name: "Pipeline",
  cicd_user_id: "Triggered By",
  job_status: "Status",
  qualified_job_name: "Qualified Name",
  job_normalized_full_name: "Qualified name",
  source_branch: "Source Branch",
  target_branch: "Destination Branch",
  stage_name: "Stage Name",
  step_name: "Step Name",
  step_status: "Step Status",
  stage_status: "Stage Status"
};

export const JENKINS_JOB_FILTERS_NAME_MAPPING = {
  repo: "repos",
  instance_name: "instance_names",
  trend: "trends",
  job_name: "job_names",
  cicd_user_id: "cicd_user_ids",
  job_status: "job_statuses",
  qualified_job_name: "qualified_job_names",
  job_normalized_full_name: "job_normalized_full_names",
  source_branch: "source_branches",
  target_branches: "target_branches",
  project_name: "projects",
  stage_name: "stage_name",
  step_name: "step_name",
  step_status: "step_status",
  stage_status: "stage_status"
};

export const JENKINS_PIPELINE_FILTERS_NAME_MAPPING = {
  instance_name: "instance_names",
  job_name: "job_names",
  cicd_user_id: "cicd_user_ids",
  job_status: "job_statuses",
  job_normalized_full_name: "job_normalized_full_names",
  project_name: "project_name"
};

export const JENKINS_PIPElINE_FILTER_LABEL_MAPPING = {
  job_normalized_full_name: "Qualified name",
  cicd_user_id: "Triggered By",
  job_name: "Pipeline",
  job_status: "Status",
  project_name: "Project",
};

export const JENKINS_CICD_ID_TOLABEL_MAPPINGS = Object.entries(JENKINS_JOB_FILTERS_NAME_MAPPING).reduce((acc: any, currentData: string[]) => ({
  ...acc,
  [currentData[1]]: JENKINS_CICD_FILTER_LABEL_MAPPING[currentData[0]]
}), {});

export const JENKINS_CICD_JOB_COUNT_CHILD_FILTER_KEY = [
  {
    'parnentKey': 'stage_name',
    'childKey': ["stage_status"],
  },
  {
    'parnentKey': 'step_name',
    'childKey': ["step_status"],
  },
]

export const JENKINS_CICD_JOB_COUNT_CHILD_FILTER_LABLE = [
  {
    'childKey': "stage_status",
    'lableName': 'Add Stage Status',
    'filterName': 'Stage Status',
    'parnentKey': 'stage_name'
  },
  {
    'childKey': "step_status",
    'lableName': 'Add Step Status',
    'filterName': 'Step Status',
    'parnentKey': 'step_name'
  },
]