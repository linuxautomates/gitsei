// tools

import * as actions from "../actionTypes";

const uri = "tools";

export const toolsGet = id => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  function: "getTool",
  method: "get"
});

export const toolsList = filters => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  function: "getTools",
  method: "list"
});

export const toolsSearch = filters => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  function: "searchTools",
  method: "search"
});

export const toolsCreate = policy => ({
  type: actions.RESTAPI_WRITE,
  data: policy
});

export const toolsUpdate = (id, policy) => ({
  type: actions.RESTAPI_WRITE,
  data: policy,
  id: id
});

export const toolsDelete = id => ({
  type: actions.RESTAPI_WRITE,
  id: id
});
