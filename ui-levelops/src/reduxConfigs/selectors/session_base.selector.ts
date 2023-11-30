import { get } from "lodash";
import { createSelector } from "reselect";
import { USERROLES } from "routes/helper/constants";
import { sessionState } from "./session_mfa.selector";
import { hasAccessFromHarness } from "helper/helper";

export const sessionRBACSelector = createSelector(sessionState, state => {
  const rbac: string = get(state, ["session_rbac"], "");
  return !!rbac ? rbac.toLowerCase() : USERROLES.ADMIN || hasAccessFromHarness();
});
