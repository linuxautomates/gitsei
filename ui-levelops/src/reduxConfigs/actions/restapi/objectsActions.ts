import * as actions from "../actionTypes";
import { OBJECTS_ROUTE } from "../../../constants/restUri";

const uri = OBJECTS_ROUTE;

export const objectsGet = (type: string, complete = null) => ({
  type: actions.RESTAPI_READ,
  id: type,
  uri: uri,
  method: "get",
  complete: complete
});

export const objectsList = (
  filters: any = { page: 0, page_size: 100 }
  // id = "0",
  // complete = null
) => ({
  type: actions.RESTAPI_WRITE,
  data: filters,
  // complete: complete,
  uri: uri,
  method: "list"
  // id: id
});
