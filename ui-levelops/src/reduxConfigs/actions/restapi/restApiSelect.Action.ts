import { REST_API_SELECT_GENERIC_LIST } from "../actionTypes";

export const restApiSelectGenericList = (
  loadAllData: boolean = false,
  uri: any,
  method: any,
  filters: any,
  complete = null,
  id = "0"
) => ({
  type: REST_API_SELECT_GENERIC_LIST,
  filters: filters,
  method: method,
  uri,
  id,
  loadAllData: loadAllData,
  complete
});
