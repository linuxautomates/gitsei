import { get } from "lodash";

export const getTriageJobResultsList = (state: any) => {
  return get(state.restapiReducer, ["fetchJobResults", "list"], {});
};
