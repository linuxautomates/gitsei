import { issueContextTypes, severityTypes } from "bugsnag";
import { jiraProgressReportTransformer } from "custom-hooks/helpers";
import { TICKET_CATEGORIZATION_UNIT_FILTER_KEY } from "dashboard/constants/bussiness-alignment-applications/constants";
import { valuesToFilters } from "dashboard/constants/constants";
import { handleError } from "helper/errorReporting.helper";
import { get, map, uniq, unset, forEach } from "lodash";
import { all, call, put, select, takeEvery } from "redux-saga/effects";
import { AZURE_PROGRESS_REPORT } from "reduxConfigs/actions/actionTypes";
import {
  restapiData,
  restapiError,
  restapiLoading,
  restapiClear,
  restapiErrorCode
} from "reduxConfigs/actions/restapi";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { PriorityOrderMapping, PriorityTypes } from "shared-resources/charts/jira-prioirty-chart/helper";
import { sanitizeObjectCompletely } from "utils/commonUtils";
import { getError, getErrorCode } from "utils/loadingUtils";
import { restapiEffectSaga } from "./restapiSaga";

const restapiState = (state: any) => state.restapiReducer;

const getApiCalls = (id: any, filters: any, records: any[]) => {
  const across = filters?.across;
  const fields = records.map(record => record?.key);
  const JIRA_TEAM_ALLOCATION_APICALL_ID = `JIRA_TEAM_ALLOCATION_APICALL_ID_${id}`;
  const apiFilters = sanitizeObjectCompletely({
    ...(filters || {}),
    filter: {
      ...get(filters, ["filter"], {}),
      [get(valuesToFilters, [across], across)]: fields
    }
  });

  return [{ data: apiFilters, uri: "azure_team_allocation", method: "list", id: JIRA_TEAM_ALLOCATION_APICALL_ID }];
};

function* getInitialTransformedData(records: any[], apiCalls: any[]): any {
  const restState = yield select(restapiState);
  const dataList: any = apiCalls.map(call =>
    get(restState, [call.uri, call.method, call.id, "data", "records"], [])
  ) as any;
  const teamAllocationRecords = (dataList?.[0]?.[0]?.[apiCalls[0]?.data?.across]?.records as any) || [];
  const priorityRecords = dataList[1];
  const progressReportRecords = map(records, record => {
    const id = record?.key;
    const team_allocations = (teamAllocationRecords || []).find((record: any) => record?.key === id);
    const priorityObject = (priorityRecords || []).find((record: any) => record?.key === id);
    const priority = get(priorityObject || {}, ["priority"], PriorityTypes.LOW);
    const newRecord = {
      ...(record || {}),
      id,
      team_allocations: team_allocations
        ? get(team_allocations, ["assignees"], []).map((user: any) => ({ name: user }))
        : [],
      priority,
      priority_order: PriorityOrderMapping[priority as PriorityTypes],
      summary: get(priorityObject || {}, ["summary"], "")
    };
    unset(newRecord, ["key"]);
    return newRecord;
  });
  progressReportRecords.sort((x, y) => {
    return x?.priority_order - y?.priority_order;
  });
  return progressReportRecords;
}

