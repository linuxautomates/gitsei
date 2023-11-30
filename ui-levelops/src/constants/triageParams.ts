export enum TRIAGE_RESULT_PARAM_KEYS {
  PRODUCT_IDS = "product_ids",
  JOB_STATUSES = "job_statuses",
  JOB_NAMES = "job_names",
  CICD_USER_IDS = "cicd_user_ids",
  PARENT_CICD_USER_IDS = "parent_cicd_job_ids",
  START_TIME = "start_time",
  END_TIME = "end_time",
  INSTANCE_NAMES = "instance_names",
  TAB = "tab",
  JOB_PATH = "job_normalized_full_names"
}

export type TRIAGE_RESULTS_PAGE_FILTERS = {
  [key in TRIAGE_RESULT_PARAM_KEYS]?: any;
};

export enum TRIAGE_TABS {
  TRIAGE_RESULTS = "triage_results",
  TRIAGE_RULES = "triage_rules",
  TRIAGE_GRID_VIEW = "triage_grid_view"
}
