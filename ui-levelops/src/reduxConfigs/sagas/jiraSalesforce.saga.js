import { put, select, take, takeEvery, all } from "redux-saga/effects";
import { JIRA_SALESFORCE } from "../actions/actionTypes";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { getData, getError } from "utils/loadingUtils";
import { restapiData, restapiLoading } from "../actions/restapi";
import { transformFilters, transformFiltersZendesk } from "utils/dashboardFilterUtils";
import { unset } from "lodash";

const restapiState = state => state.restapiReducer;

export function* jiraSalesforceEffectSaga(action) {
  const id = action.id || "0";
  yield put(restapiLoading(true, action.uri, action.method, id));
  const complete = `COMPLETE_JIRA_SALESFORCE_LIST_${id}`;
  let data = {};
  const { jiraCustomFields, zendeskCustomFields } = transformFiltersZendesk(action.data.filter);
  const salesforceFilters = transformFilters(action.data.filter, "salesforce_");
  unset(salesforceFilters, ["custom_fields"]);
  let actionList = [
    {
      uri: "salesforce_tickets_report",
      method: "list",
      filters: { across: "status", filter: salesforceFilters },
      complete: `${complete}_salesforce_tickets`,
      id: `${id}_salesforce`
    },
    {
      uri: "jira_salesforce_aggs_list_salesforce",
      method: "list",
      filters: { across: "status", filter: { ...action.data.filter, custom_fields: jiraCustomFields } },
      complete: `${complete}_escalated`,
      id: `${id}_salesforce_list`
    },
    {
      uri: "jira_salesforce",
      method: "list",
      filters: {
        ...action.data,
        across: "jira_status",
        filter: {
          ...action.data.filter,
          custom_fields: jiraCustomFields,
          jira_has_commit: true,
          jira_get_commit: false
        }
      },
      complete: `${complete}_jira_pr`,
      id: `${id}_jira_pr`
    },
    {
      uri: "jira_salesforce",
      method: "list",
      filters: {
        ...action.data,
        across: "jira_status",
        filter: {
          ...action.data.filter,
          custom_fields: jiraCustomFields,
          jira_has_commit: false,
          jira_get_commit: false
        }
      },
      complete: `${complete}_no_jira_pr`,
      id: `${id}_jira_no_pr`
    },
    {
      uri: "jira_salesforce",
      method: "list",
      filters: {
        ...action.data,
        across: "jira_status",
        filter: {
          ...action.data.filter,
          custom_fields: jiraCustomFields,
          jira_has_commit: true,
          jira_get_commit: true
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

  if (getError(apiState, "salesforce_tickets_report", "list", `${id}_salesforce`)) {
    data.salesforce_total_data = [];
    yield put(restapiData(data, action.uri, action.method, `${id}_salesforce`));
    yield put(restapiLoading(false, action.uri, action.method, `${id}_salesforce`));
  }

  let salesforceTotalData = getData(apiState, "salesforce_tickets_report", "list", `${id}_salesforce`);
  data.salesforce_total_data = salesforceTotalData.records || [];

  if (getError(apiState, "jira_salesforce_aggs_list_salesforce", "list", `${id}_salesforce_list`)) {
    data.salesforce_escalated_count = 0;
    yield put(restapiData(data, action.uri, action.method, `${id}_salesforce_list`));
    yield put(restapiLoading(false, action.uri, action.method, `${id}_salesforce_list`));
  }

  let salesforceEscalatedData = getData(
    apiState,
    "jira_salesforce_aggs_list_salesforce",
    "list",
    `${id}_salesforce_list`
  );
  data.zendesk_escalated_count = !!salesforceEscalatedData._metadata
    ? salesforceEscalatedData._metadata.total_count
    : 0;

  if (getError(apiState, "jira_salesforce", action.method, `${id}_jira_pr`)) {
    data.jira_pr_data = [];
    yield put(restapiData(data, action.uri, action.method, `${id}_jira_pr`));
    yield put(restapiLoading(false, action.uri, action.method, `${id}_jira_pr`));
  }

  let jiraCommitData = getData(apiState, "jira_salesforce", action.method, `${id}_jira_pr`);
  data.jira_pr_data = jiraCommitData.records || [];

  if (getError(apiState, "jira_salesforce", action.method, `${id}_jira_no_pr`)) {
    data.jira_no_pr_data = [];
    yield put(restapiData(data, action.uri, action.method, `${id}_jira_no_pr`));
    yield put(restapiLoading(false, action.uri, action.method, `${id}_jira_no_pr`));
  }

  let jiraNoCommitData = getData(apiState, "jira_salesforce", action.method, `${id}_jira_no_pr`);
  data.jira_no_pr_data = jiraNoCommitData.records || [];

  if (getError(apiState, "jira_salesforce", action.method, `${id}_scm_commits`)) {
    data.scm_commits_data = [];
    data.jira_has_commit_data = [];
    yield put(restapiData(data, action.uri, action.method, `${id}_scm_commits`));
    yield put(restapiLoading(false, action.uri, action.method, `${id}_scm_commits`));
  }

  let scmCommitsData = getData(apiState, "jira_salesforce", action.method, `${id}_scm_commits`);
  data.scm_commits_data = scmCommitsData.records || [];

  yield put(restapiData(data, action.uri, action.method, id));
  yield put(restapiLoading(false, action.uri, action.method, id));
}

export function* jiraSalesforceWatcherSaga() {
  yield takeEvery([JIRA_SALESFORCE], jiraSalesforceEffectSaga);
}
