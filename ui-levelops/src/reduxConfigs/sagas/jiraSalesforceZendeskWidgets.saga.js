import * as actionTypes from "reduxConfigs/actions/restapi";
import { put, select, take, takeEvery } from "redux-saga/effects";
import { get } from "lodash";
import { JIRA_ZENDESK_SALESFORCE_STAGES_WIDGETS } from "reduxConfigs/actions/actionTypes";
import { uniqBy, min, max } from "lodash";
import { isSanitizedArray } from "utils/commonUtils";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { convertToDays } from "utils/timeUtils";

const restapiState = state => state.restapiReducer;

export function* jiraZendeskSalesforceStagesWidgetsSaga(action) {
  const { data: filters, id, uri, method } = action;
  try {
    const from_state = get(filters, ["filter", "state_transition", "from_state"], "");
    const to_state = get(filters, ["filter", "state_transition", "to_state"], "");
    if (!from_state.length || !to_state.length) {
      throw new Error("Please select the stages");
    }
    const baseComplete = `JIRA_ZENDESK_SALESFORCE_STAGES`;
    yield put(actionTypes.restapiLoading(true, uri, method, id));
    yield put(
      actionTypes.genericList(
        uri,
        method,
        { ...filters, sort: [{ id: "escalation_time", desc: true }] },
        `${baseComplete}_${id}`,
        id,
        false
      )
    );
    yield take(`${baseComplete}_${id}`);

    const rState = yield select(restapiState);
    const baseRecords = get(rState, [uri, method, id, "data", "records"], []);
    let restData = get(rState, [uri, method, id], {});
    if (baseRecords.length) {
      const jiraKeys = baseRecords.reduce((acc, obj) => {
        if (!acc.includes(obj.jira_key)) {
          acc.push(obj.jira_key);
        }
        return acc;
      }, []);

      const jiraCall = {
        uri: "jira_tickets",
        method: "list",
        filters: {
          filter: {
            ...(filters.filter || {}),
            keys: jiraKeys
          },
          sort: [{ id: "state_transition_time", desc: true }]
        },
        complete: `${baseComplete}_jira_tickets_${id}`,
        id: `jira_tickets_${id}`
      };

      yield put(
        actionTypes.genericList(jiraCall.uri, jiraCall.method, jiraCall.filters, jiraCall.complete, jiraCall.id, false)
      );
      yield take(jiraCall.complete);
      const jiraData = get(
        yield select(restapiState),
        [jiraCall.uri, jiraCall.method, jiraCall.id, "data", "records"],
        []
      );

      let mappedRecords = [];

      const idKey = uri.includes("salesforce") ? "case_id" : "id";

      uniqBy(baseRecords, idKey).forEach(record => {
        const records = baseRecords.filter(rc => rc[idKey] === record[idKey]);
        const escalation_time = get(record, ["escalation_time"], 0);
        const jiraKeys = records.reduce((acc, obj) => {
          if (!acc.includes(obj.jira_key)) {
            acc.push(obj.jira_key);
          }
          return acc;
        }, []);

        let resolution_time = "NA";

        if (jiraKeys.length) {
          const filteredJiraData = jiraData.filter(data => jiraKeys.includes(data.key));
          const issueCreatedAt = filteredJiraData.map(d => d.issue_created_at);
          const issueResolvedAt = filteredJiraData.map(d => d.issue_resolved_at);
          const stateTransitionTime = filteredJiraData.map(d => d.state_transition_time);

          if (isSanitizedArray(stateTransitionTime)) {
            resolution_time = `${convertToDays(max(stateTransitionTime))} days`;
          } else if (isSanitizedArray(issueCreatedAt) && isSanitizedArray(issueResolvedAt)) {
            resolution_time = `${convertToDays(max(issueResolvedAt) - min(issueCreatedAt))} days`;
          }
        }

        mappedRecords.push({
          ...record,
          jira_ids: jiraKeys,
          integration_ids: get(filters, ["filter", "integration_ids"], []),
          escalation_time: !escalation_time ? "NA" : `${convertToDays(escalation_time)} days`,
          resolution_time: !resolution_time ? "NA" : resolution_time
        });
      });

      restData = {
        data: {
          records: mappedRecords
        }
      };

      yield put(actionTypes.restapiClear(jiraCall.uri, jiraCall.method, jiraCall.id));
    }

    yield put(actionTypes.restapiData(restData?.data || {}, uri, method, id));
    yield put(actionTypes.restapiLoading(false, uri, method, id));
  } catch (e) {
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

export function* jiraZendeskSalesforceStagesWidgetsWatcher() {
  yield takeEvery([JIRA_ZENDESK_SALESFORCE_STAGES_WIDGETS], jiraZendeskSalesforceStagesWidgetsSaga);
}
