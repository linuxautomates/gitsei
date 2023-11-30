import { get } from "lodash";
import { createSelector } from "reselect";
import { restapiState } from "./restapiSelector";

export const getCicdAggsListState = createSelector(restapiState, data => {
  return get(data, ["cicd_job_aggs", "list"], {});
});

export const getCicdAggsListSelector = createSelector(getCicdAggsListState, data => {
  return data;
});
