import { get, map, uniq, cloneDeep, unionBy } from "lodash";
import { call, put, select, takeEvery } from "redux-saga/effects";
import { AZURE_FILTER_VALUES } from "reduxConfigs/actions/actionTypes";
import { restapiData, restapiLoading, restapiError, restapiClear } from "reduxConfigs/actions/restapi";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";
import { getData } from "utils/loadingUtils";
import { restapiEffectSaga } from "./restapiSaga";
import { IssueManagementWorkItemFieldListService, RestIntegrationsService } from "services/restapi";
import { categoriesFilterValueInitialDataType } from "./saga-types/jiraFiltersSaga.types";
import { v1 as uuid } from "uuid";
import { getNormalizedFiltersData } from "./saga-helpers/azureSagas.helper";
import { AZURE_CUSTOM_FIELD_PREFIX } from "dashboard/constants/constants";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { IntegrationTransformedCFTypes } from "configurations/configuration-types/integration-edit.types";

export function* azureFiltersEffectSaga(action: {
  type: string;
  data: any;
  uri: string;
  method: string;
  complete: string;
  id: any;
}): any {
  const actionId = action.id;
  const initialId = `AZURE_SUPPORTED_FILTERS_${uuid()}`;
  const integrationService = new RestIntegrationsService();
  let integrationIds = get(action, ["data", "integration_ids"], []);
  let data: categoriesFilterValueInitialDataType = {
    integrationIds: [],
    custom_fields: [],
    custom_hygienes: [],
    records: []
  };

  try {
    // making a call to integration ids api to get integrationIds in case not provided
    if (!(integrationIds || []).length) {
      const integrationListState = yield call(integrationService.list, {
        filter: { applications: ["azure_devops"] },
        page_size: 10,
        page: 0
      });
      const integrationRecords: any[] = get(integrationListState, ["data", "records"], []);
      integrationIds = map(integrationRecords, integrationObj => integrationObj.id);
    }

    data.integrationIds = integrationIds;

    let integrationFieldListRecords: any[] = [];
    const integrationFieldListService = new IssueManagementWorkItemFieldListService();
    const response = yield call(integrationFieldListService.list, {
      filter: { integration_ids: integrationIds, transformedCustomFieldData: true }
    });

    integrationFieldListRecords = get(response, ["data", "records"], []);

    const _data = { ...action.data };
    _data.fields = _data.fields.map((field: string) => {
      if (field.includes("workitem_")) {
        if (field !== "workitem_type") {
          return field.replace("workitem_", "");
        }
        return field;
      }
      return field;
    });
    // azure supported filters call
    yield call(restapiEffectSaga, {
      uri: action.uri,
      method: action.method,
      id: initialId,
      data: _data
    });

    let apiState = yield select(restapiState);

    const fieldData = getData(apiState, action.uri, action.method, initialId);
    const records = fieldData.records.map((record: { [key: string]: any }) => {
      const key = Object.keys(record)[0];
      if (key !== "workitem_type") {
        return {
          [`workitem_${key}`]: record[key]
        };
      }
      return {
        [`${key}`]: record[key]
      };
    });

    let fieldRecords = getNormalizedFiltersData(records || []);
    data.records = fieldRecords;
    const integConfig = "jira_integration_config";
    const customFilter = "issue_management_custom_field_values";

    // api call for integration config
    yield call(restapiEffectSaga, {
      uri: integConfig,
      method: "list",
      id: actionId,
      data: { filter: action.data }
    });

    apiState = yield select(restapiState);

    const integData = getData(apiState, integConfig, "list", actionId);

    const integRecords = cloneDeep(integData.records || []);
    const aggFields = integRecords
      .reduce((agg: any, obj: any) => {
        const fields = get(obj, ["config", "agg_custom_fields"], []);
        return [...agg, ...fields];
      }, [])
      .map((field: any) => {
        const key = field.key.includes(AZURE_CUSTOM_FIELD_PREFIX)
          ? field.key.replace(AZURE_CUSTOM_FIELD_PREFIX, "")
          : field.key;
        let fieldKeyConfig: any = { key: field.key };
        if (!field.key?.includes(AZURE_CUSTOM_FIELD_PREFIX)) {
          fieldKeyConfig = {
            key: `${AZURE_CUSTOM_FIELD_PREFIX}${field.key}`,
            metadata: { transformed: AZURE_CUSTOM_FIELD_PREFIX }
          };
        }

        const fieldConfig: IntegrationTransformedCFTypes | undefined = integrationFieldListRecords.find(
          (rec: IntegrationTransformedCFTypes) => rec?.field_key === fieldKeyConfig?.key
        );

        if (fieldConfig) {
          fieldKeyConfig = {
            ...(fieldKeyConfig ?? {}),
            field_type: fieldConfig?.field_type
          };
        }

        return {
          ...field,
          name: field.name || key,
          ...fieldKeyConfig
        };
      });

    data.custom_hygienes = (integData.records || []).reduce((agg: any, obj: any) => {
      const cHygienes = get(obj, ["custom_hygienes"], []);
      agg.push(...cHygienes);
      return agg;
    }, []);
    let integFields = aggFields.map((record: { key: string; metadata?: { transformed: string } }) =>
      record?.metadata ? record?.key?.replace(record?.metadata?.transformed, "") : record?.key
    );
    integFields = uniq(integFields);

    if (integFields.length === 0) {
      return;
    }
    const filter = {
      fields: integFields.filter((field: any, index: number) => integFields.indexOf(field) === index),
      integration_ids: integrationIds,
      filter: {
        integration_ids: integrationIds
      }
    };

    // custom field api calls
    yield call(restapiEffectSaga, {
      uri: customFilter,
      method: "list",
      id: actionId,
      data: filter
    });

    apiState = yield select(restapiState);
    const customData = getData(apiState, customFilter, "list", actionId);
    const customRecords = getNormalizedFiltersData(customData.records || []).map(record => {
      const customFieldOptions = (Object.values(record)[0] || []).filter((option: any) => !!option?.key);
      let key = Object.keys(record)[0];
      const transformedKeyObject = (aggFields ?? []).find((field: any) => {
        if (field?.metadata && field?.metadata?.transformed) {
          const fieldKey = field?.key?.replace(field?.metadata?.transformed, "");
          if (fieldKey === key) {
            return true;
          }
        }
        return false;
      });
      if (transformedKeyObject) {
        key = transformedKeyObject?.key;
      }
      return {
        [key]: customFieldOptions
      };
    });

    fieldRecords.push(...customRecords);
    data.records = fieldRecords;
    data.custom_fields = unionBy(aggFields, "key");

    data._metadata = {
      ...(data._metadata || {}),
      has_next: !!(data?._metadata?.has_next || customData?._metadata?.has_next)
    };
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });
    yield put(restapiError(true, action.uri, action.method, actionId));
  } finally {
    yield put(restapiData(data, action.uri, action.method, actionId));
    yield put(restapiLoading(false, action.uri, action.method, actionId));
    yield put(restapiClear(action.uri, "list", initialId));
    if (action.complete !== null) {
      yield put({ type: action.complete });
    }
  }
}

export function* azureFilterValueWatcherSaga() {
  yield takeEvery([AZURE_FILTER_VALUES], azureFiltersEffectSaga);
}
