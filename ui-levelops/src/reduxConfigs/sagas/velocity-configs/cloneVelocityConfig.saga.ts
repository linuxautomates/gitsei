import { notification } from "antd";
import { find, get, map } from "lodash";
import { call, put, select, takeLatest } from "redux-saga/effects";
import { RestVelocityConfigs } from "classes/RestVelocityConfigs";
import { VELOCITY_CONFIG_CLONE } from "reduxConfigs/actions/actionTypes";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";
import { restapiEffectSaga } from "../restapiSaga";
import { VELOCITY_CONFIG_LIST_ID, velocityConfigsRestListSelector } from "../../selectors/velocityConfigs.selector";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";

const uri: string = "velocity_configs";

function* cloneVelocityConfigSaga(action: any): any {
  const { id } = action;

  try {
    notification.info({ message: "Cloning Profile..." });
    const configs = yield select(velocityConfigsRestListSelector, {
      id: VELOCITY_CONFIG_LIST_ID
    });

    const configToClone = find(configs, (config: RestVelocityConfigs) => config.id === id);

    if (configToClone) {
      const newConfig = new RestVelocityConfigs();
      newConfig.cloneConfig(configToClone.json);

      yield call(restapiEffectSaga, { uri, method: "create", data: newConfig });

      let restState = yield select(restapiState);

      const newConfigId = get(restState, [uri, "create", "0", "data", "id"], undefined);

      if (newConfigId) {
        yield call(restapiEffectSaga, { uri, method: "get", id: newConfigId });
        restState = yield select(restapiState);

        const clonedConfig = get(restState, [uri, "get", newConfigId, "data"], {});
        const _configs = [clonedConfig, ...map(configs, config => config.json)];

        yield put(genericRestAPISet(_configs, uri, "list", VELOCITY_CONFIG_LIST_ID));
        yield put(genericRestAPISet({}, uri, "create", "0"));
        notification.success({ message: "Profile Cloned successfully" });
      }
    } else {
      notification.error({ message: "Profile not found" });
    }
  } catch (e) {
    handleError({
      showNotfication: true,
      message: "Failed to clone profile",
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.VELOCITY,
        data: { e, action }
      }
    });
  }
}

export function* cloneVelocityConfigSagaWatcher() {
  yield takeLatest(VELOCITY_CONFIG_CLONE, cloneVelocityConfigSaga);
}
