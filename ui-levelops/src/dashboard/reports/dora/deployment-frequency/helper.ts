import { get, uniq, unset } from "lodash";
import { IM_ADO } from "dashboard/pages/dashboard-drill-down-preview/drilldownColumnsHelper";
import { extractFilterKeys, getOUFilterKeys } from "../helper";
import { getFilters } from "../filters.config";
import { CALCULATION_RELEASED_IN_KEY } from "configurations/pages/lead-time-profiles/containers/workflowDetails/components/constant";
import { WorkflowIntegrationType } from "classes/RestWorkflowProfile";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const getFilterKeysToHide = (params: any) => {
  const selectedOU = get(params, "selectedOU", {});
  const OUFilterKeys = getOUFilterKeys(selectedOU);
  const depFreqApplication = get(params.workspaceProfile, ["deployment_frequency", "application"]);
  const depFreqCalculationFiled = get(params.workspaceProfile, ["deployment_frequency", "filters", "deployment_frequency", "calculation_field"]);

  if (depFreqApplication === IntegrationTypes.JIRA && depFreqCalculationFiled === CALCULATION_RELEASED_IN_KEY) {
    const allfiiltersToRemove = (params?.allFilters || []).map((value: { key: string; }) => value.key);
    return uniq([...allfiiltersToRemove, ...OUFilterKeys]);
  } else {
    const depFreqFilters = get(params.workspaceProfile, [
      "deployment_frequency",
      "filters",
      "deployment_frequency",
      "filter"
    ]);

    const fiiltersToRemove = extractFilterKeys(depFreqFilters);
    return uniq([...fiiltersToRemove, ...OUFilterKeys]);
  }
};

export const getFilterConfig = (params: any) => {
  if (!params.workspaceProfile) return getFilters();
  const { deployment_frequency } = params.workspaceProfile;
  const deployment_frequency_data = get(deployment_frequency, ["filters", "deployment_frequency"]);
  const integrationType = deployment_frequency_data?.integration_type;

  return getFilters(integrationType, deployment_frequency_data?.deployment_route, deployment_frequency.application);
};

export const mapFiltersBeforeCall = (filter: any) => {
  let stackFilter = filter?.filter?.stacks ? { stacks: filter?.filter?.stacks } : {};

  const finalFilters = {
    ...filter,
    ...stackFilter,
    widget: "deployment_frequency_report"
  };
  unset(finalFilters, ["filter", "statClicked"]);
  unset(finalFilters, ["filter", "integration_ids"]);
  unset(finalFilters, ["filter", "count"]);
  unset(finalFilters, ["filter", "stacks"]);
  return finalFilters;
};

export const getDoraProfileIntegrationType = (params: any) => {
  if (!params.workspaceOuProfilestate) return undefined;
  const { deployment_frequency } = params.workspaceOuProfilestate;
  let integrationType = get(
    deployment_frequency,
    ["filters", "deployment_frequency", "integration_type"],
    undefined
  )?.toString();
  if (integrationType === "IM" && deployment_frequency.application === IntegrationTypes.AZURE) {
    return IM_ADO;
  } else {
    return integrationType;
  }
};

export const getShowTitle = (params: any) => {
  const { drillDownProps } = params;
  const application = get(drillDownProps, ["application"], "");
  const statClicked = get(drillDownProps, [application, "statClicked"], false);
  return !statClicked;
};

export const getDoraProfileIntegrationId = (params: any) => {
  const { workspaceOuProfilestate } = params;
  let integrationId = get(workspaceOuProfilestate, ["deployment_frequency", "integration_id"], undefined)?.toString();
  return integrationId;
};

export const getDeploymentFrequencyExtraDrilldownProps = (params: any) => {
  const { widgetId } = params;
  return {
    widgetId: widgetId as string,
    shouldDerive: ["integration_ids", "integration_id"]
  };
};

export const getDeployementProfileRoute = (params: any) => {
  const { workspaceOuProfilestate } = params;
  const deploymentFreqApplication = get(workspaceOuProfilestate, ["deployment_frequency", "application"]);
  const deploymentFreqIntegrationData = get(workspaceOuProfilestate, ["deployment_frequency", "filters", "deployment_frequency"]);

  if (deploymentFreqIntegrationData?.integration_type === WorkflowIntegrationType.IM && deploymentFreqApplication === IntegrationTypes.JIRA) {
    return deploymentFreqIntegrationData?.calculation_field;
  }

  const deployementRoute = get(workspaceOuProfilestate, ["deployment_frequency", "filters", "deployment_frequency", "deployment_route"], "pr");
  return deployementRoute;
};
