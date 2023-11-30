import { get } from "lodash";

export const getCustomTableUsingSagaSelector = (state: any, uri: string, id: any) => {
  return get(state.restapiReducer, [uri, "list", id], { loading: true, error: true });
};
