import { select, takeLatest, put, take, all } from "redux-saga/effects";
import { get, groupBy } from "lodash";
import { GET_OU_FILTERS_TO_DISPLAY } from "reduxConfigs/actions/actionTypes";
import { _dashboardsGetSelector } from "../../selectors/dashboardSelector";
import { restapiData, restapiError, restapiLoading } from "../../actions/restapi/restapiActions";
import { getGenericUUIDSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { getApiFiltersData, getApiFiltersRequirement, getIntegrationFilters, getApiCalls, getAllUsers } from "./helper";
import { genericList } from "reduxConfigs/actions/restapi";
import { valuesToFilters } from "dashboard/constants/constants";
import { API_REQUEST, API_REQUIREMENTS_TYPE, OU_Integration } from "./types";
import { getDynamicUsers } from "dashboard/components/dashboard-header/dashboard-actions/DashboardOUInfoModal/helper";

function* getFieldState(CustomFieldsApiCalls: API_REQUEST[], key: string): any {
  const jiraRequest = CustomFieldsApiCalls.find((request: any) => request.key.includes(key));
  const fieldieldsState = yield select(getGenericUUIDSelector, {
    uri: jiraRequest?.uri,
    method: "list",
    uuid: jiraRequest?.uuid
  });

  return fieldieldsState;
}

export function* getOUFiltersSaga(action: any): any {
  yield put(restapiLoading(true, action.uri, action.method, action.uuid));
  try {
    const integrations = action?.data || [];

    const apiRequirement: API_REQUIREMENTS_TYPE = getApiFiltersRequirement(integrations || []);

    const CustomFieldsApiCalls = getApiCalls(integrations, apiRequirement);

    let jiraCustomFields: any = {};
    let azureCustomFields: any = {};
    let jiraApiFiltersData: any = {};
    let allApiFiltersData: any = {};
    if (CustomFieldsApiCalls.length) {
      yield all(
        CustomFieldsApiCalls.map((apiRequest: any) =>
          put(
            genericList(
              apiRequest.uri,
              apiRequest.method,
              apiRequest.filters,
              apiRequest.complete,
              apiRequest.uuid,
              false
            )
          )
        )
      );

      yield all(CustomFieldsApiCalls.map(apiRequest => take(apiRequest.complete)));

      if (apiRequirement.jiraCustomFieldsRequired) {
        const jiraFieldsState = yield getFieldState(CustomFieldsApiCalls, "jira_custom_fields");
        const data = get(jiraFieldsState, ["data", "records"], []);
        const filteredData = groupBy(data, "integration_id");
        jiraCustomFields = {
          ...jiraCustomFields,
          ...filteredData
        };
      }

      if (apiRequirement.AzureCustomFieldsRequired) {
        const azureFieldsState = yield getFieldState(CustomFieldsApiCalls, "azure_custom_fields");
        const data = get(azureFieldsState, ["data", "records"], []);
        const filteredData = groupBy(data, "integration_id");
        azureCustomFields = {
          ...azureCustomFields,
          ...filteredData
        };
      }

      if (apiRequirement.jiraApiFiltersRequired) {
        const jiraFieldsState = yield getFieldState(CustomFieldsApiCalls, "jira_values");
        const jiraApiFiltersData = getApiFiltersData(jiraFieldsState);
        allApiFiltersData = {
          ...allApiFiltersData,
          jira: jiraApiFiltersData
        };
      }

      if (apiRequirement.azureApiFiltersRequired) {
        const jiraFieldsState = yield getFieldState(CustomFieldsApiCalls, "azure_values");
        const data = get(jiraFieldsState, ["data", "records"], {});
        jiraApiFiltersData = data.reduce((acc: any, next: any) => {
          const key = `workitem_${Object.keys(next)[0]}`;
          const Modifieldkey = get(valuesToFilters, key, key);
          const dataKey = Object.keys(next)[0];
          const value = get(next, [dataKey, "records"], []);
          return { ...acc, [Modifieldkey]: value };
        }, {});
        allApiFiltersData = {
          ...allApiFiltersData,
          azure_devops: jiraApiFiltersData
        };
      }

      if (apiRequirement.azureSCMApiFiltersRequired) {
        const jiraFieldsState = yield getFieldState(CustomFieldsApiCalls, "azure_scm_values");
        const jiraApiFiltersData = getApiFiltersData(jiraFieldsState);
        allApiFiltersData = {
          ...allApiFiltersData,
          azure_devops_scm: jiraApiFiltersData
        };
      }

      if (apiRequirement.azureCommitterApiFiltersRequired) {
        const jiraFieldsState = yield getFieldState(CustomFieldsApiCalls, "azure_committer_values");
        const jiraApiFiltersData = getApiFiltersData(jiraFieldsState);
        allApiFiltersData = {
          ...allApiFiltersData,
          azure_devops_committer: jiraApiFiltersData
        };
      }

      if (apiRequirement.githubSCMApiFiltersRequired) {
        const jiraFieldsState = yield getFieldState(CustomFieldsApiCalls, "github_scm_values");
        const jiraApiFiltersData = getApiFiltersData(jiraFieldsState);
        allApiFiltersData = {
          ...allApiFiltersData,
          github_scm: jiraApiFiltersData
        };
      }

      if (apiRequirement.githubCommitterApiFiltersRequired) {
        const jiraFieldsState = yield getFieldState(CustomFieldsApiCalls, "github_committer_values");
        const jiraApiFiltersData = getApiFiltersData(jiraFieldsState);
        allApiFiltersData = {
          ...allApiFiltersData,
          github_committer: jiraApiFiltersData
        };
      }

      if (apiRequirement.gitlabCommitterApiFiltersRequired) {
        const jiraFieldsState = yield getFieldState(CustomFieldsApiCalls, "gitlab_committer_values");
        const jiraApiFiltersData = getApiFiltersData(jiraFieldsState);
        allApiFiltersData = {
          ...allApiFiltersData,
          gitlab_committer: jiraApiFiltersData
        };
      }

      if (apiRequirement.gitlabSCMApiFiltersRequired) {
        const jiraFieldsState = yield getFieldState(CustomFieldsApiCalls, "gitlab_scm_values");
        const jiraApiFiltersData = getApiFiltersData(jiraFieldsState);
        allApiFiltersData = {
          ...allApiFiltersData,
          gitlab_scm: jiraApiFiltersData
        };
      }

      if (apiRequirement.bitbucketScmApiFiltersRequired) {
        const jiraFieldsState = yield getFieldState(CustomFieldsApiCalls, "bitbucket_server_scm_values");
        const jiraApiFiltersData = getApiFiltersData(jiraFieldsState);
        allApiFiltersData = {
          ...allApiFiltersData,
          bitbucket_server_scm: jiraApiFiltersData
        };
      }

      if (apiRequirement.bitbucketCommitterApiFiltersRequired) {
        const jiraFieldsState = yield getFieldState(CustomFieldsApiCalls, "bitbucket_server_committer_values");
        const jiraApiFiltersData = getApiFiltersData(jiraFieldsState);
        allApiFiltersData = {
          ...allApiFiltersData,
          bitbucket_server_committer: jiraApiFiltersData
        };
      }

      if (apiRequirement.gerritScmApiFiltersRequired) {
        const jiraFieldsState = yield getFieldState(CustomFieldsApiCalls, "gerrit_scm_values");
        const jiraApiFiltersData = getApiFiltersData(jiraFieldsState);
        allApiFiltersData = {
          ...allApiFiltersData,
          gerrit_scm: jiraApiFiltersData
        };
      }

      if (apiRequirement.gerritCommitterApiFiltersRequired) {
        const jiraFieldsState = yield getFieldState(CustomFieldsApiCalls, "gerrit_committer_values");
        const jiraApiFiltersData = getApiFiltersData(jiraFieldsState);
        allApiFiltersData = {
          ...allApiFiltersData,
          gerrit_committer: jiraApiFiltersData
        };
      }

      if (apiRequirement.helixScmApiFiltersRequired) {
        const jiraFieldsState = yield getFieldState(CustomFieldsApiCalls, "helix_scm_values");
        const jiraApiFiltersData = getApiFiltersData(jiraFieldsState);
        allApiFiltersData = {
          ...allApiFiltersData,
          helix_scm: jiraApiFiltersData
        };
      }

      if (apiRequirement.helixCommitterApiFiltersRequired) {
        const jiraFieldsState = yield getFieldState(CustomFieldsApiCalls, "helix_committer_values");
        const jiraApiFiltersData = getApiFiltersData(jiraFieldsState);
        allApiFiltersData = {
          ...allApiFiltersData,
          helix_committer: jiraApiFiltersData
        };
      }

      if (apiRequirement.pagerdutyApiFiltersRequired) {
        const jiraFieldsState = yield getFieldState(CustomFieldsApiCalls, "pagerduty_values");
        const modifiedState = get(jiraFieldsState, ["data", "records"], []).map((item: any) => {
          let key = Object.keys(item || {})?.[0];
          const values = item[key]?.map((value: any) => ({ key: value?.id, additional_key: value?.name }));
          if (key === "pd_service") {
            key = "pd_service_ids";
          }
          return {
            [key]: values
          };
        });
        const jiraApiFiltersData = getApiFiltersData({ data: { records: modifiedState } });
        allApiFiltersData = {
          ...allApiFiltersData,
          pagerduty: jiraApiFiltersData
        };
      }

      if (apiRequirement.usersApiRequirement) {
        const jiraFieldsState = yield getFieldState(CustomFieldsApiCalls, "org_users");
        allApiFiltersData = {
          ...allApiFiltersData,
          org_users: get(jiraFieldsState, ["data", "records"], [])
        };
      }
    }

    const integrationData = integrations.map((integration: OU_Integration) => {
      const customFieldsOptions =
        integration.type === "jira"
          ? jiraCustomFields?.[integration.id] ?? []
          : azureCustomFields?.[integration.id] ?? [];
      return {
        ...integration,
        filters: getIntegrationFilters(integration.filters, customFieldsOptions, integration.type, allApiFiltersData),
        dynamic_user_definition: getDynamicUsers(integration?.dynamic_user_definition),
        users: getAllUsers(integration?.users || [], allApiFiltersData?.org_users || [])
      };
    });
    yield put(restapiData(integrationData || [], action.uri, action.method, action.uuid));
    yield put(restapiLoading(false, action.uri, action.method, action.uuid));
  } catch (e) {
    yield put(restapiError(e, action.uri, action.method, action.uuid));
    yield put(restapiData([], action.uri, action.method, action.uuid));
  }
}

export function* getOUFiltersSagaWatcher() {
  yield takeLatest(GET_OU_FILTERS_TO_DISPLAY, getOUFiltersSaga);
}
