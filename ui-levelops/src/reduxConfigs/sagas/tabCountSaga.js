import { all, call, put, takeLatest } from "redux-saga/effects";
import {
  workbenchTabClear,
  workbenchTabData,
  workbenchTabError,
  workbenchTabLoading
} from "../actions/tabCountActions";
import { WORKBENCH_TAB_COUNTS } from "../actions/actionTypes";
import restapiService from "../../services/restapiService";

export function* workbenchTabCountEffectSaga(action) {
  let filters = action.filters;
  console.log(filters);
  // filters will be of the format
  // [name: <name>, filter: <filter>]
  yield put(workbenchTabClear());
  yield put(workbenchTabLoading(true));
  let data = [];
  let rs = new restapiService();
  try {
    let responses = yield all(filters.map(filter => call(rs.workitem.list, { page_size: 1, page: 0, filter: filter })));
    responses.forEach(response => {
      if (response && response.data && response.data._metadata) {
        data.push(response.data._metadata.total_count);
      } else {
        data.push(0);
      }
    });
    yield put(workbenchTabData(data));
    yield put(workbenchTabLoading(false));
  } catch (e) {
    yield put(workbenchTabError(true));
  }
}

export function* workbenchTabCountWatcherSaga() {
  yield takeLatest([WORKBENCH_TAB_COUNTS], workbenchTabCountEffectSaga);
}
