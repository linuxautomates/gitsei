import { get } from "lodash";
import { createSelector } from "reselect";
import { restapiState } from "./restapiSelector";

export const zendeskFieldListSelector = createSelector(restapiState, apis => {
  return get(apis, ["zendesk_fields", "list"], {});
});
