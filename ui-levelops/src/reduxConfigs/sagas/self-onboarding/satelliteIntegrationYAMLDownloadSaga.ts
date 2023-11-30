import { RestApikey } from "classes/RestApikey";
import FileSaver from "file-saver";
import { get } from "lodash";
import { call, put, select, takeLatest } from "redux-saga/effects";
import { SELF_ONBOARDING_INTEGRATION_YAML_DOWNLOAD } from "reduxConfigs/actions/actionTypes";
import yaml from "js-yaml";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";
import { restapiEffectSaga } from "../restapiSaga";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { getConfigForYAML } from "configurations/pages/self-onboarding/helpers/getConfigForYAML";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { getBaseAPIUrl } from "constants/restUri";
import { SatelliteYAMLInterface } from "configurations/pages/integrations/constants";

function* satelliteIntegrationYAMLDownloadSaga(action: any): any {
  const { payload, integration_id } = action;
  try {
    /** Downloading satellite yaml file*/
    const apikey = new RestApikey({
      name: `${payload?.name} apikey`,
      role: "INGESTION"
    } as any);

    yield call(restapiEffectSaga, {
      uri: "apikeys",
      method: "create",
      data: apikey
    });

    const restState = yield select(restapiState);
    const creatApiKey = get(restState, ["apikeys", "create", "0", "data", "key"]);

    const config: SatelliteYAMLInterface = {
      satellite: {
        tenant: action.tenant,
        api_key: creatApiKey,
        url: getBaseAPIUrl()
      },
      integrations: [{ id: integration_id, ...getConfigForYAML(payload.json()) }]
    };

    if ((payload.json()?.application || "") === "jira") {
      config["jira"] = { allow_unsafe_ssl: true };
    }

    const ymlString = yaml.dump(config, { lineWidth: -1 });
    let file = new File([ymlString], `satellite.yml`, { type: "text/plain;charset=utf-8" });
    FileSaver.saveAs(file);
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.INTEGRATIONS,
        data: { e, action }
      }
    });
  } finally {
    yield put(genericRestAPISet({}, "apikeys", "create", "-1"));
  }
}

export function* satelliteIntegrationYAMLDownloadWatcherSaga() {
  yield takeLatest([SELF_ONBOARDING_INTEGRATION_YAML_DOWNLOAD], satelliteIntegrationYAMLDownloadSaga);
}
