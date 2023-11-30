import { get } from "lodash";
import { call, put, takeEvery } from "redux-saga/effects";
import { REST_API_SELECT_GENERIC_LIST } from "reduxConfigs/actions/actionTypes";
import { restapiData, restapiError, restapiLoading } from "reduxConfigs/actions/restapi";
import RestapiService from "services/restapiService";
import { sanitizeObjectCompletely } from "utils/commonUtils";

import { handleRestApiError } from "./restapiSaga";

function* restApiSelectGenericListEffectSaga(action: any): any {
  const { uri, method, filters, id, loadAllData, complete } = action;
  let restService: any = new RestapiService();
  let response;

  yield put(restapiLoading(true, uri, method, id.toString()));
  try {
    let hasNextPage = true;
    let data: any = { count: 0 };
    let count = 0;
    while (hasNextPage) {
      const nfilters = sanitizeObjectCompletely({ ...filters, page: count });
      response = yield call(restService[action.uri][action.method], nfilters);
      data.count = data?.count + get(response, ["data", "count"], 0);
      data.records = [...(data?.records || []), ...(response?.data?.records || [])];
      data._metadata = response?.data?._metadata;
      hasNextPage = get(response, ["data", "_metadata", "has_next"], false) && loadAllData;
      count = count + 1;
    }
    yield put(restapiData(data, uri, method, id.toString()));
    yield put(restapiLoading(false, uri, method, id.toString()));
    yield put(restapiError(false, uri, method, id.toString()));
  } catch (e) {
    yield call(handleRestApiError, e, action, response);
  }
}

export function* restApiSelectGenericListWatcher() {
  yield takeEvery(REST_API_SELECT_GENERIC_LIST, restApiSelectGenericListEffectSaga);
}
