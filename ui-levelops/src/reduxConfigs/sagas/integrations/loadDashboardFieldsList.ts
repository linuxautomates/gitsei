import { IntegrationTypes } from "constants/IntegrationTypes";
import { filter, get, map } from "lodash";
import { all, call, put, select, takeLatest } from "redux-saga/effects";
import { LOAD_DASHBOARD_FIELDS_LIST } from "reduxConfigs/actions/actionTypes";
import { filterWidgetsCustomFields } from "reduxConfigs/actions/restapi";
import { BaseActionType } from "reduxConfigs/actions/restapi/action.type";
import { FIELD_LIST_ENTITY } from "reduxConfigs/actions/restapi/fields-list.action";
import RestapiService from "services/restapiService";
import { restapiClear, setSelectedEntity } from "../../actions/restapi";
import { selectedDashboardIntegrations } from "../../selectors/integrationSelector";

type FieldsListCallsType = {
  id: string;
  uri: string;
  filters: Record<string, any>;
  complete: string;
};

const getFieldsListCalls = (application: string, allIntegrations: any[]): FieldsListCallsType | undefined => {
  const selectedIntegrations = filter(allIntegrations, integration => integration.application === application);
  const integrationIds = map(selectedIntegrations, integration => integration.id);
  if (integrationIds.length) {
    const integrationKey = integrationIds.sort().join();
    let filters: any = { filter: { integration_ids: integrationIds } };
    let _uri = "jira_fields";
    let id = `${integrationKey}_${application}`;
    if (application === IntegrationTypes.AZURE) {
      _uri = "issue_management_workItem_Fields_list";
      filters = { filter: { integration_ids: integrationIds, transformedCustomFieldData: true } };
    }
    if (application === IntegrationTypes.ZENDESK) {
      _uri = "zendesk_fields";
    }
    if (application === IntegrationTypes.TESTRAILS) {
      _uri = "testrails_fields";
    }
    return {
      uri: _uri,
      filters,
      id,
      complete: `ON_FIELDS_LIST_LOADED_${integrationKey}_${application}`
    };
  }
  return;
};

const getPaginatedData = async (call: FieldsListCallsType) => {
  const restService: any = new RestapiService();
  let hasNextPage = true;
  let data: any = { count: 0 };
  let count = 0;
  const func = get(restService, [call?.uri || "", "list"]);
  while (hasNextPage) {
    const response = await func?.({ ...(call?.filters || {}), page: count });
    data.count = data?.count + get(response, ["data", "count"], 0);
    data.records = [...(data?.records || []), ...(response?.data?.records || [])];
    data._metadata = response?.data?._metadata;
    hasNextPage = get(response, ["data", "_metadata", "has_next"], false);
    count = count + 1;
  }
  return data.records;
};

function* loadDashboardFieldsListSaga(action: BaseActionType): any {
  try {
    const integrations = yield select(selectedDashboardIntegrations);
    if (integrations?.length) {
      const fieldListCalls: Array<FieldsListCallsType | undefined> = [
        getFieldsListCalls(IntegrationTypes.AZURE, integrations),
        getFieldsListCalls(IntegrationTypes.JIRA, integrations),
        getFieldsListCalls(IntegrationTypes.ZENDESK, integrations),
        getFieldsListCalls(IntegrationTypes.TESTRAILS, integrations)
      ].filter(call => !!call);

      const allData: Record<string, any[]> = {};

      // ** Had to do the calls this way because of the async function calls

      if (fieldListCalls[0]) {
        const id = fieldListCalls[0].id;
        const data = yield call(getPaginatedData, fieldListCalls[0]);
        allData[id] = data;
      }

      if (fieldListCalls[1]) {
        const id = fieldListCalls[1].id;
        const data = yield call(getPaginatedData, fieldListCalls[1]);
        allData[id] = data;
      }

      if (fieldListCalls[2]) {
        const id = fieldListCalls[2].id;
        const data = yield call(getPaginatedData, fieldListCalls[2]);
        allData[id] = data;
      }

      if (fieldListCalls[3]) {
        const id = fieldListCalls[3].id;
        const data = yield call(getPaginatedData, fieldListCalls[3]);
        allData[id] = data;
      }

      yield put(
        setSelectedEntity(FIELD_LIST_ENTITY, {
          data: allData
        })
      );

      yield all(map(fieldListCalls, call => put(restapiClear(call?.uri || "", "list", call?.id))));
      yield put(filterWidgetsCustomFields());
      return;
    }
    throw new Error("Integrations not present");
  } catch (e) {
    console.error("Failed to load fields list", e);
  }
}

export function* loadDashboardFieldsListWatcher() {
  yield takeLatest(LOAD_DASHBOARD_FIELDS_LIST, loadDashboardFieldsListSaga);
}
