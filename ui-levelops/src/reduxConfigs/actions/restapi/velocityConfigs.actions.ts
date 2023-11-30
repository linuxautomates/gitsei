import * as actions from "../actionTypes";
import { VELOCITY_CONFIG_GET } from "../actionTypes";

const uri = "velocity_configs";

export const velocityConfigsCreate = (item: any, id = "0") => ({
  type: actions.RESTAPI_WRITE,
  data: item,
  uri: uri,
  method: "create",
  id: id
});

export const velocityConfigsUpdate = (id: string, item: any) => ({
  type: actions.RESTAPI_WRITE,
  id: id,
  data: item,
  uri: uri,
  method: "update"
});

export const velocityConfigsGet = (id: string, complete = null) => ({
  type: actions.VELOCITY_CONFIG_GET,
  id: id,
  uri: uri,
  method: "get",
  complete: complete
});

export const velocityConfigsList = (filters: any = {}, id = "0", complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: uri,
  method: "list",
  id: id
});

export const velocityConfigsDelete = (id: string) => ({
  type: actions.VELOCITY_CONFIG_DELETE,
  id: id
});

export const velocityConfigsSetDefault = (id: string) => ({
  type: actions.VELOCITY_CONFIG_SET_TO_DEFAULT,
  uri,
  id
});

export const velocityConfigsClone = (cloneId: string) => ({
  type: actions.VELOCITY_CONFIG_CLONE,
  uri,
  method: "setDefault",
  id: cloneId
});

export const workflowProfileClone = (cloneId: string) => ({
  type: actions.WORKFLOW_PROFILE_CLONE,
  id: cloneId
});

export const velocityConfigsBasicTemplateGet = (id = "0", complete: string | null = null) => ({
  type: actions.RESTAPI_READ,
  id: id,
  uri: uri,
  method: "baseConfig",
  complete: complete
});
