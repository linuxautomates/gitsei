import { cloneDeep, get, unset } from "lodash";
import moment from "moment";
import {
  APPEND_CACHED_INTEGRATION_DATA_STATE,
  CACHED_INTEGRATION_INVALIDATE,
  CLEAR_CACHED_INTEGRATION,
  INVALIDATE_HAS_ALL_INT,
  SET_CACHED_INTEGRATION_DATA_STATE,
  SET_CACHED_INTEGRATION_ERROR_STATE,
  SET_CACHED_INTEGRATION_LOADING_STATE,
  SET_INTEGRAIONS
} from "reduxConfigs/actions/actionTypes";
import { IntegrationHandlerType } from "./intergrationReducerUtils.types";

const _setCachedIntegrationDataState: IntegrationHandlerType = (state, action) => ({
  ...(state ?? {}),
  data: action.payload ?? {},
  expires_in: action.expires_in ?? 3600, // seconds
  cached_at: moment().unix()
});

const _appendCachedIntegrationDataState: IntegrationHandlerType = (state, action) => ({
  ...(state ?? {}),
  data: {
    ...(state?.data ?? {}),
    ...(action?.payload ?? {})
  },
  expires_in: action.expires_in ?? 3600, // seconds
  cached_at: moment().unix()
});

const _setIntegraionsState: IntegrationHandlerType = (state, action) => ({
  ...(state ?? {}),
  data: action.payload || {},
  expires_in: action.expires_in ?? 3600, // seconds
  cached_at: moment().unix(),
  hasAllIntegrations: true
});

const _setCachedIntegrationLoadingState: IntegrationHandlerType = (state, action) => ({
  ...(state ?? {}),
  loading: action.loading ?? state?.loading
});

const _setCachedIntegrationErrorState: IntegrationHandlerType = (state, action) => ({
  ...(state ?? {}),
  error: action.error ?? state?.error
});

const _clearCachedIntegrationState: IntegrationHandlerType = (state, action) => {
  const newData = cloneDeep(get(state, ["data"], {}));
  if (action?.integration_id) unset(newData, [action?.integration_id]);
  return {
    ...(state ?? {}),
    data: newData
  };
};

const _cachedIntegrationInvalidate: IntegrationHandlerType = state => {
  const _data = get(state, ["data"]);
  if (_data) {
    const newState = cloneDeep(state);
    const expiresIn = state?.expires_in ?? 3600;
    const cachedAt = state?.cached_at;
    const now = moment().unix();
    const isDataExpired = expiresIn + cachedAt < now;
    if (isDataExpired) {
      unset(newState, ["data"]);
      unset(newState, ["hasAllIntegrations"]);
      return newState;
    }
    return state;
  }
  if (state.hasAllIntegrations) {
    return {
      ...state,
      hasAllIntegrations: false
    };
  }
  return state;
};

const _invalidateHasAllIntegration: IntegrationHandlerType = state => {
  if (state.hasAllIntegrations) {
    return {
      ...state,
      hasAllIntegrations: false
    };
  }
  return state;
};

export const integrationHandler: Record<string, IntegrationHandlerType> = {
  [SET_CACHED_INTEGRATION_DATA_STATE]: _setCachedIntegrationDataState,
  [APPEND_CACHED_INTEGRATION_DATA_STATE]: _appendCachedIntegrationDataState,
  [SET_CACHED_INTEGRATION_LOADING_STATE]: _setCachedIntegrationLoadingState,
  [SET_CACHED_INTEGRATION_ERROR_STATE]: _setCachedIntegrationErrorState,
  [CLEAR_CACHED_INTEGRATION]: _clearCachedIntegrationState,
  [CACHED_INTEGRATION_INVALIDATE]: _cachedIntegrationInvalidate,
  [SET_INTEGRAIONS]: _setIntegraionsState,
  [INVALIDATE_HAS_ALL_INT]: _invalidateHasAllIntegration
};
