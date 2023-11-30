import { all, put, select, take, takeLatest } from "redux-saga/effects";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { USERS_GET_OR_CREATE } from "../actions/actionTypes";
import { RestUsers } from "../../classes/RestUsers";
import { get } from "lodash";

const restapiState = state => state.restapiReducer;

export function* getOrCreateUsersEffectSage(action) {
  const URI = "users";
  const METHOD = "getOrCreate";
  const ID = action.id;
  let error = false;

  try {
    const { users } = action;
    if (!users || users.length === 0) {
      return;
    }

    yield put(actionTypes.configsList({}, "default_rbac", `COMPLETE_DEFAULT_RBAC`));
    yield take(`COMPLETE_DEFAULT_RBAC`);
    const rbacState = yield select(restapiState);
    const configs = get(rbacState, ["configs", "list", "default_rbac", "data", "records"], []);
    const rbacConfig = configs.find(c => c.name === "AUTO_PROVISIONED_ROLE");
    const samlConfig = configs.find(c => c.name === "DEFAULT_SAML_ENABLED");
    const pwdConfig = configs.find(c => c.name === "DEFAULT_PASSWORD_ENABLED");
    let rbac = "LIMITED_USER";
    let saml = false;
    let pwd = false;
    if (rbacConfig) {
      rbac = rbacConfig.value;
    }
    if (samlConfig) {
      saml = samlConfig.value === "true";
    }
    if (pwdConfig) {
      pwd = pwdConfig.value === "true";
    }
    const toCreateUserNames = users.filter(tag => tag.includes("create:"));

    if (toCreateUserNames.length > 0) {
      const usersToCreate = toCreateUserNames.map(
        user =>
          new RestUsers({
            email: user.replace("create:", ""),
            first_name: user.replace("create:", "").split("@")[0],
            last_name: "N/A",
            user_type: rbac,
            saml_auth_enabled: saml,
            password_auth_enabled: pwd,
            notify_user: false
          })
      );
      yield all(
        usersToCreate.map(user => put(actionTypes.usersCreate(user, user.username, `COMPLETE_${user.username}`)))
      );
      yield all(usersToCreate.map(user => take(`COMPLETE_${user.username}`)));
      const state = yield select(restapiState);
      const newlyCreatedUsers = usersToCreate.map(user => ({
        id: get(state, ["users", "create", user.username, "data", "id"], undefined),
        email: user.username
      }));
      yield put(actionTypes.restapiData(newlyCreatedUsers, URI, METHOD, ID));
      if (action.hasOwnProperty("complete") && action.complete !== null) {
        yield put({ type: action.complete });
      }
      return;
    }
    if (action.hasOwnProperty("complete") && action.complete !== null) {
      yield put({ type: action.complete });
    }
  } catch (e) {
    error = true;
  } finally {
    yield put(actionTypes.restapiError(error, URI, METHOD, ID));
    yield put(actionTypes.restapiLoading(false, URI, METHOD, ID));
  }
}

export function* getOrCreateUsersWatcherSaga() {
  yield takeLatest([USERS_GET_OR_CREATE], getOrCreateUsersEffectSage);
}
