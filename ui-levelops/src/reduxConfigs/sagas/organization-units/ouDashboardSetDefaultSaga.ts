import { cloneDeep, get, unset } from "lodash";
import { call, put, select, takeLatest } from "redux-saga/effects";
import { SET_OU_DEFAULT_DASHBOARD } from "reduxConfigs/actions/actionTypes";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";
import { restapiEffectSaga } from "../restapiSaga";
import { transformDraftOrgUnitData } from "configurations/pages/Organization/Helpers/OrgUnit.helper";
import { OrganizationUnitGet } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { OrganizationUnitService } from "services/restapi";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";

function* ouDashboardSetDefaultSaga(action: {
  type: string;
  ouId: string;
  defaultDashboardId: string;
}): Generator<Record<any, any>> {
  try {
    let restOrgUnitService = new OrganizationUnitService();
    const orgUnitToUpdate: any = yield call(restOrgUnitService.get, action.ouId, {});
    const data = get(orgUnitToUpdate, ["data"], undefined);
    const status = get(orgUnitToUpdate, ["status"], undefined);
    if (data && status === 200) {
      let nOrgUnitToUpdate = cloneDeep(data);
      nOrgUnitToUpdate.default_dashboard_id = action.defaultDashboardId;
      nOrgUnitToUpdate.id = action.ouId;
      unset(nOrgUnitToUpdate, "version");

      /** updating collection */
      yield call(restapiEffectSaga, {
        data: [nOrgUnitToUpdate],
        uri: "organization_unit_management",
        id: action.ouId,
        method: "update"
      });
      const restState = yield select(restapiState);
      const status: string = get(restState, ["organization_unit_management", "update", action.ouId, "data"], "false");

      if (status === "ok") {
        yield put(OrganizationUnitGet(action.ouId));
      }
    }
  } catch (e) {
    handleError({
      showNotfication: true,
      message: "Failed to set default dashboard",
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.ORG_UNITS,
        data: { e, action }
      }
    });
  }
}

export function* ouDashboardSetDefaultSagaWatcher() {
  yield takeLatest(SET_OU_DEFAULT_DASHBOARD, ouDashboardSetDefaultSaga);
}
