import * as actions from "../actionTypes";

export const jiraSalesforceZendeskStagesWidgets = (
  uri: string,
  method: string,
  filters: any,
  complete = null,
  id = "0"
) => ({
  type: actions.JIRA_ZENDESK_SALESFORCE_STAGES_WIDGETS,
  data: filters,
  id,
  uri,
  method,
  complete
});

export const jiraSalesforceZendeskC2FWidgets = (
  uri: string,
  method: string,
  filters: any,
  complete = null,
  id = "0"
) => ({
  type: actions.JIRA_ZENDESK_SALESFORCE_C2F_WIDGETS,
  data: filters,
  id,
  uri,
  method,
  complete
});
