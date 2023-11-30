import { WorkflowIntegrationType } from "classes/RestWorkflowProfile";
import { CONTAINS, STARTS_WITH } from "dashboard/graph-filters/components/tag-select/TagSelect";

export const DORA_CONFIG_METRICS: Array<{ label: string; value: string }> = [
  { label: "STAGES", value: "fixed_stages" },
  { label: "RELEASES", value: "release" },
  { label: "DEPLOYMENT", value: "deployment" },
  { label: "HOTFIX", value: "hotfix" },
  { label: "DEFECT", value: "defect" }
];

export const DORA_METRIC_CONFIGURABLE_DEFINITIONS: Record<string, string> = {
  target_branch: "Pull Requests to branches that",
  source_branch: "Pull Requests from branches that",
  commit_branch: "Direct merges to branches that",
  tags: "Tags on commits of merged PRs that",
  labels: "Labels on pull request that"
};

export const DORA_SCM_DEFINITIONS: Record<string, string> = {
  target_branch: "Target branches",
  source_branch: "Source branches",
  commit_branch: "Commit branches",
  tags: "Tags on commits",
  labels: "Labels"
}

export const DEFINITION_PARTIAL_OPTIONS = [
  {
    label: "starts with",
    value: STARTS_WITH
  },
  {
    label: "contains",
    value: CONTAINS
  }
];

export const DEFAULT_METRIC_IDENTIFIERS: Record<string, string> = {
  release: "release",
  deployment: "deploy",
  hotfix: "hotfix,hf",
  defect: "bugfix,fix,bug"
};

export enum TRIGGER_EVENT_TEXT {
  issueManagement = "issue_management",
  cicd = "cicd"
}

export enum STAGE_TYPE {
  preDevCustomStages = "pre_development_custom_stages",
  postDevCustomStages = "post_development_custom_stages"
}

export const STAGE_TRIGGER_EVENT_DISABLED_MESSAGE = "This option cannot be selected because you have chosen other CICD tools/ Github Action for previous stages.";
export const STAGE_TRIGGER_EVENT_HARNESS_DISABLED_MESSAGE = "This option cannot be selected because you have chosen Harness CICD/ Github Action tools for previous stages.";
export const STAGE_TRIGGER_EVENT_HARNESSCD_DISABLED_MESSAGE = "This can be selected only if Harness CI /Github Action are configured in any of the previous stages.";
export const STAGE_TRIGGER_EVENT_BEFOR_MESSAGE = "The selected trigger event does not have any ";
export const STAGE_TRIGGER_EVENT_AFTER_MESSAGE = " under it. Please try selecting any other trigger event.";

export const gitlabIntTypeOptions = [{
  key: WorkflowIntegrationType.SCM,
  label: "SCM"
}, {
  key: WorkflowIntegrationType.CICD,
  label: "CI/CD"
}]

export const azureIntTypeOptions = [{
  key: WorkflowIntegrationType.IM,
  label: "BOARDS"
}, {
  key: WorkflowIntegrationType.SCM,
  label: "REPOS"
}, {
  key: WorkflowIntegrationType.CICD,
  label: "PIPELINES"
}]

export const JIRA_RELEASE_INFO_MESSAGE = 'Releases can be used to schedule how features are rolled out to your customers, or as a way to organize work that has been completed for the project.';
export const JIRA_RELEASE_EVENT_DISABLED_MESSAGE = 'Since lead time is being measured by JIRA statuses only the start event will be ticket created by default.';
export const JIRA_RELEASE_SAVE_DISABLED_MESSAGE = 'You need to configure at least one stage apart from the Release stage.';