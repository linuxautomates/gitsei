export const complexFilterKeys = [
  "workitem_custom_fields",
  "custom_fields",
  "exclude",
  "partial_match",
  "custom_hygienes",
  "missing_fields",
  "metadata",
  "workitem_attributes"
];
export const valuesKeysForCount = [
  "jira_filter_values",
  "jira_salesforce_filter_values",
  "ncc_group_issues_values",
  "scm_issues_filter_values",
  "snyk_issues_values"
];

export const jira_filter_values = [
  { key: "epics", defaultValue: [] },
  { key: "hygiene_types", defaultValue: [], label: "HYGIENE" },
  { key: "issue_created_at", defaultValue: undefined, label: "ISSUE CREATED IN" },
  { key: "issue_updated_at", defaultValue: undefined, label: "ISSUE UPDATED IN" },
  { key: "story_points", defaultValue: { $gt: undefined, $lt: undefined }, label: "STORY POINT" },
  { key: "parent_story_points", defaultValue: { $gt: undefined, $lt: undefined }, label: "PARENT STORY POINT" }
];

export const issue_management_workitem_values = [
  { key: "workitem_created_at", defaultValue: undefined, label: "WORKITEM CREATED IN" },
  { key: "workitem_updated_at", defaultValue: undefined, label: "WORKITEM UPDATED IN" },
  { key: "workitem_resolved_at", defaultValue: undefined, label: "WORKITEM RESOLVED IN" },
  { key: "code_area", defaultValue: undefined, label: "AZURE AREAS" },
  { key: "teams", defaultValue: undefined, label: "AZURE TEAMS" }
];
export const jira_zendesk_filter_values = [{ key: "created_at", defaultValue: undefined, label: "ISSUE CREATED IN" }];

export const jira_salesforce_filter_values = [
  { key: "salesforce_created_at", defaultValue: undefined, label: "SALESFORCE ISSUE CREATED DATE" },
  { key: "salesforce_updated_at", defaultValue: undefined, label: "SALESFORCE ISSUE UPDATED DATE" }
];

export const jenkins_job_config_filter_values = [
  { key: "agg_type", defaultValue: undefined, label: "AGGREGATION TYPE" },
  { key: "parameters", defaultValue: undefined, label: "JENKINS PARAMETERS" },
  { key: "time_period", defaultValue: undefined }
];

export const jenkins_jobs_filter_values = [
  { key: "parameters", defaultValue: undefined, label: "JOB RUN PARAMETERS" },
  { key: "time_period", defaultValue: undefined, label: "JOB START DATE" }
];

export const cicd_filter_values = [
  { key: "agg_type", defaultValue: undefined, label: "AGGREGATION TYPE" },
  { key: "parameters", defaultValue: undefined, label: "JOB RUN PARAMETERS" },
  { key: "time_period", defaultValue: undefined, label: "JOB START DATE" }
];

export const jenkins_pipelines_jobs_filter_values = [
  { key: "parameters", defaultValue: undefined, label: "JENKINS PARAMETER" },
  { key: "end_time", defaultValue: undefined, label: "JOB END DATE" }
];

export const jobs_run_tests_filter_values = [
  { key: "parameters", defaultValue: undefined, label: "JENKINS PARAMETERS" }
];

export const github_prs_filter_values = [
  { key: "pr_created_at", defaultValue: undefined, label: "PR CREATED IN" },
  { key: "pr_closed_at", defaultValue: undefined, label: "PR CLOSED TIME" }
];

export const github_commits_filter_values = [{ key: "committed_at", defaultValue: undefined, label: "COMMITTED IN" }];

export const scm_files_filter_values = [
  { key: "sort", defaultValue: undefined },
  { key: "committed_at", defaultValue: undefined, label: "COMMITTED IN" },
  { key: "module", defaultValue: true },
  { key: "group_by_modules", defaultValue: undefined }
];

export const scm_issues_filter_values = [
  { key: "hygiene_types", defaultValue: undefined, label: "HYGIENE" },
  { key: "issue_created_at", defaultValue: undefined, label: "ISSUE CREATED IN" }
];

export const zendesk_filter_values = [{ key: "hygiene_types", defaultValue: undefined, label: "HYGIENE" }];

export const salesforce_filter_values = [{ key: "hygiene_types", defaultValue: undefined, label: "HYGIENE" }];

export const praetorian_issues_values = [
  { key: "n_last_reports", defaultValue: undefined, label: "LAST REPORTS" },
  { key: "ingested_at", defaultValue: undefined, label: "INGESTED IN" }
];

export const bullseye_filter_values = [{ key: "metric", defaultValue: undefined }];

export const ncc_group_issues_values = [
  { key: "n_last_reports", defaultValue: undefined, label: "LAST REPORTS" },
  { key: "created_at", defaultValue: undefined, label: "CREATED IN" },
  { key: "ingested_at", defaultValue: undefined, label: "INGESTED IN" }
];

export const snyk_issues_values = [
  { key: "score_range", defaultValue: { $gt: undefined, $lt: undefined }, label: "PRIORITY SCORE" },
  { key: "disclosure_range", defaultValue: undefined, label: "DISCLOSURE TIME" },
  { key: "publication_range", defaultValue: undefined, label: "PUBLICATION TIME" }
];

