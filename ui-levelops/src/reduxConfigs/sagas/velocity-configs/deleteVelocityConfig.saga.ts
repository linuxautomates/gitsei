import { notification } from "antd";
import { filter, map } from "lodash";
import { call, put, select, takeLatest } from "redux-saga/effects";
import { VELOCITY_CONFIG_DELETE } from "reduxConfigs/actions/actionTypes";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { restapiEffectSaga } from "../restapiSaga";
import { VELOCITY_CONFIG_LIST_ID, velocityConfigsRestListSelector } from "../../selectors/velocityConfigs.selector";

const uri: string = "velocity_configs";

function* deleteVelocityConfigSaga(action: any): any {
  const { id } = action;

  try {
    const configs = yield select(velocityConfigsRestListSelector, {
      id: VELOCITY_CONFIG_LIST_ID
    });

    yield call(restapiEffectSaga, { uri, method: "delete", id });
    const _configs = filter(
      map(configs, config => config.json),
      config => config.id !== id
    );

    yield put(genericRestAPISet(_configs, uri, "list", VELOCITY_CONFIG_LIST_ID));
    notification.success({ message: "Profile Deleted successfully" });
  } catch (e) {
    console.error("Failed to delete profile", e);
  }
}

export function* deleteVelocityConfigSagaWatcher() {
  yield takeLatest(VELOCITY_CONFIG_DELETE, deleteVelocityConfigSaga);
}
