import { forEach, get } from "lodash";
import { Integration } from "model/entities/Integration";
import { EntityState } from "reduxConfigs/reducers/base/EntityState";
import { IntegrationState } from "reduxConfigs/reducers/integration/intergrationReducerUtils.types";
import { createSelector } from "reselect";
import { createParameterSelector } from "./selector";

const CACHED_INTEGRATION_REDUCER = "cachedIntegrationReducer";

export const cachedIntegrationsState = (state: any): IntegrationState => get(state, [CACHED_INTEGRATION_REDUCER], {});

const getID = createParameterSelector((params: any) => params.integration_id);
const getIDs = createParameterSelector((params: any) => params.integration_ids);

const _cachedIntegrationsDataState = createSelector(
  cachedIntegrationsState,
  (state: { data: EntityState<Integration> }) => state?.data ?? {}
);

export const cachedIntegrationsLoadingAndError = createSelector(
  cachedIntegrationsState,
  (state: { loading: boolean; error: boolean }) => ({ loading: state?.loading, error: state?.error })
);

export const hasAllIntegraionsSelector = createSelector(
  cachedIntegrationsState,
  (state: any) => !!state.hasAllIntegrations
);

/** @function
 * This function will get integrations corresponding to each integration
 * id passed and merge them into an array
 */
export const cachedIntegrationsListSelector = createSelector(
  _cachedIntegrationsDataState,
  getIDs,
  (cachedIntegrations: EntityState<Integration>, integrationIDs: string[]) => {
    const cachedIntegrationsList: Integration[] = [];
    let allIntegrationsAvailable: boolean = true;
    const existingIntegrationIds: string[] = Object.keys(cachedIntegrations) ?? [];
    forEach(integrationIDs, id => {
      if (existingIntegrationIds.includes(id)) {
        cachedIntegrationsList.push(cachedIntegrations[id]);
      } else {
        allIntegrationsAvailable = false;
      }
    });
    return allIntegrationsAvailable ? cachedIntegrationsList : [];
  }
);

/** This function gets a single integration if present */
export const cachedIntegrationGetSelector = createSelector(
  _cachedIntegrationsDataState,
  getID,
  (cachedIntegrations: EntityState<Integration>, integrationID: string) => get(cachedIntegrations, [integrationID])
);
