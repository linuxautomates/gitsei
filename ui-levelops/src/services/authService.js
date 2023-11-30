import { restLogin, restRefresh } from "../utils/restRequest";
import decode from "jwt-decode";
import Cookies from "js-cookie";
import {
  TOKEN_KEY,
  USER_ID,
  USER_NAME,
  TOKEN_IAT,
  TOKEN_EAT,
  USER_ORG,
  USER_FIRST_NAME,
  USER_LAST_NAME,
  ERROR,
  RBAC,
  PREV_LOCATION,
  PREV_QUERY,
  USER_EMAIL,
  DEFAULT_ROUTE,
  APPLICATION_RESTRICTIONS,
  SESSION_SELECTED_WORKSPACE_ID
} from "../constants/localStorageKeys";
import { getBaseUrl, SIGN_IN_PAGE } from "constants/routePaths";
import envConfig from "env-config";

const AUTH_MODE = envConfig.get("APP_MODE");

export default class AuthService {
  constructor() {
    this.login = this.login.bind(this);
    this.logout = this.logout.bind(this);
    this.setToken = this.setToken.bind(this);
    this.getToken = this.getToken.bind(this);
    this.loggedIn = this.loggedIn.bind(this);
    this.login = this.login.bind(this);
    this.refresh = this.refresh.bind(this);
    this.isRefreshNeeded = this.isRefreshNeeded.bind(this);
    this.clearError = this.clearError.bind(this);
    this.clearPrevLocation = this.clearPrevLocation.bind(this);
  }

  getToken(key = TOKEN_KEY) {
    return localStorage.getItem(key);
  }

  setToken(idToken, key) {
    localStorage.setItem(key, idToken);
  }

  loggedIn(sessionToken = null) {
    // Checks if there is a saved token and it's still valid
    let token = undefined;
    if (sessionToken !== null) {
      token = decode(sessionToken);
      //return true;
    } else {
      token = this.getToken(); // GEtting token from localstorage
      console.debug("Checking for logged in");
      console.debug(token);
      // TODO handle expiry here and potential refresh issues
      //return !!token
    }
    if (AUTH_MODE === "test") {
      //console.log("This is test environment, not checking expiry");
      return !!token;
    }
    let returnValue = !!token && !this.isTokenExpired(token["exp"]);
    // console.log(`Login check is ${returnValue}`);
    return !!token && !this.isTokenExpired(token["exp"]); // handwaiving here
  }

  logout() {
    if (!window.isStandaloneApp) return;
    // Clear user token and profile data from localStorage
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(TOKEN_IAT);
    localStorage.removeItem(TOKEN_EAT);
    localStorage.removeItem(USER_NAME);
    localStorage.removeItem(USER_ORG);
    localStorage.removeItem(USER_LAST_NAME);
    localStorage.removeItem(USER_LAST_NAME);
    localStorage.removeItem(USER_ID);
    localStorage.removeItem(RBAC);
    localStorage.removeItem(USER_EMAIL);
    localStorage.removeItem(DEFAULT_ROUTE);
    localStorage.removeItem(APPLICATION_RESTRICTIONS);
    localStorage.removeItem(SESSION_SELECTED_WORKSPACE_ID);
    //localStorage.removeItem(PREV_LOCATION);
    //localStorage.removeItem(PREV_QUERY);
    console.log(localStorage.getItem(TOKEN_KEY));
    Cookies.remove("token", { path: "" });
    Cookies.remove("token");
    Cookies.remove("token", { path: "/", domain: ".levelops.io" });
  }

  clearPrevLocation() {
    localStorage.removeItem(PREV_LOCATION);
    localStorage.removeItem(PREV_QUERY);
  }

  getUser() {
    const user = localStorage.getItem(USER_NAME);
    return user;
  }

