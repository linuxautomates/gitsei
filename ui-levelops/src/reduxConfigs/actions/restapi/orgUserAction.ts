import * as actions from "../actionTypes";

const uri = "org_users";
const versionsUri = "org_users_version";
const importUri = "org_users_import";
const schemaUri = "org_users_schema";
const filterUri = "org_users_filter";

export const OrgUserCreate = (item: any, id = "0") => ({
  type: actions.RESTAPI_WRITE,
  data: item,
  uri: uri,
  method: "create",
  id: id
});

export const OrgUserUpdate = (id: string, item: any) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  data: item,
  uri: uri,
  method: "update"
});

export const OrgUserGet = (id: string, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  method: "get",
  complete: complete
});

export const OrgUserList = (filters: any = {}, id = "0", complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: uri,
  method: "list",
  id: id
});

export const orgUserDelete = (id = "0", ids: string[] | number[]) => ({
  type: actions.RESTAPI_WRITE,
  payload: ids,
  uri,
  method: "bulkDelete",
  id: id
});

export const OrgUserVersionList = (filters: any = {}, id = "0", complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: versionsUri,
  method: "get",
  id: id
});

export const OrgUserVersionCreate = (item: any, id = "0") => ({
  type: actions.RESTAPI_WRITE,
  data: item,
  uri: versionsUri,
  method: "create",
  id: id
});

export const OrgUserImport = (item: any, uri: string, method: string, uuid = "0") => ({
  type: actions.RESTAPI_WRITE,
  data: item,
  uri: uri,
  method: method,
  id: uuid
});

export const OrgUserSchemaGet = (id: string, uri: string, complete = null, queryparams = {}) => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  method: "get",
  complete: complete,
  queryparams
});

export const OrgUserSchemaCreate = (item: any, id = "0") => ({
  type: actions.RESTAPI_WRITE,
  data: item,
  uri: schemaUri,
  method: "create",
  id: id
});

export const OrgUserFiltersList = (filters: any = {}, id = "0", complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: filterUri,
  method: "list",
  id: id
});

export const OrgUserContributorsRolesGet = (id: string, uri: string, complete = null, queryparams = {}) => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  method: "get",
  complete: complete,
  queryparams
});