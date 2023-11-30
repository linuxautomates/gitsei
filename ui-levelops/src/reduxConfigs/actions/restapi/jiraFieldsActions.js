import * as actions from "../actionTypes";

const uri = "jira_fields";

export const JIRA_CUSTOM_FIELDS_LIST = "jira_custom_filter_fields";
export const AZURE_CUSTOM_FIELDS_LIST = "azure_custom_filter_fields";
export const ZENDESK_CUSTOM_FIELDS_LIST = "zendesk_custom_filter_fields";
export const TESTRAILS_CUSTOM_FIELDS_LIST = "testrails_custom_filter_fields";

export const DEFAULT_JIRA_CUSTOM_FIELDS_LIST_ID = "default_jira_custom_fields_list_id";
export const DEFAULT_AZURE_CUSTOM_FIELDS_LIST_ID = "default_azure_custom_fields_list_id";
export const DEFAULT_ZENDESK_CUSTOM_FIELDS_LIST_ID = "default_zendesk_custom_fields_list_id";
export const DEFAULT_TESTRAILS_CUSTOM_FIELDS_LIST_ID = "default_testrails_custom_fields_list_id";

export const jiraFieldsList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  complete: complete,
  method: "list"
});

export const jiraCustomFilterFieldsList = (filters, id = "0", complete = null) => ({
  type: actions.JIRA_INTEGRATION_CUSTOM_FIELDS,
  data: filters,
  uri: JIRA_CUSTOM_FIELDS_LIST,
  complete: complete,
  method: "list",
  id: id
});

export const azureCustomFilterFieldsList = (filters, id = "0", complete = null) => ({
  type: actions.AZURE_INTEGRATION_CUSTOM_FIELDS,
  data: filters,
  uri: AZURE_CUSTOM_FIELDS_LIST,
  complete: complete,
  method: "list",
  id: id
});

export const zendeskCustomFilterFieldsList = (filters, id = "0", complete = null) => ({
  type: actions.ZENDESK_INTEGRATION_CUSTOM_FIELDS,
  data: filters,
  uri: ZENDESK_CUSTOM_FIELDS_LIST,
  complete: complete,
  method: "list",
  id: id
});

export const testrailsCustomFilterFieldsList = (filters, id = "0", complete = null) => ({
  type: actions.TESTRAILS_INTEGRATION_CUSTOM_FIELDS,
  data: filters,
  uri: TESTRAILS_CUSTOM_FIELDS_LIST,
  complete: complete,
  method: "list",
  id: id
});