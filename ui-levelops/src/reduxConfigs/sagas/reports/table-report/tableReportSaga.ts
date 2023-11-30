import { call, put, select, takeEvery } from "@redux-saga/core/effects";
import { severityTypes, issueContextTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { get } from "lodash";
import { TABLE_REPORT_GET_DATA } from "reduxConfigs/actions/actionTypes";
import {
  genericRestAPIError,
  genericRestAPILoading,
  genericRestAPISet
} from "reduxConfigs/actions/restapi/genericSet.action";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";
import { ConfigTablesService, OrganizationUnitService } from "services/restapi";

function* tableReportRestApiSaga(action: any): any {
  const { tableId, ou_id } = action;
  const tableService = new ConfigTablesService();
  const orgUnitService = new OrganizationUnitService();
  const tableURI = "config_tables";
  const orgUnitURI = "organization_unit_management";
  let restState = yield select(restapiState);
  try {
    const tableData = get(restState, [tableURI, "get", tableId, "data"], {});
    if (!Object.keys(tableData).length) {
      yield put(genericRestAPILoading(true, tableURI, "get", tableId));
      const response = yield call(tableService.get, tableId);
      yield put(genericRestAPISet(get(response, ["data"], {}), tableURI, "get", tableId));
      yield put(genericRestAPIError(false, tableURI, "get", tableId));
      yield put(genericRestAPILoading(false, tableURI, "get", tableId));
    }
    const orgUnitData = get(restState, [orgUnitURI, "list", ou_id, "data"], {});
    if (!Object.keys(orgUnitData).length) {
      yield put(genericRestAPILoading(true, orgUnitURI, "list", ou_id));
      const response = yield call(orgUnitService.list, { filter: { parent_ref_id: ou_id } });
      yield put(genericRestAPISet(get(response ?? {}, ["data", "records"], []), orgUnitURI, "list", ou_id));
      yield put(genericRestAPILoading(false, orgUnitURI, "list", ou_id));
      yield put(genericRestAPIError(false, orgUnitURI, "list", ou_id));
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

export function* tableReportRestApiSagaWatcher() {
  yield takeEvery(TABLE_REPORT_GET_DATA, tableReportRestApiSaga);
}
