import { get } from "lodash";
import { call, put, select, takeEvery } from "redux-saga/effects";
import { SET_SESSION_SELECTED_WORKSPACE, WORKSPACE_READ } from "reduxConfigs/actions/actionTypes";
import { setSelectedWorkspace, setWorkspaceState } from "reduxConfigs/actions/workspaceActions";
import {
  WorkspaceDataType,
  WorkspaceModel,
  WorkspaceResponseType
} from "reduxConfigs/reducers/workspace/workspaceTypes";
import { SELECTED_WORKSPACE_ID } from "reduxConfigs/selectors/workspace/constant";
import { getSelectedWorkspace } from "reduxConfigs/selectors/workspace/workspace.selector";
import LocalStoreService from "services/localStoreService";
import { WorkspaceService } from "services/restapi/workspace.service";
import { isSelfOnboardingUser } from "reduxConfigs/selectors/session_current_user.selector";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";

export function* workspaceEffectSaga(action: any) {
  let { id, method, data } = action;
  const workspaceService = new WorkspaceService();
  let payload: WorkspaceDataType = {
    loading: false,
    error: false,
    data: ""
  };

  try {
    yield put(setWorkspaceState(method, id, { loading: true, error: false, data: "" }));
    const trialTenent: boolean = yield select(isSelfOnboardingUser);
    if (trialTenent && method === "list") {
      data = {
        ...(data ?? {}),
        filter: {
          ...get(data, ["filter"], {}),
          demo: true
        }
      };
    }

    const response: WorkspaceResponseType = !!data
      ? yield call((workspaceService as any)[method], data, id)
      : yield call((workspaceService as any)[method], id);
    payload.data = response;
    if (response.hasOwnProperty("data")) {
      payload.data = get(response, ["data"], {});
    }
    yield put(setWorkspaceState(method, id, payload));
  } catch (e) {
    handleError({
      bugsnag: {
        message: data?.message || (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WORKSPACE,
        data: { e, action }
      }
    });

    payload.data = data;
    payload.error = true;
    yield put(setWorkspaceState(method, id, payload));
  }
}

function* selectedWorkspaceEffectSaga(action: Record<string, string>) {
  const ls = new LocalStoreService();
  let workspaceId = ls.getSelectedWorkspaceId();
  try {
    const selectedWorkspace: WorkspaceModel = yield select(getSelectedWorkspace);
    const { queryParamWorkspaceId = undefined } = action;
    if (queryParamWorkspaceId || (workspaceId && !Object.keys(selectedWorkspace).length)) {
      workspaceId = queryParamWorkspaceId ? queryParamWorkspaceId : workspaceId;
      const workspaceService = new WorkspaceService();
      const workspace: { data: WorkspaceModel } = yield call(workspaceService.get, workspaceId || "");
      yield put(setSelectedWorkspace(SELECTED_WORKSPACE_ID, workspace?.data ?? {}));
    }
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WORKSPACE,
        data: { e }
      }
    });
  }
}

export function* WorkspaceSagaWatcher() {
  yield takeEvery(WORKSPACE_READ, workspaceEffectSaga);
}

export function* SelectedWorkspaceSagaWatcher() {
  // @ts-ignore
  yield takeEvery(SET_SESSION_SELECTED_WORKSPACE, selectedWorkspaceEffectSaga);
}
