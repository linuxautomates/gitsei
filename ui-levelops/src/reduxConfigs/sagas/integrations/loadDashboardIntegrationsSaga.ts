import { call, put, select, take, takeLatest } from "redux-saga/effects";
import { get } from "lodash";

import { LOAD_DASHBOARD_INTEGRATIONS } from "reduxConfigs/actions/actionTypes";
import { selectedDashboard } from "../../selectors/dashboardSelector";
import { _integrationListSelector } from "../../selectors/integrationSelector";
import { loadSelectedDashboardIntegrationsConfig, setSelectedEntity } from "../../actions/restapi";
import { loadSelectedDashboardFieldsList } from "reduxConfigs/actions/restapi/fields-list.action";
import { cachedIntegrationEffectSaga } from "./cachedIntegrationSaga";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { Integration } from "model/entities/Integration";

function* loadDashboardIntegrationsSaga(action: { type: string }): any {
  try {
    const dashboard = yield select(selectedDashboard);

    const integrationIds = get(dashboard, ["query", "integration_ids"], []);

    yield call(cachedIntegrationEffectSaga as any, { payload: { method: "list", integrationIds: integrationIds } });
    const integrations: Array<Integration> = yield select(cachedIntegrationsListSelector, {
      integration_ids: integrationIds
    });

    // Waiting for new store.
    yield put(
      setSelectedEntity("selected-dashboard-integrations", {
        error: false,
        loading: false,
        loaded: true,
        records: integrations ?? []
      })
    );
    yield put(loadSelectedDashboardIntegrationsConfig());
    yield put(loadSelectedDashboardFieldsList());
  } catch (e) {
    console.error("Failed to load integrations", e);
  }
}

export function* loadDashboardIntegrationWatcher() {
  yield takeLatest(LOAD_DASHBOARD_INTEGRATIONS, loadDashboardIntegrationsSaga);
}
