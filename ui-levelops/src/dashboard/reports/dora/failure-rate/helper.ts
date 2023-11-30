import { get, uniq, unset } from "lodash";
import { IM_ADO } from "dashboard/pages/dashboard-drill-down-preview/drilldownColumnsHelper";
import { extractFilterKeys, getOUFilterKeys } from "../helper";
import { getFilters } from "../filters.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const getFilterKeysToHide = (params: any) => {
  const selectedOU = get(params, "selectedOU", {});
  const OUFilterKeys = getOUFilterKeys(selectedOU);
  const changeFailureFailedDeploymentFilters = get(params.workspaceProfile, [
    "change_failure_rate",
    "filters",
    "failed_deployment",
    "filter"
  ]);

  const changeFailureTotalDep = get(params.workspaceProfile, ["change_failure_rate", "filters", "total_deployment"]);
  const failedDepFilter = extractFilterKeys(changeFailureFailedDeploymentFilters);
  if (changeFailureTotalDep) {
    const totalDepFilters = extractFilterKeys(changeFailureTotalDep.filter);
    return uniq([...failedDepFilter, ...totalDepFilters, ...OUFilterKeys]);
  }

  return uniq([...failedDepFilter, ...OUFilterKeys]);
};

export const getFilterConfig = (params: any) => {
  if (!params.workspaceProfile) return getFilters();
  const { change_failure_rate } = params.workspaceProfile;
  const change_failure_rate_data = get(change_failure_rate, ["filters", "failed_deployment"]);
  const integrationType = change_failure_rate_data?.integration_type;

  return getFilters(integrationType, change_failure_rate_data?.deployment_route, change_failure_rate.application);
};

export const mapFiltersBeforeCall = (filter: any) => {
  let stackFilter = filter?.filter?.stacks ? { stacks: filter?.filter?.stacks } : {};

  const finalFilters = {
    ...filter,
    ...stackFilter,
    widget: "change_failure_rate"
  };
  unset(finalFilters, ["filter", "statClicked"]);
  unset(finalFilters, ["filter", "integration_ids"]);
  unset(finalFilters, ["filter", "count"]);
  unset(finalFilters, ["filter", "stacks"]);
  return finalFilters;
};

export const getDoraProfileIntegrationType = (params: any) => {
  if (!params.workspaceOuProfilestate) return undefined;
  const { change_failure_rate } = params.workspaceOuProfilestate;
  let integrationType = get(
    change_failure_rate,
    ["filters", "failed_deployment", "integration_type"],
    undefined
  )?.toString();
  if (integrationType === "IM" && change_failure_rate.application === IntegrationTypes.AZURE) {
    return IM_ADO;
  }
  return integrationType;
};

export const getShowTitle = (params: any) => {
  const { drillDownProps } = params;
  const application = get(drillDownProps, ["application"], "");
  const statClicked = get(drillDownProps, [application, "statClicked"], false);
  return !statClicked;
};

export const getDoraProfileIntegrationId = (params: any) => {
  const { workspaceOuProfilestate } = params;
  let integrationId = get(workspaceOuProfilestate, ["change_failure_rate", "integration_id"], undefined)?.toString();
  return integrationId;
};

export const getDoraSingleStateValue = (params: any) => {
  const { isRelative, count, realValue, descStringValue } = params;
  if (isRelative) {
    return `${realValue} failures of ${count} ${descStringValue}`;
  } else {
    return `${count} ${descStringValue}`;
  }
};

export const getChangeFailureRateExtraDrilldownProps = (params: any) => {
  const { widgetId } = params;
  return {
    widgetId: widgetId as string,
    shouldDerive: ["integration_ids", "integration_id"]
  };
};

export const getDeployementProfileRoute = (params: any) => {
  const { workspaceOuProfilestate } = params;
  const deployementRoute = get(
    workspaceOuProfilestate,
    ["change_failure_rate", "filters", "failed_deployment", "deployment_route"],
    "pr"
  );
  return deployementRoute;
};
