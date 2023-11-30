import LocalStoreService from "../../services/localStoreService";
import { actionChannel, call, fork, put, take, takeEvery, all, select } from "redux-saga/effects";
import { notification } from "antd";
import {
  setEntity,
  restapiData,
  restapiError,
  restapiErrorCode,
  restapiLoading,
  clearWidgetsHistory,
  setSelectedEntity,
  getDataUpdateDashboardWidgets
} from "../actions/restapi";
import * as actions from "../actions/actionTypes";
import BackendService from "../../services/backendService";
import RestapiService from "../../services/restapiService";
import AuthService from "services/authService";
import FileSaver from "file-saver";
import { notifyRestAPIError } from "bugsnag";
import { uniq, get } from "lodash";
import { processDashboardApiResponse } from "utils/dashboardFilterUtils";
import { restapiClear, setEntities } from "../actions/restapi/restapiActions";
import { trimPreviewFromId } from "shared-resources/containers/sprint-api-wrapper/sprintApiHelper";
import { customTimeFilterKeysSelector, velocityConfiglistSelector } from "reduxConfigs/selectors/jira.selector";
import { REQUEST_TIMEOUT_ERROR, ECONNABORTED } from "../../shared-resources/helpers/server-error/constant";
import { _integrationListSelector } from "../selectors/integrationSelector";
import { SIGN_IN_PAGE } from "constants/routePaths";
import queryString from "query-string";
import {
  API_AUTH_DISABLED_ERROR_MESSAGE,
  JIRA_RELEASE_WRONG_PROFILE_ERROR_MESSAGE,
  NEW_AUTH_DISABLED_ERROR_MESSAGE
} from "reduxConfigs/constants/restapiSaga.constant";
import { orgUnitSelector } from "reduxConfigs/selectors/orgUsersSelector";
import { getSelectedWorkspace } from "reduxConfigs/selectors/workspace/workspace.selector";
import { cachedIntegrationEffectSaga } from "./integrations/cachedIntegrationSaga";
import { cachedIntegrationsListSelector } from "reduxConfigs/selectors/CachedIntegrationSelector";
import { updateDashboardOUSaga } from "./dashboards/updateSelectedDashboard.saga";
import contentDisposition from "content-disposition";
import { invalidateHasAllIntFlag } from "../actions/cachedIntegrationActions";
import { ROLLBACK_KEY } from "configurations/pages/Organization/Filters/harnessng/harnessng-job-filter.config";
import { getParamOuArray } from "helper/openReport.helper";

const as = new AuthService();

const IGNORE_LOGOUT_URI_UNAUTH = ["self_onboarding_repos"];

function _getFileName(disposition) {
  let result = contentDisposition.parse(disposition);
  return result.parameters.filename;
}

function readAsBinary(blob) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      resolve(reader.result);
    };
    reader.onerror = reject;
    reader.readAsDataURL(blob);
  });
}

