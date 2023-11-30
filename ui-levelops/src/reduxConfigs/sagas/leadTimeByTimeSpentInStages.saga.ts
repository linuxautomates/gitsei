import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { get, uniqueId } from "lodash";
import { all, call, put, select, takeEvery } from "redux-saga/effects";
import { LEAD_TIME_BY_TIME_SPENT_IN_STAGES } from "reduxConfigs/actions/actionTypes";
import { restapiClear, restapiData, restapiError, restapiLoading } from "reduxConfigs/actions/restapi";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";
import { VELOCITY_CONFIGS, VELOCITY_CONFIG_LIST_ID } from "reduxConfigs/selectors/velocityConfigs.selector";
import { restapiEffectSaga } from "./restapiSaga";

function* leadTimeByTimeSpentInStagesEffectSaga(action: any): any {
  const { uri, method, id, filters } = action;
  const integrationIds: Array<string> = get(filters, ["filter", "integration_ids"], [uniqueId()]);
  const stageDataId = integrationIds.join("_").concat(id);
  try {
    let restState = yield select(restapiState);
    let velocityConfigsList = get(
      restState,
      [VELOCITY_CONFIGS, "list", VELOCITY_CONFIG_LIST_ID, "data", "records"],
      []
    );
    const apiCalls = [{ uri, method, id: stageDataId, filters }];
    if (!velocityConfigsList?.length) {
      apiCalls.push({ uri: VELOCITY_CONFIGS, method: "list", filters: {}, id: VELOCITY_CONFIG_LIST_ID });
    }

    yield all(
      apiCalls.map(apiCall =>
        call(restapiEffectSaga, {
          uri: apiCall.uri,
          method: apiCall.method,
          id: apiCall.id,
          data: apiCall.filters
        })
      )
    );

    restState = yield select(restapiState);
    let stagesData: any[] = get(restState, [uri, method, stageDataId, "data", "records"], []);
    velocityConfigsList = get(restState, [VELOCITY_CONFIGS, "list", VELOCITY_CONFIG_LIST_ID, "data", "records"], []);

    /** initial data transformation */
    const selectedConfigId = get(filters, ["filter", "velocity_config_id"]);
    const selectedConfig = velocityConfigsList?.find((config: { id: string }) => config.id === selectedConfigId);
    const preVelocityStageResults = get(selectedConfig, ["pre_development_custom_stages"], []);
    const postVelocityStageResults = get(selectedConfig, ["post_development_custom_stages"], []);
    const fixedStageResults = get(selectedConfig, ["fixed_stages"], []);
    const allStagesResult = [...preVelocityStageResults, ...postVelocityStageResults, ...fixedStageResults];

    stagesData = stagesData.map((data: any) => {
      const corVelocityStageResult = allStagesResult.find(
        (result: { name: string }) => result?.name?.toLowerCase() === data?.key?.toLowerCase()
      );
      return {
        ...(data ?? {}),
        velocity_stage_result: {
          ...(corVelocityStageResult ?? {})
        }
      };
    });

    /** setting data into the store */
    yield put(restapiData({ records: stagesData }, uri, method, id));
    yield put(restapiClear(uri, method, stageDataId));
    yield put(restapiError(false, uri, method, id));
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });

    yield put(restapiError(true, uri, method, id));
  } finally {
    yield put(restapiLoading(false, uri, method, id));
  }
}

export function* leadTimeByTimeSpentInStagesWatcherSaga() {
  yield takeEvery([LEAD_TIME_BY_TIME_SPENT_IN_STAGES], leadTimeByTimeSpentInStagesEffectSaga);
}
