import { createSelector } from "reselect";
import { get } from "lodash";
import moment from "moment";
import { getFormStore } from "./formSelector";

const integrationsRestState = state =>
  state.restapiReducer.integrations.list[0] && state.restapiReducer.integrations.list[0].data
    ? state.restapiReducer.integrations.list[0].data.records
    : [];

export const getIntegrationStore = store => {
  return store.integrationReducer;
};

export const getIntegrationType = store => {
  const integrations = getIntegrationStore(store);
  return integrations.integration_type;
};

export const getIntegrationStep = store => {
  const integrations = getIntegrationStore(store);
  return integrations.integration_step;
};

export const getAllIntegrationsTypesSelector = createSelector(integrationsRestState, integrations => {
  if (!integrations.length) {
    return {};
  }

  const integrationTypes = integrations.reduce((acc, item) => {
    if (!acc[item.application]) {
      acc[item.application] = {
        id: item.application,
        icon: item.application,
        method: item.method
      };
      return acc;
    }
    return acc;
  }, {});
  console.log(integrationTypes);

  return integrationTypes;
});

export const getAllIntegrationsSelector = createSelector(integrationsRestState, integrations => {
  if (!integrations.length) {
    return [];
  }

  return integrations.map(integration => ({
    ...integration,
    teamsCount: integration.teams ? integration.teams.length : 0,
    productsCount: integration.products ? integration.products.length : 0,
    icon: integration.application,
    description: integration.description || "some description here",
    lastUpdated: integration.last_updated ? moment(integration.last_updated).format("MMM DD, YYYY") : ""
  }));
});

export const integrationsListState = (state, id = "0") => {
  return get(state.restapiReducer, ["integrations", "list", id], { loading: true, error: true });
};

export const integrationsGetState = state => {
  return get(state.restapiReducer, ["integrations", "get"]);
};

export const integrationsUpdateState = state => {
  return get(state.restapiReducer, ["integrations", "update"]);
};

export const getIntegrationForm = store => {
  const integration = getIntegrationStore(store);
  return integration.integration_form || {};
};

export const hasJiraIntegrations = createSelector(
  integrationsListState,
  integrationList =>
    !!integrationList?.data?.records?.find(integration => integration.application.toLowerCase() === "jira")
);
