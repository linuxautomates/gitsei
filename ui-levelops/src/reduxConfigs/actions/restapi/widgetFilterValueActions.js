import * as actions from "../actionTypes";

// TODO if the uri is jira, then trigger a saga here that will set the values for that uri??
export const widgetFilterValuesGet = (uri, filters, complete, id = "0", isConfigLoaded = false) => {
  if (isConfigLoaded && uri === "jira_filter_values") {
    return {
      type: actions.JIRA_FILTER_VALUES_PRE_FETCHED,
      data: filters,
      uri: uri,
      method: "list",
      complete: complete,
      id
    };
  }
  if (uri === "zendesk_filter_values") {
    return {
      type: actions.ZENDESK_FILTER_VALUES,
      data: filters,
      uri: uri,
      method: "list",
      complete: complete,
      id
    };
  }
  if (uri === "jira_filter_values") {
    return {
      type: actions.JIRA_FILTER_VALUES,
      data: filters,
      uri: uri,
      method: "list",
      complete: complete,
      id
    };
  }
  if (uri === "jira_zendesk_filter_values") {
    return {
      type: actions.JIRA_ZENDESK_FILTER_VALUES,
      data: filters,
      uri: uri,
      method: "list",
      complete: complete,
      id
    };
  }
  if (uri === "jira_salesforce_filter_values") {
    return {
      type: actions.JIRA_SALESFORCE_FILTER_VALUES,
      data: filters,
      uri: uri,
      method: "list",
      complete: complete,
      id
    };
  }
  if (uri === "lead_time_filter_values") {
    return {
      type: actions.LEAD_TIME_FILTER_VALUES,
      data: filters,
      uri: uri,
      method: "list",
      complete: complete,
      id
    };
  }
  if (uri === "scm_issue_management_workitem_values") {
    return {
      type: actions.ISSUE_LEAD_TIME_FILTER_VALUES,
      data: filters,
      uri: uri,
      method: "list",
      complete: complete,
      id
    };
  }
  if (uri === "issue_management_workitem_values") {
    return {
      type: actions.AZURE_FILTER_VALUES,
      data: filters,
      uri: uri,
      method: "list",
      complete: complete,
      id
    };
  }
  if (uri === "code_vol_vs_deployment_values") {
    return {
      type: actions.CODE_VOL_VS_DEPLOYMENT_VALUES,
      data: filters,
      uri: uri,
      method: "list",
      complete: complete,
      id
    };
  }
  if (uri === "testrails_tests_values") {
    return {
      type: actions.TESTRAILS_FILTER_VALUES,
      data: filters,
      uri: uri,
      method: "list",
      complete: complete,
      id
    };
  }
  return {
    type: actions.RESTAPI_READ,
    data: filters,
    uri: uri,
    method: "list",
    complete: complete,
    id
  };
};

export const genericWidgetFilterValuesGet = (
  supportedFilters,
  integrationIds,
  id = "0",
  additionalFilters = {},
  removeIntegration = false
) => {
  return {
    type: actions.GENERIC_FILTER_VALUES,
    supportedFilters,
    integrationIds,
    id,
    additionalFilters,
    removeIntegration: removeIntegration
  };
};
