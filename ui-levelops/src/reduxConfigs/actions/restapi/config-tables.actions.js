import * as actions from "../actionTypes";

const uri = "config_tables";

export const configTablesGet = (tableId, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: tableId,
  uri,
  complete,
  method: "get"
});

export const configTablesList = (filters, id = "0", complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri,
  method: "list",
  id: id,
  complete
});

export const configTablesCreate = (tableData, complete = null) => ({
  type: actions.RESTAPI_WRITE,
  data: tableData,
  uri,
  complete,
  method: "create"
});

export const configTablesUpdate = (tableId, tableData, complete = null) => ({
  type: actions.RESTAPI_WRITE,
  id: tableId,
  data: tableData,
  uri,
  complete,
  method: "update"
});

export const configTablesDelete = tableId => ({
  type: actions.RESTAPI_WRITE,
  id: tableId,
  uri,
  method: "delete"
});

export const configTablesBulkDelete = ids => ({
  type: actions.RESTAPI_WRITE,
  id: "0",
  uri,
  method: "bulkDelete",
  payload: ids
});
