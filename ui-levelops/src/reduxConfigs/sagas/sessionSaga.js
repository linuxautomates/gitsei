import { sessionError, sessionProfile } from "../actions/sessionActions";
import {
  CHANGE_PASSWORD,
  RESET_PASSWORD,
  SESSION_LOGIN,
  SESSION_SSO_LOGIN,
  SESSION_LOGOUT,
  CLEAR_STORE
} from "../actions/actionTypes";
import { passwordReset, resetPasswordReq, restLogin } from "../../utils/restRequest";
import AuthService from "../../services/authService";
import BackendService from "../../services/backendService";
import { addError } from "../actions/errorActions";
import { call, put, takeLatest } from "redux-saga/effects";
import { push } from "react-router-redux";
import decode from "jwt-decode";
import { getBaseUrl, SIGN_IN_PAGE } from "constants/routePaths";
import { notification } from "antd";

function getDefaultRoute() {
  return getBaseUrl();
}

function* loginEffectSaga(action) {
  try {
    let response = yield call(restLogin, action.username, action.password, action.company);
    let bs = new BackendService();
    let data = response.data;
    response.data.first_name = "";
    response.data.last_name = "";
    let as = new AuthService();
    let decoded = decode(response.data.token);
    if (as.storeResponse(response)) {
      as.clearError();
      let userResponse = yield call(bs.getProfile);
      const defaultRoute = getDefaultRoute(userResponse, decoded.user_type);
      data.first_name = userResponse.data.first_name;
      data.last_name = userResponse.data.last_name;
      data.user_id = userResponse.data.id;
      data.default_route = defaultRoute;

      as.storeResponse(response);
      yield put(
        sessionProfile(
          response.data.token,
          userResponse.data.id,
          decoded.sub,
          response.data.firstName,
          response.data.lastName,
          decoded.company,
          decoded.user_type,
          getHomePage(),
          dat.application_restrictions
        )
      );
    } else {
      yield put(push("/"));
      yield put(sessionError());
    }
  } catch (e) {
    if (e.hasOwnProperty("response") && e.response.status === 401) {
      yield put(sessionError());
    } else {
      let error = { error: true, error_header: "Error", error_message: e.toString() };
      yield put(addError(error));
    }
    yield put(push("/"));
  }
}

function* ssoEffectSaga(action) {
  try {
    // first take the jwt token, decode it and make sense
    let as = new AuthService();
    let bs = new BackendService();
    let decoded = decode(action.token);
    // craft a json payload that looks like what a authenticate would return, but with
    // first name and last name empty
    let payload = {
      data: {
        token: action.token,
        first_name: "",
        last_name: ""
      }
    };
    as.storeResponse(payload);
    // second, make a user/me call with the jwt token and get first name and last name
    console.log("trying to get profile");
    let response = yield call(bs.getProfile);
    const defaultRoute = getDefaultRoute(response, decoded.user_type);
    console.log("SSO token");
    console.log(decoded);

    payload.data.first_name = response.data.first_name;
    payload.data.last_name = response.data.last_name;
    payload.data.user_id = response.data.id;
    payload.data.default_route = defaultRoute;
    payload.data.application_restrictions = response.data.application_restrictions;
    console.log(payload);
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
  } catch (e) {
    console.log(e);
    if (e === undefined) {
      let error = { error: true, error_header: "Error", error_message: "Undefined" };
      yield put(addError(error));
    } else {
      if (e.hasOwnProperty("response")) {
        //&& e.response.status === 401
        let eMsg = "Invalid username or password";
        if (e.response.status !== 401) {
          eMsg = e.toString();
        }
        yield put(sessionError(eMsg));
      } else {
        let error = { error: true, error_header: "Error", error_message: e.toString() };
        yield put(addError(error));
      }
    }
    yield put(push("/"));
  }
}

function* passwordResetEffectSaga(action) {
  try {
    // eslint-disable-next-line no-unused-vars
    let response = yield call(passwordReset, action.username, action.company);
  } catch (e) {
    if (e === undefined) {
      let error = { error: true, error_header: "Error", error_message: "Undefined" };
      yield put(addError(error));
    } else {
      if (e.hasOwnProperty("response")) {
        //&& e.response.status === 401
        let eMsg = "Unable to initiate password reset";
        if (e.response.status !== 401) {
          eMsg = e.toString();
        }
        yield put(sessionError(eMsg));
      } else {
        let error = { error: true, error_header: "Error", error_message: e.toString() };
        yield put(addError(error));
      }
    }
  }
}

function* changePasswordEffectSaga(action) {
  try {
    yield call(resetPasswordReq, action.payload);
    const authService = new AuthService();
    if (authService.loggedIn()) {
      authService.logout();
    }
    notification.success({ message: "Password change successful" });
    action.history.push(SIGN_IN_PAGE);
  } catch (e) {
    if (e === undefined) {
      let error = { error: true, error_header: "Error", error_message: "Undefined" };
      yield put(addError(error));
    } else {
      if (e.response) {
        let eMsg = "Unable to change password";
        if (e.response.status === 410) {
          eMsg = "Password link has expired";
        }
        if (e.response.status !== 401 && e.response.status !== 410) {
          eMsg = e.toString();
        }
        yield put(sessionError(eMsg));
      } else {
        let error = { error: true, error_header: "Error", error_message: e.toString() };
        yield put(addError(error));
      }
    }
  }
}

function* sessionLogoutEffectSaga(action) {
  yield put({ type: CLEAR_STORE });
}

export function* ssoWatcherSaga() {
  yield takeLatest([SESSION_SSO_LOGIN], ssoEffectSaga);
}

export function* sessionWatcherSaga() {
  yield takeLatest([SESSION_LOGIN], loginEffectSaga);
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
