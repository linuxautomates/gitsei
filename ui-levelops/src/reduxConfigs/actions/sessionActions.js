import {
  CHANGE_PASSWORD,
  RESET_PASSWORD,
  SESSION_ERROR,
  SESSION_LOAD,
  SESSION_LOGIN,
  SESSION_LOGOUT,
  SESSION_PROFILE,
  SESSION_REFRESH,
  SESSION_SSO_LOGIN,
  SESSION_MFA,
  SESSION_CURRENT_USER,
  SESSION_GET_ME,
  CURRENT_USER_ENTITLEMENTS,
  CLEAR_SESSION_ERROR
} from "./actionTypes";

export const sessionLogin = (username, password, company, otp, rbac = "admin") => ({
  type: SESSION_LOGIN,
  email: username,
  password: password,
  company: company,
  otp,
  rbac: rbac
});

export const sessionLogout = () => ({ type: SESSION_LOGOUT });

export const sessionLoad = () => ({ type: SESSION_LOAD });
export const sessionErrorClear = () => ({ type: CLEAR_SESSION_ERROR });

export const sessionProfile = (
  token,
  userid,
  username,
  firstName,
  lastName,
  company,
  rbac = "admin",
  defaultRoute,
  application_restrictions
) => ({
  type: SESSION_PROFILE,
  token: token,
  username: username,
  first_name: firstName,
  last_name: lastName,
  company: company,
  rbac: rbac,
  id: userid,
  default_route: defaultRoute,
  application_restrictions
});

export const sessionCurrentUser = (currentUser = {}) => ({
  type: SESSION_CURRENT_USER,
  payload: currentUser
});

export const sessionEntitlements = (currentUserEnt = []) => {
  return {
    type: CURRENT_USER_ENTITLEMENTS,
    payload: currentUserEnt
  };
};

export const sessionError = (error = "Invalid username or password") => ({ type: SESSION_ERROR, message: error });

export const sessionRefresh = () => ({ type: SESSION_REFRESH });

export const sessionSSOLogin = token => ({ type: SESSION_SSO_LOGIN, token: token });

export const resetPassword = (username, company) => ({ type: RESET_PASSWORD, company: company, username: username });

export const sessionResetPasswordAction = (params, meta) => ({
  type: CHANGE_PASSWORD,
  payload: params,
  history: meta
});

export const sessionGetMe = () => ({ type: SESSION_GET_ME });
