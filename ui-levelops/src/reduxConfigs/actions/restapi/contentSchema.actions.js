import * as actions from "../actionTypes";

const uri = "content_schema";

export const contentSchemaList = (filters, complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete: complete,
  uri: uri,
  method: "list"
});
