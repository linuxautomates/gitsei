import { call, select, takeEvery } from "@redux-saga/core/effects";
import { issueContextTypes, severityTypes } from "bugsnag";
import { jiraBurnDownDataTransformer } from "custom-hooks/helpers";
import {
  BA_TIME_RANGE_FILTER_KEY,
  TICKET_CATEGORIZATION_SCHEMES_KEY
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { valuesToFilters } from "dashboard/constants/constants";
import { handleError } from "helper/errorReporting.helper";
import { cloneDeep, forEach, get, unset } from "lodash";
import { all, put } from "redux-saga/effects";
import { JIRA_BURN_DOWN_REPORT } from "reduxConfigs/actions/actionTypes";
import { restapiData, restapiError, restapiLoading } from "reduxConfigs/actions/restapi";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { slugifyId } from "utils/stringUtils";
import { restapiEffectSaga } from "./restapiSaga";
import { getBATimeRanges } from "./saga-helpers/BASprintReport.helper";

const restapiState = (state: any) => state.restapiReducer;

const JIRA_IN_PROGRESS_API_CALL_ID = "JIRA_IN_PROGRESS_API_CALL_ID";
const JIRA_ISSUE_CREATED_API_CALL_ID = "JIRA_ISSUE_CREATED_API_CALL_ID";
const JIRA_ISSUE_RESOLVED_API_CALL_ID = "JIRA_ISSUE_RESOLVED_API_CALL_ID";
const MAX_SPRINTS = 8;

const getApiCalls = (action: any, filters: any, parentList: any[]) => {
  const nameKey = filters?.across === "epic" ? "key" : "name";
  const filterKey = get(valuesToFilters, [filters?.across], filters?.across);
  let resulttantCalls: any[] = [];
  forEach(parentList, record => {
    if (!record?.[nameKey]) return;
    const id = slugifyId(record?.[nameKey]);
    const apiCalls = [
      {
        data: {
          ...(filters || {}),
          across: "trend",
          filter: {
            ...get(filters, ["filter"], {}),
            status_category: "In Progress",
            [filterKey]: [record?.[nameKey]]
          }
        },
        uri: action.uri,
        method: action.method,
        id: `${JIRA_IN_PROGRESS_API_CALL_ID}-${id}`
      },
      {
        data: {
          ...(filters || {}),
          across: "issue_created",
          filter: {
            ...get(filters, "[filter]", {}),
            [filterKey]: [record?.[nameKey]]
          }
        },
        uri: action.uri,
        method: action.method,
        id: `${JIRA_ISSUE_CREATED_API_CALL_ID}-${id}`
      },
      {
        data: {
          ...(filters || {}),
          across: "issue_resolved",
          filter: {
            ...get(filters, "[filter]", {}),
            [filterKey]: [record?.[nameKey]]
          }
        },
        uri: action.uri,
        method: action.method,
        id: `${JIRA_ISSUE_RESOLVED_API_CALL_ID}-${id}`
      }
    ];
    resulttantCalls.push(...apiCalls);
  });
  return resulttantCalls;
};

function* getInitialTransformedData(action: any, parentList: any[], nameKey: string): any {
  const restState = yield select(restapiState);
  let apiDataList: any[] = [];
  const across = get(action.data, ["across"], "epic");
  forEach(parentList, record => {
    if (!record?.[nameKey]) return;
    const id = slugifyId(record?.[nameKey]);
    const inProgressId = `${JIRA_IN_PROGRESS_API_CALL_ID}-${id}`;
    const issueCreatedId = `${JIRA_ISSUE_CREATED_API_CALL_ID}-${id}`;
    const issueResolvedId = `${JIRA_ISSUE_RESOLVED_API_CALL_ID}-${id}`;
    const inProgressRecords = get(restState, [action.uri, action.method, inProgressId, "data", "records"], []);
    const issueCreatedRecords = get(restState, [action.uri, action.method, issueCreatedId, "data", "records"], []);
    const issueResolvedRecords = get(restState, [action.uri, action.method, issueResolvedId, "data", "records"], []);
    apiDataList.push({
      [record?.[nameKey]]: {
        remaining: inProgressRecords,
        new: issueCreatedRecords,
        completed: issueResolvedRecords
      },
      summary: across === "epic" ? record?.summary : ""
    });
  });
  return apiDataList;
}

function* jiraBurnDownEffectSaga(action: any): any {
  const actionId = action.id;
  const filters = cloneDeep(action.data);
  const across = filters?.across;
  const maxCalls = 8;
  const selectedTimeRange = get(action.data, ["filter", BA_TIME_RANGE_FILTER_KEY], undefined);
  const EPIC_PRIORITY_LIST_ID = `EPIC_PRIORITY_LIST_ID_${actionId}`; // id for epic list api call
  unset(filters, ["filter", "completed_at"]);
  unset(filters, ["filter", BA_TIME_RANGE_FILTER_KEY]);
  try {
    let epicList: any[] = [];
    let categoryList: any[] = [];
    let apiCallsToClear: any[] = [];
    let apiCalls: any[] = [];
    let restState = yield select(restapiState);
    if (across === "epic") {
      // if across is epic we call api to bring epic list
      const epicListApiCallObj = {
        data: {
          ...(filters || {}),
          filter: {
            ...get(filters, ["filter"], {}),
            issue_types: ["EPIC"]
          }
        },
        uri: "jira_tickets",
        method: "list",
        id: EPIC_PRIORITY_LIST_ID
      };
      yield call(restapiEffectSaga, epicListApiCallObj);
      restState = yield select(restapiState);
      const epicListStateData = get(restState, ["jira_tickets", "list", EPIC_PRIORITY_LIST_ID, "data"], {});

      if (Object.keys(epicListStateData).length > 0) {
        epicList = get(epicListStateData, ["records"], []);
      }
      // sorting data on the basis of priority
      if (epicList.length) {
        epicList.sort((a, b) => {
          return a?.priority_order - b?.priority_order;
        });
      }

      apiCalls = getApiCalls(action, filters, (epicList || []).slice(0, Math.min(maxCalls, epicList.length))); // TODO: get alternate for slicing
      apiCallsToClear.push(epicListApiCallObj, ...apiCalls);
    } else if (across === "ticket_category") {
      const selectedSchemeId = get(filters, ["filter", TICKET_CATEGORIZATION_SCHEMES_KEY], undefined);
      const ticketCategorySchemeCallObj = {
        id: selectedSchemeId,
        uri: "ticket_categorization_scheme",
        method: "get"
      };
      yield call(restapiEffectSaga, ticketCategorySchemeCallObj);
      restState = yield select(restapiState);
      if (selectedSchemeId) {
        const scheme = get(restState, ["ticket_categorization_scheme", "get", selectedSchemeId], undefined);
        if (scheme) {
          categoryList = Object.values(get(scheme, ["data", "config", "categories"], {}));
        }
      }
      apiCalls = getApiCalls(action, filters, categoryList.slice(0, Math.min(maxCalls, categoryList.length))); // TODO: get alternate for slicing
      apiCallsToClear.push(ticketCategorySchemeCallObj, ...apiCalls);
    }

    yield all(apiCalls.map((curCall: any) => call(restapiEffectSaga, curCall)));
    restState = yield select(restapiState);

    const parentList = filters?.across === "epic" ? epicList : categoryList;
    const nameKey = filters?.across === "epic" ? "key" : "name";

    const initialTransformedDataRecords = yield getInitialTransformedData(
      action,
      parentList.slice(0, Math.min(maxCalls, parentList.length)),
      nameKey
    );
    const timeRangeList: any[] = selectedTimeRange ? getBATimeRanges(selectedTimeRange) || [] : [];
    const resultData = jiraBurnDownDataTransformer(
      initialTransformedDataRecords,
      timeRangeList,
      across !== "epic" ? categoryList : undefined
    );
    yield put(restapiData({ records: resultData }, action.uri, action.method, actionId));
    yield put(restapiError(false, action.uri, action.method, action.id));
    yield put(restapiLoading(false, action.uri, action.method, actionId));
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

    yield put(restapiError(true, action.uri, action.method, actionId));
    yield put(restapiLoading(false, action.uri, action.method, actionId));
  }
}

export function* jiraBurnDownReportWatcherSaga() {
  yield takeEvery([JIRA_BURN_DOWN_REPORT], jiraBurnDownEffectSaga);
}
