import { notification } from "antd";
import { find, map } from "lodash";
import { call, put, select, takeLatest } from "redux-saga/effects";
import { RestVelocityConfigs } from "classes/RestVelocityConfigs";
import { VELOCITY_CONFIG_SET_TO_DEFAULT } from "reduxConfigs/actions/actionTypes";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { restapiEffectSaga } from "../restapiSaga";
import { VELOCITY_CONFIG_LIST_ID, velocityConfigsRestListSelector } from "../../selectors/velocityConfigs.selector";

const uri: string = "velocity_configs";

function* setToDefaultVelocityConfigSaga(action: any): any {
  const { id } = action;

  try {
    const configs = yield select(velocityConfigsRestListSelector, {
      id: VELOCITY_CONFIG_LIST_ID
    });

    let configToSetDefault = find(configs, (scheme: RestVelocityConfigs) => scheme.id === id);
    let configToRemoveDefault = find(configs, (scheme: RestVelocityConfigs) => scheme.defaultConfig);

    configToSetDefault.defaultConfig = true;

    if (configToRemoveDefault) {
      configToRemoveDefault.defaultConfig = false;
    }

    const _schemes = map(configs, config => config.json);

    yield call(restapiEffectSaga, { uri, method: "setDefault", id });
    yield put(genericRestAPISet(_schemes, uri, "list", VELOCITY_CONFIG_LIST_ID));
    notification.success({ message: "Profile set to default successfully" });
  } catch (e) {
    console.error("Failed to set default profile", e);
  }
}

export function* setToDefaultVelocityConfigSagaWatcher() {
  yield takeLatest(VELOCITY_CONFIG_SET_TO_DEFAULT, setToDefaultVelocityConfigSaga);
}
