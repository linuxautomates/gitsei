import { RestWorkflowProfile } from "classes/RestWorkflowProfile";
import { CALCULATION_RELEASED_IN_KEY } from "configurations/pages/lead-time-profiles/containers/workflowDetails/components/constant";
import { get } from "lodash";

export const DORA_CALCULATION_FIELD_LABEL: Record<string, string> = {
  pr_merged_at: "Pr merged time",
  pr_closed_at: "Pr closed time",
  issue_resolved_at: "Issue resolved in",
  issue_updated_at: "Issue updated in",
  workitem_resolved_at: "Workitem resolved in",
  workitem_updated_at: "Workitem updated in",
  end_time: "End date",
  start_time: "Start date",
  commit_pushed_at: "Commit pushed at",
  committed_at: "Committed at",
  [CALCULATION_RELEASED_IN_KEY]: "Release Date In"
};

export const getCalculationField = (workflowProfile: RestWorkflowProfile, reportNameKey: string) => {
  let calculationField = get(workflowProfile, [reportNameKey, "calculation_field"], "");

  if (!workflowProfile) return "";
  // @ts-ignore
  const reportConfig = workflowProfile[reportNameKey];
  const { filters } = reportConfig;
  switch (reportNameKey) {
    case "change_failure_rate":
      const failedDeployments = filters.failed_deployment;
      calculationField = failedDeployments?.calculation_field || calculationField;
      break;
    case "deployment_frequency":
      const depFreq = filters.deployment_frequency;
      calculationField = depFreq?.calculation_field || calculationField;
      break;
  }
  return calculationField;
};

export const getCalculationFieldLabel = (workflowProfile: RestWorkflowProfile, reportNameKey: string) => {
  const calculationField = getCalculationField(workflowProfile, reportNameKey);
  return DORA_CALCULATION_FIELD_LABEL[calculationField];
};
