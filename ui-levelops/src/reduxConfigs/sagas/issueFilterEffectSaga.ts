import { put, select, take, takeEvery } from "redux-saga/effects";
import { restapiData, restapiLoading } from "../actions/restapi/restapiActions";
import { get } from "lodash";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { ISSUE_LEAD_TIME_FILTER_VALUES } from "../actions/actionTypes";

const restapiState = (state: any) => state.restapiReducer;

export function* issueFiltersEffectSaga(action: any): any {
  const actionId = action.id;
  yield put(restapiLoading(true, action.uri, action.method, actionId));
  const issueFields = (action.data.fields || []).filter((field: string) => field.includes("workitem_"));

  const cicdFields = (action.data.fields || []).filter((field: string) => !field.includes("workitem_"));

  if (issueFields.length) {
    yield put(
      actionTypes.widgetFilterValuesGet(
        "issue_management_workitem_values",
        { ...action.data, fields: issueFields },
        `COMPLETE_ISSUE_FILTERS`,
        actionId
      )
    );
    yield take(`COMPLETE_ISSUE_FILTERS`);
  }

  if (cicdFields.length) {
    yield put(
      actionTypes.widgetFilterValuesGet(
        "github_prs_filter_values",
        { ...action.data, fields: cicdFields },
        `COMPLETE_CICD_FILTERS`,
        actionId
      )
    );
    yield take(`COMPLETE_CICD_FILTERS`);
  }

  const apiState = yield select(restapiState);

  const mappedIssueFields = get(
    apiState,
    ["issue_management_workitem_values", "list", actionId, "data", "records"],
    []
  ).map(record => {
    const labelKey = Object.keys(record || [null])[0];
    if (labelKey) {
      return {
        [`${labelKey}`]: record[labelKey]
      };
    }
    return true;
  });

  const customFields = get(
    apiState,
    ["issue_management_workitem_values", "list", actionId, "data", "custom_fields"],
    []
  );
  const mappedCicdFields = get(apiState, ["github_prs_filter_values", "list", actionId, "data", "records"], []).map(
    record => {
      const labelKey = Object.keys(record || [null])[0];
      if (labelKey) {
        return {
          [labelKey]: record[labelKey]
        };
      }
      return true;
    }
  );

  let data: any = {
    records: []
  };

  if (mappedIssueFields.length) {
    data = {
      ...data,
      records: [...mappedIssueFields],
      custom_fields: customFields
    };
  }

  if (cicdFields.length) {
    data = {
      ...data,
      records: [...data.records, ...mappedCicdFields]
    };
  }

  yield put(restapiData(data, action.uri, action.method, actionId));
  yield put(restapiLoading(false, action.uri, action.method, actionId));
}

export function* issueFiltersEffectWatcherSaga() {
  yield takeEvery([ISSUE_LEAD_TIME_FILTER_VALUES], issueFiltersEffectSaga);
}
