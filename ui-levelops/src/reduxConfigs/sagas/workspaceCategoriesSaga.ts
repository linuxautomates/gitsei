import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { get } from "lodash";
import { call, put, takeEvery } from "redux-saga/effects";
import { GET_WORKSPACE_CATEGORIES } from "reduxConfigs/actions/actionTypes";
import { setWorkspaceState } from "reduxConfigs/actions/workspaceActions";
import { WorkspaceDataType } from "reduxConfigs/reducers/workspace/workspaceTypes";
import { WorkspaceCategoriesService } from "services/restapi/workspace.service";

export function* workspaceCategoriesSaga(action: any): Generator<Record<any, any>> {
  const { id, method, data } = action;
  const workspaceService = new WorkspaceCategoriesService();
  let payload: WorkspaceDataType = {
    loading: false,
    error: false,
    data: ""
  };
  try {
    yield put(setWorkspaceState(method, id, { loading: true, error: false, data: "" }));
    const response: any = yield call(
      workspaceService.list,
      { filter: { enabled: true } },
      action.workspaceId,
      action.dashboardId
    );
    payload.data = response;
    if (response.hasOwnProperty("data")) {
      payload.data = get(response, ["data"], []);
    }
    yield put(setWorkspaceState(method, id, payload));
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WORKSPACE,
        data: { e, action }
      }
    });
    payload.error = true;
    yield put(setWorkspaceState(method, id, payload));
  }
}

export function* workspaceCategoriesSagaWatcher() {
  yield takeEvery(GET_WORKSPACE_CATEGORIES, workspaceCategoriesSaga);
}
