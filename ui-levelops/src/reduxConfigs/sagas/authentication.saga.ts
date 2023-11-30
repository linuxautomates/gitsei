import { notification } from "antd";
import { issueContextTypes, severityTypes } from "bugsnag";
import { getBaseUrl, SIGN_IN_PAGE } from "constants/routePaths";
import { handleError } from "helper/errorReporting.helper";
import decode from "jwt-decode";
import { get, unset } from "lodash";
import { call, put, select, takeLatest } from "redux-saga/effects";
import {
  CHANGE_PASSWORD,
  CLEAR_STORE,
  RESET_PASSWORD,
  SESSION_LOGIN,
  SESSION_LOGOUT,
  SESSION_GET_ME,
  SESSION_MFA_GET,
  SESSION_SSO_LOGIN
} from "reduxConfigs/actions/actionTypes";
import { addError } from "reduxConfigs/actions/errorActions";
import { sessionMFA } from "reduxConfigs/actions/restapi/mfa.action";
import {
  sessionCurrentUser,
  sessionError,
  sessionProfile,
  sessionEntitlements
} from "reduxConfigs/actions/sessionActions";
import { setSelectedWorkspace } from "reduxConfigs/actions/workspaceActions";
import { sessionMFASelector, sessionState } from "reduxConfigs/selectors/session_mfa.selector";
import { SELECTED_WORKSPACE_ID } from "reduxConfigs/selectors/workspace/constant";
import { LoginActionType } from "reduxConfigs/types/actions/login.action";
import { MeResponse } from "reduxConfigs/types/response/me.response";
import { MFAResponse } from "reduxConfigs/types/response/mfa.response";
import AuthService from "services/authService";
import BackendService from "services/backendService";
import { passwordReset, resetPasswordReq, restLogin } from "utils/restRequest";
import { postSuccessfulLogin } from "./saga-helpers/authentication.helper";

function getDefaultRoute() {
  return getBaseUrl();
}

function* setLoginResponse(action: { token: string }) {
  try {
    const { token } = action;
    const authService = new AuthService();
    const BEService = new BackendService();
    const decoded: any = decode(token);
    authService.setRestInstanceToken({ token: token, iat: decoded.iat, exp: decoded.exp });
    yield put(sessionCurrentUser({ loading: true, error: false, data: {} }));
    const user: { data: MeResponse } = yield call(BEService.getProfile);
    const default_route = getDefaultRoute();
    yield put(sessionCurrentUser({ loading: false, error: false, data: get(user, ["data"], {}) }));
    authService.storeLoginResponse({
      company: decoded.company,
      default_route,
      ...(user.data || {})
    });

    yield put(
      sessionProfile(
        token,
        user.data.id,
        user.data.email,
        user.data.first_name,
        user.data.last_name,
        decoded.company,
        user.data.user_type,
        default_route,
        user.data.application_restrictions
      )
    );
    yield put(sessionEntitlements(get(user, ["data", "entitlements"], [])));

    postSuccessfulLogin(user.data);
  } catch (e) {
    console.error("[LOGIN RESPONSE SET ERROR]", e);
    if ((e as any)?.response?.status === 401) {
      yield put(sessionError());
    }
    yield put(sessionMFA({ loading: false, error: (e as any)?.response || "SOMETHING WENT WRONG" }));
  }
}

function* mfaEffectSaga(action: LoginActionType) {
  try {
    yield put(sessionMFA({ loading: true }));
    let mfa: { data: MFAResponse } = yield call(restLogin, action.email, action.password, action.company);
    if (mfa.data.token && !mfa.data.mfa_enrollment) {
      yield call(setLoginResponse, { token: mfa.data.token });
      yield put(sessionMFA({ loading: false }));
      return;
    } else {
      if (mfa.data.token) {
        const authService = new AuthService();
        const decoded: any = decode(mfa.data.token);
        authService.setRestInstanceToken({ token: mfa.data.token, iat: decoded.iat, exp: decoded.exp });
      }
      unset(mfa.data, "token");
      yield put(sessionMFA({ ...mfa.data, loading: false, error: false }));
    }
  } catch (e) {
    console.error("[MFA ERROR]", e);
    handleError({
      showNotfication: true,
      message: "Sorry, there was an issue trying to log in",
      bugsnag: {
        message: "Failed to log in",
        severity: severityTypes.ERROR,
        context: issueContextTypes.AUTHENTICATION,
        data: {
          e,
          email: action.email,
          company: action.company
        }
      }
    });
    if ((e as any)?.response?.status === 401) {
      yield put(sessionError());
    }
    yield put(sessionMFA({ loading: false, error: (e as any)?.response || "SOMETHING WENT WRONG" }));
  }
}

