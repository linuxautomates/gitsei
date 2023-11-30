import { get } from "lodash";
import { createSelector } from "reselect";
import { selectedDashboardIntegrations } from "./integrationSelector";
import { restapiState } from "./restapiSelector";
import { createParameterSelector } from "./selector";

const INTEGRATIONS_CONFIG = "jira_integration_config";

const getID = createParameterSelector((params: any) => params.config_key);
const getIssueManagementSystem = createParameterSelector((params: any) => params?.issue_management_system);
const getIgnoreCustomFiltersKeys = createParameterSelector((params: any) => params?.ignore_custom_filter_keys);

export const integrationsConfigSelector = createSelector(restapiState, (data: any) => {
  return get(data, [INTEGRATIONS_CONFIG], {});
});

export const _integrationConfigListSelector = createSelector(integrationsConfigSelector, (integrations: any) => {
  return get(integrations, ["list"], {});
});

export const integrationListSelector = createSelector(
  _integrationConfigListSelector,
  getID,
  (integrations: any, config_key: string) => {
    return get(integrations, [config_key || "0"], { loading: true, error: false });
  }
);

export const _selectedDashboardIntegrationsConfig = createSelector(restapiState, (data: any) => {
  return get(data, "selected-dashboard-integrations-config", {
    error: false,
    loading: true,
    loaded: false,
    records: []
  });
});

export const selectedDashboardIntegrationsConfig = createSelector(
  _selectedDashboardIntegrationsConfig,
  (data: { loaded: boolean; error: boolean; loading: boolean; records: [] }) => {
    return data.records || [];
  }
);

export const filteredSelectedDashboardIntegrationsConfig = createSelector(
  selectedDashboardIntegrationsConfig,
  selectedDashboardIntegrations,
  getIssueManagementSystem,
  (selectedConfig: any[], selectedIntegrations: any[], issueManagementSystem?: string[]) => {
    if (issueManagementSystem) {
      const filteredIntegrations = (selectedIntegrations || [])
        .filter((item: any) => issueManagementSystem.includes(item.application))
        .map((item: any) => item.id);
      return (selectedConfig || []).filter((item: any) => filteredIntegrations.includes(item.integration_id));
    }
    return selectedConfig;
  }
);

export const selectedDashboardCustomFields = createSelector(
  filteredSelectedDashboardIntegrationsConfig,
  getIgnoreCustomFiltersKeys,
  (data: any[], ignoreCustomFilterKeys: string[]) => {
    const allCustomFields: Array<Record<"key" | "name", string>> = [];
    (data || []).forEach((item: any) => {
      const customFields = get(item, ["config", "agg_custom_fields"], []);
      allCustomFields.push(...customFields);
    });
    return allCustomFields.filter(custom => !ignoreCustomFilterKeys.includes((custom?.name ?? "").toLowerCase()));
  }
);

export const selectedDashboardCustomEpicFields = createSelector(
  filteredSelectedDashboardIntegrationsConfig,
  getIgnoreCustomFiltersKeys,
  (data: any[], ignoreCustomFilterKeys: string[]) => {
    const allCustomFields: Array<Record<"key" | "name", string>> = [];
    (data || []).forEach((item: any) => {
      const customFields = get(item, ["config", "epic_field"], []);
      allCustomFields.push(...customFields);
    });
    return allCustomFields.filter(custom => !ignoreCustomFilterKeys.includes((custom?.name ?? "").toLowerCase()));
  }
);

export const selectedDashboardCustomHygienes = createSelector(
  filteredSelectedDashboardIntegrationsConfig,
  (data: any[]) => {
    const allCustomHygienes: Array<any> = [];
    (data || []).forEach((item: any) => {
      const customFields = get(item, ["custom_hygienes"], []);
      allCustomHygienes.push(...customFields);
    });
    return allCustomHygienes;
  }
);
