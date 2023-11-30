import { CURRENT_USER_ENTITLEMENTS } from "../actions/actionTypes";

const INITIAL_STATE = {
  current_user_entitlements: []
};

const entitlementsReducer = (state = INITIAL_STATE, action: any) => {
  switch (action.type) {
    case CURRENT_USER_ENTITLEMENTS:
      return {
        ...state,
        current_user_entitlements: [...(state.current_user_entitlements || []), ...action.payload]
      };
    default:
      return state;
  }
};

export default entitlementsReducer;
