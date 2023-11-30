import { issueContextTypes, severityTypes } from "bugsnag";
import { jiraEpicPriorityDataTransformer } from "custom-hooks/helpers";
import { BA_TIME_RANGE_FILTER_KEY } from "dashboard/constants/bussiness-alignment-applications/constants";
import { handleError } from "helper/errorReporting.helper";
import { forEach, set, unset } from "lodash";
import { get } from "lodash";
import moment from "moment";
import { call, put, select, takeEvery, all } from "redux-saga/effects";
import { JIRA_EPIC_PRIORITY_REPORT } from "reduxConfigs/actions/actionTypes";
import { restapiData, restapiError, restapiLoading } from "reduxConfigs/actions/restapi";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { slugifyId } from "utils/stringUtils";
import { restapiEffectSaga } from "./restapiSaga";
import { getBATimeRanges, getMappedSelectedTimeRange, timeRangeToUnitMap } from "./saga-helpers/BASprintReport.helper";

const restapiState = (state: any) => state.restapiReducer;

const getApiCalls = (method: string, filters: any, epicList: any[], timeRangeList: any[], interval: string) => {
  let apiCalls: any[] = [];
  const timeRangeLength = (timeRangeList || []).length;
  const completed_at = {
    $gt: timeRangeLength ? `${timeRangeList[0]?.start_date}` : `${moment().unix()}`,
    $lt: timeRangeLength ? `${timeRangeList[timeRangeLength - 1]?.end_date}` : `${moment().unix()}`
  };
  const integrationIds = get(filters, ["filter", "integration_ids"], undefined);
  forEach(epicList, (epic: any) => {
    const id = slugifyId(epic?.key);
    const teamAllocationFilters = {
      across: "trend",
      interval,
      filter: {
        epics: [epic?.key],
        integration_ids: integrationIds,
        assignees_range: completed_at
      }
    };
    const epicTrendFilters = {
      across: "trend",
      interval,
      filter: {
        key: epic?.key,
        integration_id: epic?.integration_id
      }
    };
    const calls = [
      { data: teamAllocationFilters, uri: "team_allocation_report", method, id },
      { data: epicTrendFilters, uri: "epic_priority_trend_report", method, id }
    ];
    apiCalls.push(...calls);
  });
  return apiCalls;
};

function* getInitialTransformedData(method: string, epicList: any[]): any {
  const restState = yield select(restapiState);
  let apiDataList: any[] = [];
  forEach(epicList, record => {
    const id = slugifyId(record?.key);
    const teamAllocationRecords = get(restState, ["team_allocation_report", method, id, "data", "records"], []);
    const priorityRecords = get(restState, ["epic_priority_trend_report", method, id, "data", "records"], []);
    apiDataList.push({
      [record?.key || ""]: {
        team_allocations: teamAllocationRecords,
        priority: priorityRecords
      },
      summary: record?.summary || ""
    });
  });
  return apiDataList;
}

const getTimeRangeInterval = (selectedTimeRange: { $lt: string; $gt: string } | string) => {
  const mappedTimeRange =
    typeof selectedTimeRange !== "string" ? getMappedSelectedTimeRange(selectedTimeRange) : selectedTimeRange;
  return timeRangeToUnitMap[mappedTimeRange || ""] || "";
};

function* jiraEpicPriorityReportEffectSaga(action: any): any {
  const actionId = action.id;
  const JIRA_EPIC_REGULAR_APICALL_ID = `JIRA_EPIC_REGULAR_APICALL_ID_${actionId}`;
  const selectedTimeRange = get(action.data, ["filter", BA_TIME_RANGE_FILTER_KEY], undefined);
  unset(action.data, ["filter", BA_TIME_RANGE_FILTER_KEY]);
  const maxCalls = get(action.extra, ["maxRecords"], 5);
  try {
    let apiCallsToClear: any[] = [];
    let filters = {
      ...(action?.data || {}),
      filter: {
        ...get(action.data, ["filter"], {}),
        issue_types: ["EPIC"]
      }
    };
    const epics: string[] = get(filters, ["filter", "epics"], []);
    if (epics.length) {
      set(filters, "filter.keys", epics);
      unset(filters, ["filter", "epics"]);
    }

    const epicPriorityApiCallObj = {
      data: filters,
      uri: action?.uri,
      method: action?.method,
      id: JIRA_EPIC_REGULAR_APICALL_ID
    };

    yield call(restapiEffectSaga, epicPriorityApiCallObj);
    let restState = yield select(restapiState);
    const epicListData = get(restState, [action?.uri, action?.method, JIRA_EPIC_REGULAR_APICALL_ID, "data"], {});
    let epicList = get(epicListData, ["records"], []);
    epicList = epicList.slice(0, Math.min(maxCalls, epicList.length));
    const timeRangeList: any[] = selectedTimeRange ? getBATimeRanges(selectedTimeRange) || [] : [];
    const interval = getTimeRangeInterval(selectedTimeRange);
    const apiCalls = getApiCalls(action.method, action.data, epicList, timeRangeList, interval);

    yield all(apiCalls.map(curCall => call(restapiEffectSaga, curCall)));

    const initialTransformedDataRecords = yield getInitialTransformedData(action.method, epicList);

    restState = yield select(restapiState);

    const resultData = jiraEpicPriorityDataTransformer(timeRangeList, initialTransformedDataRecords);

    yield put(restapiData({ records: resultData }, action.uri, action.method, actionId));
    yield put(restapiError(false, action.uri, action.method, action.id));
    yield put(restapiLoading(false, action.uri, action.method, actionId));

    apiCallsToClear.push(epicPriorityApiCallObj, ...apiCalls);
    yield all(apiCallsToClear.map(call => put(genericRestAPISet({}, call.uri, call.method, call.id))));
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });

    yield put(restapiError(true, action.uri, action.method, action.id));
    yield put(restapiLoading(false, action.uri, action.method, actionId));
  }
}

export function* jiraEpicPriorityReportWatcherSaga() {
  yield takeEvery([JIRA_EPIC_PRIORITY_REPORT], jiraEpicPriorityReportEffectSaga);
}
