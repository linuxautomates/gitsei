import { notification } from "antd";
import { FAILED_TO_FETCH_INTEGRATIONS_WARNING } from "constants/formWarnings";
import { get } from "lodash";
import { Integration } from "model/entities/Integration";
import { call, put, select, takeEvery } from "redux-saga/effects";
import { CACHED_INTEGRATION_READ } from "reduxConfigs/actions/actionTypes";
import {
  appendCachedIntegrations,
  CachedIntegrationSagaActionType,
  CachedIntegrationSagaPayloadType,
  cachedIntegrationValidate,
  setCachedIntegrationError,
  setCachedIntegrationLoading,
  setIntegrations
} from "reduxConfigs/actions/cachedIntegrationActions";
import {
  cachedIntegrationGetSelector,
  cachedIntegrationsListSelector,
  hasAllIntegraionsSelector
} from "reduxConfigs/selectors/CachedIntegrationSelector";
import { RestIntegrationsService } from "services/restapi";
import { transformArrayToObjectResponse } from "../saga-helpers/cachedSaga.helper";

function* hasExistingData({ method, integrationIds, integrationId }: CachedIntegrationSagaPayloadType): any {
  let hasData = false;
  let response = undefined;
  switch (method) {
    case "list":
      if (integrationIds && integrationIds.length) {
        response = yield select(cachedIntegrationsListSelector, { integration_ids: integrationIds });
      } else {
        return yield select(hasAllIntegraionsSelector);
      }
      break;
    case "get":
      response = yield select(cachedIntegrationGetSelector, { integration_id: integrationId });
      break;
  }
  if (
    !!response &&
    ((Array.isArray(response) && response.length) || (typeof response === "object" && Object.keys(response).length))
  ) {
    hasData = true;
  }
  return hasData;
}

export function* cachedIntegrationEffectSaga(action: CachedIntegrationSagaActionType): any {
  const { method, integrationIds, integrationId } = action.payload;
  const integrationService = new RestIntegrationsService();
  yield put(cachedIntegrationValidate());
  const hasData: boolean = yield hasExistingData(action.payload);
  if (!hasData) {
    try {
      yield put(setCachedIntegrationLoading(true));
      let response = undefined;
      switch (method) {
        case "list":
          if (integrationIds && integrationIds.length) {
            response = yield call(integrationService.list, { filter: { integration_ids: integrationIds } });
            const records: Array<Integration> = get(response, ["data", "records"], []);
            yield put(appendCachedIntegrations(transformArrayToObjectResponse<Integration>(records)));
          } else {
            response = yield call(integrationService.list, { filter: {} });
            const records: Array<Integration> = get(response, ["data", "records"], []);
            yield put(setIntegrations(transformArrayToObjectResponse<Integration>(records)));
          }
          break;
        case "get":
          response = yield call(integrationService.get, integrationId);
          const integration: Integration = get(response, ["data"], {});
          yield put(appendCachedIntegrations(transformArrayToObjectResponse<Integration>([integration])));
          break;
      }
      yield put(setCachedIntegrationError(false));
    } catch (e) {
      notification.error({
        message: FAILED_TO_FETCH_INTEGRATIONS_WARNING
      });
      yield put(setCachedIntegrationError(true));
    } finally {
      yield put(setCachedIntegrationLoading(false));
    }
  }
}

export function* cachedIntegrationSagaWatcher() {
  yield takeEvery(CACHED_INTEGRATION_READ, cachedIntegrationEffectSaga);
}
