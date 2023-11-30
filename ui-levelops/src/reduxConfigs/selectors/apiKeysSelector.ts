import { get } from "lodash";

export const apiKeyDeleteState = (state: any) => {
  return get(state.restapiReducer, ["apikeys", "delete"], {});
};

export const apiKeyCreateState = (state: any) => {
  return get(state.restapiReducer, ["apikeys", "create"], {});
};
