import { filter, get } from "lodash";
import { createSelector } from "reselect";
import { restapiState } from "./restapiSelector";

const JENKINS_INTEGRATIONS = "jenkins_integrations";

export const jenkinsIntegrationsSelector = createSelector(restapiState, (data: any) => {
  return get(data, [JENKINS_INTEGRATIONS], {});
});

export const jenkinsIntegrationListSelector = createSelector(jenkinsIntegrationsSelector, (data: any) => {
  return get(data, ["list", "0", "data", "records"], []);
});

export const jenkinsAvailableIntegrationListSelector = createSelector(jenkinsIntegrationsSelector, (data: any) => {
  return get(data, ["list", "available_integrations", "data", "records"], []);
});

export const jenkinsAttachedIntegrationListSelector = createSelector(
  jenkinsIntegrationListSelector,
  (integrations: any[]) => {
    if (!integrations) {
      return [];
    }
    return filter(integrations, function (integration) {
      return !!integration.integration_id;
    });
  }
);
