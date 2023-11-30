import { restapiState } from "reduxConfigs/selectors/restapiSelector.js";
import { get } from "lodash";
import { createSelector } from "reselect";
import { createParameterSelector } from "./selector";
import { EntityIdentifier } from "types/entityIdentifier";
import { RestUsers } from "classes/RestUsers";

const getUserId = createParameterSelector((params: any) => params.user_id);

export const userCreateState = (state: any) => {
  return get(state.restapiReducer, ["users", "create"], {});
};

export const userDeleteState = (state: any) => {
  return get(state.restapiReducer, ["users", "delete"], {});
};

export const userGetState = (state: any) => {
  return get(state.restapiReducer, ["users", "get"], {});
};

export const userUpdateState = (state: any) => {
  return get(state.restapiReducer, ["users", "update"], {});
};

export const userProfileUpdateState = createSelector(restapiState, (state: any) => {
  return get(state, ["user_profile", "update", "0"], {});
});

export const userGetOrCreateState = (state: any) => {
  return get(state.restapiReducer, ["users", "getOrCreate"], {});
};

export const usersSelectorState = (state: any) => {
  return {
    create: userCreateState(state),
    delete: userDeleteState(state),
    get: userGetState(state),
    update: userUpdateState(state),
    getOrCreate: userGetOrCreateState(state)
  };
};

export const usersSelector = createSelector(usersSelectorState, data => data);

export const meProfileSelector = createSelector(restapiState, state => state?.users?.me?.[0]?.data || {});

const _userProfileGetState = createSelector(restapiState, state => get(state, ["user_profile", "get"], {}));
export const userProfileData = createSelector(_userProfileGetState, state => get(state, ["0", "data"], {}));

export const getUserUpdateState = createSelector(
  userUpdateState,
  getUserId,
  (state: any, id: EntityIdentifier) => state?.[id] || {}
);

export const userTrellisUpdateState = createSelector(restapiState, state =>
  get(state, ["trellis_user_permission", "update"], {})
);
