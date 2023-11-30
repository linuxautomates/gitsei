import { call, put, select, takeLatest } from "@redux-saga/core/effects";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { get } from "lodash";
import { CACHED_GENERIC_REST_API_READ } from "reduxConfigs/actions/actionTypes";
import {
  cachedGenericRestAPIAppend,
  cachedGenericRestAPIError,
  cachedGenericRestAPIInvalidate,
  cachedGenericRestAPILoading,
  cachedGenericRestAPISet,
  CachedStateActionType
} from "reduxConfigs/actions/cachedState.action";
import { cachedRestApiState } from "reduxConfigs/selectors/cachedRestapiSelector";
import RestapiService from "services/restapiService";

function* cachedRestApiEffectSaga(action: CachedStateActionType): any {
  const { uri, method, id, data, storageType, forceLoad, uniqueByKey } = action.payload;
  const restService = new RestapiService();
  let response: any = {};

  /** removing the cached state if stored data for current request is expired */
  yield put(cachedGenericRestAPIInvalidate(uri, method, id));
  const cachedRestState = yield select(cachedRestApiState);
  const isCachedData = !!get(cachedRestState, [uri, method, id], undefined);
  if (!isCachedData || forceLoad) {
    try {
      yield put(cachedGenericRestAPILoading(true, uri, method, id));
      switch (method) {
        case "list":
          response = yield call(get(restService, [uri, method]), data);
          break;
        case "get":
          response = yield call(get(restService, [uri, method]), id);
          break;
      }
      if (storageType === "set") yield put(cachedGenericRestAPISet(response?.data, uri, method, id));
      else yield put(cachedGenericRestAPIAppend(response?.data, uri, method, id, 3600, uniqueByKey));

      yield put(cachedGenericRestAPIError(false, uri, method, id));
    } catch (e) {
      handleError({
        bugsnag: {
          message: (e as any)?.message,
          severity: severityTypes.ERROR,
          context: issueContextTypes.APIS,
          data: { e, action, response }
        }
      });
      yield put(cachedGenericRestAPIError(true, uri, method, id));
    }
  }
}

export function* cachedRestApiSagaWatcher() {
  yield takeLatest(CACHED_GENERIC_REST_API_READ, cachedRestApiEffectSaga);
}