function* loginEffectSaga(action: LoginActionType) {
  try {
    const login: { data: { token: string } } = yield call(
      restLogin,
      action.email,
      action.password,
      action.company,
      action.otp
    );
    const sessionMFAState = select(sessionMFASelector);
    const mfaRequired = get(sessionMFAState, ["mfa_required"], false);

    if (mfaRequired && !action.otp) {
      throw new Error("OTP is Required");
    }
    yield call(setLoginResponse, { token: login.data.token });
  } catch (e) {
    console.error("[LOGIN ERROR]", e);
    handleError({
      showNotfication: true,
      message: "Sorry, there was an issue trying to log in",
      bugsnag: {
        message: "Failed to log in",
        severity: severityTypes.ERROR,
        context: issueContextTypes.AUTHENTICATION,
        data: {
          e,
          email: action.email,
          company: action.company
        }
      }
    });
    if ((e as any)?.response?.status === 401 && !action.otp) {
      yield put(sessionError());
    } else if ((e as any)?.response?.status === 401 && action.otp) {
      yield put(sessionMFA({ loading: false, error: (e as any)?.response || "SOMETHING WENT WRONG" }));
    } else {
      let error = { error: true, error_header: "Error", error_message: (e as any).toString() };
      yield put(addError(error));
    }
  }
}

function* ssoEffectSaga(action: any) {
  try {
    // first take the jwt token, decode it and make sense
    let as = new AuthService();
    let bs = new BackendService();
    let decoded: any = decode(action.token);
    // craft a json payload that looks like what a authenticate would return, but with
    // first name and last name empty
    let payload = {
      data: {
        token: action.token,
        first_name: "",
        last_name: "",
        user_id: "",
        default_route: ""
      }
    };
    as.storeResponse(payload);
    // second, make a user/me call with the jwt token and get first name and last name
    console.log("trying to get profile");
    yield put(sessionCurrentUser({ loading: true, error: false, data: {} }));
    let response: { data: MeResponse } = yield call(bs.getProfile);
    yield put(sessionCurrentUser({ loading: false, error: false, data: get(response, ["data"], {}) }));
    const defaultRoute = getDefaultRoute();
    console.log("SSO token");
    console.log(decoded);

    payload.data.first_name = response.data.first_name;
    payload.data.last_name = response.data.last_name;
    payload.data.user_id = response.data.id;
    payload.data.default_route = defaultRoute;

    // then store everything together and every is one big happy family
    as.storeResponse(payload);
    yield put(
      sessionProfile(
        action.token,
        response.data.id,
        decoded.sub,
        response.data.first_name,
        response.data.last_name,
        decoded.company,
        decoded.user_type,
        defaultRoute,
        response.data.application_restrictions
      )
    );

    postSuccessfulLogin(response.data);
  } catch (e) {
    console.log(e);
    yield put(sessionCurrentUser({ loading: false, error: true, data: {} }));
    handleError({
      showNotfication: true,
      message: "Sorry, there was an issue trying to log in",
      bugsnag: {
        message: "Failed to log in",
        severity: severityTypes.ERROR,
        context: issueContextTypes.AUTHENTICATION,
        data: {
          e,
          email: action.email,
          company: action.company
        }
      }
    });
    if (e === undefined) {
      let error = { error: true, error_header: "Error", error_message: "Undefined" };
      yield put(addError(error));
    } else {
      if ((e as any).hasOwnProperty("response")) {
        //&& e.response.status === 401
        let eMsg = "Invalid username or password";
        if ((e as any).response.status !== 401) {
          eMsg = (e as any).toString();
        }
        yield put(sessionError(eMsg));
      } else {
        let error = { error: true, error_header: "Error", error_message: (e as any).toString() };
        yield put(addError(error));
      }
    }
  }
}

