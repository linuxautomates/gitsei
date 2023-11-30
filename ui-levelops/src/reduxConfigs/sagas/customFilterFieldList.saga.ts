import {
  JIRA_INTEGRATION_CUSTOM_FIELDS,
  AZURE_INTEGRATION_CUSTOM_FIELDS,
  ZENDESK_INTEGRATION_CUSTOM_FIELDS,
  TESTRAILS_INTEGRATION_CUSTOM_FIELDS
} from "../actions/actionTypes";
import { takeEvery, call, select, put, take } from "redux-saga/effects";
import { handleRestApiError } from "./restapiSaga";
import { restapiState } from "../selectors/restapiSelector";
import { getData } from "../../utils/loadingUtils";
import { get, uniq, cloneDeep } from "lodash";
import { restapiData, restapiLoading } from "../actions/restapi/restapiActions";
import * as actionTypes from "reduxConfigs/actions/restapi";
import RestapiService from "services/restapiService";
import { handleError } from "helper/errorReporting.helper";
import { issueContextTypes, severityTypes } from "bugsnag";
import { AZURE_CUSTOM_FIELD_PREFIX, TESTRAILS_CUSTOM_FIELD_PREFIX } from "dashboard/constants/constants";

export function* jiraFiltersFieldsListEffectSaga(action: {
  type: string;
  data: any;
  uri: string;
  method: string;
  complete: string;
  id: any;
}): any {
  let restService: any = new RestapiService();
  let response;

  const jiraFields = "jira_fields";
  const application_field_list_uuid = "jira_application_field_list";
  yield put(restapiLoading(true, action.uri, action.method, action.id));
  yield put(restapiLoading(true, jiraFields, "list", application_field_list_uuid));
  try {
    const actionId = action.id;
    const integrationIds = get(action, ["data", "integration_ids"], []);
    let hasNextPage = true;
    const integConfig = "jira_integration_config";

    let data: any = { count: 0 };
    const configComplete = `COMPLETE_INTEG_CONFIG_FOR_JIRA_FIELDS_${actionId}`;

    // @ts-ignore
    yield put(actionTypes.genericList(integConfig, "list", { filter: action.data }, configComplete, actionId, false));

    yield take(configComplete);

    let apiState = yield select(restapiState);
    let count = 0;
    const integData = getData(apiState, integConfig, "list", actionId);
    const configRecords = get(integData, ["records"], []);
    while (hasNextPage) {
      response = yield call(restService[jiraFields]["list"], {
        filter: { integration_ids: integrationIds },
        page: count
      });
      data.count = data?.count + get(response, ["data", "count"], 0);
      data.records = [...(data?.records || []), ...(response?.data?.records || [])];
      data._metadata = response?.data?._metadata;
      hasNextPage = get(response, ["data", "_metadata", "has_next"], false);
      count = count + 1;
    }
    yield put(restapiData(data, jiraFields, "list", application_field_list_uuid));
    yield put(restapiLoading(false, jiraFields, "list", application_field_list_uuid));
    const supportedCustomFields: string[] = [];

    configRecords.forEach((item: any) => {
      item?.config?.agg_custom_fields?.forEach((_item: any) => supportedCustomFields.push(_item.key));
    });

    const finalSupportedKeys = uniq(supportedCustomFields);

    const records = get(data, "records", []).filter((item: any) => finalSupportedKeys.includes(item.field_key));

    yield put(restapiData(records, action.uri, action.method, actionId));
    yield put(restapiLoading(false, action.uri, action.method, actionId));
    if (action.complete !== null) {
      yield put({ type: action.complete });
    }
  } catch (e) {
    yield call(handleRestApiError, e, action, response);
  }
}

