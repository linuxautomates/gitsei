import { get, map, uniq, cloneDeep, unionBy } from "lodash";
import { call, put, select, takeEvery } from "redux-saga/effects";
import { AZURE_FILTER_VALUES, TESTRAILS_FILTER_VALUES } from "reduxConfigs/actions/actionTypes";
import { restapiData, restapiLoading, restapiError, restapiClear } from "reduxConfigs/actions/restapi";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";
import { getData } from "utils/loadingUtils";
import { restapiEffectSaga } from "./restapiSaga";
import { IssueManagementWorkItemFieldListService, RestIntegrationsService } from "services/restapi";
import { categoriesFilterValueInitialDataType } from "./saga-types/jiraFiltersSaga.types";
import { v1 as uuid } from "uuid";
import { getNormalizedFiltersData } from "./saga-helpers/azureSagas.helper";
import { AZURE_CUSTOM_FIELD_PREFIX, TESTRAILS_CUSTOM_FIELD_PREFIX } from "dashboard/constants/constants";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { IntegrationTransformedCFTypes } from "configurations/configuration-types/integration-edit.types";

export function* testrailsFiltersEffectSaga(action: {
    type: string;
    data: any;
    uri: string;
    method: string;
    complete: string;
    id: any;
}): any {
    const actionId = action.id;
    const initialId = `TESTRAILS_SUPPORTED_FILTERS_${uuid()}`;
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
                filter: { applications: ["testrails"] },
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
            return field;
        });

        // testrails supported filters call
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
            return {
                [`${key}`]: record[key]
            };
        });

        let fieldRecords = records || [];;
        data.records = fieldRecords;

        const integConfig = "jira_integration_config";
        const customFilter = "testrails_custom_filter_values";

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
                const key = field.key.includes(TESTRAILS_CUSTOM_FIELD_PREFIX)
                    ? field.key.replace(TESTRAILS_CUSTOM_FIELD_PREFIX, "")
                    : field.key;
                let fieldKeyConfig: any = { key: field.key };
                if (!field.key?.includes(TESTRAILS_CUSTOM_FIELD_PREFIX)) {
                    fieldKeyConfig = {
                        key: `${TESTRAILS_CUSTOM_FIELD_PREFIX}${field.key}`,
                        metadata: { transformed: TESTRAILS_CUSTOM_FIELD_PREFIX }
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
        const customRecords = customData.records.map((record: { [key: string]: any }) => {
            const key = Object.keys(record)[0];
            return {
                [`${key}`]: record[key]
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

export function* testrailsFilterValueWatcherSaga() {
    yield takeEvery([TESTRAILS_FILTER_VALUES], testrailsFiltersEffectSaga);
}
