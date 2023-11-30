import * as actions from "../actionTypes";

const uri = "automation_rules";

export const automationRulesCreate = (item: any, id = "0") => ({
  type: actions.RESTAPI_WRITE,
  data: item,
  uri: uri,
  method: "create",
  id: id
});

export const automationRulesDelete = (id: string) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  uri: uri,
  method: "delete"
});

export const automationRulesUpdate = (id: string, item: any) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  data: item,
  uri: uri,
  method: "update"
});

export const automationRulesGet = (id: string, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  method: "get",
  complete: complete
});

export const automationRulesList = (filters: any, id = "0", complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: uri,
  method: "list",
  id: id
});

export const automationRulesBulk = (filters: any, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: uri,
  method: "bulk"
});
