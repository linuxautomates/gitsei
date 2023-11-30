import { getBaseUrl } from "../../constants/routePaths";
import AuthService from "../../services/authService";
import {
  SESSION_ERROR,
  SESSION_LOAD,
  SESSION_LOGOUT,
  SESSION_MFA,
  SESSION_MFA_CLEAR,
  SESSION_MFA_ENROLL,
  SESSION_MFA_ENROLL_CLEAR,
  SESSION_PROFILE,
  SESSION_CURRENT_USER,
  CLEAR_SESSION_ERROR
} from "../actions/actionTypes";

const as = new AuthService();
const localState = as.getResponse();

const INITIAL_STATE = {
  ...{
    session_error: false,
    session_error_message: "",
    session_token: null,
    session_username: null,
    session_user_id: null,
    session_first_name: null,
    session_last_name: null,
    session_company: null,
    session_iat: null,
    session_eat: null,
    session_rbac: "",
    session_default_route: getBaseUrl(),
    reset_requested: false
  },
  ...localState
};

const sessionReducer = (state = INITIAL_STATE, action) => {
  let as = new AuthService();
  switch (action.type) {
    case SESSION_PROFILE:
      // console.log(action);
      return {
        ...state,
        session_token: action.token,
        session_username: action.username,
        session_first_name: action.first_name,
        session_last_name: action.last_name,
        session_company: action.company,
        session_error: false,
        session_error_message: "",
        session_rbac: action.rbac,
        session_user_id: action.id,
        session_default_route: action.default_route,
        application_restrictions: action.application_restrictions
      };
    case SESSION_CURRENT_USER:
      return {
        ...state,
        session_current_user: action.payload
      };
    case SESSION_LOAD:
      let session = as.getResponse();
      console.log(`Loading session ${session}`);
      return { ...state, ...session };
    case SESSION_ERROR:
      return { ...INITIAL_STATE, session_error: true, session_error_message: action.message };
    case SESSION_LOGOUT:
      console.log(`Logging out of session`);
      as.clearPrevLocation();
      as.logout();
      return {
        session_error: false,
        session_error_message: "",
        session_token: null,
        session_username: null,
        session_first_name: null,
        session_last_name: null,
        session_company: null,
        session_iat: null,
        session_eat: null,
        session_rbac: ""
      };
    case SESSION_MFA:
      const hasMFAError = action?.payload?.error;
      if (!!hasMFAError) {
        return { ...state, session_mfa: { error: action?.payload?.error } };
      }
      return { ...state, session_mfa: { ...(state?.session_mfa || {}), ...(action?.payload || {}) } };
    case SESSION_MFA_ENROLL:
      const hasMFAEnrollError = action?.payload?.error;
      const method = action?.method; // get => code,qr post => enrollment verification
      if (!!hasMFAEnrollError) {
        return {
          ...state,
          session_mfa_enroll: { ...(state?.session_mfa_enroll || {}), [method]: { error: action?.payload?.error } }
        };
      }
      return {
        ...state,
        session_mfa_enroll: {
          ...(state?.session_mfa_enroll || {}),
          [method]: { ...(state?.session_mfa_enroll?.[method] || {}), ...(action?.payload || {}) }
        }
      };
    case SESSION_MFA_CLEAR:
      return {
        ...state,
        session_mfa: {}
      };
    case SESSION_MFA_ENROLL_CLEAR:
      if (action?.method) {
        return {
          ...state,
          session_mfa_enroll: { ...(state?.session_mfa_enroll || {}), [method]: {} }
        };
      }
      return {
        ...state,
        session_mfa_enroll: {}
      };
    case CLEAR_SESSION_ERROR:
      return {
        ...state,
        session_error: false,
        session_error_message: ""
      };
    default:
      return state;
  }
};

export default sessionReducer;
