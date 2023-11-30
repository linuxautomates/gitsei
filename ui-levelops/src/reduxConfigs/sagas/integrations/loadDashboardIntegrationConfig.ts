import { put, select, take, takeLatest } from "redux-saga/effects";
import { get } from "lodash";
import { v1 as uuid } from "uuid";

import { LOAD_DASHBOARD_INTEGRATIONS_CONFIG } from "reduxConfigs/actions/actionTypes";
import { selectedDashboard } from "../../selectors/dashboardSelector";
import { _integrationListSelector } from "../../selectors/integrationSelector";
import { integrationConfigsList, setSelectedEntity } from "../../actions/restapi";
import { BaseActionType } from "reduxConfigs/actions/restapi/action.type";
import { _integrationConfigListSelector } from "reduxConfigs/selectors/integration-config.selector";

function* loadDashboardIntegrationsSaga(action: BaseActionType): any {
  const ON_INTEGRATIONS_CONFIGS_LOADED = "ON_INTEGRATIONS_CONFIGS_LOADED";
  try {
    const dashboard = yield select(selectedDashboard);

    const integrationIds = get(dashboard, ["query", "integration_ids"], []);

    const integrationKey = integrationIds.length ? integrationIds.sort().join("_") : uuid();

    yield put(
      integrationConfigsList(
        { filter: { integration_ids: integrationIds } },
        ON_INTEGRATIONS_CONFIGS_LOADED,
        integrationKey
      )
    );

    yield take(ON_INTEGRATIONS_CONFIGS_LOADED);

    const integrationsConfigListState = yield select(_integrationConfigListSelector);

    const integrations = get(integrationsConfigListState, [integrationKey, "data", "records"], undefined);

    // Waiting for new store.
    yield put(
      setSelectedEntity("selected-dashboard-integrations-config", {
        error: false,
        loading: false,
        loaded: true,
        records: integrations
      })
    );
  } catch (e) {
    console.error("Failed to load integrations", e);
  }
}

export function* loadDashboardIntegrationConfigWatcher() {
  yield takeLatest(LOAD_DASHBOARD_INTEGRATIONS_CONFIG, loadDashboardIntegrationsSaga);
}
