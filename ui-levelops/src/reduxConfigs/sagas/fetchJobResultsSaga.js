import { put, select, take, takeLatest } from "redux-saga/effects";
import { FETCH_JOB_RESULTS } from "../actions/actionTypes";
import * as actionTypes from "../actions/restapi";
import { paginationGet } from "../actions/paginationActions";
import { restapiState } from "../selectors/restapiSelector";
import { get } from "lodash";

export function* fetchJobResultsEffectSaga(action) {
  const URI = "fetchJobResults";
  const METHOD = "list";
  const ID = action.id || "0";
  let error = false;
  let filters = action.filters;

  try {
    yield put(actionTypes.restapiLoading(true, URI, METHOD, ID));
    const DATA_LOADED_ACTION = "DATA_LOADED_ACTION";
    yield put(paginationGet("jenkins_pipeline_triage_runs", METHOD, filters, ID, false, "all", DATA_LOADED_ACTION));
    yield take(DATA_LOADED_ACTION);
    const state = yield select(restapiState);
    const data = get(state, ["jenkins_pipeline_triage_runs", "list", ID, "data", "records"], []);
    yield put(
      actionTypes.restapiData(
        data.map(d => d.id),
        URI,
        METHOD,
        ID
      )
    );
  } catch (e) {
    error = true;
  } finally {
    yield put(actionTypes.restapiLoading(false, URI, METHOD, ID));
    yield put(actionTypes.restapiError(error, URI, METHOD, ID));
  }
}

export function* fetchJobResultsWatcherSaga() {
  yield takeLatest([FETCH_JOB_RESULTS], fetchJobResultsEffectSaga);
}
