import { select, takeLatest } from "@redux-saga/core/effects";
import { issueContextTypes, severityTypes } from "bugsnag";
import { AZURE_CUSTOM_FIELD_PREFIX, CUSTOM_FIELD_PREFIX } from "dashboard/constants/constants";
import {
  issueManagementStatusSupportedFilters,
  jiraSupportedStatusFilter
} from "dashboard/constants/supported-filters.constant";
import { handleError } from "helper/errorReporting.helper";
import { Dictionary, find, forEach, get, map, uniq, uniqBy } from "lodash";
import { all, call, put } from "redux-saga/effects";
import {
  AZURE_CATEGORIES_FILTER_VALUES_WITH_STATUS,
  CATEGORIES_FILTERS_VALUES_WITH_STATUS
} from "reduxConfigs/actions/actionTypes";
import { restapiClear } from "reduxConfigs/actions/restapi";
import { setCategoriesFiltersValuesData } from "reduxConfigs/actions/restapi/ticketCategorizationSchemes.action";
import { RestIntegrationsService } from "services/restapi";
import { toTitleCase } from "utils/stringUtils";
import { v1 as uuid } from "uuid";
import { restapiEffectSaga } from "../restapiSaga";
import { getNormalizedFiltersData } from "../saga-helpers/azureSagas.helper";
import {
  categoriesFilterValueInitialDataType,
  customFiltersOptionsType,
  filterValueApiConfigType,
  jiraFiltersSagaFilterType
} from "../saga-types/jiraFiltersSaga.types";

const restapiState = (state: any) => state.restapiReducer;
const jiraFilterValues = "jira_filter_values";
const azureIntegrationConfig = "issue_management_workItem_Fields_list";
const azureFilterValues = "issue_management_workitem_values";
const azureCustomFilter = "issue_management_custom_field_values";

const transformCategoriesFiltersData = (
  data: categoriesFilterValueInitialDataType,
  supportedFiltersValues = jiraSupportedStatusFilter.values
) => {
  let supportedFiltersRecords = (data?.records || []).filter((record: any) => {
    const filterKey = Object.keys(record)[0];
    return (
      supportedFiltersValues.includes(filterKey) || filterKey.includes("customfield_") || filterKey.includes("Custom")
    );
  });
  const cf = data?.custom_fields || [];
  const cHygienes = data?.custom_hygienes || [];
  if (supportedFiltersRecords && supportedFiltersRecords.length > 0) {
    supportedFiltersRecords = [...supportedFiltersRecords, { custom_hygienes: cHygienes }];
    if (cf) {
      supportedFiltersRecords = [...supportedFiltersRecords, { custom_fields: cf }];
    }
  }
  return supportedFiltersRecords;
};

