import { get } from "lodash";
import { put, select, takeLatest, take, call } from "redux-saga/effects";
import { VELOCITY_CONFIG_GET } from "reduxConfigs/actions/actionTypes";
import { VELOCITY_CONFIG_BASIC_TEMPLATE_FIXED_STAGE } from "../../selectors/velocityConfigs.selector";
import { velocityConfigsBasicTemplateGet } from "../../actions/restapi/velocityConfigs.actions";
import { restapiState } from "../../selectors/restapiSelector";
import RestapiService from "../../../services/restapiService";
import { restapiData, restapiLoading } from "../../actions/restapi/restapiActions";

const uri: string = "velocity_configs";

function* getVelocityConfigSaga(action: any): any {
  const { id } = action;

  try {
    let restService: any = new RestapiService();
    let response;

    // @ts-ignore
    yield put(restapiLoading(true, action.uri, action.method, id.toString()));

    const dynamicFn = restService[action.uri][action.method];
    if (!dynamicFn) {
      console.error("Failed to call API. Function is not defined");
    }

    response = yield call(dynamicFn, action.id);

    const completeBasicTemplate = `velocity_configs_basic_template_${id}_complete`;

    yield put(velocityConfigsBasicTemplateGet(VELOCITY_CONFIG_BASIC_TEMPLATE_FIXED_STAGE, completeBasicTemplate));

    yield take(completeBasicTemplate);

    const state = yield select(restapiState);
    let configs = response;
    let existingStages = get(configs, ["data", "fixed_stages"], []);
    const configsBasicTemplateStages = get(
      state,
      [uri, "baseConfig", VELOCITY_CONFIG_BASIC_TEMPLATE_FIXED_STAGE, "data", "fixed_stages"],
      []
    );
    if (existingStages.length > 0) {
      existingStages = configsBasicTemplateStages
        .filter((stage: any) => {
          // filter Lead time to first commit if starting_event_is_commit_created
          if (
            stage.name === "Lead time to first commit" &&
            get(configs, ["data", "starting_event_is_commit_created"], false)
          ) {
            return false;
          }
          return true;
        })
        .map((stage: any) => {
          let _stage = existingStages.find((item: any) => item.order === stage.order);
          if (_stage) {
            _stage.enabled = true;
          } else {
            _stage = stage;
            _stage.enabled = false;
          }
          return _stage;
        });

      configs.data.fixed_stages = existingStages;
    } else {
      configs.data.fixed_stages_enabled = false;
      configs.data.fixed_stages = configsBasicTemplateStages;
    }

    yield put(restapiData({ ...(configs.data || {}) }, uri, "get", id));

    // @ts-ignore
    yield put(restapiLoading(false, action.uri, action.method, id.toString()));

    if (action.hasOwnProperty("complete") && action.complete !== null) {
      yield put({ type: action.complete });
    }
  } catch (e) {
    console.error("Failed to delete profile", e);
  }
}

export function* getVelocityConfigSagaWatcher() {
  yield takeLatest(VELOCITY_CONFIG_GET, getVelocityConfigSaga);
}
