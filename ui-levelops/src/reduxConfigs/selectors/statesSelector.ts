import { get } from "lodash";

export const stateCreateState = (state: any) => {
  return get(state.restapiReducer, ["states", "create"], {});
};

export const stateDeleteState = (state: any) => {
  return get(state.restapiReducer, ["states", "delete"], {});
};

export const stateGetState = (state: any) => {
  return get(state.restapiReducer, ["states", "get"], {});
};

export const stateUpdateState = (state: any) => {
  return get(state.restapiReducer, ["states", "update"], {});
};
