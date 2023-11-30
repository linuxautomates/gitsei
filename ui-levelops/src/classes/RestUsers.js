// Users object to be used by rest api to add / update users

import { cloneDeep, get, set } from "lodash";

export const USER_ROLE_TYPE_ADMIN = "ADMIN";
export const USER_ROLE_TYPE_LIMITED_USER = "LIMITED_USER";
export const USER_ROLE_TYPE_AUDITOR = "AUDITOR";
export const USER_ROLE_TYPE_RESTRICTED_USER = "RESTRICTED_USER";
export const USER_ROLE_TYPE_ASSIGNED_ISSUES_USER = "ASSIGNED_ISSUES_USER";
export const USER_ROLE_TYPE_PUBLIC_DASHBOARD = "PUBLIC_DASHBOARD";

export class RestUsers {
  static TYPES = Object.freeze([
    USER_ROLE_TYPE_ADMIN,
    USER_ROLE_TYPE_LIMITED_USER,
    USER_ROLE_TYPE_AUDITOR,
    USER_ROLE_TYPE_RESTRICTED_USER,
    USER_ROLE_TYPE_ASSIGNED_ISSUES_USER,
    USER_ROLE_TYPE_PUBLIC_DASHBOARD
  ]);
  constructor(restData) {
    this._rbac = 1;
    this._username = undefined;
    this._firstName = undefined;
    this._lastName = undefined;
    this._password = undefined;
    this._samlAuth = undefined;
    this._passwordAuth = undefined;
    this._userType = undefined;
    this._notifyUser = true;
    this._mfa_enabled = undefined;
    this._mfa_enrollment_end = undefined;
    this._mfa_reset_at = undefined;
    this._mfa_enforced = undefined;
    this._managed_ou_ref_ids = [];
    this._scopes = {};
    this._metadata = {};

    if (restData) {
      this._username = restData?.email;
      this._firstName = restData?.first_name;
      this._lastName = restData?.last_name;
      this._samlAuth = restData?.saml_auth_enabled;
      this._passwordAuth = restData?.password_auth_enabled;
      this._userType = restData?.user_type;
      if (restData?.notify_user !== undefined) {
        this._notifyUser = restData?.notify_user;
      }
      if (restData?.mfa_enabled !== undefined) {
        this._mfa_enabled = restData?.mfa_enabled;
      }
      if (restData?.mfa_enrollment_end !== undefined) {
        this._mfa_enrollment_end = restData?.mfa_enrollment_end;
      }
      if (restData?.mfa_reset_at !== undefined) {
        this._mfa_reset_at = restData?.mfa_reset_at;
      }
      if (restData?.mfa_enforced !== undefined) {
        this._mfa_enforced = restData?.mfa_enforced;
      }
      if (restData?.metadata !== undefined) {
        this._metadata = restData?.metadata;
      }
      if (restData?.scopes !== undefined) {
        this._scopes = restData?.scopes;
      }

      if (restData?.managed_ou_ref_ids) {
        this._managed_ou_ref_ids = restData?.managed_ou_ref_ids;
      }
    }
    this.transformedMetadata = this.transformedMetadata.bind(this);
    this.getManagedOURefIds = this.getManagedOURefIds.bind(this);
    this.json = this.json.bind(this);
  }

  get notifyUser() {
    return this._notifyUser;
  }
  set notifyUser(notify) {
    this._notifyUser = notify;
  }

  get username() {
    return this._username;
  }

  set username(email) {
    this._username = email;
  }

  get userType() {
    return this._userType;
  }

  set userType(type) {
    this._userType = type;
  }

  get firstName() {
    return this._firstName;
  }

  set firstName(fn) {
    this._firstName = fn;
  }

  get lastName() {
    return this._lastName;
  }

  set lastName(ln) {
    this._lastName = ln;
  }

  get password() {
    return this._password;
  }

  set password(password) {
    this._password = password;
  }

