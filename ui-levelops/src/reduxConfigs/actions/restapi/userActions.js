// user actions

import * as actions from "../actionTypes";
import { RestUsers } from "../../../classes/RestUsers";

const uri = "users";

export const me = () => ({
  type: actions.RESTAPI_READ,
  uri: "users",
  method: "me"
});

export const userProfileGet = () => window.isStandaloneApp ? ({
  type: actions.RESTAPI_READ,
  uri: "user_profile",
  method: "get"
}) : ({ type: actions.NO_ACTION});

export const usersList = (filters, id) => ({
  type: actions.RESTAPI_READ,
  id: id,
  data: filters,
  uri: uri,
  function: "getUsers",
  method: "list"
});

export const usersGet = userId => ({
  type: actions.RESTAPI_READ,
  id: userId,
  uri: uri,
  function: "getUser",
  method: "get",
  validator: RestUsers
});

export const usersDelete = userId => ({
  type: actions.RESTAPI_WRITE,
  id: userId,
  uri: uri,
  function: "deleteUser",
  method: "delete"
});

export const usersCreate = (user, id = "0", complete = null) => ({
  type: actions.RESTAPI_WRITE,
  data: user,
  uri: uri,
  function: "addUser",
  method: "create",
  complete: complete,
  id: id
});

export const usersUpdate = (userId, user) => ({
  type: actions.RESTAPI_WRITE,
  id: userId,
  data: user,
  uri: uri,
  function: "updateUser",
  method: "update"
});

export const userProfileUpdate = user => window.isStandaloneApp ? ({
  type: actions.RESTAPI_WRITE,
  id: "0",
  data: user,
  uri: "user_profile",
  method: "update"
}) : ({ type: actions.NO_ACTION});

export const usersBulkUpdate = (data, id = "0") => ({
  type: actions.RESTAPI_WRITE,
  id,
  data: data,
  uri: uri,
  method: "bulk"
});

export const usersGetOrCreate = (users, id = "0", complete = null) => ({
  type: actions.USERS_GET_OR_CREATE,
  users,
  complete,
  id: id
});

export const userTrellisPermissionUpdate = (user, isUpdateType = false) => ({
  type: actions.USER_TRELLIS_PERMISSION_UPDATE,
  user,
  isUpdateType
});
