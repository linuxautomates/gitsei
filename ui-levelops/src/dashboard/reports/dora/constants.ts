import { getToggleComponent, onChangeHandler } from "./helper";

export const CHANGE_FAILURE_RATE_WIDGET_DESCRIPTION =
  "The failure rate is defined as the percentage of deployments that lead to a production failure. For the Elite performing teams, the failure rate is less than 15%, the High is between 16 to 30%, the Medium is 31 to 45%, and the Low is anything greater than 45%";

export const DEPLOYMENT_FREQUENCY_DESCRIPTION =
  "Deployment Frequency is a measure of how often a team successfully releases or deploys code to production. For the Elite performing teams, deployment frequency is greater than multiple deploys per day, the High is between once per day and once per week, the Medium is between once per week and once per month, and the Low is less than once per month";

export const LEAD_TIME_CHANGES_DESCRIPTION =
  "Lead Time for Changes as per DORA metrics is defined as the amount of time it takes a commit to get into production. For the Elite performing teams, the lead time value is less than one day, the High is between one day and one week, the Medium is between one week and one month, and the Low is greater than one month.";

export const TIME_TO_RECOVER_DESCRIPTION =
  "Mean time to restore is a measure of how long it takes a team to recover from a failure in production. For the Elite performing teams, MTTR is less than 1 hour, the High is less than 1 day, the Medium is less than 1 week, and the Low is more than a week.";

export const doraApiBasedFilterKeyMapping = {
  assignees: "assignee",
  reporters: "reporter",
  first_assignees: "first_assignee",
  jira_assignees: "jira_assignee",
  jira_reporters: "jira_reporter",
  workitem_assignees: "workitem_assignee",
  workitem_reporters: "workitem_reporter",
  authors: "author",
  committers: "committer",
  creators: "creator"
};

export const doraIMApiBasedFilters = [
  "reporters",
  "assignees",
  "workitem_assignees",
  "workitem_reporters",
  "reporters",
  "assignees",
  "authors",
  "committers",
  "creators"
];
export const doraSupportedFilters = {
  uri: "jira_filter_values", // keeping this uri, but that is not used as we have conditionalbaseduri based on itegrationtype
  values: [
    // jira based below
    "status",
    "priority",
    "issue_type",
    "assignee",
    "project",
    "component",
    "label",
    "reporter",
    "fix_version",
    "version",
    "resolution",
    "status_category",
    // azure based below
    "workitem_project",
    "workitem_status",
    "workitem_priority",
    "workitem_type",
    "workitem_status_category",
    "workitem_parent_workitem_id",
    "workitem_epic",
    "workitem_assignee",
    "workitem_version",
    "workitem_fix_version",
    "workitem_reporter",
    "workitem_label",
    // scm based filters below
    "creator",
    "repo_id",
    "project",
    // cicd based filters below
    "cicd_user_id",
    "job_status",
    "instance_name"
  ]
};

export const FILTER_WARNING_LABEL =
  "Please avoid using filters at the widget level. Instead, add the necessary filters in the associated profile.";

export const TOGGLE_TITLE = "Show list of direct merged";

export const DRILLDOWN_TOGGLE_CONFIG= {
  getToggleComponent: getToggleComponent,
  title: TOGGLE_TITLE,
  initialValue: false,
  onChangeHandler: onChangeHandler
}

export const FIELDKEYSFORFILTERS = 
    {
        assignees: "assignee",
        reporters: "reporter",
        first_assignees: "first_assignee",
        jira_assignees: "jira_assignee",
        jira_reporters: "jira_reporter",
        authors: "author",
        committers: "committer",
        workitem_assignees: "workitem_assignee",
        workitem_reporters: "workitem_reporter",
        creators:"creator",
        reviewers:"reviewer",
        
      }

      export const APIBASEDFILTERS =  ["jira_reporters", "jira_assignees", "workitem_assignees",
      "workitem_reporters",
      "reporters",
      "assignees",
      "authors",
      "committers",
      "creators", "reviewers"]

export const LEAD_TIME_MTTR_DESCRIPTION = "The amount of time involved from the start event to getting into production is depicted by splitting into all the involved stages. It helps in identifying the bottlenecks by displaying if each of the stages is in a good, acceptable, or slow state as per the threshold defined.";
export const keysToNeglect = ["calculation","work_items_type","ratings","limit_to_only_applicable_data"];