function* CategoriesFiltersValueTrellisProfileSaga(action: { type: string; integrationIds: string[] }): any {
  const integrationService = new RestIntegrationsService();
  const UNIQUE_FACTOR = uuid();

  const JIRA_FILTER_VALUES_ID = `JIRA_FILTER_VALUES_ID_${UNIQUE_FACTOR}`;
  let supportedFiltersData: any[] = [];
  let data: categoriesFilterValueInitialDataType = {
    integrationIds: [],
    custom_fields: [],
    custom_hygienes: [],
    records: []
  };

  let apiToClear: {
    uri: string;
    method: string;
    id: string;
  }[] = [];

  try {
    yield put(setCategoriesFiltersValuesData({ loading: true }));
    let integrationIds = action.integrationIds;
    if (!(integrationIds || []).length) {
      const integrationListState = yield call(integrationService.list, {
        filter: { applications: ["jira"] },
        page_size: 10,
        page: 0
      });
      const integrationRecords: any[] = get(integrationListState, ["data", "records"], []);
      integrationIds = map(integrationRecords, integrationObj => integrationObj.id);
    }

    data.integrationIds = integrationIds;

    const apiFilters = {
      fields: jiraSupportedStatusFilter.values,
      filter: {
        integration_ids: integrationIds
      },
      integration_ids: integrationIds
    };

    let apiCalls: {
      uri: string;
      method: string;
      id: string;
      filters: jiraFiltersSagaFilterType | { filter: jiraFiltersSagaFilterType };
    }[] = [{ uri: jiraFilterValues, method: "list", id: JIRA_FILTER_VALUES_ID, filters: apiFilters }];

    yield all(
      apiCalls.map(calls =>
        call(restapiEffectSaga, {
          uri: calls.uri,
          method: calls.method,
          id: calls.id,
          data: calls.filters
        })
      )
    );

    let customFieldValuesCalls: {
      uri: string;
      method: string;
      id: string;
      filters: jiraFiltersSagaFilterType | { filter: jiraFiltersSagaFilterType };
    }[] = [];

    let restState = yield select(restapiState);
    forEach(apiCalls, calls => {
      switch (calls.uri) {
        case jiraFilterValues:
          const filterValuesData = get(restState, [jiraFilterValues, calls.method, calls.id, "data", "records"], []);
          supportedFiltersData = [...filterValuesData];
          break;
      }
    });

    if (customFieldValuesCalls.length) {
      yield all(
        customFieldValuesCalls.map(calls =>
          call(restapiEffectSaga, {
            uri: calls.uri,
            method: calls.method,
            id: calls.id,
            data: calls.filters
          })
        )
      );

      restState = yield select(restapiState);

      let customFiltersData: customFiltersOptionsType[] = [];
      forEach(customFieldValuesCalls, calls => {
        const customRecords: customFiltersOptionsType[] = get(
          restState,
          [calls.uri, calls.method, calls.id, "data", "records"],
          []
        );
        customFiltersData = [...customFiltersData, ...customRecords];
      });

      supportedFiltersData.push(...customFiltersData);
    }
    data.records = supportedFiltersData;
    const updatedSupportedFilterRecords = transformCategoriesFiltersData(data);
    yield put(
      setCategoriesFiltersValuesData({
        payload: { integrationIds: data.integrationIds, data: updatedSupportedFilterRecords },
        loading: false,
        error: false
      })
    );
    apiToClear.push(...[...apiCalls, ...customFieldValuesCalls]);
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });
    yield put(setCategoriesFiltersValuesData({ error: !!(e as any)?.message, loading: false }));
  } finally {
    yield all(apiToClear.map(calls => put(restapiClear(calls.uri, calls.method, calls.id))));
  }
}