  get samlAuth() {
    return this._samlAuth;
  }

  set samlAuth(auth) {
    this._samlAuth = auth;
  }

  get passwordAuth() {
    return this._passwordAuth;
  }

  set passwordAuth(auth) {
    this._passwordAuth = auth;
  }

  get mfa_enabled() {
    return this._mfa_enabled;
  }

  set mfa_enabled(val) {
    this._mfa_enabled = val;
  }

  get mfa_enrollment_end() {
    return this._mfa_enrollment_end;
  }

  set mfa_enrollment_end(val) {
    this._mfa_enrollment_end = val;
  }

  get mfa_reset_at() {
    return this._mfa_reset_at;
  }

  set mfa_reset_at(val) {
    this._mfa_reset_at = val;
  }

  get mfa_enforced() {
    return this._mfa_enforced;
  }

  set mfa_enforced(val) {
    this._mfa_enforced = val;
  }

  get scopes() {
    return this._scopes;
  }

  set scopes(val) {
    this._scopes = val;
  }

  get metadata() {
    return this._metadata;
  }

  transformedMetadata() {
    let nMetadata = cloneDeep(this._metadata);
    const workspaces = get(nMetadata, ["workspaces"], {});
    const workspaceValues = Object.values(workspaces);
    let selections = {};
    if (workspaceValues.length) {
      if (Array.isArray(workspaceValues[0])) {
        selections = workspaces;
      } else {
        selections = Object.values(workspaces).reduce((acc, next) => {
          if (next.workspaceId)
            return {
              ...acc,
              [next.workspaceId]: next.orgUnitIds
            };
          return acc;
        }, {});
      }
    }
    if (this.userType === USER_ROLE_TYPE_ADMIN && this.scopes.hasOwnProperty("dev_productivity_write")) {
      selections = {};
    }
    set(nMetadata, ["workspaces"], selections);
    return nMetadata;
  }

  getManagedOURefIds() {
    let nMetadata = cloneDeep(this._metadata);
    const workspaces = get(nMetadata, ["workspaces"], {});
    let ouRefIds = Object.values(workspaces).reduce((acc, next) => {
      if (Array.isArray(next)) return [...acc, ...next];
      if (next?.workspaceId) return [...acc, ...(next?.orgUnitIds ?? [])];
      return acc;
    }, []);
    if (this.userType === USER_ROLE_TYPE_ADMIN && this.scopes.hasOwnProperty("dev_productivity_write")) {
      ouRefIds = [];
    }
    return ouRefIds;
  }

  json() {
    let payload = {
      email: this._username,
      //password: this._password,
      first_name: this._firstName,
      last_name: this._lastName,
      saml_auth_enabled: this._samlAuth,
      password_auth_enabled: this._passwordAuth,
      user_type: this._userType,
      notify_user: this._notifyUser,
      mfa_enabled: this._mfa_enabled,
      mfa_enrollment_end: this._mfa_enrollment_end,
      mfa_reset_at: this._mfa_reset_at,
      mfa_enforced: this._mfa_enforced,
      metadata: this.transformedMetadata(),
      managed_ou_ref_ids: this.getManagedOURefIds(),
      scopes: this._scopes
    };
    if (payload.password === undefined || payload.password === null) {
      delete payload.password;
    }
    return payload;
  }

  static validate(data) {
    let result = true;
    result = result && data.hasOwnProperty("email");
    result = result && data.hasOwnProperty("first_name");
    result = result && data.hasOwnProperty("last_name");
    result = result && data.hasOwnProperty("user_type");
    return result;
  }
}

export class UserList {
  constructor() {
    this._users = [];
  }

  set users(records) {
    let result = [];
    for (let user in records) {
      let obj = new RestUsers();
      obj.username = user.email;
      obj.firstName = user.first_name;
      obj.lastName = user.last_name;
      result.push(obj);
    }
    this._users = result;
  }

  [Symbol.iterator]() {
    return this._users.values();
  }
}
