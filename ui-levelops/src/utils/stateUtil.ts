import { get } from "lodash";

export function restAPILoadingState(restAPIState: any, id: string = "0") {
  return {
    loading: get(restAPIState, [id, "loading"], true),
    error: get(restAPIState, [id, "error"], false)
  };
}
