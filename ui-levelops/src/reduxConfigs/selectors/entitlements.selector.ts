import { createSelector } from "reselect";
import { get } from "lodash";
import { setStoredEntitlements } from "custom-hooks/helpers/entitlements.helper";

export const entitlementsState = (state: any) => state.entitlementsReducer;

export const userEntitlementsState = createSelector(entitlementsState, (user: any) => {
  let entitlements = get(user, ["current_user_entitlements"], []);
  setStoredEntitlements(entitlements);
  return entitlements;
});

export const userEntitlements = createSelector(userEntitlementsState, (user: Array<string>) => {
  return user;
});
