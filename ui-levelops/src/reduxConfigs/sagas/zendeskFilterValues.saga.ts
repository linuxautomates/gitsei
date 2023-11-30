import { get, map } from "lodash";
import { call, put, select, takeEvery } from "redux-saga/effects";
import { ZENDESK_FILTER_VALUES } from "reduxConfigs/actions/actionTypes";
import { restapiData, restapiError, restapiLoading } from "reduxConfigs/actions/restapi";
import { getData, getError } from "utils/loadingUtils";

import { restapiEffectSaga } from "./restapiSaga";

const restapiState = (state: any) => state.restapiReducer;

function* zendeskFiltersEffectSaga(action: any): any {
  const actionId = action.id;
  const integrationIds = get(action, ["data", "integration_ids"], []);

  yield call(restapiEffectSaga, {
    data: action.data,
    id: action.id,
    uri: action.uri,
    method: action.method,
    set_loading: false
  });

  const fieldState = yield select(restapiState);
  if (getError(fieldState, action.uri, action.method, actionId)) {
    return;
  }

  let data = getData(fieldState, action.uri, action.method, actionId);
  let fieldRecords = data.records || [];

  const integConfig = "jira_integration_config"; // integration config API
  const customFilter = "zendesk_custom_filter_values";

  yield call(restapiEffectSaga, {
    data: { filter: action.data },
    id: action.id,
    uri: integConfig,
    method: "list",
    set_loading: false
  });

  const apiState = yield select(restapiState);

  if (getError(apiState, integConfig, "list", actionId)) {
    data.custom_fields = [];
    yield put(restapiData(data, action.uri, action.method, actionId));
    yield put(restapiLoading(false, action.uri, action.method, actionId));
    return;
  }

  const integData = getData(apiState, integConfig, "list", actionId);

  const aggFields = (integData.records || [])
    .reduce((agg: any[], obj: any) => {
      const fields = get(obj, ["config", "agg_custom_fields"], []);
      agg.push(...fields);
      return agg;
    }, [])
    .filter((field: any) => !field.key.includes("customfield_"));
  // need to change the logic ,
  // we need to call the integration config API
  //with only zendesk or jira integration ids

  const integFields = aggFields.map((record: any) => record.key);

  if (integFields.length === 0) {
    data.custom_fields = [];
    yield put(restapiData(data, action.uri, action.method, actionId));
    yield put(restapiLoading(false, action.uri, action.method, actionId));
    if (action.complete !== null) {
      yield put({ type: action.complete });
    }
    return;
  }

  const filter = {
    fields: integFields.filter((field: any, index: number) => integFields.indexOf(field) === index),
    integration_ids: integrationIds,
    filter: {
      integration_ids: integrationIds
    }
  };

  yield call(restapiEffectSaga, {
    data: filter,
    id: action.id,
    uri: customFilter,
    method: "list",
    set_loading: false
  });

  const customApiState = yield select(restapiState);
  if (getError(customApiState, customFilter, "list", actionId)) {
    data.custom_fields = [];
    yield put(restapiError(true, action.uri, action.method, actionId));
    yield put({ type: action.complete });
    yield put(restapiData(data, action.uri, action.method, actionId));
    yield put(restapiLoading(false, action.uri, action.method, actionId));
    return;
  }

  const customData = getData(customApiState, customFilter, "list", actionId);
  const mapCustomData = map(customData.records || [], cData => ({
    [`customfield_${Object.keys(cData)[0]}`]: cData[Object.keys(cData)[0]]
  }));
  fieldRecords.push(...(mapCustomData || []));
  data.records = fieldRecords;
  const customFields = aggFields.filter(
    (field: any, index: number) => aggFields.map((f: any) => f.key).indexOf(field.key) === index
  );
  data.custom_fields = map(customFields, cf => ({ ...(cf || {}), key: `customfield_${cf.key || ""}` }));
  data._metadata = {
    ...(data._metadata || {}),
    has_next: !!(data?._metadata?.has_next || customData?._metadata?.has_next)
  };

  yield put(restapiData(data, action.uri, action.method, actionId));
  yield put(restapiLoading(false, action.uri, action.method, actionId));
  if (action.complete !== null) {
    yield put({ type: action.complete });
  }
}

export function* zendeskFiltersWatcherSaga() {
  yield takeEvery([ZENDESK_FILTER_VALUES], zendeskFiltersEffectSaga);
}
