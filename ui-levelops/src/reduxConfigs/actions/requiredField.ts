import { SET_REQUIRED_FIELD_ERROR } from "./actionTypes";

export const setRequiredFieldError = (payload: any) => ({
  type: SET_REQUIRED_FIELD_ERROR,
  payload
});
