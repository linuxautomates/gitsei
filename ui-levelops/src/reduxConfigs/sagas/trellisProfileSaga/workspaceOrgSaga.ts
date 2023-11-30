import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { call, put, takeEvery } from "redux-saga/effects";
import { trellisProfileActions } from "reduxConfigs/actions/actionTypes";
import { saveWorkspaceOUList } from "reduxConfigs/actions/restapi/trellisProfileActions";
import { _integrationListSelector } from "reduxConfigs/selectors/integrationSelector";
import { OrganizationUnitPivotListService, OrganizationUnitService } from "services/restapi";

function* workspaceOUListEffectSaga(action: any) {
  try {
    const organizationUnitPivotListService = new OrganizationUnitPivotListService();
    const getCategoryFilter = {
      filter: {
        workspace_id: [action.workspaceId ?? ""]
      }
    };

    // @ts-ignore
    const categoryResponse = yield call(organizationUnitPivotListService.list, getCategoryFilter);
    if (categoryResponse.error) {
      yield put(saveWorkspaceOUList(action.workspaceId, []));
      return;
    }
    const ou_category_id = categoryResponse.data.records
      ?.map((category: any) => category.id)
      .filter((categoryId: string) => !!categoryId);

    const organizationUnitService = new OrganizationUnitService();
    const getOUListFilter = {
      filter: {
        ou_category_id
      }
    };

    // @ts-ignore
    const orgResponse = yield call(organizationUnitService.list, getOUListFilter);
    if (categoryResponse.error) {
      yield put(saveWorkspaceOUList(action.workspaceId, []));
      return;
    }
    const orgMap: Map<string, any> = new Map<string, any>();
    orgResponse.data.records.forEach((org: any) => {
      if (!orgMap.has(org.id)) {
        orgMap.set(org.id, org);
      }
    });
    yield put(saveWorkspaceOUList(action.workspaceId, Array.from(orgMap.values())));
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.ORG_UNITS,
        data: { e, action }
      }
    });

    yield put(saveWorkspaceOUList(action.workspaceId, []));
  }
}

export function* workspaceOUListWatcherSaga() {
  yield takeEvery(trellisProfileActions.GET_WORKSPACE_OU_LIST, workspaceOUListEffectSaga);
}
