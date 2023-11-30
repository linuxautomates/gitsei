import { call, select, takeLatest, put, take } from "redux-saga/effects";
import { get } from "lodash";
import { UPDATE_SELECTED_DASHBOARD_DATA } from "reduxConfigs/actions/actionTypes";
import { selectedDashboard, _dashboardsGetSelector } from "../../selectors/dashboardSelector";
import { restapiData, restapiError, restapiLoading, setSelectedEntity } from "../../actions/restapi/restapiActions";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import {
  azureCustomFilterFieldsList,
  AZURE_CUSTOM_FIELDS_LIST,
  jiraCustomFilterFieldsList,
  JIRA_CUSTOM_FIELDS_LIST,
  loadSelectedDashboardIntegrationsConfig,
  zendeskCustomFilterFieldsList,
  ZENDESK_CUSTOM_FIELDS_LIST,
  TESTRAILS_CUSTOM_FIELDS_LIST,
  testrailsCustomFilterFieldsList
} from "reduxConfigs/actions/restapi";
import { cachedIntegrationEffectSaga } from "../integrations/cachedIntegrationSaga";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { Integration } from "model/entities/Integration";
import { IntegrationTypes } from "constants/IntegrationTypes";

export function* updateDashboardOUSaga(action: any): any {
  try {
    yield put(restapiLoading(true, action.uri, action.method, action.uuid));
    const dashboard = yield select(selectedDashboard);
    const newIntegrations = action.integrations;
    const integrationKey = newIntegrations.sort().join("_");

    yield call(cachedIntegrationEffectSaga as any, { payload: { method: "list", integrationIds: newIntegrations } });
    const integrations: Array<Integration> = yield select(cachedIntegrationsListSelector, {
      integration_ids: newIntegrations
    });
    const _dashboard = { ...dashboard, query: { ...dashboard?.query, integration_ids: newIntegrations } };
    yield put(setSelectedEntity("selected-dashboard", _dashboard));
    const applications = integrations.map((integration: any) => integration.application);
    //@ts-ignore
    const jiraFieldsSelector = yield select(getGenericRestAPISelector, {
      uri: JIRA_CUSTOM_FIELDS_LIST,
      method: "list",
      uuid: integrationKey || 0
    });
    const jiraCustomFieldsData = get(jiraFieldsSelector, "data", undefined);
    if (applications.includes(IntegrationTypes.JIRA) && !jiraCustomFieldsData) {
      yield put(
        jiraCustomFilterFieldsList(
          { integration_ids: newIntegrations },
          integrationKey,
          `${integrationKey}_jira_fields_complete_id` as any
        )
      );
      yield take(`${integrationKey}_jira_fields_complete_id`);
    }
    //@ts-ignore
    const azureFieldsSelector = yield select(getGenericRestAPISelector, {
      uri: AZURE_CUSTOM_FIELDS_LIST,
      method: "list",
      uuid: integrationKey || 0
    });
    const azureCustomFieldsData = get(azureFieldsSelector, "data", undefined);
    if (applications.includes(IntegrationTypes.AZURE) && !azureCustomFieldsData) {
      yield put(
        azureCustomFilterFieldsList(
          { integration_ids: newIntegrations },
          integrationKey,
          `${integrationKey}_azure_fields_complete_id` as any
        )
      );
      yield take(`${integrationKey}_azure_fields_complete_id`);
    }
    //@ts-ignore
    const zendeskFieldsSelector = yield select(getGenericRestAPISelector, {
      uri: ZENDESK_CUSTOM_FIELDS_LIST,
      method: "list",
      uuid: integrationKey || 0
    });
    const zendeskCustomFieldsData = get(zendeskFieldsSelector, "data", undefined);
    if (applications.includes(IntegrationTypes.ZENDESK) && !zendeskCustomFieldsData) {
      yield put(
        zendeskCustomFilterFieldsList(
          { integration_ids: newIntegrations },
          integrationKey,
          `${integrationKey}_zendesk_fields_complete_id` as any
        )
      );
      yield take(`${integrationKey}_zendesk_fields_complete_id`);
    }

    //@ts-ignore
    const testrailsFieldsSelector = yield select(getGenericRestAPISelector, {
      uri: TESTRAILS_CUSTOM_FIELDS_LIST,
      method: "list",
      uuid: integrationKey || 0
    });

    const testrailsCustomFieldsData = get(testrailsFieldsSelector, "data", undefined);
    if (applications.includes(IntegrationTypes.TESTRAILS) && !testrailsCustomFieldsData) {
      yield put(
        testrailsCustomFilterFieldsList(
          { integration_ids: newIntegrations },
          integrationKey,
          `${integrationKey}_testrails_fields_complete_id` as any
        )
      );
      yield take(`${integrationKey}_testrails_fields_complete_id`);
    }

    yield put(restapiLoading(false, action.uri, action.method, action.uuid));
    yield put(restapiData([], action.uri, action.method, action.uuid));

    yield put(
      setSelectedEntity("selected-dashboard-integrations", {
        error: false,
        loading: false,
        loaded: true,
        records: integrations || []
      })
    );
    yield put(loadSelectedDashboardIntegrationsConfig());
  } catch (e) {
    yield put(restapiError(e, action.uri, action.method, action.uuid));
    yield put(restapiData([], action.uri, action.method, action.uuid));
  }
}

export function* updateDashboardOUSagaWatcher() {
  yield takeLatest(UPDATE_SELECTED_DASHBOARD_DATA, updateDashboardOUSaga);
}
