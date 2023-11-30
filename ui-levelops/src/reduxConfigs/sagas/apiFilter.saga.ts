import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { get } from "lodash";
import { call, put, takeEvery } from "redux-saga/effects";
import { GET_API_FILTER_DATA } from "reduxConfigs/actions/actionTypes";
import { BaseActionType } from "reduxConfigs/actions/restapi/action.type";
import { APIFilterActionType } from "reduxConfigs/actions/restapi/apiFilter.action";
import {
  genericRestAPIError,
  genericRestAPILoading,
  genericRestAPISet
} from "reduxConfigs/actions/restapi/genericSet.action";
import RestapiService from "services/restapiService";
import getUniqueId from "utils/uniqueID";

type APIFilterSagaActionType = APIFilterActionType & BaseActionType;

function* apiFilterEffectSaga(action: APIFilterSagaActionType): any {
  const uri = action.uri;
  const method = action.method ?? "list";
  const id = action.id ?? `${uri}_${method}_${getUniqueId()}`;
  const payload = action.payload ?? {};
  try {
    const restService = new RestapiService();
    const restFunction = get(restService, [uri, method]);
    let response: any;
    if (restFunction) {
      yield put(genericRestAPILoading(true, uri, method, id));
      switch (method) {
        case "bulk":
        case "list":
          response = yield call(restFunction, payload);
          break;
        case "get":
          response = yield call(restFunction, id);
          break;
        default:
          throw Error("Rest Method Not Found");
      }
      yield put(genericRestAPISet(response?.data ?? response, uri, method, id));
      yield put(genericRestAPILoading(false, uri, method, id));
    } else {
      yield put(genericRestAPIError("Rest Service Function Not Found", uri, method, id));
    }
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });
    yield put(genericRestAPIError(e, uri, method, id));
  }
}

export function* apiFilterWatcherSaga() {
  yield takeEvery([GET_API_FILTER_DATA], apiFilterEffectSaga);
}