function* azureBAProgressReportEffectSaga(action: any): any {
  const actionId = action.id;
  const JIRA_PROGRESS_REGULAR_APICALL_ID = `JIRA_PROGRESS_REGULAR_APICALL_ID_${actionId}`;
  const JIRA_PROGRESS_COMPLETED_APICALL_ID = `JIRA_PROGRESS_COMPLETED_APICALL_ID_${actionId}`;
  const JIRA_EPIC_PRIORITY_APICALL_ID = `JIRA_EPIC_PRIORITY_APICALL_ID_${actionId}`;
  let filters = action.data;
  const maxRecords = get(action.extra, ["maxRecords"], 100);
  const across = filters?.across;
  const unit = get(filters, ["filter", TICKET_CATEGORIZATION_UNIT_FILTER_KEY], "story_point_report");
  unset(filters, ["filter", TICKET_CATEGORIZATION_UNIT_FILTER_KEY]);
  unset(filters, ["filter", "workitem_parent_workitem_types"]);
  try {
    let apiCallsToclear: any[] = [];
    let epicPriorityList: any[] = [];
    let epicKeys: string[] = [];
    let apiError = false,
      errorCode = "";
    const epicPriorityFilters = sanitizeObjectCompletely({
      ...(filters || {}),
      across: "",
      filter: {
        ...get(filters, ["filter"], {}),
        workitem_types: ["Epic"]
      }
    });
    const epicPriorityCallObj = {
      data: epicPriorityFilters,
      uri: "issue_management_list",
      method: "list",
      id: JIRA_EPIC_PRIORITY_APICALL_ID
    };
    if (across && across === "epic") {
      yield call(restapiEffectSaga, epicPriorityCallObj);
      let apiState = yield select(restapiState);

      /* if any api call fails, get error and errorcode and dispatch error so error handling can be handled in BAWidgetAPi Wrapper */
      forEach(epicPriorityCallObj, apiCall => {
        if (getError(apiState, action.uri, action.method, apiCall.id)) {
          errorCode = getErrorCode(apiState, action.uri, action.method, apiCall.id);
          apiError = true;
        }
      });
      if (apiError) {
        yield put(restapiError(true, action.uri, action.method, action.id as any));
        yield put(restapiErrorCode(errorCode, action.uri, action.method, action.id as any));
        yield put(restapiLoading(false, action.uri, action.method, action.id));
        yield put(restapiClear(epicPriorityCallObj.uri, epicPriorityCallObj.method, epicPriorityCallObj.id));
        return;
      }

      const epicPriorityListState = get(
        apiState,
        ["issue_management_list", "list", JIRA_EPIC_PRIORITY_APICALL_ID],
        undefined
      );
      if (epicPriorityListState) {
        epicPriorityList = get(epicPriorityListState, ["data", "records"], []);
        if (epicPriorityList.length) {
          epicKeys = epicPriorityList.map(epic => epic?.workitem_id);
          filters = {
            ...(filters || {}),
            filter: {
              ...get(filters, ["filter"], {}),
              epics: uniq(epicKeys)
            }
          };
        }
      }
    }

    const apiCalls = [
      {
        data: filters,
        id: JIRA_PROGRESS_REGULAR_APICALL_ID,
        method: "list",
        uri: action.uri
      },
      {
        data: {
          ...(filters || {}),
          filter: {
            ...get(filters, ["filter"], {}),
            workitem_status_categories: ["Done", "Resolved", "Closed", "Completed"]
          }
        },
        id: JIRA_PROGRESS_COMPLETED_APICALL_ID,
        method: "list",
        uri: action.uri
      }
    ];
    yield all(apiCalls.map(curCall => call(restapiEffectSaga, curCall)));
    let apiState = yield select(restapiState);
    /* if any api call fails, get error and errorcode and dispatch error so error handling can be handled in BAWidgetAPi Wrapper */
    forEach(apiCalls, apiCall => {
      if (getError(apiState, action.uri, action.method, apiCall.id)) {
        errorCode = getErrorCode(apiState, action.uri, action.method, apiCall.id);
        apiError = true;
      }
    });
    if (apiError) {
      yield put(restapiError(true, action.uri, action.method, action.id as any));
      yield put(restapiErrorCode(errorCode, action.uri, action.method, action.id as any));
      yield put(restapiLoading(false, action.uri, action.method, action.id));
      apiCallsToclear.push(...apiCalls);
      yield all(apiCallsToclear.map(call => put(genericRestAPISet({}, call.uri, call.method, call.id))));
      return;
    }

    const dataList = apiCalls.map(call => get(apiState, [call.uri, call.method, call.id, "data"], undefined));
    let totalStoryPointRecords = get(dataList[0], ["records", "0", action.data.across, "records"], []);
    totalStoryPointRecords = totalStoryPointRecords.slice(0, Math.min(maxRecords, totalStoryPointRecords.length));
    const completedStoryPointsRecords = get(dataList[1], ["records", "0", action.data.across, "records"], []);
    const moreApiCalls = getApiCalls(action.id, action.data, totalStoryPointRecords);

    yield all(moreApiCalls.map(apiCall => call(restapiEffectSaga, apiCall)));
    const restStateTeamAlloc = yield select(restapiState);
    /* if any api call fails, get error and errorcode and dispatch error so error handling can be handled in BAWidgetAPi Wrapper */
    forEach(moreApiCalls, apiCall => {
      if (getError(restStateTeamAlloc, apiCall.uri, apiCall.method, apiCall.id)) {
        errorCode = getErrorCode(restStateTeamAlloc, apiCall.uri, apiCall.method, apiCall.id);
        apiError = true;
      }
    });
    if (apiError) {
      yield put(restapiError(true, action.uri, action.method, action.id as any));
      yield put(restapiErrorCode(errorCode, action.uri, action.method, action.id as any));
      yield put(restapiLoading(false, action.uri, action.method, action.id));
      apiCallsToclear.push(...apiCalls, ...moreApiCalls);
      yield all(apiCallsToclear.map(call => put(genericRestAPISet({}, call.uri, call.method, call.id))));
      return;
    }

    let initialTransformedDataRecords = yield getInitialTransformedData(totalStoryPointRecords, [
      ...moreApiCalls,
      epicPriorityCallObj
    ]);

    const resultRecords = jiraProgressReportTransformer(
      completedStoryPointsRecords,
      initialTransformedDataRecords,
      unit
    );

    yield put(restapiData({ records: resultRecords }, action.uri, action.method, actionId));
    yield put(restapiError(false, action.uri, action.method, action.id));
    yield put(restapiLoading(false, action.uri, action.method, actionId));
    apiCallsToclear.push(...apiCalls, ...moreApiCalls);
    yield all(apiCallsToclear.map(call => put(genericRestAPISet({}, call.uri, call.method, call.id))));
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

export function* azureBAProgressReportWatcher() {
  yield takeEvery([AZURE_PROGRESS_REPORT], azureBAProgressReportEffectSaga);
}
