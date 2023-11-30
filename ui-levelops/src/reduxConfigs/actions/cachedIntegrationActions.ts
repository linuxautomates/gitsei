import { Integration } from "model/entities/Integration";
import { EntityState } from "reduxConfigs/reducers/base/EntityState";
import { IntegrationActionType } from "reduxConfigs/reducers/integration/intergrationReducerUtils.types";
import {
  APPEND_CACHED_INTEGRATION_DATA_STATE,
  CACHED_INTEGRATION_INVALIDATE,
  CACHED_INTEGRATION_READ,
  CLEAR_CACHED_INTEGRATION,
  INVALIDATE_HAS_ALL_INT,
  SET_CACHED_INTEGRATION_ERROR_STATE,
  SET_CACHED_INTEGRATION_LOADING_STATE,
  SET_INTEGRAIONS
} from "./actionTypes";
import { BaseActionType } from "./restapi/action.type";

export type CachedIntegrationSagaPayloadType = {
  method: "list" | "get";
  integrationIds?: string[];
  integrationId?: string;
};
export interface CachedIntegrationSagaActionType extends BaseActionType {
  payload: CachedIntegrationSagaPayloadType;
}

export const getCachedIntegrations = (
  method: "list" | "get",
  integrationId?: string,
  integrationIds?: string[]
): CachedIntegrationSagaActionType => ({
  type: CACHED_INTEGRATION_READ,
  payload: { method, integrationId, integrationIds }
});

export const appendCachedIntegrations = (payload: EntityState<Integration>): IntegrationActionType => ({
  type: APPEND_CACHED_INTEGRATION_DATA_STATE,
  payload
});

export const setIntegrations = (payload: EntityState<Integration>): IntegrationActionType => ({
  type: SET_INTEGRAIONS,
  payload
});

export const setCachedIntegrationLoading = (loading: boolean): IntegrationActionType => ({
  type: SET_CACHED_INTEGRATION_LOADING_STATE,
  loading
});

export const setCachedIntegrationError = (error: boolean): IntegrationActionType => ({
  type: SET_CACHED_INTEGRATION_ERROR_STATE,
  error
});

export const clearCachedIntegration = (integrationId: string): IntegrationActionType => ({
  type: CLEAR_CACHED_INTEGRATION,
  integration_id: integrationId
});

export const cachedIntegrationValidate = (): IntegrationActionType => ({
  type: CACHED_INTEGRATION_INVALIDATE
});

export const invalidateHasAllIntFlag = (): IntegrationActionType => ({
  type: INVALIDATE_HAS_ALL_INT
});
