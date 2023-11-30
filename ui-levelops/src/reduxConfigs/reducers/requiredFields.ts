import { SET_REQUIRED_FIELD_ERROR } from "../actions/actionTypes";
import { createReducer } from "../createReducer";

const INITIAL_STATE = {
  is_required_error_field: false,
  required_field_msg: ""
};

const setRequiredField = (state: any, { payload }: any) => {
  return {
    ...state,
    is_required_error_field: payload.is_required_error_field,
    required_field_msg: payload.required_field_msg
  };
};

const requiredFieldReducer = createReducer(INITIAL_STATE, {
  [SET_REQUIRED_FIELD_ERROR]: setRequiredField
});

export default requiredFieldReducer;