function* AzureCategoriesFiltersValueTrellisProfileSaga(action: { type: string; integrationIds: string[] }): any {
  const integrationService = new RestIntegrationsService();
  const UNIQUE_FACTOR = uuid();

  const AZURE_FILTER_VALUES_ID = `AZURE_FILTER_VALUES_ID_${UNIQUE_FACTOR}`;
  const INTEGRATION_CONFIG_ID = `INTEGRATION_CONFIG_ID_${UNIQUE_FACTOR}`;

  let supportedFiltersData: any[] = [];
  let data: categoriesFilterValueInitialDataType = {
    integrationIds: [],
    custom_fields: [],
    custom_hygienes: [],
    records: []
  };

  let apiToClear: {
    uri: string;
    method: string;
    id: string;
  }[] = [];

  const azureSupportedFilters = issueManagementStatusSupportedFilters.values.map((field: string) => {
    if (field.includes("workitem_")) {
      return field.replace("workitem_", "");
    }
    return field;
  });

  try {
    yield put(setCategoriesFiltersValuesData({ loading: true }));
    let integrationIds = action.integrationIds;

    if (!(integrationIds || []).length) {
      const integrationListState = yield call(integrationService.list, {
        filter: { applications: ["azure_devops"] },
        page_size: 100,
        page: 0
      });
      const integrationRecords: any[] = get(integrationListState, ["data", "records"], []);
      integrationIds = map(integrationRecords, integrationObj => integrationObj.id);
    }

    data.integrationIds = integrationIds;

    const apiFilters = {
      fields: azureSupportedFilters,
      filter: {
        integration_ids: integrationIds
      },
      integration_ids: integrationIds
    };

    let apiCalls: filterValueApiConfigType[] = [
      { uri: azureFilterValues, method: "list", id: AZURE_FILTER_VALUES_ID, filters: apiFilters },
      { uri: azureIntegrationConfig, method: "list", id: INTEGRATION_CONFIG_ID, filters: { filter: apiFilters } }
    ];

    yield all(
      apiCalls.map(calls =>
        call(restapiEffectSaga, {
          uri: calls.uri,
          method: calls.method,
          id: calls.id,
          data: calls.filters
        })
      )
    );

    let customFieldValuesCalls: filterValueApiConfigType[] = [];

    let restState = yield select(restapiState);

    forEach(apiCalls, calls => {
      switch (calls.uri) {
        case azureFilterValues:
          const filterValuesDataRecords = get(
            restState,
            [azureFilterValues, calls.method, calls.id, "data", "records"],
            []
          );
          const newSupportedData: any[] = [];
          [...getNormalizedFiltersData(filterValuesDataRecords)].forEach(record => {
            const key = Object.keys(record)[0];
            if (key !== "workitem_type") {
              newSupportedData.push({ [`workitem_${key}`]: record[key] });
            } else {
              newSupportedData.push(record);
            }
          });

          supportedFiltersData = newSupportedData;
          break;
        case azureIntegrationConfig:
          const integrationConfigData = get(
            restState,
            [azureIntegrationConfig, calls.method, calls.id, "data", "records"],
            []
          );
          const aggFields = (integrationConfigData || [])
            .filter(
              (field: { field_key: string }) =>
                field.field_key.includes(AZURE_CUSTOM_FIELD_PREFIX) || field.field_key.includes(CUSTOM_FIELD_PREFIX)
            )
            .map((field: { field_key: string }) => ({ ...field, key: field.field_key }));

          const customFieldsList: string[] = uniq((aggFields || []).map((obj: { key: string }) => obj.key));
          const integrationIds: string[] = uniq(
            (aggFields || []).map((obj: { integration_id: string }) => obj?.integration_id)
          );

          forEach(integrationIds, (integrationId: string) => {
            const customFieldFilters: jiraFiltersSagaFilterType = {
              fields: customFieldsList,
              filter: {
                integration_ids: [integrationId]
              },
              integration_ids: [integrationId]
            };

            customFieldValuesCalls.push({
              uri: azureCustomFilter,
              method: "list",
              id: `CUSTOM_FILTERS_GET_ID_${integrationId}`,
              filters: customFieldFilters
            });
          });

          data.custom_hygienes = (integrationConfigData || []).reduce((agg: any[], obj: any) => {
            const cHygienes = get(obj, ["custom_hygienes"], []);
            agg.push(...cHygienes);
            return agg;
          }, []);

          if (aggFields.length) {
            data.custom_fields = uniqBy(aggFields, "key");
          } else {
            data.custom_fields = [];
          }
          break;
      }
    });

    if (customFieldValuesCalls.length) {
      yield all(
        customFieldValuesCalls.map(calls =>
          call(restapiEffectSaga, {
            uri: calls.uri,
            method: calls.method,
            id: calls.id,
            data: calls.filters
          })
        )
      );

      restState = yield select(restapiState);

      let customFiltersData: customFiltersOptionsType[] = [];
      forEach(customFieldValuesCalls, calls => {
        let customRecords: customFiltersOptionsType[] = get(
          restState,
          [calls.uri, calls.method, calls.id, "data", "records"],
          []
        );

        customRecords = getNormalizedFiltersData(customRecords).map(record => {
          const customFieldOptions = (Object.values(record)[0] || []).filter((option: any) => !!option?.key);
          return {
            [Object.keys(record)[0]]: customFieldOptions
          };
        });
        customFiltersData = [...customFiltersData, ...customRecords];
      });

      const customFieldMap: Dictionary<any[]> = {};
      forEach(customFiltersData, data => {
        const key = Object.keys(data)[0];
        const finalRecords: any[] = [...(customFieldMap[key] || []), ...(data[key] || [])];
        customFieldMap[key] = uniqBy(finalRecords, "key");
      });

      const newCustomFieldRecords = Object.keys(customFieldMap).map(key => {
        return { [key]: customFieldMap[key] };
      });

      supportedFiltersData.push(...newCustomFieldRecords);
    }
    data.records = supportedFiltersData;

    const updatedSupportedFilterRecords = transformCategoriesFiltersData(
      data,
      issueManagementStatusSupportedFilters.values
    );
    yield put(
      setCategoriesFiltersValuesData({
        payload: { integrationIds: data.integrationIds, data: updatedSupportedFilterRecords },
        loading: false,
        error: false
      })
    );
    apiToClear.push(...[...apiCalls, ...customFieldValuesCalls]);
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });

    yield put(setCategoriesFiltersValuesData({ error: !!(e as any)?.message, loading: false }));
  } finally {
    yield all(apiToClear.map(calls => put(restapiClear(calls.uri, calls.method, calls.id))));
  }
}

export function* categoriesFiltersTrellisProfileValueSagaWatcher() {
  yield takeLatest(CATEGORIES_FILTERS_VALUES_WITH_STATUS, CategoriesFiltersValueTrellisProfileSaga);
}

export function* azureCategoriesTrellisProfileFiltersValueSagaWatcher() {
  yield takeLatest(AZURE_CATEGORIES_FILTER_VALUES_WITH_STATUS, AzureCategoriesFiltersValueTrellisProfileSaga);
}
