import { get, uniq } from "lodash";
import { all, call, put, select, take, takeLatest } from "redux-saga/effects";
import { SLA_FILTERS_DATA } from "reduxConfigs/actions/actionTypes";
import { integrationsList, restapiData, restapiLoading } from "reduxConfigs/actions/restapi";
import { getData } from "utils/loadingUtils";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { handleRestApiError } from "./restapiSaga";
import { getNormalizedFiltersData } from "./saga-helpers/azureSagas.helper";
import { IntegrationTypes } from "constants/IntegrationTypes";
const ApplicationUriMapping = {
  jira: "jira_filter_values",
  azure_devops: "issue_management_workitem_values"
};

const FIELDS_VALUES_MAPPING = {
  jira: ["status", "priority", "issue_type", "assignee", "project", "component"],
  azure_devops: ["status", "priority", "workitem_type", "assignee", "project", "component"]
};
const validApplications = ["jira", "azure_devops"];
const restapiState = (state: { [state: string]: { [subState: string]: any } }) => state.restapiReducer;

function* slaModuleEffectSaga(action: any) {
  const { uri, filters, id, method } = action;
  try {
    yield put(restapiLoading(true, action.uri, action.method, id.toString()));
    const sla_integration_list_id = "sla_integration_list";
    const complete = `COMPLETE_${uri}_${method}_${id}`;
    yield put(
      integrationsList(
        { filter: { integration_ids: filters.integration_ids } },
        complete as any,
        sla_integration_list_id
      )
    );
    yield take(complete);

    //@ts-ignore
    let listState = yield select(restapiState);

    const listData = getData(listState, "integrations", method, sla_integration_list_id);
    const listRecords = listData.records || [];
    if (listRecords.length) {
      const allApplicationType = uniq(
        listRecords.map((integration: any) => {
          if (integration.application && validApplications.includes(integration.application)) {
            return integration.application;
          }
          return undefined;
        })
      ).filter((application: any) => !!application);

      if (allApplicationType.length) {
        yield all(
          allApplicationType.map(application => {
            const complete_values_data = `${application}_sla`;
            return put(
              actionTypes.genericList(
                get(ApplicationUriMapping, [application as any], application),
                "list",
                {
                  filter: filters,
                  fields: get(FIELDS_VALUES_MAPPING, [application as any], application)
                },
                complete_values_data as any,
                "sla"
              )
            );
          })
        );

        yield all(allApplicationType.map(application => take(`${application}_sla`)));
        //@ts-ignore
        listState = yield select(restapiState);
        const allApplicationData = allApplicationType.reduce((acc, application) => {
          const applicationUri = get(ApplicationUriMapping, [application as any], undefined);
          if (application === IntegrationTypes.AZURE) {
            return {
              ...(acc as any),
              [application as any]: getNormalizedFiltersData(
                get(listState, [applicationUri, "list", "sla", "data", "records"], [])
              )
            };
          }
          return {
            ...(acc as any),
            [application as any]: get(listState, [applicationUri, "list", "sla", "data", "records"], [])
          };
        }, {});
        yield put(restapiData(allApplicationData, action.uri, action.method, id.toString()));
        yield put(restapiLoading(false, action.uri, action.method, id.toString()));
      } else {
        yield put(restapiData({}, action.uri, action.method, id.toString()));
        yield put(restapiLoading(false, action.uri, action.method, id.toString()));
      }
    }
  } catch (error) {
    yield call(handleRestApiError, error, action, {});
  }
}

export function* slaModuleWatcher() {
  yield takeLatest(SLA_FILTERS_DATA, slaModuleEffectSaga);
}