export function* handleRestApiError(e, action, response = {}) {
  let id = action?.id || "0";
  let statusCode = 0;
  const errorMessage = e ? e?.toString() : e;
  const error = {
    error: true,
    error_header: `REST API Error ${action?.uri}`,
    error_message: errorMessage,
    id: `${action?.uri}-${action?.method}`
  };

  if (e?.code?.trim() === ECONNABORTED) {
    statusCode = REQUEST_TIMEOUT_ERROR;
    yield put(restapiError(true, action?.uri, action?.method, id.toString()));
    yield put(restapiErrorCode(statusCode, action?.uri, action?.method, id.toString()));
    yield put(restapiLoading(false, action?.uri, action?.method, id.toString()));
    return;
  }

  if (e?.request?.status === 401 && !IGNORE_LOGOUT_URI_UNAUTH.includes(action.uri) && window.isStandaloneApp) {
    as.logout();
    window.location.href = SIGN_IN_PAGE;
    return;
  }

  if (e?.request?.status > 0) {
    statusCode = e.request.status;
  } else if (e && e?.response && e?.response?.status) {
    statusCode = e.response.status;
  } else if (errorMessage.includes("Network Error")) {
    statusCode = 502;
  }

  // Report to bugsnag
  notifyRestAPIError(e, action, response);

  if (action?.method === "generic") {
    if (id !== "0") {
      yield put(setEntity({ error: e || "" }, action.uri, id.toString()));
    }
  } else {
    yield put(restapiError(true, action?.uri, action?.method, id.toString()));
    yield put(restapiErrorCode(statusCode, action?.uri, action?.method, id.toString()));
    yield put(restapiData((e?.response && e?.response?.data) || {}, action?.uri, action?.method, id.toString()));
    yield put(restapiLoading(false, action?.uri, action?.method, id.toString()));
  }

  if (e?.request?.status === 400) {
    error.error_message = e?.response?.data?.message || "";
    if (error.error_message !== JIRA_RELEASE_WRONG_PROFILE_ERROR_MESSAGE) {
      notification.error({
        message: error?.error_header,
        description:
          error?.error_message === API_AUTH_DISABLED_ERROR_MESSAGE
            ? NEW_AUTH_DISABLED_ERROR_MESSAGE
            : error?.error_message,
        key: error?.id
      });
    }
  }

  if (statusCode / 100 === 5) {
    // COMMENT THIS BECAUSE IT RUNS MULTIPLE TIME & MADE PAGE UNRESPONSIVE FOR SEI-2099
    // notification.error({
    //   message: "Internal server issue occurred."
    // });
  } else if (statusCode === 409) {
    notification.error({
      message: "Conflict: Key must be unique."
    });
  } else if (statusCode === 403) {
    if (action?.showNotfication !== false) {
      notification.error({
        message: "Only the creator of the dashboard can edit settings"
      });
    }
  }

  if (action?.hasOwnProperty("complete") && action?.complete !== null) {
    yield put({ type: action.complete });
  }
}

export function* newRestEffectSaga(action) {
  let response;

  try {
    const id = action.id;
    let restService = new RestapiService();

    const dynamicFn = restService[action.uri][action.function];
    if (!dynamicFn) {
      console.error("Failed to call API. Function is not defined");
    }

    if (action.setState && id) {
      yield put(setEntity(action.setState, action.uri, id.toString()));
    }

    response = yield call(dynamicFn, action.payload);

    if (id) {
      yield put(setEntity(response.data, action.uri, id.toString()));
    }

    if (action.hasOwnProperty("complete") && action.complete !== null) {
      // console.log("Sending back complete action");
      // console.log(action.complete);
      yield put({ type: action.complete });
    }
  } catch (e) {
    yield call(handleRestApiError, e, action, response);
  }
}

