import {
  resetPassword,
  sessionError,
  sessionLoad,
  sessionLogin,
  sessionLogout,
  sessionGetMe,
  sessionResetPasswordAction,
  sessionSSOLogin
} from "../actions/sessionActions";
import AuthService from "../../services/authService";
import { sessionCurrentUserLicense, isSelfOnboardingUser } from "reduxConfigs/selectors/session_current_user.selector";

export const mapSessionStatetoProps = state => {
  const as = new AuthService();
  const localState = as.getResponse();
  return {
    session_username: state.sessionReducer.session_username,
    session_first_name: state.sessionReducer.session_first_name,
    session_last_name: state.sessionReducer.session_last_name,
    session_company: state.sessionReducer.session_company,
    session_token: state.sessionReducer.session_token,
    session_error: state.sessionReducer.session_error,
    session_user_id: state.sessionReducer.session_user_id,
    //session_error_message: state.sessionReducer.session_error_message,
    session_rbac: state.sessionReducer.session_rbac,
    session_mfa: state.sessionReducer.session_mfa,
    session_license: sessionCurrentUserLicense(state),
    isSelfOnboardingUser: isSelfOnboardingUser(state),
    application_restrictions: state.sessionReducer.application_restrictions,
    ...localState,
    session_error_message:
      localState.session_error_message !== ""
        ? localState.session_error_message
        : state.sessionReducer.session_error_message
  };
};

export const mapSessionDispatchtoProps = dispatch => {
  return {
    sessionLogin: (username, password, company, otp, rbac = "admin") =>
      dispatch(sessionLogin(username, password, company, otp, rbac)),
    sessionLogout: () => dispatch(sessionLogout()),
    sessionLoad: () => dispatch(sessionLoad()),
    sessionSSOLogin: token => dispatch(sessionSSOLogin(token)),
    sessionError: (error = "Invalid username or password") => dispatch(sessionError(error)),
    resetPassword: (username, company) => dispatch(resetPassword(username, company)),
    sessionResetPassword: (params, meta) => dispatch(sessionResetPasswordAction(params, meta)),
    sessionGetMe: () => dispatch(sessionGetMe())
  };
};
