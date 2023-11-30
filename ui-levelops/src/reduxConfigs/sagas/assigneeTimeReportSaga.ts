import { restapiData } from "../actions/restapi";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { put, select, take, takeEvery } from "redux-saga/effects";
import { issueContextTypes, notifyRestAPIError, severityTypes } from "bugsnag";
import { getData, getError } from "../../utils/loadingUtils";
import { ASSIGNEE_TIME_REPORT_FETCH } from "reduxConfigs/actions/actionTypes";
import {
  AssigneeTimeReportCompactResponse,
  AssigneeTimeReportResponse
} from "reduxConfigs/actions/restapi/response-types/jiraResponseTypes";
import { IntegrationDetailResponse } from "../actions/restapi/response-types/integrationResponseTypes";
import { handleError } from "helper/errorReporting.helper";

const restapiState = (state: { [state: string]: { [subState: string]: any } }) => state.restapiReducer;

export function* assigneeTimeReportEffectSaga(
  action: { uri: string; method: string; filters: any; id: string; complete?: string } | any
): any {
  try {
    const uri = action.uri;
    const method = action.method;
    const filters = action.filters;
    const id = "ASSIGNEE_TIME_REPORT_FETCH_LIST";
    const complete = `COMPLETE_${uri}_${method}_ASSIGNEE_TIME_REPORT_${id}`;

    // @ts-ignore
    yield put(actionTypes.genericList(uri, method, filters, complete, id));
    yield take(complete);

    const listState = yield select(restapiState);

    if (getError(listState, uri, method, id)) {
      console.log(`Ran into error for ${uri} ${id}`);
      return;
    }

    const listData = getData(listState, uri, method, id);
    const listRecords = listData.records || [];

    yield put(actionTypes.restapiClear("integrations", "list", id));

    if (listRecords.length > 0) {
      const bulkIds: string[] = [];
      const bulkId = action.id === "0" ? "bulk" : id;
      let integrationComplete = `ASSIGNEE_INTEGRATIONS_${uri}_${bulkId}`;
      listRecords.forEach((record: AssigneeTimeReportCompactResponse) => {
        if (!bulkIds.includes(record.integration_id)) {
          bulkIds.push(record.integration_id.toString());
        }
      });

      yield put(
        actionTypes.genericList(
          "integrations",
          "list",
          { filter: { integration_ids: bulkIds } },
          // @ts-ignore
          integrationComplete,
          bulkId,
          false
        )
      );
      yield take(integrationComplete);

      const newState = yield select(restapiState);

      const integrations = newState.integrations.list[bulkId].data.records;
      listRecords.forEach((record: AssigneeTimeReportResponse) => {
        const currentIntegration = integrations.filter(
          (integration: IntegrationDetailResponse) => record.integration_id === integration.id
        );
        record.integration_name = currentIntegration.length === 0 ? "" : currentIntegration?.[0]?.name;
        record.integration_url = currentIntegration.length === 0 ? "" : currentIntegration?.[0]?.url;
        record.integration_application = currentIntegration?.[0]?.application || "";
      });
      yield put(actionTypes.restapiClear("integrations", "list", bulkId));
    }

    yield put(restapiData(listData, uri, method, action.id));
    yield put(actionTypes.restapiLoading(false, uri, method, action.id));

    if (action.hasOwnProperty("complete") && action.complete !== null) {
      yield put({ type: action.complete });
    }
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });
  }
}

export function* assigneeTimeReportWatcherSaga() {
  yield takeEvery([ASSIGNEE_TIME_REPORT_FETCH], assigneeTimeReportEffectSaga);
}
