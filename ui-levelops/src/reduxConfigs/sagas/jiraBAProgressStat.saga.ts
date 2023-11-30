import { issueContextTypes, severityTypes } from "bugsnag";
import { graphStatReportTransformer } from "custom-hooks/helpers/graphStatHelper";
import { handleError } from "helper/errorReporting.helper";
import { get } from "lodash";
import { all, call, put, select, takeEvery } from "redux-saga/effects";
import { JIRA_PROGRESS_STAT } from "reduxConfigs/actions/actionTypes";
import { restapiClear, restapiData, restapiError, restapiLoading } from "reduxConfigs/actions/restapi";
import { restapiEffectSaga } from "./restapiSaga";

const restapiState = (state: any) => state.restapiReducer;

function* jiraBAProgressStatEffectSaga(action: any): any {
  const actionId = action.id;
  const JIRA_PROGRESS_REGULAR_APICALL_ID = `JIRA_PROGRESS_REGULAR_APICALL_ID_${actionId}`;
  const JIRA_PROGRESS_COMPLETED_APICALL_ID = `JIRA_PROGRESS_COMPLETED_APICALL_ID_${actionId}`;
  const filters = action?.data;
  const apiCalls = [
    {
      data: filters,
      id: JIRA_PROGRESS_REGULAR_APICALL_ID,
      method: "list",
      uri: action?.uri
    },
    {
      data: {
        ...(filters || {}),
        filter: {
          ...get(filters, ["filter"], {}),
          status_categories: ["Done"]
        }
      },
      id: JIRA_PROGRESS_COMPLETED_APICALL_ID,
      method: "list",
      uri: action.uri
    }
  ];
  try {
    yield all(apiCalls.map(newCall => call(restapiEffectSaga, newCall)));
    let restState = yield select(restapiState);
    const apiDataList = apiCalls.map(call => {
      return get(restState, [call.uri, call.method, call.id], {});
    });
    const totalDataRecords = get(apiDataList[0], ["data", "records"], []);
    const completedDataRecords = get(apiDataList[1], ["data", "records"], []);
    const resultData = graphStatReportTransformer(filters, totalDataRecords, completedDataRecords, "total_tickets");
    yield put(restapiData({ records: resultData }, action.uri, action.method, actionId));
    yield put(restapiError(false, action.uri, action.method, action.id));
    yield put(restapiLoading(false, action.uri, action.method, actionId));
    yield all(apiCalls.map(call => put(restapiClear(call.uri, call.method, call.id))));
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });
    yield put(restapiError(true, action.uri, action.method, actionId));
    yield put(restapiLoading(false, action.uri, action.method, actionId));
  }
}

export function* jiraBAProgressStatWatcherSaga() {
  yield takeEvery([JIRA_PROGRESS_STAT], jiraBAProgressStatEffectSaga);
}
