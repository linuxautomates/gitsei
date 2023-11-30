import { get } from "lodash";
import { put, select, take, takeEvery } from "redux-saga/effects";
import { INGESTION_INTEGRATION } from "reduxConfigs/actions/actionTypes";
import { restapiData } from "reduxConfigs/actions/restapi";
import { getIngestionIntegrationStatus } from "reduxConfigs/actions/restapi/ingestion.action";
import { getData } from "utils/loadingUtils";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { INTEGRATION_LIST_UUID } from "configurations/pages/integrations/integrations-list/constant";
import { handleError } from "helper/errorReporting.helper";
import { issueContextTypes, severityTypes } from "bugsnag";

const restapiState = (state: any) => state.restapiReducer;

export function* integrationIngestionSaga(action: any): any {
  let error = false;
  const URI = action.uri;
  const METHOD = action.method;
  const ID = action.id;

  try {
    if (action.id) {
      const state: any = yield select(restapiState);
      const existingData = get(state, [URI, METHOD, ID, "data"], {});
      if (Object.keys(existingData).length === 0) {
        const complete = `ingestion_integration_status_${ID}`;
        yield put(getIngestionIntegrationStatus(ID, complete));
        yield take(complete);
      }

      //@ts-ignore
      const lstate: any = yield select(restapiState);
      const listData = getData(lstate, "integrations", "list", INTEGRATION_LIST_UUID);
      const listRecords = listData.records || [];
      const status = get(lstate, [URI, METHOD, ID, "data", "status"]);
      const listRecordsIndex = listRecords.findIndex((element: any) => element.id === ID);
      const integration = listRecords[listRecordsIndex];
      listRecords[listRecordsIndex] = { ...integration, status, statusUpdated: true };
      listData["records"] = listRecords;
      yield put(restapiData(listData, "integrations", "list", INTEGRATION_LIST_UUID));
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

    error = true;
  } finally {
    yield put(actionTypes.restapiLoading(false, URI, METHOD, ID));
    yield put(actionTypes.restapiError(error, URI, METHOD, action.id));
  }
}

export function* integrationIngestionWatcherSaga() {
  yield takeEvery([INGESTION_INTEGRATION], integrationIngestionSaga);
}
