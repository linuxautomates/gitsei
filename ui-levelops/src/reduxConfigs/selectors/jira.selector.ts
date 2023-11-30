import { createSelector } from "reselect";
import { get } from "lodash";
import { emptyObj, restapiState } from "./restapiSelector";

const CustomTimeBasedTypes = ["date", "datetime", "dateTime"]

export const getIntegrationConfigSelector = createSelector(restapiState, apis => {
  return apis.jira_integration_config || emptyObj;
});

export const jiraIntegrationConfigListSelector = createSelector(
  getIntegrationConfigSelector,
  integrationConfigState => {
    return get(integrationConfigState, ["list", "0"], { loading: true, error: false });
  }
);

export const jiraFieldsSelector = createSelector(restapiState, apis => {
  return get(apis, ["jira_fields"], {});
});

export const jiraFieldsListSelector = createSelector(restapiState, (state: any) => {
  return get(state, ["jira_fields", "list"], {});
});

export const jiraFieldRecordsSelector = createSelector(jiraFieldsListSelector, (state: any) =>
  get(state, ["jira_application_field_list", "data", "records"], [])
);

export const customTimeFiltersSelector = createSelector(jiraFieldRecordsSelector, (fieldList: Array<any>) =>
  fieldList.filter((field: any) => CustomTimeBasedTypes.includes(field.field_type))
);

export const customTimeFilterKeysSelector = createSelector(customTimeFiltersSelector, (customTimeFilters: Array<any>) =>
  customTimeFilters.map((filter: any) => filter.field_key)
);

export const velocityConfiglistSelector = createSelector(restapiState, apis => {
  return get(apis, ["velocity_configs", "list", "velocity_configs_list_id", "data", "records"], {});
});