function* passwordResetEffectSaga(action: any) {
  try {
    yield call(passwordReset, action.username, action.company);
  } catch (e) {
    handleError({
      showNotfication: true,
      message: "Sorry, there was an issue trying to log in",
      bugsnag: {
        message: "Failed to log in",
        severity: severityTypes.ERROR,
        context: issueContextTypes.AUTHENTICATION,
        data: {
          e,
          email: action.username,
          company: action.company
        }
      }
    });
    if (e === undefined) {
      let error = { error: true, error_header: "Error", error_message: "Undefined" };
      yield put(addError(error));
    } else {
      if ((e as any).hasOwnProperty("response")) {
        //&& e.response.status === 401
        let eMsg = "Unable to initiate password reset";
        if ((e as any).response.status !== 401) {
          eMsg = (e as any).toString();
        }
        yield put(sessionError(eMsg));
      } else {
        let error = { error: true, error_header: "Error", error_message: (e as any).toString() };
        yield put(addError(error));
      }
    }
  }
}

function* changePasswordEffectSaga(action: any) {
  try {
    yield call(resetPasswordReq, action.payload);
    const authService = new AuthService();
    if (authService.loggedIn()) {
      authService.logout();
    }
    notification.success({ message: "Password change successful" });
    action.history.push(SIGN_IN_PAGE);
  } catch (e) {
    handleError({
      showNotfication: !!(e as any)?.response?.data?.message,
      message: "Sorry, there was an issue trying to log in",
      bugsnag: {
        message: "Failed to log in",
        severity: severityTypes.ERROR,
        context: issueContextTypes.AUTHENTICATION,
        data: {
          e
        }
      }
    });
    if (e === undefined) {
      let error = { error: true, error_header: "Error", error_message: "Undefined" };
      yield put(addError(error));
    } else {
      if ((e as any).response) {
        let eMsg = "Unable to change password";
        if ((e as any).response.status === 410) {
          eMsg = "Password link has expired";
        }
        if ((e as any).response.status !== 401 && (e as any).response.status !== 410) {
          eMsg = (e as any).toString();
        }
        yield put(sessionError(eMsg));
      } else {
        let error = { error: true, error_header: "Error", error_message: (e as any).toString() };
        yield put(addError(error));
      }
    }
  }
}

function* sessionGetMeEffectSaga(): any {
  try {
    const BEService = new BackendService();
    const authService = new AuthService();
    const session = yield select(sessionState);
    const currentUser = get(session, ["session_current_user", "data"], {});
    if (authService.loggedIn()) {
      if (!Object.keys(currentUser).length) {
        yield put(sessionCurrentUser({ loading: true, error: false, data: {} }));
        const user: { data: MeResponse } = yield call(BEService.getProfile);
        const selectedWorkspace = get(user, ["data", "metadata", "selected_workspace"], {});
        if (Object.keys(selectedWorkspace || {})?.length) {
          yield put(setSelectedWorkspace(SELECTED_WORKSPACE_ID, selectedWorkspace));
        }
        yield put(sessionCurrentUser({ loading: false, error: false, data: get(user, ["data"], {}) }));
        yield put(sessionEntitlements(get(user, ["data", "entitlements"], [])));
      }
    } else {
    }
  } catch (e) {
    console.error("[GET ME ERROR]", e);
    handleError({
      showNotfication: false,
      bugsnag: {
        message: (e as any)?.response?.data?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.AUTHENTICATION,
        data: {
          e
        }
      }
    });
    const authService = new AuthService();
    authService.logout();
    // Logout if the get me API fails
    yield put({ type: CLEAR_STORE });
  }
}

function* sessionLogoutEffectSaga() {
  yield put({ type: CLEAR_STORE });
}

export function* ssoWatcherSaga() {
  yield takeLatest([SESSION_SSO_LOGIN], ssoEffectSaga);
}

export function* sessionWatcherSaga() {
  //@ts-ignore
  yield takeLatest([SESSION_LOGIN], loginEffectSaga);
}

export function* sessionMFAWatcherSaga() {
  //@ts-ignore
  yield takeLatest([SESSION_MFA_GET], mfaEffectSaga);
}

export function* passwordResetWatcherSaga() {
  yield takeLatest([RESET_PASSWORD], passwordResetEffectSaga);
}

export function* changePasswordWatcherSaga() {
  yield takeLatest([CHANGE_PASSWORD], changePasswordEffectSaga);
}

export function* sessionLogoutWatcherSaga() {
  yield takeLatest([SESSION_LOGOUT], sessionLogoutEffectSaga);
}

export function* sessionGetMeWatcherSaga() {
  yield takeLatest([SESSION_GET_ME], sessionGetMeEffectSaga);
}
