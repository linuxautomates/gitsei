import {
  DEFAULT_ROUTE,
  RBAC,
  TOKEN_EAT,
  TOKEN_IAT,
  TOKEN_KEY,
  USER_EMAIL,
  USER_FIRST_NAME,
  USER_ID,
  USER_LAST_NAME,
  USER_NAME,
  USER_ORG,
  SESSION_SELECTED_WORKSPACE_ID,
  NEW_INTEGRATION_ADDED
} from "../constants/localStorageKeys";

export default class LocalStoreService {
  /*
    This class will interact with the local redux and get / set values
     */

  _getKey(key) {
    return localStorage.getItem(key);
  }

  _getSessionKey(key) {
    return sessionStorage.getItem(key);
  }

  _setKey(key, value) {
    return localStorage.setItem(key, value);
  }

  _setSessionKey(key, value) {
    return sessionStorage.setItem(key, value);
  }

  _removeSessionKey(key) {
    return sessionStorage.removeItem(key);
  }

  getUserName() {
    //return this._getKey(USER_NAME);
    let rbac = this._getKey(RBAC);
    if (rbac !== null && rbac.toLowerCase() !== "admin") {
      return this.getUserEmail();
    }
    let firstName = this._getKey(USER_FIRST_NAME);
    let lastName = this._getKey(USER_LAST_NAME);
    return `${firstName} ${lastName}`;
  }

  getFirstName() {
    return this._getKey(USER_FIRST_NAME);
  }

  getUserCompany() {
    return this._getKey(USER_ORG);
  }

  getUserEmail() {
    return this._getKey(USER_EMAIL);
  }

  getUserRbac() {
    return this._getKey(RBAC);
  }

  getUserId() {
    return this._getKey(USER_ID);
  }

  getToken() {
    return this._getKey(TOKEN_KEY);
  }

  getTokenExpiry() {
    return this._getKey(TOKEN_EAT);
  }

  getTokenIssued() {
    return this._getKey(TOKEN_IAT);
  }

  getDefaultRoute() {
    return this._getKey(DEFAULT_ROUTE);
  }

  getSelectedWorkspaceId() {
    return this._getKey(SESSION_SELECTED_WORKSPACE_ID);
  }

  setDefaultRoute(route) {
    return this._setKey(DEFAULT_ROUTE, route);
  }

  setUserName(name) {
    return this._setKey(USER_NAME, name);
  }

  setUserId(id) {
    return this._setKey(USER_ID, id);
  }

  setUserEmail(email) {
    return this._setKey(USER_EMAIL, email);
  }

  setToken(token) {
    return this._setKey(TOKEN_KEY, token);
  }

  setTokenExpiry(expiry) {
    return this._setKey(TOKEN_EAT, expiry);
  }

  setSelectedWorkspaceId(workspaceId) {
    return this._setKey(SESSION_SELECTED_WORKSPACE_ID, workspaceId);
  }

  setNewIntegrationAdded(added) {
    return this._setSessionKey(NEW_INTEGRATION_ADDED, added);
  }

  getNewIntegrationAdded() {
    return this._getSessionKey(NEW_INTEGRATION_ADDED);
  }

  removeNewIntegrationAdded() {
    return this._removeSessionKey(NEW_INTEGRATION_ADDED);
  }
}
