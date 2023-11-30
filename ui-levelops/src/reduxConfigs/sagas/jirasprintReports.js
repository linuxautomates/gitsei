import { takeEvery, put, call } from "redux-saga/effects";
import { JIRA_SPRINT_REPORTS_LIST, JIRA_SPRINT_FILTER_LIST } from "reduxConfigs/actions/actionTypes";
import RestapiService from "../../services/restapiService";
import { restapiData, restapiError, restapiLoading } from "../actions/restapi/restapiActions";
import { handleRestApiError } from "./restapiSaga";
import { get } from "lodash";
import { handleError } from "helper/errorReporting.helper";
import { issueContextTypes, severityTypes } from "bugsnag";

export function* jiraSprintReportListEffectSaga(action) {
  const actionId = action.id;
  let payload = action.data;


  let restService = new RestapiService();
  let response;
  let hasNextPage = true;
  let data = {};

  yield put(restapiLoading(true, action.uri, action.method, actionId));

  try {
    let page = 0;
    while (hasNextPage) {
      payload = {
        ...payload,
        page,
        page_size: 1000
      };

      response = yield call(restService[action.uri][action.method], payload);

      data.count = (data.count || 0) + (response.data.count || 0);
      data.records = [...(data.records || []), ...(response.data.records || [])];
      data._metadata = response._metadata;
      hasNextPage = get(response, ["data", "_metadata", "has_next"], false);
      page = hasNextPage ? page + 1 : page;
    }

    yield put(restapiData(data, action.uri, action.method, actionId));
    yield put(restapiLoading(false, action.uri, action.method, actionId));
    yield put(restapiError(false, action.uri, action.method, actionId));
  } catch (e) {
    handleError({
      bugsnag: {
        message: e?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });
    yield call(handleRestApiError, e, action, response);
  }
}

export function* jiraSprintReportsList() {
  yield takeEvery([JIRA_SPRINT_REPORTS_LIST], jiraSprintReportListEffectSaga);
}

export function* jiraSprintFilterList() {
  yield takeEvery([JIRA_SPRINT_FILTER_LIST], jiraSprintReportListEffectSaga);
}
