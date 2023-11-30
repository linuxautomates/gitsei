import { RestWidget } from "classes/RestDashboards";
import { basicActionType } from "dashboard/dashboard-types/common-types";
import { cloneDeep, get } from "lodash";
import { call, put, select, takeEvery } from "redux-saga/effects";
import { JIRA_RELEASE_TABLE_CSV_REPORT } from "reduxConfigs/actions/actionTypes";
import { restapiData, restapiError, restapiErrorCode, restapiLoading } from "reduxConfigs/actions/restapi";
import { restapiEffectSaga } from "reduxConfigs/sagas/restapiSaga";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { getError, getErrorCode } from "utils/loadingUtils";

function* jiraReleaseTableReportSaga(action: basicActionType<any>): any {
    const widget: RestWidget = yield select(getWidget, { widget_id: action?.id });
    let filters = cloneDeep(action.data);
    const JIRA_RELASE_TABLE_APICALL_ID = `JIRA_RELASE_TABLE_APICALL_ID_${action.id}`;

    yield call(restapiEffectSaga, {
        id: JIRA_RELASE_TABLE_APICALL_ID,
        uri: action.uri,
        method: "list",
        data: filters
    });

    let restState = yield select(restapiState);
    const records = get(
        restState,
        [action.uri, action.method, JIRA_RELASE_TABLE_APICALL_ID, "data", "records"],
        []
    );

    let apiError = false,
        errorCode = "";
    if (getError(restState, action.uri, action.method, action.id)) {
        errorCode = getErrorCode(restState, action.uri, action.method, action.id);
        apiError = true;
    }
    if (apiError) {
        yield put(restapiError(true, action.uri, action.method, action.id));
        yield put(restapiErrorCode(errorCode, action.uri, action.method, action.id as any));
        yield put(restapiLoading(false, action.uri, action.method, action.id));
        return;
    }
    yield put(
        restapiData(
            { records: { apidata: records } },
            action.uri,
            action.method,
            action.id
        )
    );

}


export function* jiraReleaseTableReportWatcherSaga() {
    yield takeEvery([JIRA_RELEASE_TABLE_CSV_REPORT], jiraReleaseTableReportSaga);
}
