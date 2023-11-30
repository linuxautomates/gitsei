import { get } from "lodash";
import { all, call, put, select, takeEvery } from "redux-saga/effects";
import { INTEGRATION_MONITORING_ACTION } from "reduxConfigs/actions/actionTypes";
import { restapiData } from "reduxConfigs/actions/restapi";
import { getData } from "utils/loadingUtils";
import { restapiEffectSaga } from "./restapiSaga";
import { MONITORED_INTEGRATION_LIST_UUID } from "dashboard/components/dashboard-view-page-secondary-header/integration-monitoring/constant";
import moment from "moment";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";

function* integrationMonitoringEffectSaga(action: any): any {
  const ID = action.integration_id;
  const statusURI = "ingestion_integration_status";
  const logsURI = "ingestion_integration_logs";
  try {
    if (ID) {
      let restState: any = yield select(restapiState);
      const existingData = get(restState, [statusURI, "get", ID, "data"], {});
      const existingLogsData = get(restState, [logsURI, "list", ID, "data"], {});
      const apiCalls: any = [];
      if (Object.keys(existingData).length === 0) {
        apiCalls.push({
          uri: statusURI,
          id: ID,
          method: "get",
          data: {}
        });
      }

      if (Object.keys(existingLogsData).length === 0) {
        apiCalls.push({
          uri: logsURI,
          id: ID,
          method: "list",
          data: { filter: { id: ID } }
        });
      }

      yield all(apiCalls.map((apiCall: any) => call(restapiEffectSaga, apiCall)));

      restState = yield select(restapiState);
      const listData = getData(restState, "integrations", "list", MONITORED_INTEGRATION_LIST_UUID);
      const listRecords = listData?.records || [];
      const status = get(restState, [statusURI, "get", ID, "data", "status"]);
      const logsRecords: Array<any> = get(restState, [logsURI, "list", ID, "data", "records"], []);
      let last_ingested_at = "";
      if (logsRecords.length) {
        const latestIngetionRecord = logsRecords[0];
        const lastIngestionTimeStamp = get(latestIngetionRecord, ["to"]);
        if (lastIngestionTimeStamp) {
          last_ingested_at = moment.unix(lastIngestionTimeStamp).fromNow();
        } else {
          last_ingested_at = "NA";
        }
      } else {
        last_ingested_at = "NA";
      }
      const listRecordsIndex = listRecords.findIndex((element: any) => element.id === ID);
      const integration = listRecords[listRecordsIndex];
      listRecords[listRecordsIndex] = { ...integration, status, statusUpdated: true, last_ingested_at };
      listData["records"] = listRecords;
      yield put(restapiData(listData, "integrations", "list", MONITORED_INTEGRATION_LIST_UUID));
    }
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.INTEGRATIONS,
        data: { e, action }
      }
    });
  }
}

export function* integrationMonitoringWatcherSaga() {
  yield takeEvery([INTEGRATION_MONITORING_ACTION], integrationMonitoringEffectSaga);
}
