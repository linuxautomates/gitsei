import { WORKFLOW_PROFILE_TABS } from "../constant";

const prCalculationCriterias = [
  {
    label: "a PR is merged with or without closing it.",
    value: "pr_merged"
  },
  {
    label: "a PR is closed with or without merging it.",
    value: "pr_closed"
  },
  {
    label: "a merged PR is closed",
    value: "pr_merged_closed"
  }
];

const commitCalculationCriterias = [
  {
    label: "a commit is directly pushed to a target branch with or without a tag.",
    value: "commit_merged_to_branch"
  },
  {
    label: "a tag is added to a commit with or without pushing to a target branch.",
    value: "commit_with_tag"
  },
  {
    label: "a tag is added to a commit or a commit is pushed to a target branch",
    value: "commit_merged_to_branch_with_tag"
  }
];

const cicdCalculationCriterias = [
  {
    label: "jobs completed",
    value: "end_time"
  },
  {
    label: "job started",
    value: "start_time"
  },
];

const cicdHarnessCalculationCriterias = [
  {
    label: "pipelines completed",
    value: "end_time"
  },
  {
    label: "pipelines started",
    value: "start_time"
  },
];
const jiraDFIMCalculationCriterias = [
  {
    label: "issue resolved",
    value: "issue_resolved_at"
  },
  {
    label: "issue updated",
    value: "issue_updated_at"
  },
];

const jiraCRFIMCalculationCriterias = [
  {
    label: "issue resolved",
    value: "issue_resolved_at"
  },
  {
    label: "issue updated",
    value: "issue_updated_at"
  },
];

const azureDFIMCalculationCriterias = [
  {
    label: "issue resolved",
    value: "workitem_resolved_at"
  },
  {
    label: "issue updated",
    value: "workitem_updated_at"
  },
];

const azureCRFIMCalculationCriterias = [
  {
    label: "issue resolved",
    value: "workitem_resolved_at"
  },
  {
    label: "issue updated",
    value: "workitem_updated_at"
  },
];

export const getCalculationCriteriaOptions = (calculationRoute: 'pr' | 'commit' | 'jobs'| 'pipelines' | string, applicationName?:string) => {
  switch (calculationRoute) {
    case 'pr':
      return prCalculationCriterias;
      break;
    case 'commit':
      return commitCalculationCriterias;
      break;
    case 'jobs':
      return cicdCalculationCriterias;
      break;
    case 'pipelines':
      return cicdHarnessCalculationCriterias;
      break;
    case WORKFLOW_PROFILE_TABS.DEPLOYMENT_FREQUENCY_TAB:
        if(applicationName === "jira"){
          return jiraDFIMCalculationCriterias
        }
        return azureDFIMCalculationCriterias
    case WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_FAILED_DEPLOYMENT_TAB:
    case WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_TOTAL_DEPLOYMENT_TAB:
      if(applicationName === "jira"){
        return jiraCRFIMCalculationCriterias
      }
      return azureCRFIMCalculationCriterias
    default:
      break;
  }
};
