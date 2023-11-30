import { createSelector } from "reselect";
import { restapiState } from "./restapiSelector";
import { get } from "lodash";
import { createParameterSelector } from "./selector";

export const ORG_USERS = "org_users";
export const ORG_USERS_FILTER = "org_users_filter";

export const ORG_USER_LIST_ID = "org-user-list";
export const ORG_USER_SCHEMA_ID = "org-user-schema";
export const ORG_USER_DELETE_ID = "org-user-list-delete";

const getID = createParameterSelector((params: any) => params.id || "0");
const getMethod = createParameterSelector((params: any) => params.method || "0");
const getUri = createParameterSelector((params: any) => params.uri || "0");

export const orgUsersGenericSelector = createSelector(
  restapiState,
  getUri,
  getMethod,
  getID,
  (data: any, uri: string, method: string, id: string) => {
    return get(data, [uri, method, id], { loading: true.valueOf, error: false });
  }
);
export const orgUserState = createSelector(restapiState, state => {
  return get(state, [ORG_USERS], {});
});

export const orgUserGetState = createSelector(orgUserState, state => {
  return get(state, ["get"], { loading: true, error: false });
});

export const orgUserUpdateState = createSelector(orgUserState, state => {
  return get(state, ["update"], { loading: true, error: false });
});

export const orgUserDeleteState = createSelector(orgUserState, state => {
  return get(state, ["bulkDelete"], { loading: true, error: false });
});

export const orgUserListSelector = createSelector(orgUserState, state => {
  return get(state, ["list"], { loading: true, error: true });
});

export const orgUserGetId = (teamID: string) => {
  return createSelector(orgUserGetState, state => {
    return get(state, [teamID], { loading: true, error: true });
  });
};

export const orgUserCreateState = createSelector(orgUserState, state => {
  return get(state, ["create"], { loading: true, error: false });
});

export const orgUsersFilterState = createSelector(restapiState, state => {
  return get(state, [ORG_USERS_FILTER], { loading: true, error: true });
});

export const orgUnitSelector = createSelector(restapiState, state => {
  return get(state, ["organization_unit_management", "get"], {});
});