export const uiFiltersMapping: { [uri: string]: { key: string; defaultValue: any }[] } = {
  jira_filter_values,
  jira_zendesk_filter_values,
  jira_salesforce_filter_values,
  jenkins_job_config_filter_values,
  jenkins_jobs_filter_values,
  cicd_filter_values,
  jenkins_pipelines_jobs_filter_values,
  jobs_run_tests_filter_values,
  github_prs_filter_values,
  github_commits_filter_values,
  scm_files_filter_values,
  scm_issues_filter_values,
  zendesk_filter_values,
  salesforce_filter_values,
  praetorian_issues_values,
  bullseye_filter_values,
  ncc_group_issues_values,
  snyk_issues_values,
  issue_management_workitem_values,
  "scm_files_filter_values-jira_filter_values": [
    { key: "scm_module", defaultValue: true },
    { key: "group_by_modules", defaultValue: undefined }
  ]
};
// These changes represent the label and key mapping for global Filters, for changing the mapping in widget filters we will need to change in the respective widget constants
export const supportedFiltersLabelMapping: { [uri: string]: { [filterkKey: string]: any } } = {
  jenkins_jobs_filter_values: {
    job_normalized_full_name: {
      label: "JENKINS JOB PATH",
      key: "job_normalized_full_names"
    },
    project_name: {
      label: "PROJECT NAME"
    },
    instance_name: {
      label: "INSTANCE NAME"
    }
  },
  cicd_filter_values: {
    job_normalized_full_name: {
      key: "job_normalized_full_names",
      label: "JENKINS JOB PATH"
    },
    repo: {
      label: "SCM REPOS"
    },
    project_name: {
      label: "PROJECT NAME"
    },
    instance_name: {
      label: "INSTANCE NAME"
    }
  },
  scm_files_filter_values: {
    repo_id: {
      label: "REPO ID",
      key: "repo_ids"
    },
    project: {
      label: "PROJECT NAME"
    }
  },
  jenkins_pipelines_jobs_filter_values: {
    job_normalized_full_name: {
      label: "JENKINS JOB PATH",
      key: "job_normalized_full_names"
    }
  },
  scm_issues_filter_values: {
    label: {
      label: "SCM LABEL"
    }
  },
  github_prs_filter_values: {
    label: {
      label: "SCM LABEL"
    },
    project: {
      label: "PROJECT"
    }
  },
  jira_filter_values: {
    version: {
      label: "AFFECTS VERSION"
    }
  },
  bullseye_filter_values: {
    job_normalized_full_name: {
      label: "JENKINS JOB PATH",
      key: "job_normalized_full_names"
    }
  },
  "scm_files_filter_values-jira_filter_values": {
    repo_id: {
      label: "SCM FILES REPO ID",
      key: "scm_file_repo_ids"
    },
    project: {
      label: "PROJECT NAME"
    }
  },
  github_commits_filter_values: {
    project: {
      label: "PROJECT NAME"
    }
  }
};

export const jenkinsConfigTimePeriodOptions = [
  {
    label: "Last day",
    value: 1
  },
  {
    label: "Last 7 days",
    value: 7
  },
  {
    label: "Last 30 days",
    value: 30
  }
];

export const jenkinsConfigAggsType = [
  {
    label: "Average",
    value: "average"
  },
  {
    label: "Total",
    value: "total"
  }
];

export const jenkinsJobsAggsType = [
  {
    label: "Average Initial Commit to Deploy Time",
    value: "average"
  },
  {
    label: "Median Initial Commit to Deploy Time",
    value: "median"
  }
];

export const scmFilesSortOptions = [
  {
    label: "Num Commits",
    value: "num_commits"
  },
  {
    label: "Changes",
    value: "changes"
  },
  {
    label: "Deletions",
    value: "deletions"
  },
  {
    label: "Additions",
    value: "additions"
  }
];

export enum GlobalFilesFilters {
  SCM_JIRA_FILES_FILTERS = "scm_files_filter_values-jira_filter_values",
  SCM_FILES_FILTERS = "scm_files_filter_values"
}

export const filesFilters = [GlobalFilesFilters.SCM_JIRA_FILES_FILTERS, GlobalFilesFilters.SCM_FILES_FILTERS];

export const SELECT_NAME_TYPE_SPRINT: string = "sprint";
export const SELECT_NAME_TYPE_ITERATION: string = "azure iteration";

export const ACTIVE_SPRINT_TYPE_FILTER_KEY: string = "jira_sprint_states";
export const ACTIVE_ITERATION_TYPE_FILTER_KEY: string = "workitem_sprint_states";
export const SPRINT_STATE_KEY = "active_sprint_state";

export const ACTIVE_SPRINT_TYPE_ACTIVE_STATE = "ACTIVE";
export const ACTIVE_ITERATION_TYPE_ACTIVE_STATE = "current";
export const ACTIVE_SPRINT_STATE_ACTIVE_STATE = "active";

export const ACTIVE_SPRINT_TYPES = [SELECT_NAME_TYPE_SPRINT, SELECT_NAME_TYPE_ITERATION];
export const ACTIVE_SPRINT_CONFIG_BY_TYPE = {
  [SELECT_NAME_TYPE_SPRINT]: {
    label: "Include active sprints only",
    filterKey: ACTIVE_SPRINT_TYPE_FILTER_KEY
  },
  [SELECT_NAME_TYPE_ITERATION]: {
    label: "Include active iterations only",
    filterKey: ACTIVE_ITERATION_TYPE_FILTER_KEY
  }
};

export const ACTIVE_SPRINT_CONFIG_BY_FILTER_KEY = {
  [ACTIVE_SPRINT_TYPE_FILTER_KEY]: {
    activeState: ACTIVE_SPRINT_TYPE_ACTIVE_STATE
  },
  [ACTIVE_ITERATION_TYPE_FILTER_KEY]: {
    activeState: ACTIVE_ITERATION_TYPE_ACTIVE_STATE
  },
  [SPRINT_STATE_KEY]: {
    activeState: ACTIVE_SPRINT_STATE_ACTIVE_STATE,
    filterKey: "state",
    valueFormat: "string"
  }
};
