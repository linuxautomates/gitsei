import { issueContextTypes, severityTypes } from "bugsnag";
import { RestOrganizationUnit } from "classes/RestOrganizationUnit";
import { basicActionType } from "dashboard/dashboard-types/common-types";
import { handleError } from "helper/errorReporting.helper";
import { call, put, select, takeLatest } from "redux-saga/effects";
import { OU_SCORE_OVERVIEW } from "reduxConfigs/actions/actionTypes";
import { OrganizationUnitGet } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { orgUnitGetRestDataSelect } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { restapiEffectSaga } from "../restapiSaga";

function* ouScoreOverviewEffectSaga(action: basicActionType<any>) {
  try {
    yield put(OrganizationUnitGet(action?.id, { isAllOrgUsersRequired: false, pageSize: 1000000 }));
    yield call(restapiEffectSaga, {
      uri: "dev_productivity_org_unit_score_report",
      method: "list",
      id: action?.id,
      data: action.data
    });
    const orgUnit: RestOrganizationUnit = yield select(orgUnitGetRestDataSelect, { id: action.id });
    const tags = orgUnit.tags ?? [];
    yield call(restapiEffectSaga, {
      uri: "tags",
      id: action?.id,
      method: "list",
      data: {
        filter: {
          tag_ids: tags
        }
      }
    });
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.TRELLIS,
        data: { e, action }
      }
    });
  }
}

export function* ouScoreOverviewWatcherSaga() {
  yield takeLatest([OU_SCORE_OVERVIEW], ouScoreOverviewEffectSaga);
}