  storeResponse(response) {
    console.log("[response]", response);
    console.debug("Login response received");
    // validate the response here
    if (!response.data.hasOwnProperty("token")) {
      console.log("token missing");
      //return Promise.reject("Invalid response");
      return false;
    }
    console.debug(response.data.token);
    let decoded;
    try {
      decoded = decode(response.data.token);
    } catch (e) {
      if (window.isStandaloneApp) {
        this.logout();
        window.location.href = SIGN_IN_PAGE;
      }
    }

    // validate the token fields here
    console.debug(decoded);
    if (
      !decoded.hasOwnProperty("sub") ||
      !decoded.hasOwnProperty("iat") ||
      !decoded.hasOwnProperty("exp") ||
      !decoded.hasOwnProperty("company") ||
      !decoded.hasOwnProperty("user_type")
    ) {
      console.log("token decode issue");
      //return Promise.reject("Invalid jwt token");
      return false;
    }
    this.setToken("", ERROR);
    this.setToken(response.data.token, TOKEN_KEY);
    this.setToken(decoded.sub, USER_EMAIL);
    this.setToken(decoded.iat, TOKEN_IAT);
    this.setToken(decoded["exp"], TOKEN_EAT);
    this.setToken(decoded.company, USER_ORG);
    this.setToken(decoded.user_type, RBAC);
    this.setToken(response.data.default_route, DEFAULT_ROUTE);
    this.setToken(response.data.application_restrictions, APPLICATION_RESTRICTIONS);
    if (response.data.hasOwnProperty("first_name")) {
      this.setToken(response.data["first_name"].concat(" ").concat(response.data["last_name"]), USER_NAME);
      this.setToken(response.data["first_name"], USER_FIRST_NAME);
      this.setToken(response.data["last_name"], USER_LAST_NAME);
    }
    if (response.data.hasOwnProperty("user_id")) {
      this.setToken(response.data.user_id, USER_ID);
    }
    return true;
  }

  setRestInstanceToken(data) {
    this.setToken(data.token, TOKEN_KEY);
    this.setToken("", ERROR);
    this.setToken(data.iat, TOKEN_IAT);
    this.setToken(data.exp, TOKEN_EAT);
  }

  storeLoginResponse(data) {
    this.setToken(data.id, USER_ID);
    this.setToken(data.email, USER_EMAIL);
    this.setToken(data.company, USER_ORG);
    this.setToken(data.user_type, RBAC);
    this.setToken(data.default_route, DEFAULT_ROUTE);
    this.setToken(data["first_name"].concat(" ").concat(data["last_name"]), USER_NAME);
    this.setToken(data["first_name"], USER_FIRST_NAME);
    this.setToken(data["last_name"], USER_LAST_NAME);
    this.setToken(data.application_restrictions, APPLICATION_RESTRICTIONS);
  }

  clearError() {
    this.setToken("", ERROR);
  }

  getResponse() {
    // error message set in localstorage is readable only once
    // delete after getResponse
    let error = this.getToken(ERROR) || "";
    //this.clearError();
    let token = this.getToken(TOKEN_KEY);
    let decodedToken = null;
    if (token !== null) {
      try {
        decodedToken = decode(token);
      } catch (e) {
        if (window.isStandaloneApp) {
          this.logout();
          window.location.href = SIGN_IN_PAGE;
        }
      }
    }
    return {
      session_token: this.getToken(TOKEN_KEY) || null,
      session_username: decodedToken === null ? decodedToken : decodedToken.sub,
      session_first_name: this.getToken(USER_FIRST_NAME) || null,
      session_last_name: this.getToken(USER_LAST_NAME) || null,
      session_company: decodedToken === null ? decodedToken : decodedToken.company,
      session_iat: decodedToken === null ? decodedToken : decodedToken.iat,
      session_exp: decodedToken === null ? decodedToken : decodedToken["exp"],
      session_error_message: error,
      session_rbac: decodedToken === null ? "" : decodedToken.user_type,
      session_user_id: this.getToken(USER_ID) || null,
      session_default_route: getBaseUrl(),
      application_restrictions: this.getToken(APPLICATION_RESTRICTIONS) || []
    };
  }

  handleAuthError(error) {
    console.log("Catching login error");
    console.log(error);
    if (error.response) {
      console.log(error.response.status);
    } else if (error.request) {
      console.log(error.request);
    }
    console.log(error.config);
  }

  login(username, password, customer = "") {
    return restLogin(username, password, customer)
      .then(response => {
        let result = this.storeResponse(response);
        return result;
      })
      .catch(error => {
        this.handleAuthError(error);
        return false;
      });
  }

  isRefreshNeeded() {
    if (!window.isStandaloneApp) return false;
    const eat = localStorage.getItem(TOKEN_EAT);
    if (!eat) {
      return true;
    }
    const timeStamp = Math.floor(Date.now()) / 1000;
    // this is 18 seconds
    // total validity is 3 mins
    return eat - timeStamp < 180;
  }

  isTokenExpired(tokenExpiry = null) {
    const eat = tokenExpiry === null ? localStorage.getItem(TOKEN_EAT) : tokenExpiry;
    const timeStamp = Math.floor(Date.now() / 1000);
    return timeStamp > eat;
  }

  refresh() {
    const token = this.getToken();
    return restRefresh(token);
  }
}
