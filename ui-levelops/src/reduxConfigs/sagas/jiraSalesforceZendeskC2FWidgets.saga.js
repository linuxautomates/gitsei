import * as actionTypes from "reduxConfigs/actions/restapi";
import { put, select, take, takeEvery, all } from "redux-saga/effects";
import { JIRA_ZENDESK_SALESFORCE_C2F_WIDGETS } from "reduxConfigs/actions/actionTypes";
import { get } from "lodash";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";

const restapiState = state => state.restapiReducer;

export function* jiraZendeskSalesforceC2FWidgetsSaga(action) {
  const { data: filters, id, uri, method, complete } = action;
  try {
    const baseComplete = `JIRA_ZENDESK_SALESFORCE_C2F`;
    const calls = [
      {
        uri,
        method,
        filters: {
          ...(filters || {}),
          filter: {
            ...(filters.filter || {}),
            jira_has_commit: true
          }
        },
        complete: `${baseComplete}_${id}_commit_true`,
        id: `${id}_commit_true`
      },
      {
        uri,
        method,
        filters: {
          ...(filters || {}),
          filter: {
            ...(filters.filter || {}),
            jira_has_commit: false
          }
        },
        complete: `${baseComplete}_${id}_commit_false`,
        id: `${id}_commit_false`
      }
    ];

    yield put(actionTypes.restapiLoading(true, uri, method, id));
    yield all(
      calls.map(call =>
        put(actionTypes.genericList(call.uri, call.method, call.filters, call.complete, call.id, false))
      )
    );

    yield all(calls.map(call => take(call.complete)));

    const rState = yield select(restapiState);

    let records = {};

    calls.forEach(call => {
      const record = get(rState, [call.uri, call.method, call.id, "data", "records"], []);
      const key = call.id.includes("true") ? "has_commit" : "no_commit";
      record.forEach(rc => {
        records[rc.key] = {
          ...(records[rc.key] || {}),
          ...rc,
          [key]: rc["total_tickets"]
        };
      });
    });

    yield put(actionTypes.restapiData({ records: Object.keys(records).map(rc => records[rc]) }, uri, method, id));
    yield all(calls.map(call => put(actionTypes.restapiClear(call.uri, call.method, call.id))));
    yield put(actionTypes.restapiLoading(false, uri, method, id));
  } catch (e) {
    yield put(actionTypes.restapiData({ records: [] }, uri, method, id));
    yield put(actionTypes.restapiLoading(false, uri, method, id));
    handleError({
      bugsnag: {
        message: e?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });
  }
}

export function* jiraZendeskSalesforceC2FWidgetsWatcher() {
  yield takeEvery([JIRA_ZENDESK_SALESFORCE_C2F_WIDGETS], jiraZendeskSalesforceC2FWidgetsSaga);
}
