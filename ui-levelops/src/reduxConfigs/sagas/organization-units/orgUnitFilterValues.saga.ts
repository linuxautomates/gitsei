import { cloneDeep, forEach, get, isArray } from "lodash";
import { all, call, put, select, takeEvery } from "redux-saga/effects";
import { ORGANIZATION_UNIT_FILTER_VALUES } from "reduxConfigs/actions/actionTypes";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { orgUnitFiltersMapping } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";
import { restapiEffectSaga } from "../restapiSaga";
import { ORGANIZATION_UNIT_NODE, OUSupportedFiltersByApplication } from "configurations/pages/Organization/Constants";
import widgetConstants from "dashboard/constants/widgetConstants";
import { restapiClear, widgetFilterValuesGet } from "reduxConfigs/actions/restapi";
import { getOUTransformedFilterValuesOptions } from "configurations/pages/Organization/Helpers/OrgUnit.helper";
import { jiraFiltersEffectSaga } from "../jiraFilterValues.saga";
import { GITHUB_APPLICATIONS } from "../../../utils/reportListUtils";
import { azureFiltersEffectSaga } from "../azureFilterValueSaga";
import { basicActionType, basicMappingType } from "../../../dashboard/dashboard-types/common-types";
import { jiraSprintReportListEffectSaga } from "../jirasprintReports";
import { pagerdutyValuesToFilters } from "dashboard/constants/applications/pagerduty/pagerduty-time-to-resolve/constant";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
const AZURE_VALUES_URI = "issue_management_workitem_values";
const AZURE_ATTRIBUTE_VALUE_URI = "issue_management_attributes_values";
const AZURE_SPRINT_VALUE_URI = "issue_management_sprint_filters";

