import { call, select, takeLatest, put } from "redux-saga/effects";
import { get } from "lodash";
import { LOAD_DASHBOARD } from "reduxConfigs/actions/actionTypes";
import { restapiEffectSaga } from "../restapiSaga";
import { _dashboardsGetSelector } from "../../selectors/dashboardSelector";
import { restapiData, setSelectedChildId, setSelectedEntity } from "../../actions/restapi/restapiActions";
import { loadSelectedDashboardIntegrations } from "../../actions/restapi";
import { OrganizationUnitService } from "services/restapi/OrganizationUnit.services";
import queryString from "query-string";
import { getParamOuArray } from "helper/openReport.helper";

export function* loadDashboardSaga(action: any): any {
  const { id } = action;

  // THIS WILL USE IN OPEN REPORT
  const queryParamOUArray = getParamOuArray(window.location.href);

  const OU: string =
    action?.OU ??
    (queryString.parseUrl(window.location.href, { parseFragmentIdentifier: true })?.query?.OU as string) ??
    queryParamOUArray?.[0];

  try {
    let restOrgUnitService = new OrganizationUnitService();
    if (OU) {
      const key = `${OU}_integrations`;
      const response: any = yield call(restOrgUnitService.get, OU, {});
      yield put(setSelectedChildId(response?.data, "selected-OU"));
      yield put(restapiData(response?.data, "organization_unit_management", "get", key));
    }

    yield call(restapiEffectSaga, { uri: "dashboards", method: "get", id, OU: OU });
    const dashboards = yield select(_dashboardsGetSelector);
    const dashboard = get(dashboards, [id, "data"], {});
    yield put(setSelectedEntity("selected-dashboard", dashboard));

    // load dashboard integrations.
    yield put(loadSelectedDashboardIntegrations());
  } catch (e) {
    console.error("Failed to load dashboard", e);
  }
}

export function* loadDashboardSagaWatcher() {
  yield takeLatest(LOAD_DASHBOARD, loadDashboardSaga);
}
