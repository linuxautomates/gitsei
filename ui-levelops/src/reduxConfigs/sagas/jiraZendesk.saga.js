import { all, put, select, take, takeEvery } from "redux-saga/effects";
import { JIRA_ZENDESK } from "../actions/actionTypes";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { getData, getError } from "utils/loadingUtils";
import { restapiData, restapiLoading } from "../actions/restapi";
import { transformFilters, transformFiltersZendesk } from "utils/dashboardFilterUtils";
import { sanitizeObject } from "utils/commonUtils";
import { get } from "lodash";

const restapiState = state => state.restapiReducer;

export function* jiraZendeskEffectSaga(action) {
  const id = action.id || "0";
  yield put(restapiLoading(true, action.uri, action.method, id));
  const complete = `COMPLETE_JIRA_ZENDESK_LIST_${id}`;
  let data = {};
  const createdAtKey = Object.keys(action.data.filter).find(key => key.includes("issue_created_at"));

  const updatedFilters = action.data.filter || {};

  const { jiraCustomFields, zendeskCustomFields } = transformFiltersZendesk(action.data.filter);
  let actionList = [
    {
      uri: "zendesk_tickets_report",
      method: "list",
      filters: {
        across: "status",
        filter: {
          ...sanitizeObject({
            ...(transformFilters(action.data.filter, "zendesk_") || {}),
            created_at: createdAtKey ? action.data.filter[createdAtKey] : undefined
          }),
          custom_fields: zendeskCustomFields
        }
      },
      complete: `${complete}_zendesk_tickets`,
      id: `${id}_zendesk`
    },
    {
      uri: "jira_zendesk_aggs_list_zendesk",
      method: "list",
      filters: {
        across: "status",
        filter: {
          ...sanitizeObject({
            ...updatedFilters,
            zendesk_created_at: createdAtKey ? action.data.filter[createdAtKey] : undefined,
            [createdAtKey]: undefined
          }),
          custom_fields: {
            ...jiraCustomFields,
            ...zendeskCustomFields
          }
        }
      },
      complete: `${complete}_escalated`,
      id: `${id}_zendesk_list`
    },
    {
      uri: "jira_zendesk",
      method: "list",
      filters: {
        ...action.data,
        across: "jira_status",
        filter: {
          ...sanitizeObject({
            ...updatedFilters,
            jira_has_commit: true,
            jira_get_commit: false,
            [createdAtKey]: undefined,
            zendesk_created_at: createdAtKey ? action.data.filter[createdAtKey] : undefined
          }),
          custom_fields: {
            ...jiraCustomFields,
            ...zendeskCustomFields
          }
        }
      },
      complete: `${complete}_jira_pr`,
      id: `${id}_jira_pr`
    },
    {
      uri: "jira_zendesk",
      method: "list",
      filters: {
        ...action.data,
        across: "jira_status",
        filter: {
          ...sanitizeObject({
            ...updatedFilters,
            jira_has_commit: false,
            jira_get_commit: false,
            [createdAtKey]: undefined,
            zendesk_created_at: createdAtKey ? action.data.filter[createdAtKey] : undefined
          }),
          custom_fields: {
            ...jiraCustomFields,
            ...zendeskCustomFields
          }
        }
      },
      complete: `${complete}_no_jira_pr`,
      id: `${id}_jira_no_pr`
    },
    {
      uri: "jira_zendesk",
      method: "list",
      filters: {
        ...action.data,
        across: "jira_status",
        filter: {
          ...sanitizeObject({
            ...updatedFilters,
            jira_has_commit: true,
            jira_get_commit: true,
            [createdAtKey]: undefined,
            zendesk_created_at: createdAtKey ? action.data.filter[createdAtKey] : undefined
          }),
          custom_fields: {
            ...jiraCustomFields,
            ...zendeskCustomFields
          }
        }
      },
      complete: `${complete}_commits`,
      id: `${id}_scm_commits`
    }
  ];

  yield all(
    actionList.map(call =>
      put(actionTypes.genericList(call.uri, call.method, call.filters, call.complete, call.id, false))
    )
  );
  yield all(actionList.map(call => take(call.complete)));

  const apiState = yield select(restapiState);

  // yield put(
  //   actionTypes.genericList(
  //     "zendesk_tickets_report",
  //     "list",
  //     { across: "status", filter: transformZendeskFilters(action.data.filter) },
  //     complete,
  //     `${id}_zendesk`
  //   )
  // );
  // yield take(complete);
  //
  // const zendeskState = yield select(restapiState);
  if (getError(apiState, "zendesk_tickets_report", "list", `${id}_zendesk`)) {
    data.zendesk_total_data = [];
    yield put(restapiData(data, action.uri, action.method, `${id}_zendesk`));
    yield put(restapiLoading(false, action.uri, action.method, `${id}_zendesk`));
  }

  let zendeskTotalData = getData(apiState, "zendesk_tickets_report", "list", `${id}_zendesk`);
  data.zendesk_total_data = zendeskTotalData.records || [];

  // yield put(
  //   actionTypes.genericList(
  //     "jira_zendesk_aggs_list_zendesk",
  //     "list",
  //     //{ across: "status", filter: transformZendeskFilters(action.data.filter) },
  //     { across: "status", filter: action.data.filter },
  //     complete,
  //     `${id}_zendesk_list`
  //   )
  // );
  // yield take(complete);
  //
  // const zendeskEscalatedState = yield select(restapiState);
  if (getError(apiState, "jira_zendesk_aggs_list_zendesk", "list", `${id}_zendesk_list`)) {
    data.zendesk_escalated_count = 0;
    yield put(restapiData(data, action.uri, action.method, `${id}_zendesk_list`));
    yield put(restapiLoading(false, action.uri, action.method, `${id}_zendesk_list`));
  }

  let zendeskEscalatedData = getData(apiState, "jira_zendesk_aggs_list_zendesk", "list", `${id}_zendesk_list`);
  data.zendesk_escalated_count = !!zendeskEscalatedData._metadata ? zendeskEscalatedData._metadata.total_count : 0;

  // yield put(
  //   actionTypes.genericList(
  //     "jira_zendesk",
  //     action.method,
  //     {
  //       ...action.data,
  //       across: "jira_status",
  //       filter: {
  //         ...action.data.filter,
  //         jira_has_commit: true,
  //         jira_get_commit: false
  //       }
  //     },
  //     complete,
  //     `${id}_jira_pr`
  //   )
  // );
  // yield take(complete);
  //
  // const jiraHasCommitState = yield select(restapiState);

  if (getError(apiState, "jira_zendesk", action.method, `${id}_jira_pr`)) {
    data.jira_pr_data = [];
    yield put(restapiData(data, action.uri, action.method, `${id}_jira_pr`));
    yield put(restapiLoading(false, action.uri, action.method, `${id}_jira_pr`));
  }

  let jiraCommitData = getData(apiState, "jira_zendesk", action.method, `${id}_jira_pr`);
  data.jira_pr_data = jiraCommitData.records || [];

  // yield put(
  //   actionTypes.genericList(
  //     "jira_zendesk",
  //     action.method,
  //     {
  //       ...action.data,
  //       across: "jira_status",
  //       filter: {
  //         ...action.data.filter,
  //         jira_has_commit: false,
  //         jira_get_commit: false
  //       }
  //     },
  //     complete,
  //     `${id}_jira_no_pr`
  //   )
  // );
  // yield take(complete);
  //
  // const jiraNoCommitState = yield select(restapiState);

  if (getError(apiState, "jira_zendesk", action.method, `${id}_jira_no_pr`)) {
    data.jira_no_pr_data = [];
    yield put(restapiData(data, action.uri, action.method, `${id}_jira_no_pr`));
    yield put(restapiLoading(false, action.uri, action.method, `${id}_jira_no_pr`));
  }

  let jiraNoCommitData = getData(apiState, "jira_zendesk", action.method, `${id}_jira_no_pr`);
  data.jira_no_pr_data = jiraNoCommitData.records || [];

  // yield put(
  //   actionTypes.genericList(
  //     "jira_zendesk",
  //     action.method,
  //     {
  //       ...action.data,
  //       across: "jira_status",
  //       filter: {
  //         ...action.data.filter,
  //         jira_has_commit: true,
  //         jira_get_commit: true
  //       }
  //     },
  //     complete,
  //     `${id}_scm_commits`
  //   )
  // );
  // yield take(complete);
  //
  // const scmCommitStats = yield select(restapiState);

  if (getError(apiState, "jira_zendesk", action.method, `${id}_scm_commits`)) {
    data.scm_commits_data = [];
    data.jira_has_commit_data = [];
    yield put(restapiData(data, action.uri, action.method, `${id}_scm_commits`));
    yield put(restapiLoading(false, action.uri, action.method, `${id}_scm_commits`));
  }

  let scmCommitsData = getData(apiState, "jira_zendesk", action.method, `${id}_scm_commits`);
  data.scm_commits_data = scmCommitsData.records || [];

  yield put(restapiData(data, action.uri, action.method, id));
  yield put(restapiLoading(false, action.uri, action.method, id));
}

export function* jiraZendeskWatcherSaga() {
  yield takeEvery([JIRA_ZENDESK], jiraZendeskEffectSaga);
}