export function* restapiEffectSaga(action) {
  if (action.method === "generic") {
    yield call(newRestEffectSaga, action);
    return;
  }

  // for every action, if loading is already set to true for that id,
  // then dont do anything, especially for the changes
  let id = action.id || "0";
  let restService = new RestapiService();
  let bs = new BackendService();
  let response;
  let proceedWithAPICall = true;
  const ls = new LocalStoreService();
  const userRole = ls.getUserRbac();

  // CONVERT ROLLBACK KEY AS BOOLEN FOR API CALL IN THIS FIVE WIDGET
  if (
    [
      "jobs_count_report",
      "pipelines_jobs_duration_report",
      "jobs_duration_report",
      "pipelines_jobs_count_report",
      "pipelines_jobs_duration_report",
      "pipeline_job_runs",
      "cicd_scm_job_runs_tickets"
    ].includes(action.uri)
  ) {
    if (action?.data?.filter && action?.data?.filter.hasOwnProperty(ROLLBACK_KEY)) {
      action.data.filter = {
        ...action?.data?.filter,
        [ROLLBACK_KEY]: action?.data?.filter[ROLLBACK_KEY] === "true"
      };
    }

    if (action?.data?.filter?.exclude && action?.data?.filter?.exclude.hasOwnProperty(ROLLBACK_KEY)) {
      action.data.filter = {
        ...action?.data?.filter,
        exclude: {
          ...action?.data?.filter.exclude,
          [ROLLBACK_KEY]: action?.data?.filter.exclude[ROLLBACK_KEY] === "true"
        }
      };
    }
  }

  // first set the state to loading and set the cancel token for the call
  if (action.set_loading !== false) {
    yield put(restapiLoading(true, action.uri, action.method, id.toString()));
    if (action.isWidget) {
      yield put(
        restapiLoading(true, action.uri, action.method, `${trimPreviewFromId(id.toString())}-preview`)
      );
    }
  }
  try {
    const restFunction = bs[action.function] || restService[action.uri][action.method];

    /**
     * In a special case, PUBLIC_DASHBOARD user can make local changes to the dashboard; however, apis call to update
     * should not be made
     */
    if (action.uri === "dashboards" && action.method === "update" && userRole === "PUBLIC_DASHBOARD") {
      proceedWithAPICall = false;
    }

    if (proceedWithAPICall) {
      switch (action.method) {
        case "bulk":
        case "list": {
          response = yield call(restService[action.uri][action.method], action.data, action?.queryparams);
          break;
        }
        case "head":
          response = yield call(restService[action.uri][action.method], action.id);
          break;
        case "get":
          if (!!action?.queryparams) {
            response = yield call(restService[action.uri][action.method], action.id, action?.queryparams);
          } else {
            response = yield call(restService[action.uri][action.method], action.id);
          }
          if (action.hasOwnProperty("validator")) {
            let valid = action.validator.validate(response.data);
            if (!valid) {
              throw new Error(`Schema validation failed for ${action.type}`);
            }
          }
          break;
        case "delete":
          response = yield call(restService[action.uri][action.method], action.id);
          break;
        case "bulkDelete":
          response = yield call(restService[action.uri][action.method], action.payload);
          break;
        case "send":
        case "create":
          response = yield call(restFunction, action.data);
          break;
        case "values":
        case "patch":
        case "update":
        case "idList":
          response = yield call(restService[action.uri][action.method], action.id, action.data);
          break;
        case "aggregate":
        case "series":
          response = yield call(bs[action.function], action.data);
          break;
        case "search":
          response = yield call(restService[action.uri][action.method], action.data);
          break;
        case "upload":
          response = yield call(restService[action.uri][action.method], action.id, action.file, action.data);
          break;
        case "diff":
          response = yield call(restService[action.uri][action.method], action.before, action.after);
          break;
        case "trigger":
          response = yield call(restService[action.uri][action.method], action.id, action.data);
          break;
        case "me":
          response = yield call(restService[action.uri][action.method]);
          break;
        case "setDefault":
          response = yield call(restService[action.uri][action.method], action.id);
          break;
        case "baseConfig":
          response = yield call(restService[action.uri][action.method], action.id);
          break;
        default:
          break;
      }
    }

    if (action.uri === "integrations" && ["delete", "create", "bulkDelete"].includes(action.method)) {
      yield put(invalidateHasAllIntFlag());
    }

    // rest call succeeded, so set the data and set loading to false
    // console.log(`setting restapi data for ${action.type}`);
    if ((action.uri === "files" && action.method === "get") || action.uri === "assessment_download") {
      let downloadFileName = response.headers.hasOwnProperty("content-disposition")
        ? _getFileName(response.headers["content-disposition"])
        : action.file_name;
      if (action.download === true || action.uri === "assessment_download") {
        if (action.filters && action.filters.view === true) {
          const url = window.URL.createObjectURL(new Blob([response.data], { type: "application/pdf" }));
          window.open(url);
        } else {
          FileSaver.saveAs(response.data, downloadFileName);
        }
        yield put(restapiData("", action.uri, action.method, id.toString()));
      } else {
        const content = yield call(readAsBinary, response.data);
        yield put(restapiData(content, action.uri, action.method, id.toString()));
      }
    } else if (action.uri === "files" && action.method === "head") {
      yield put(restapiData(response.request.responseURL, action.uri, action.method, id.toString()));
    } else if (action.uri === "sonarqube_tickets") {
      const integration_ids = uniq(response.data.records.map(record => record.integration_id));

      const idCalls = integration_ids.map(id => call(restService["integrations"]["get"], id));
      const integrationResponse = yield all(idCalls);

      const integrationData = integrationResponse.reduce(
        (acc, integration) => ({ ...acc, [integration.data.id]: integration.data.url }),
        {}
      );

      const updatedResponse = {
        ...response.data,
        records: response.data.records.map(record => ({ ...record, base_url: integrationData[record.integration_id] }))
      };
      yield put(restapiData(updatedResponse, action.uri, action.method, id.toString()));
    } else if (action.uri === "dashboards" && (action.method === "get" || action.method === "update")) {
      // THIS WILL USE IN OPEN REPORT
      const queryParamOUArray = getParamOuArray(window.location.href);
      const OU =
        action?.OU ??
        queryString.parseUrl(window.location.href, { parseFragmentIdentifier: true })?.query?.OU ??
        queryParamOUArray?.[0];
      const dashboardData = action.method === "get" ? response : { data: action.data };
      let integration_ids = dashboardData?.data?.demo
        ? []
        : get(dashboardData, ["data", "query", "integration_ids"], []);
      if (OU && !dashboardData?.data?.demo && action.method === "get") {
        const key = `${OU}_integrations`;
        const orgUnitState = yield select(orgUnitSelector);
        const orgUnitData = get(orgUnitState, [key, "data", "sections"], []);
        integration_ids = orgUnitData.reduce((acc, section) => {
          const ids = Object.keys(section?.integrations || {});
          return [...acc, ...ids];
        }, []);
        if (!integration_ids?.length) {
          const selectedWorkspace = yield select(getSelectedWorkspace);
          integration_ids = get(selectedWorkspace, ["integration_ids"], []).map(id => id?.toString());
        }
        yield put(restapiClear("organization_unit_management", "get", key));
      }

      yield call(updateDashboardOUSaga, {
        uri: "integrations",
        method: "list",
        uuid: "integrations_custom_field_data",
        integrations: integration_ids
      });
      const VELOCITY_COMPLETE = "VELOCITY_COMPLETE";
      yield put(getDataUpdateDashboardWidgets({ velocity_complete: VELOCITY_COMPLETE }, integration_ids));
      yield take(VELOCITY_COMPLETE);
      const velocityData = yield select(velocityConfiglistSelector);
      yield call(cachedIntegrationEffectSaga, { payload: { method: "list", integrationIds: integration_ids } });
      const integrations = yield select(cachedIntegrationsListSelector, {
        integration_ids: integration_ids
      });
      const customTimeFilterKeys = yield select(customTimeFilterKeysSelector);
      const updatedResponseData = processDashboardApiResponse(
        dashboardData,
        customTimeFilterKeys,
        velocityData,
        integrations,
        integration_ids
      );
      const widgets = get(updatedResponseData, ["widgets"], []);
      updatedResponseData.widgets = widgets.map(w => w.id);
      yield put(clearWidgetsHistory());
      yield put(setEntities(widgets, "widgets"));
      yield put(setSelectedEntity("selected-dashboard", updatedResponseData));
      yield put(restapiData(updatedResponseData, action.uri, "get", id.toString()));
      yield put(restapiData(updatedResponseData, action.uri, "update", id.toString()));
    } else {
      yield put(restapiData(response.data, action.uri, action.method, id.toString()));
      if (action.isWidget) {
        yield put(restapiData(response.data, action.uri, action.method, `${trimPreviewFromId(id.toString())}-preview`));
      }
    }
    yield put(restapiError(false, action.uri, action.method, id.toString()));
    if (action.isWidget) {
      yield put(restapiError(false, action.uri, action.method, `${trimPreviewFromId(id.toString())}-preview`));
    }
    if (action.set_loading !== false) {
      yield put(restapiLoading(false, action.uri, action.method, id.toString(), null));
      if (action.isWidget) {
        yield put(
          restapiLoading(false, action.uri, action.method, `${trimPreviewFromId(id.toString())}-preview`, null)
        );
      }
    }

    if (action.hasOwnProperty("complete") && action.complete !== null) {
      // console.log("Sending back complete action");
      // console.log(action.complete);
      yield put({ type: action.complete });
    }
  } catch (e) {
    yield call(handleRestApiError, e, action, response);
  }
}

export function* restapiChangeWatcherSaga() {
  yield takeEvery([actions.RESTAPI_CALL], restapiEffectSaga);
}

export function* watchRequests() {
  // create a channel to queue incoming requests
  const chan = yield actionChannel([actions.RESTAPI_CALL, actions.RESTAPI_READ]);

  // create 3 worker 'threads'
  for (let i = 0; i < 20; i++) {
    yield fork(handleRequest, chan);
  }
}

export function* watchWriteRequests() {
  // create a channel to queue incoming requests
  const chan = yield actionChannel([actions.RESTAPI_WRITE]);

  // create 3 worker 'threads'
  for (let i = 0; i < 6; i++) {
    yield fork(handleRequest, chan);
  }
}

function* handleRequest(chan) {
  while (true) {
    const action = yield take(chan);
    yield call(restapiEffectSaga, action);
  }
}
