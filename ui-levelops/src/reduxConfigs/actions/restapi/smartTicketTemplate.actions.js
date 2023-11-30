import * as actions from "../actionTypes";

const uri = "ticket_templates";

export const smartTicketTemplatesList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: uri,
  function: "listSmartTicketTemplates",
  method: "list"
});

export const smartTicketTemplatesGet = (id, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  complete: complete,
  uri: uri,
  function: "getSmartTicketTemplate",
  method: "get"
});

export const smartTicketTemplatesUpdate = (id, item, complete = null) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  data: item,
  complete: complete,
  uri: uri,
  function: "updateSmartTicketTemplate",
  method: "update"
});

export const smartTicketTemplatesDelete = (id, complete = null) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  complete: complete,
  uri: uri,
  function: "deleteSmartTicketTemplate",
  method: "delete"
});

export const smartTicketTemplatesCreate = (item, complete = null) => ({
  type: actions.RESTAPI_WRITE,
  data: item,
  complete: complete,
  uri: uri,
  function: "createSmartTicketTemplate",
  method: "create"
});

export const smartTicketTemplatesBulkDelete = ids => ({
  type: actions.RESTAPI_WRITE,
  payload: ids,
  id: "0",
  uri,
  method: "bulkDelete"
});
