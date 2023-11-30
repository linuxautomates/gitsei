import { basicMappingType } from "dashboard/dashboard-types/common-types";

export const METRIC_OPTIONS = [
  { value: "line_count", label: "Line Count" },
  { value: "file_count", label: "File Count" }
];

export const SCM_CODE_VOLUME_FILTER_KEY_MAPPING: basicMappingType<string> = {
  cicd_user_id: "cicd_user_ids",
  job_status: "job_statuses",
  job_name: "job_names",
  repo: "repos",
  project_name: "projects",
  instance_name: "instance_names",
  job_normalized_full_name: "job_normalized_full_names"
};

export const REQUIRED_CODE_VOL_VS_DEPLOYMENT_FILTERS: Array<string> = ["build_job_name", "deploy_job_name"];

export const CODE_VOL_VS_DEPLOYMENT_FILTER_INFO_MAPPING: Record<string, string> = {
  build_job_name: "Either Choose BUILD JOB NAME or Build JOB NORMALIZED FULL NAME",
  build_job_normalized_full_name: "Either Choose BUILD JOB NAME or Build JOB NORMALIZED FULL NAME",
  deploy_job_name: "Either Choose DEPLOY JOB NAME or DEPLOY JOB NORMALIZED FULL NAME",
  deploy_job_normalized_full_name: "Either Choose DEPLOY JOB NAME or DEPLOY JOB NORMALIZED FULL NAME"
};
