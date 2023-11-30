import { WorkflowIntegrationType } from "classes/RestWorkflowProfile";

const IMDoraJIRAParameter = [
  {
    label: "issue resolved in insight time range.",
    value: "issue_resolved_at"
  },
  {
    label: "issue updated in insight time range.",
    value: "issue_updated_at"
  }
];

const IMDoraADOParameter = [
  {
    label: "Ticket resolved in insight time range.",
    value: "workitem_resolved_at"
  },
  {
    label: "Ticket updated in insight time range.",
    value: "workitem_updated_at"
  }
];

const SCMDoraPRParameters = [
  {
    label: "PRs merged",
    value: "pr_merged_at"
  },
  {
    label: "PRs closed",
    value: "pr_closed_at"
  }
];

const SCMDoraCommitParameters = [
  {
    label: "commits pushed",
    value: "commit_pushed_at"
  },
  {
    label: "commits created",
    value: "committed_at"
  }
];

const CICDDoraParameters = [
  {
    label: "Job finished in insight time range.",
    value: "end_time"
  },
  {
    label: "Job started in insight time range.",
    value: "start_time"
  }
];

export const getParameterOptions = (
  integrationType: WorkflowIntegrationType | undefined,
  applicationName: string,
  deploymentRoute?: "pr" | "commit"
) => {
  switch (integrationType) {
    case WorkflowIntegrationType.IM: {
      if (applicationName === "jira") {
        return IMDoraJIRAParameter;
      }
      return IMDoraADOParameter;
    }
    case WorkflowIntegrationType.SCM:
      return deploymentRoute === "commit" ? SCMDoraCommitParameters : SCMDoraPRParameters;
    case WorkflowIntegrationType.CICD:
      return CICDDoraParameters;
    default:
      return [];
  }
};
