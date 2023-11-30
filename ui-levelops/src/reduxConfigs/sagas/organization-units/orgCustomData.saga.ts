import { get } from "lodash";
import { call, put, select, take, takeEvery } from "redux-saga/effects";
import { ORGANIZATION_CUSTOM_FIELDS_SAGA } from "reduxConfigs/actions/actionTypes";
import { restapiData, restapiLoading } from "reduxConfigs/actions/restapi";
import RestapiService from "services/restapiService";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { handleRestApiError } from "../restapiSaga";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";
import { getData } from "utils/loadingUtils";

export function* orgCustomDataEffectSaga(action: {
  type: string;
  integrationConfigUri: string;
  fieldListId: string;
  integConfigId: string;
  fieldListUri: string;
  integrations: string[];
}): any {
  const { integrations, fieldListUri, integrationConfigUri, fieldListId, integConfigId } = action;
  let restService: any = new RestapiService();
  let response;

  const jiraFields = fieldListUri;
  const application_field_list_uuid = fieldListId;
  yield put(restapiLoading(true, jiraFields, "list", application_field_list_uuid));
  try {
    const actionId = integConfigId;
    const integrationIds = integrations;
    const integConfig = integrationConfigUri;
    let hasNextPage = true;

    let data: any = { count: 0 };
    const configComplete: any = `COMPLETE_INTEG_CONFIG_FOR_JIRA_FIELDS_${integConfigId}`;

    yield put(
      actionTypes.genericList(
        integConfig,
        "list",
        { filter: { integration_ids: integrationIds } },
        configComplete,
        actionId,
        false
      )
    );

    yield take(configComplete);

    let apiState = yield select(restapiState);
    let count = 0;
    const integData = getData(apiState, integConfig, "list", actionId);
    while (hasNextPage) {
      const response = yield call(restService[jiraFields]["list"], {
        filter: { integration_ids: integrationIds, transformedCustomFieldData: true },
        page: count
      });
      data.count = data?.count + get(response, ["data", "count"], 0);
      data.records = [...(data?.records || []), ...(response?.data?.records || [])];
      data._metadata = response?.data?._metadata;
      hasNextPage = get(response, ["data", "_metadata", "has_next"], false);
      count = count + 1;
    }
    const fieldsData = data?.records
      ?.map((field: any) => {
        return {
          type: field?.field_type,
          name: field?.name,
          key: field?.field_key
        };
      })
      .filter((item: any) => item.name !== "Sprint");

    yield put(restapiData(fieldsData, jiraFields, "list", application_field_list_uuid));
    yield put(restapiLoading(false, jiraFields, "list", application_field_list_uuid));

    const aggFields = (integData.records || [])
      .reduce((agg: any, obj: any) => {
        const fields = get(obj, ["config", "agg_custom_fields"], []);
        agg.push(...fields);
        return agg;
      }, [])
      .filter((field: any) => !["customfield_", "Custom."].includes(field.key));
    const modifiedCustomField = aggFields.filter((item: any) => item.name !== "Sprint");
    yield put(restapiData(modifiedCustomField, integConfig, "list", actionId));
    yield put(restapiLoading(false, integConfig, "list", actionId));
    if (configComplete !== null) {
      yield put({ type: configComplete });
    }
  } catch (e) {
    yield call(handleRestApiError, e, action, response);
  }
}

export function* orgCustomDataWatcherSaga() {
  yield takeEvery([ORGANIZATION_CUSTOM_FIELDS_SAGA], orgCustomDataEffectSaga);
}
