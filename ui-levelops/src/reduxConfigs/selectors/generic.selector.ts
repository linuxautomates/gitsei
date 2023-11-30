import { get } from "lodash";

type genericApi = {
  id: string;
  uri: string;
  method: "list" | "get";
  data?: any;
};

export const genericApiState = (state: any, api: genericApi) => {
  return get(state.restapiReducer, [api.uri, api.method, api.id], { loading: true, error: true });
};