export function* azureFilterFieldListEffectSaga(action: {
  type: string;
  data: any;
  uri: string;
  method: string;
  complete: string;
  id: any;
}): any {
  yield put(restapiLoading(true, action.uri, action.method, action.id));

  let restService: any = new RestapiService();
  const azureFields = "issue_management_workItem_Fields_list";
  const application_field_list_uuid = "azure_devops_application_field_list";
  yield put(restapiLoading(true, azureFields, "list", application_field_list_uuid));
  try {
    const integrationIds = get(action, ["data", "integration_ids"], []);
    const integConfig = "jira_integration_config";
    let data: any = { count: 0 };
    const configComplete = `COMPLETE_INTEG_CONFIG_FOR_AZURE_FIELDS_${action.id}`;
    let hasNextPage = true;
    let count = 0;

    // @ts-ignore
    yield put(actionTypes.genericList(integConfig, "list", { filter: action.data }, configComplete, action.id, false));

    yield take(configComplete);

    while (hasNextPage) {
      let response = yield call(restService[azureFields]["list"], {
        filter: { integration_ids: integrationIds, transformedCustomFieldData: true },
        page: count
      });
      data.count = data?.count + get(response, ["data", "count"], 0);
      data.records = [...(data?.records || []), ...(response?.data?.records || [])];
      data._metadata = response?.data?._metadata;
      hasNextPage = get(response, ["data", "_metadata", "has_next"], false);
      count = count + 1;
    }
    yield put(restapiData(data, azureFields, "list", application_field_list_uuid));
    yield put(restapiLoading(false, azureFields, "list", application_field_list_uuid));
    let apiState = yield select(restapiState);
    const integData = getData(apiState, integConfig, "list", action.id);

    const integRecords = integData.records || [];

    const supportedCustomFields: string[] = [];

    integRecords.forEach((item: any) => {
      const fields = get(item, ["config"], {});
      fields?.agg_custom_fields?.forEach((field: { key: string; name: string }) => {
        let fieldKey = field.key;
        if (!fieldKey.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
          fieldKey = `${AZURE_CUSTOM_FIELD_PREFIX}${fieldKey}`;
        }
        supportedCustomFields.push(fieldKey);
      });
    }, []);

    const customFieldsRecords = cloneDeep(data.records || []);
    const records = customFieldsRecords.filter((record: any) => supportedCustomFields.includes(record.field_key));

    yield put(restapiData(records, action.uri, action.method, action.id));
    yield put(restapiLoading(false, action.uri, action.method, action.id));
    if (action.complete !== null) {
      yield put({ type: action.complete });
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

export function* zendeskFilterFieldListEffectSaga(action: {
  type: string;
  data: any;
  uri: string;
  method: string;
  complete: string;
  id: any;
}): any {
  yield put(restapiLoading(true, action.uri, action.method, action.id));
  let restService: any = new RestapiService();
  let response;

  const zendeskFields = "zendesk_fields";
  const application_field_list_uuid = "zendesk_application_field_list";
  try {
    yield put(restapiLoading(true, zendeskFields, "list", application_field_list_uuid));
    const integConfig = "jira_integration_config";
    const integrationIds = get(action, ["data", "integration_ids"], []);
    const configComplete = `COMPLETE_INTEG_CONFIG_FOR_ZENDESK_FIELDS_${action.id}`;

    let hasNextPage = true;
    let count = 0;
    let data: any = { count: 0 };

    // @ts-ignore
    yield put(actionTypes.genericList(integConfig, "list", { filter: action.data }, configComplete, action.id, false));

    yield take(configComplete);

    while (hasNextPage) {
      response = yield call(restService[zendeskFields]["list"], {
        filter: { integration_ids: integrationIds },
        page: count
      });
      data.count = data?.count + get(response, ["data", "count"], 0);
      data.records = [...(data?.records || []), ...(response?.data?.records || [])];
      data._metadata = response?.data?._metadata;
      hasNextPage = get(response, ["data", "_metadata", "has_next"], false);
      count = count + 1;
    }

    yield put(restapiData(data, zendeskFields, "list", application_field_list_uuid));
    yield put(restapiLoading(false, zendeskFields, "list", application_field_list_uuid));

    let apiState = yield select(restapiState);

    const integData = getData(apiState, integConfig, "list", action.id);

    const integRecords = integData.records || [];

    const supportedCustomFields: string[] = [];

    integRecords.forEach((item: any) => {
      const fields = get(item, ["config"], {});
      fields?.agg_custom_fields?.forEach((field: { key: string; name: string }) =>
        supportedCustomFields.push(field.key)
      );
    }, []);

    const fieldsList = getData(apiState, zendeskFields, "list", application_field_list_uuid);
    const customFieldsRecords = cloneDeep(fieldsList.records || []).map((item: any) => {
      return {
        ...item,
        field_key: item.field_id.toString()
      };
    });

    const records = customFieldsRecords.filter((record: any) => supportedCustomFields.includes(record.field_key));

    yield put(restapiData(records, action.uri, action.method, action.id));
    yield put(restapiLoading(false, action.uri, action.method, action.id));
    if (action.complete !== null) {
      yield put({ type: action.complete });
    }
  } catch (e) {
    yield call(handleRestApiError, e, action, response);
  }
}

export function* testrailsFilterFieldListEffectSaga(action: {
  type: string;
  data: any;
  uri: string;
  method: string;
  complete: string;
  id: any;
}): any {
  yield put(restapiLoading(true, action.uri, action.method, action.id));
  let restService: any = new RestapiService();
  const testrailsFields = "testrails_fields";
  const application_field_list_uuid = "testrails_application_field_list";
  yield put(restapiLoading(true, testrailsFields, "list", application_field_list_uuid));
  try {
    const integrationIds = get(action, ["data", "integration_ids"], []);
    const integConfig = "jira_integration_config";
    let data: any = { count: 0 };
    const configComplete = `COMPLETE_INTEG_CONFIG_FOR_TESTRAILS_FIELDS_${action.id}`;
    let hasNextPage = true;
    let count = 0;

    // @ts-ignore
    yield put(actionTypes.genericList(integConfig, "list", { filter: action.data }, configComplete, action.id, false));

    yield take(configComplete);

    while (hasNextPage) {
      let response = yield call(restService[testrailsFields]["list"], {
        filter: { integration_ids: integrationIds, transformedCustomFieldData: true },
        page: count
      });

      data.count = data?.count + get(response, ["data", "count"], 0);
      data.records = [...(data?.records || []), ...(response?.data?.records || [])];
      data._metadata = response?.data?._metadata;
      hasNextPage = get(response, ["data", "_metadata", "has_next"], false);
      count = count + 1;
    }

    yield put(restapiData(data, testrailsFields, "list", application_field_list_uuid));
    yield put(restapiLoading(false, testrailsFields, "list", application_field_list_uuid));

    let apiState = yield select(restapiState);
    const integData = getData(apiState, integConfig, "list", action.id);
    const integRecords = integData.records || [];
    const supportedCustomFields: string[] = [];

    integRecords.forEach((item: any) => {
      const fields = get(item, ["config"], {});
      fields?.agg_custom_fields?.forEach((field: { key: string; name: string }) => {
        let fieldKey = field.key;
        if (!fieldKey.includes(TESTRAILS_CUSTOM_FIELD_PREFIX)) {
          fieldKey = `${TESTRAILS_CUSTOM_FIELD_PREFIX}${fieldKey}`;
        }
        supportedCustomFields.push(fieldKey);
      });
    }, []);

    const customFieldsRecords = cloneDeep(data.records || []);
    const records = customFieldsRecords.filter((record: any) => supportedCustomFields.includes(record.field_key));

    yield put(restapiData(records, action.uri, action.method, action.id));
    yield put(restapiLoading(false, action.uri, action.method, action.id));

    if (action.complete !== null) {
      yield put({ type: action.complete });
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

export function* jiraCustomFilterFieldListWatcherSaga() {
  yield takeEvery([JIRA_INTEGRATION_CUSTOM_FIELDS], jiraFiltersFieldsListEffectSaga);
}

export function* azureCustomFieldListWatcherSaga() {
  yield takeEvery([AZURE_INTEGRATION_CUSTOM_FIELDS], azureFilterFieldListEffectSaga);
}

export function* zendeskCustomFieldListWatcherSaga() {
  yield takeEvery([ZENDESK_INTEGRATION_CUSTOM_FIELDS], zendeskFilterFieldListEffectSaga);
}

export function* testrailsCustomFieldListWatcherSaga() {
  yield takeEvery([TESTRAILS_INTEGRATION_CUSTOM_FIELDS], testrailsFilterFieldListEffectSaga);
}