function* orgUnitFiltersValueSaga(action: { type: string; integration: string; id: string }): any {
  const applicationAndId: string[] = (action.integration || "").split("@");
  let integrationIds = [applicationAndId[1]];
  let integration: string = applicationAndId[0];
  try {
    const mapping: { [x: string]: any } = yield select(orgUnitFiltersMapping);
    if (!mapping.hasOwnProperty(integration || "")) {
      let supportedFilters: any = { uri: "", values: [] };

      if (Object.keys(OUSupportedFiltersByApplication).includes(integration)) {
        supportedFilters = OUSupportedFiltersByApplication[integration];
      } else if (GITHUB_APPLICATIONS.includes(integration)) {
        supportedFilters = OUSupportedFiltersByApplication.github;
      } else if (["azure_devops"].includes(integration)) {
        supportedFilters = OUSupportedFiltersByApplication.azure_devops;
      } else {
        forEach(widgetConstants, report => {
          if ((report as any)?.application === integration && !supportedFilters.uri) {
            supportedFilters = get(report, ["supported_filters"], { uri: "", values: [] });
          }
        });
      }

      const isValidFilters =
        !!supportedFilters?.uri ||
        ([...GITHUB_APPLICATIONS, "github", "azure_devops"].includes(integration) &&
          isArray(supportedFilters) &&
          supportedFilters.every((filter: any) => !!filter.uri));
      if (isValidFilters) {
        let fields = supportedFilters?.values || [];
        const filters = {
          fields: fields,
          filter: {
            integration_ids: integrationIds
          }
        };

        if (integration === "jira") {
          yield call(
            jiraFiltersEffectSaga,
            widgetFilterValuesGet(
              supportedFilters.uri,
              {
                ...filters,
                integration_ids: integrationIds
              },
              null,
              action.id
            )
          );
        } else if (integration === "azure_devops") {
          let apiCallsConfig: Array<Omit<basicActionType<any>, "type"> & { saga: Generator<any> }> = (
            supportedFilters || []
          ).map((supportedFilter: any) => {
            if (supportedFilter.uri === AZURE_VALUES_URI) {
              return {
                saga: azureFiltersEffectSaga as any,
                ...widgetFilterValuesGet(
                  supportedFilter.uri,
                  {
                    fields: supportedFilter.values || [],
                    filter: {
                      integration_ids: integrationIds
                    },
                    integration_ids: integrationIds
                  },
                  null,
                  action.id
                ),
                uri: supportedFilter.uri
              };
            } else if (["cicd_filter_values", "jenkins_jobs_filter_values"].includes(supportedFilter.uri)) {
              return {
                saga: restapiEffectSaga,
                data: {
                  fields: supportedFilter.values,
                  filter: {
                    integration_ids: integrationIds,
                    cicd_integration_ids: integrationIds
                  }
                },
                uri: supportedFilter.uri,
                method: "list",
                id: integrationIds?.[0]
              };
            }
            return {
              saga: restapiEffectSaga,
              data: {
                fields: supportedFilter.values,
                filter: {
                  integration_ids: integrationIds
                }
              },
              uri: supportedFilter.uri,
              method: "list",
              id: integrationIds?.[0]
            };
          });
          apiCallsConfig = [
            ...apiCallsConfig,
            {
              saga: restapiEffectSaga as any,
              data: { ...filters, fields: ["teams", "code_area"] },
              uri: AZURE_ATTRIBUTE_VALUE_URI,
              id: action.id,
              method: "list"
            },
            {
              saga: jiraSprintReportListEffectSaga as any,
              data: {
                filter: {
                  integration_ids: integrationIds || []
                }
              },
              uri: AZURE_SPRINT_VALUE_URI,
              id: action.id,
              method: "list"
            }
          ];
          yield all(apiCallsConfig.map(apiCall => call(apiCall.saga as any, apiCall)));
        } else if ([...GITHUB_APPLICATIONS, "github"].includes(integration)) {
          const apicalls = (supportedFilters || []).map((supportedFilter: any) => ({
            data: {
              fields: supportedFilter.values,
              filter: {
                integration_ids: integrationIds
              }
            },
            uri: supportedFilter.uri,
            method: "list",
            id: integrationIds?.[0]
          }));
          yield all(apicalls.map((apiCall: any) => call(restapiEffectSaga, apiCall)));
        } else {
          yield call(restapiEffectSaga, {
            data: filters,
            uri: supportedFilters.uri,
            method: "list",
            id: action.id
          });
        }

        const restState = yield select(restapiState);
        let resultRecords: any[] = get(restState, [supportedFilters.uri, "list", action.id, "data", "records"], []);
        let customFieldsMapping: any[] = [];

        if (integration === "jira") {
          customFieldsMapping = get(restState, [supportedFilters.uri, "list", action.id, "data", "custom_fields"], []);
        } else if (integration === "azure_devops") {
          resultRecords = get(restState, [AZURE_VALUES_URI, "list", action.id, "data", "records"], []);
          customFieldsMapping = get(restState, [AZURE_VALUES_URI, "list", action.id, "data", "custom_fields"], []);
          let workitemAttributesFilterValues = get(
            restState,
            [AZURE_ATTRIBUTE_VALUE_URI, "list", action.id, "data", "records"],
            []
          );
          workitemAttributesFilterValues = workitemAttributesFilterValues.map(
            (record: basicMappingType<Array<any>>) => {
              const key = Object.keys(record)[0];
              const values = get(record?.[key] ?? {}, ["records"], []);
              return { [key]: values };
            }
          );

          const azureIterationApiData = get(
            restState,
            [AZURE_SPRINT_VALUE_URI, "list", action?.id, "data", "records"],
            []
          );

          const azureIterationValues = azureIterationApiData.map((item: { name: string; parent_sprint: string }) => ({
            key: `${item.parent_sprint}\\${item.name}`
          }));

          const uriList = [
            "github_prs_filter_values",
            "github_commits_filter_values",
            "jenkins_jobs_filter_values",
            "cicd_filter_values"
          ];
          const differentUriRecords = uriList.reduce((acc: any, uri: string) => {
            const valueRecords = get(restState, [uri, "list", integrationIds?.[0], "data", "records"], []).map(
              (item: any) => {
                const key = Object.keys(item)[0];
                return { [key]: item[key] };
              }
            );
            return [...acc, ...valueRecords];
          }, []);

          resultRecords = [
            ...resultRecords,
            ...workitemAttributesFilterValues,
            { workitem_sprint_full_names: azureIterationValues },
            ...differentUriRecords
          ];
        } else if ([...GITHUB_APPLICATIONS, "github"].includes(integration)) {
          const prValues = get(
            restState,
            ["github_prs_filter_values", "list", integrationIds?.[0], "data", "records"],
            []
          ).map((item: any) => {
            const key = Object.keys(item)[0];
            return { [key]: item[key] };
          });

          const commitValues = get(
            restState,
            ["github_commits_filter_values", "list", integrationIds?.[0], "data", "records"],
            []
          ).map((item: any) => {
            const key = Object.keys(item)[0];
            return { [key]: item[key] };
          });

          resultRecords = [...prValues, ...commitValues];
        }
        let integrationFilterMapping = getOUTransformedFilterValuesOptions(resultRecords, customFieldsMapping);
        if (["azure_devops", "jira"].includes(integration)) {
          integrationFilterMapping = integrationFilterMapping.filter((item: any) => {
            const label = (item?.label || "").toLowerCase();
            return !label.includes("sprint");
          });
          integrationFilterMapping.push({
            label: "Sprint",
            value: "sprint",
            options: []
          });
          integrationFilterMapping = integrationFilterMapping.map((filter: any) => {
            if (filter?.value === "job_normalized_full_names") {
              return { ...filter, label: "Job Normalized Full Names" };
            }
            return filter;
          });
        }

        if (integration === "pagerduty") {
          integrationFilterMapping = integrationFilterMapping.map((filter: any) => {
            const value = get(pagerdutyValuesToFilters, [filter.value], filter.value);
            if (filter.value === "pd_service_id") {
              return { ...filter, value: value, label: "Service" };
            }
            if (filter.value === "user_ids") {
              return { ...filter, value: value, label: "USER (ENGINEER)" };
            }
            return { ...filter, value: value };
          });
        }

        let newMapping = {
          ...(mapping || {}),
          [action.integration]: integrationFilterMapping
        };

        yield put(genericRestAPISet(newMapping, ORGANIZATION_UNIT_NODE, action.id, "-1"));
        yield put(restapiClear(supportedFilters.uri, "list", action.id));
      }
    } else {
      let newMapping = cloneDeep(mapping);
      yield put(genericRestAPISet(newMapping, ORGANIZATION_UNIT_NODE, action.id, "-1"));
    }
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.ORG_UNITS,
        data: { e, action }
      }
    });
  }
}

export function* orgUnitFiltersValueSagaWatcher() {
  yield takeEvery(ORGANIZATION_UNIT_FILTER_VALUES, orgUnitFiltersValueSaga);
}
