import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { get, map } from "lodash";
import { call, put, select, takeLatest } from "redux-saga/effects";
import { SUPPORTED_FILTER_GET } from "reduxConfigs/actions/actionTypes";
import * as formActions from "reduxConfigs/actions/formActions";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";

import { restapiEffectSaga } from "./restapiSaga";

function* supportedFilterGetEffectSaga(action: any): any {
  const { uri, filters, id } = action;
  try {
    yield put(formActions.formInitialize(id, { loading: true }));
    let restState = yield select(restapiState);
    let filtersGetState = restState?.[uri]?.list?.[id] || {};
    let apiData = get(filtersGetState, ["data", "records"], []);
    if (apiData.length > 0) {
      apiData = map(apiData, (item: any) => {
        return item[Object.keys(item)[0]];
      })[0];
      yield put(formActions.formUpdateObj(id, { records: apiData, loading: false }));
      return;
    }
    yield call(restapiEffectSaga, { uri, method: "list", data: filters, id });
    restState = yield select(restapiState);
    filtersGetState = restState?.[uri]?.list?.[id] || {};
    apiData = get(filtersGetState, ["data", "records"], []);
    if (apiData.length > 0) {
      apiData = map(apiData, (item: any) => {
        return item[Object.keys(item)[0]];
      })[0];
    }
    yield put(formActions.formUpdateObj(id, { records: apiData, loading: false }));
  } catch (e) {
    yield put(formActions.formUpdateObj(id, { loading: false }));
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });
  }
}

export function* supportedFilterGetWatcher() {
  yield takeLatest(SUPPORTED_FILTER_GET, supportedFilterGetEffectSaga);
}
