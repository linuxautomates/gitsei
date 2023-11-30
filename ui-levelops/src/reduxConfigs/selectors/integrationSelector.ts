import { get } from "lodash";
import { createSelector } from "reselect";
import { restapiState } from "./restapiSelector";
import { createParameterSelector } from "./selector";

const INTEGRATIONS = "integrations";

const getID = createParameterSelector((params: any) => params.integration_key);
const getApplication = createParameterSelector((params: any) => params.application);

export const integrationsSelector = createSelector(restapiState, (data: any) => {
  return get(data, [INTEGRATIONS], {});
});

export const _integrationListSelector = createSelector(integrationsSelector, (integrations: any) => {
  return get(integrations, ["list"], {});
});

export const _integrationGetSelector = createSelector(integrationsSelector, (integrations: any) => {
  return get(integrations, ["get"], {});
});

const _integratinCreateSelector = createSelector(integrationsSelector, (integrations: any) => {
  return get(integrations, ["create"], {});
});

export const integrationCreateSelector = createSelector(_integratinCreateSelector, (integrationCreatState: any) =>
  get(integrationCreatState, ["0"], { loading: true, error: true })
);

export const integrationListSelector = createSelector(
  _integrationListSelector,
  getID,
  (integrations: any, integration_key: string) => {
    return get(integrations, [integration_key || "0"], {});
  }
);

export const _selectedDashboardIntegrations = createSelector(restapiState, (data: any) => {
  return get(data, "selected-dashboard-integrations", { error: false, loading: true, loaded: false, records: [] });
});

export const selectedDashboardIntegrations = createSelector(
  _selectedDashboardIntegrations,
  (data: { loaded: boolean; error: boolean; loading: boolean; records: [] }) => {
    return data.records || [];
  }
);

export const selectedDashboardByIntegrationData = createSelector(
  selectedDashboardIntegrations,
  getApplication,
  (data: any[], application: string) => data.filter(integration => integration.application === application)
);
