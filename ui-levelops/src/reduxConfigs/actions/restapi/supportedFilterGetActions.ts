import * as actions from "../actionTypes";

export const supportedFilterGet = (uri: string, filters: any, id: string) => ({
  type: actions.SUPPORTED_FILTER_GET,
  uri,
  filters,
  id
});
