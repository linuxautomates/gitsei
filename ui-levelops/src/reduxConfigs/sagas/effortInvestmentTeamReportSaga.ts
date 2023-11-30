import { all, takeEvery } from "@redux-saga/core/effects";
import { issueContextTypes, severityTypes } from "bugsnag";
import { effortInvestmentTeamReportTransformer } from "custom-hooks/helpers/jiraBAReportTransformers";
import {
  BA_TIME_RANGE_FILTER_KEY,
  TICKET_CATEGORIZATION_SCHEMES_KEY,
  TICKET_CATEGORIZATION_UNIT_FILTER_KEY
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { handleError } from "helper/errorReporting.helper";
import { cloneDeep, forEach, get, map, uniq, unset } from "lodash";
import { call, put, select } from "redux-saga/effects";
import { EFFORT_INVESTMENT_TEAM_REPORT } from "reduxConfigs/actions/actionTypes";
import { restapiData, restapiError, restapiLoading } from "reduxConfigs/actions/restapi";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { restapiEffectSaga } from "./restapiSaga";
import { getBATimeRanges } from "./saga-helpers/BASprintReport.helper";

const restapiState = (state: any) => state.restapiReducer;

const getApiCalls = (assigneesList: any[], timeRangeList: any[], action: any) => {
  let apiCalls: any[] = [];
  const filters = action.data;
  const JIRA_COMPLETED_DATAPOINTS_APICALL_ID = `JIRA_COMPLETED_DATAPOINTS_APICALL_ID`;
  let newFilters = {
    ...(filters || {}),
    interval: "week",
    across: "assignee",
    stacks: ["ticket_category"],
    filter: {
      ...get(filters, ["filter"], {}),
      assignees: assigneesList
    }
  };
  unset(newFilters, ["filter", TICKET_CATEGORIZATION_UNIT_FILTER_KEY]);
  unset(newFilters, ["filter", BA_TIME_RANGE_FILTER_KEY]);
  forEach(timeRangeList, (timeRange: any) => {
    const completed_at = { $lt: `${parseInt(timeRange?.end_date)}`, $gt: `${parseInt(timeRange?.start_date)}` };
    newFilters = {
      ...newFilters,
      filter: {
        ...get(newFilters, ["filter"], []),
        completed_at
      }
    };
    apiCalls.push({
      data: newFilters,
      id: `${JIRA_COMPLETED_DATAPOINTS_APICALL_ID}-${timeRange?.id}`,
      uri: action.uri,
      method: action.method
    });
  });
  return apiCalls;
};

function* getInitialTransformedData(action: any, timeRangeList: any[]): any {
  const restState = yield select(restapiState);
  const JIRA_COMPLETED_DATAPOINTS_APICALL_ID = `JIRA_COMPLETED_DATAPOINTS_APICALL_ID`;
  return map(timeRangeList, timeRange => {
    const id = `${JIRA_COMPLETED_DATAPOINTS_APICALL_ID}-${timeRange?.id}`;
    const data = get(restState, [action.uri, action.method, id, "data"], {});
    return {
      ...(timeRange || {}),
      apiData: get(data, ["records"], [])
    };
  });
}

const getAssigneesFromTeams = (teams: any[]) => {
  let assignees: any[] = [];
  forEach(teams, team => {
    const members = get(team, ["members"], []);
    if (members.length > 0) {
      const records = get(members[0], ["records"], []);
      assignees = [...assignees, ...(records.map((record: any) => record?.full_name) || [])];
    }
  });
  return uniq(assignees);
};

const getAssigneeTeamMap = (teams: any[]) => {
  let assigneeTeamMap: any = {};
  const assignees = getAssigneesFromTeams(teams);
  forEach(assignees, assignee => {
    const exitingTeams: string[] = [];
    forEach(teams, team => {
      const members = get(team, ["members"], []);
      if (members.length > 0) {
        const records: string[] = get(members[0], ["records"], []).map((record: any) => record?.full_name);
        if (records.includes(assignee)) {
          exitingTeams.push(team?.id);
        }
      }
    });
    assigneeTeamMap = {
      ...assigneeTeamMap,
      [assignee]: exitingTeams
    };
  });
  return assigneeTeamMap;
};

function* effortInvestmentTeamReportEffectSaga(action: any): any {
  const actionId = action.id;
  const filters = cloneDeep(action.data);
  const TEAMS_API_CALL_ID = `TEAMS_API_CALL_ID_${actionId}`;
  const unit = get(filters, ["filter", TICKET_CATEGORIZATION_UNIT_FILTER_KEY], "tickets_report");
  const selectedTimeRange = get(action.data, ["filter", BA_TIME_RANGE_FILTER_KEY], undefined);
  unset(filters, ["filter", BA_TIME_RANGE_FILTER_KEY]);
  unset(filters, ["filter", TICKET_CATEGORIZATION_UNIT_FILTER_KEY]);
  unset(filters, ["filter", TICKET_CATEGORIZATION_SCHEMES_KEY]);

  try {
    let apiCallsToClear: any[] = [];
    let teamsList: any[] = [];

    const initialApiCalls: any[] = [
      {
        data: filters,
        uri: "teams_list",
        method: "list",
        id: TEAMS_API_CALL_ID
      }
    ];

    yield all(initialApiCalls.map(apiCall => call(restapiEffectSaga, apiCall)));
    let restState = yield select(restapiState);

    restState = yield select(restapiState);
    const teams = get(restState, ["teams_list", "list", TEAMS_API_CALL_ID, "data"], {});

    if (Object.keys(teams).length > 0) {
      // teamsList = get(teams, ["records"], []);
      teamsList = [
        // hardcoding for now
        {
          id: "123-456-67567-5675",
          name: "Team Red",
          members: [
            {
              records: [
                {
                  full_name: "Piyush Mishra",
                  email: "piyush@company.com"
                },
                {
                  full_name: "Kushagra Saxena",
                  email: "kush@company.com"
                }
              ]
            }
          ]
        }
      ];
    }
    const assigneeList = getAssigneesFromTeams(teamsList);
    const timeRangeLists = selectedTimeRange ? getBATimeRanges(selectedTimeRange) || [] : [];
    const moreGetApiCalls = getApiCalls(assigneeList, timeRangeLists, action);
    yield all(moreGetApiCalls.map(apiCall => call(restapiEffectSaga, apiCall)));
    const initialTransformedDataRecords = yield getInitialTransformedData(action, timeRangeLists);
    const assigneeTeamMap = getAssigneeTeamMap(teamsList);
    const resultData = effortInvestmentTeamReportTransformer(
      initialTransformedDataRecords,
      teamsList,
      assigneeTeamMap,
      unit
    );
    yield put(restapiData({ records: resultData }, action.uri, action.method, actionId));
    yield put(restapiError(false, action.uri, action.method, action.id));
    yield put(restapiLoading(false, action.uri, action.method, actionId));
    apiCallsToClear.push(...initialApiCalls, ...moreGetApiCalls);
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

export function* effortInvestmentTeamReportWatcher() {
  yield takeEvery([EFFORT_INVESTMENT_TEAM_REPORT], effortInvestmentTeamReportEffectSaga);
}
