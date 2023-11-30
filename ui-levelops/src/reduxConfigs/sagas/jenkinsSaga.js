import { JENKINS_INTEGRATION_LIST } from "../actions/actionTypes";
import { put, select, call, takeEvery } from "redux-saga/effects";
import { get } from "lodash";
import { restapiData, restapiError, restapiLoading } from "../actions/restapi/restapiActions";
import RestapiService from "../../services/restapiService";
import { handleRestApiError } from "./restapiSaga";
import { handleError } from "helper/errorReporting.helper";
import { issueContextTypes, severityTypes } from "bugsnag";

const restapiState = state => state.restapiReducer;

export function* jenkinsIntegrationList(action) {
  const actionId = action.id;
  let payload = action.data;
  const isPolling = action.isPolling;
  const fetchNextPage = action.fetchNextPage;


  let restService = new RestapiService();
  let response;

  yield put(restapiLoading(true, action.uri, action.method, actionId));

  try {
    if (fetchNextPage) {
      const apiState = yield select(restapiState);
      const currentPage = get(apiState, [action.uri, action.method, actionId, "data", "_metadata", "page"]);
      payload = {
        ...payload,
        page: currentPage + 1
      };
    }

    response = yield call(restService[action.uri][action.method], payload);

    const data = response.data;
    let updateResponse = true;

    if (fetchNextPage) {
      const state = yield select(restapiState);
      const prevData = get(state, [action.uri, action.method, actionId, "data"], {});
      data.count = prevData.count + data.count;
      data.records = [...prevData.records, ...data.records];
    }

    if (isPolling) {
      const state = yield select(restapiState);
      const prevData = get(state, [action.uri, action.method, actionId, "data"], {});
      const prevRecords = [...prevData.records].map(record => record.id);
      const newRecords = data.records.map(record => record.id);

      if (data._metadata.page_size < data._metadata.total_count) {
        prevRecords.splice(data._metadata.page_size);
      }

      const _diff = newRecords.every(id => prevRecords.includes(id));
      if (!_diff && prevData._metadata.total_count === data._metadata.total_count) {
        updateResponse = false;
      }
    }

    if (updateResponse) {
      yield put(restapiData(data, action.uri, action.method, actionId));
    }
    yield put(restapiLoading(false, action.uri, action.method, actionId));
    yield put(restapiError(false, action.uri, action.method, actionId));
  } catch (e) {
    handleError({
      bugsnag: {
        message: e?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.INTEGRATIONS,
        data: { e, action }
      }
    });

    yield call(handleRestApiError, e, action, response);
  }
}

export function* jenkinsIntegrationWatcherSaga() {
  yield takeEvery([JENKINS_INTEGRATION_LIST], jenkinsIntegrationList);
}
