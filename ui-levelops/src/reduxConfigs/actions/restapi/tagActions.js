// tags

import * as actions from "../actionTypes";

const uri = "tags";

export const tagsGet = id => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  function: "getTag",
  method: "get"
});

export const tagsDelete = id => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  uri: uri,
  function: "deleteTag",
  method: "delete"
});

export const tagsCreate = (tag, id = "0", complete = null) => ({
  type: actions.RESTAPI_WRITE,
  data: tag,
  uri: uri,
  id: id,
  function: "createTag",
  method: "create",
  complete
});

export const tagsList = (filter, id = 0, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filter,
  complete: complete,
  uri: uri,
  function: "getTags",
  method: "list",
  id
});

export const tagsBulkList = (filter, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filter,
  complete: complete,
  uri: uri,
  function: "getTags",
  method: "bulk"
});

export const tagsSearch = filters => ({
  type: actions.RESTAPI_READ,
  data: filters,
  uri: uri,
  function: "searchTags",
  method: "search"
});

export const tagsGetOrCreate = (tag_names, complete = null, id = "0") => ({
  type: actions.TAGS_GET_OR_CREATE,
  tag_names,
  complete,
  id: id
});
