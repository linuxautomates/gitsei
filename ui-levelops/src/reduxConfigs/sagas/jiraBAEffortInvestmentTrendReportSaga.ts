import { all, takeEvery } from "@redux-saga/core/effects";
import { jiraEffortInvestmentTrendReportTransformer } from "custom-hooks/helpers/jiraBAReportTransformers";
import {
  ACTIVE_WORK_UNIT_FILTER_KEY,
  BA_COMPLETED_WORK_STATUS_BE_KEY,
  BA_EFFORT_ATTRIBUTION_BE_KEY,
  BA_HISTORICAL_ASSIGNEES_STATUS_BE_KEY,
  BA_IN_PROGRESS_STATUS_BE_KEY,
  JIRA_STATUS_CATEGORIES,
  TICKET_CATEGORIZATION_SCHEMES_KEY,
  TICKET_CATEGORIZATION_SCHEME_CATEGORY_KEY,
  TICKET_CATEGORIZATION_UNIT_FILTER_KEY,
  TIME_RANGE_DISPLAY_FORMAT_CONFIG,
  WORKITEM_STATUS_CATEGORIES_ADO,
  WORKITEW_STATUS_CATEGORIES
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { IntervalType, jiraBAReportTypes } from "dashboard/constants/enums/jira-ba-reports.enum";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { cloneDeep, forEach, get, uniqBy, unset } from "lodash";
import { call, put, select } from "redux-saga/effects";
import { JIRA_EFFORT_INVESTMENT_TREND_REPORT } from "reduxConfigs/actions/actionTypes";
import {
  restapiData,
  restapiError,
  restapiLoading,
  restapiErrorCode,
  restapiClear
} from "reduxConfigs/actions/restapi";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { restapiEffectSaga } from "./restapiSaga";
import { getAzureReportApiRecords, getEITrendReportTimeRangeList } from "./saga-helpers/BASprintReport.helper";
import { ISSUE_MANAGEMENT_REPORTS } from "dashboard/constants/applications/names";
import { basicActionType, basicMappingType } from "dashboard/dashboard-types/common-types";
import { commitCountCompletedWorkURIs } from "reduxConfigs/constants/effort-investment.constants";
import { getError, getErrorCode } from "utils/loadingUtils";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { formUpdateField } from "reduxConfigs/actions/formActions";

const restapiState = (state: any) => state.restapiReducer;

const getApiCalls = (
  categoryList: { name: string }[],
  action: basicActionType<any>,
  filters: basicMappingType<any>,
  isIssueManagement: boolean
) => {
  let apiCalls: Omit<basicActionType<any>, "type">[] = [];
  const isCommitCount = commitCountCompletedWorkURIs.includes(action?.uri);
  const interval = get(filters, ["interval"], IntervalType.BI_WEEK);
  const usingOU = !!get(filters || {}, ["ou_ids"], undefined);
  const JIRA_COMPLETED_DATAPOINTS_APICALL_ID = `JIRA_COMPLETED_DATAPOINTS_APICALL_ID_${action.id}`;
  const activeWorkURI = get(filters, ["filter", ACTIVE_WORK_UNIT_FILTER_KEY], "");
  const widgetId = action.data.widget_id || null;
  unset(filters, ["filter", ACTIVE_WORK_UNIT_FILTER_KEY]);

  let ticketCategoryBEKey = isIssueManagement
    ? "workitem_ticket_categories"
    : TICKET_CATEGORIZATION_SCHEME_CATEGORY_KEY;

  let newFilters: basicMappingType<any> = {
    across: "issue_resolved_at",
    interval,
    filter: {
      ...get(filters, ["filter"], {})
    }
  };

  if (isIssueManagement) {
    newFilters = {
      across: "workitem_resolved_at",
      interval,
      filter: {
        ...get(filters, ["filter"], {}),
        workitem_status_categories: WORKITEM_STATUS_CATEGORIES_ADO,
        workitem_ticket_categorization_scheme: get(filters, ["filter", TICKET_CATEGORIZATION_SCHEMES_KEY], "")
      }
    };
    unset(newFilters, ["filter", TICKET_CATEGORIZATION_SCHEMES_KEY]);
  }

  if (usingOU) {
    newFilters = {
      ...(newFilters || {}),
      ou_ids: get(filters || {}, ["ou_ids"], []),
      ou_user_filter_designation: get(filters || {}, ["ou_user_filter_designation"], {})
    };
  }

  if (isCommitCount) {
    newFilters = {
      across: "committed_at",
      interval,
      filter: {
        ...get(newFilters, ["filter"], {})
      }
    };
    const keysToUnset = ["issue_resolved_at", "workitem_resolved_at", BA_EFFORT_ATTRIBUTION_BE_KEY];
    forEach(keysToUnset, key => {
      unset(newFilters, ["filter", key]);
    });
  }

  forEach(categoryList || [], category => {
    const id = category?.name;
    const apiFilters = {
      ...(newFilters || {}),
      filter: {
        ...get(newFilters, ["filter"], {}),
        [ticketCategoryBEKey]: [id]
      },
      widget_id: widgetId
    };
    apiCalls.push({
      data: apiFilters,
      id: `${JIRA_COMPLETED_DATAPOINTS_APICALL_ID}-${id}`,
      uri: action.uri,
      method: action.method
    });
  });

  return apiCalls;
};

function* getInitialTransformedData(action: basicActionType<any>, categoryList: { name: string }[]): any {
  const restState = yield select(restapiState);

  const JIRA_COMPLETED_DATAPOINTS_APICALL_ID = `JIRA_COMPLETED_DATAPOINTS_APICALL_ID_${action.id}`;
  let apiDataList: any[] = [];
  forEach(categoryList, record => {
    const completedPointsId = `${JIRA_COMPLETED_DATAPOINTS_APICALL_ID}-${record?.name}`;
    let completedPointsRecords = get(restState, [action.uri, action.method, completedPointsId, "data", "records"], []);
    let errorEI = get(restState, [action.uri, action.method, completedPointsId, "error"], false);
    let errorCode = get(restState, [action.uri, action.method, completedPointsId, "error_code"], false);
    if ((action.uri || "").includes("issue_management")) {
      completedPointsRecords = getAzureReportApiRecords(completedPointsRecords);
    }
    apiDataList.push({
      ...(record || {}),
      completed_points: completedPointsRecords,
      error: errorEI,
      error_code: errorCode
    });
  });

  return apiDataList;
}

function* jiraBAEffortInvestmentTrendReportEffectSaga(action: basicActionType<any>): any {
  const actionId = action.id;
  const filters = cloneDeep(action.data);
  const isIssueManagement = get(action.extra ?? {}, ["application"], "jira") === "azure_devops";
  const effortInvestmentTrendYaxis = get(action.extra ?? {}, ["effortInvestmentTrendYaxis"], false);

  const reportType = isIssueManagement
    ? ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_TREND_REPORT
    : jiraBAReportTypes.EFFORT_INVESTMENT_TREND_REPORT;

  const unit = get(filters, ["filter", TICKET_CATEGORIZATION_UNIT_FILTER_KEY], "tickets_report");
  unset(filters, ["filter", TICKET_CATEGORIZATION_UNIT_FILTER_KEY]);

  try {
    let categoryList: any[] = [];
    let apiCallsToClear: Omit<basicActionType<any>, "type">[] = [];

    const selectedSchemeId = get(filters, ["filter", TICKET_CATEGORIZATION_SCHEMES_KEY], undefined);

    const initialApiCalls = [
      {
        id: selectedSchemeId,
        uri: "ticket_categorization_scheme",
        method: "get"
      }
    ];

    yield all(initialApiCalls.map(apiCall => call(restapiEffectSaga, apiCall)));
    yield put(restapiError(false, action.uri, action.method, action.id));
    let restState = yield select(restapiState);

    /* if any api call fails, get error and errorcode and dispatch error so error handling can be handled in BAWidgetAPi Wrapper */
    let apiError = false,
      errorCode = "";
    forEach(initialApiCalls, apiCall => {
      if (getError(restState, action.uri, action.method, apiCall.id)) {
        errorCode = getErrorCode(restState, action.uri, action.method, apiCall.id);
        apiError = true;
      }
    });
    if (apiError) {
      yield put(restapiError(true, action.uri, action.method, action.id as any));
      yield put(restapiErrorCode(errorCode, action.uri, action.method, action.id as any));
      yield put(restapiLoading(false, action.uri, action.method, action.id));
      yield all(initialApiCalls.map(call => put(restapiClear(call.uri, call.method, call.id))));
      return;
    }

    if (selectedSchemeId) {
      const scheme = get(restState, ["ticket_categorization_scheme", "get", selectedSchemeId], undefined);
      if (scheme) {
        categoryList = Object.values(get(scheme, ["data", "config", "categories"], {}));
        categoryList.push({ name: "Other", color: get(scheme, ["data", "config", "uncategorized", "color"], "") });
        categoryList = (categoryList || []).filter(category => category?.name !== "Uncategorized");
        yield put(
          formUpdateField([selectedSchemeId], "effort_investment_profile_ids", [
            { id: selectedSchemeId, name: scheme?.data?.name }
          ])
        );
      }
    }

    uniqBy(categoryList, "name");

    restState = yield select(restapiState);
    const intervalDisplayFormatMapping = getWidgetConstant(reportType, TIME_RANGE_DISPLAY_FORMAT_CONFIG);
    const interval = get(filters, ["interval"], IntervalType.BI_WEEK);
    const datapointGetApiCalls = getApiCalls(categoryList, action, filters, isIssueManagement);
    yield all(datapointGetApiCalls.map(apiCall => call(restapiEffectSaga, apiCall)));

    /* if any api call fails, get error and errorcode and dispatch error so error handling can be handled in BAWidgetAPi Wrapper */
    const restStateActiveWork = yield select(restapiState);
    forEach(datapointGetApiCalls, apiCall => {
      if (
        ![
          "active_effort_investment_tickets",
          "active_azure_ei_ticket_count",
          "active_azure_ei_story_point",
          "active_effort_investment_story_points"
        ].includes(apiCall.uri) &&
        getError(restStateActiveWork, apiCall.uri, apiCall.method, apiCall.id)
      ) {
        errorCode = getErrorCode(restStateActiveWork, apiCall.uri, apiCall.method, apiCall.id);
        apiError = true;
      }
    });
    if (apiError) {
      yield put(restapiError(true, action.uri, action.method, action.id as any));
      yield put(restapiErrorCode(errorCode, action.uri, action.method, action.id as any));
      yield put(restapiLoading(false, action.uri, action.method, action.id));
      apiCallsToClear.push(...datapointGetApiCalls);
      yield all(apiCallsToClear.map(call => put(genericRestAPISet({}, call.uri, call.method, call.id))));
      return;
    }

    const initialTransformedDataRecords = yield getInitialTransformedData(action, categoryList);
    const timeRangeList: any[] = getEITrendReportTimeRangeList(
      initialTransformedDataRecords,
      interval,
      intervalDisplayFormatMapping[interval]
    );
    const resultData = jiraEffortInvestmentTrendReportTransformer(
      initialTransformedDataRecords,
      timeRangeList,
      unit,
      interval,
      reportType,
      effortInvestmentTrendYaxis
    );

    yield put(restapiData({ records: resultData }, action.uri, action.method, actionId));
    yield put(restapiError(false, action.uri, action.method, action.id));
    yield put(restapiLoading(false, action.uri, action.method, actionId));
    apiCallsToClear.push(...datapointGetApiCalls);
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

export function* jiraBAEffortInvestmentTrendReportWatcher() {
  yield takeEvery([JIRA_EFFORT_INVESTMENT_TREND_REPORT], jiraBAEffortInvestmentTrendReportEffectSaga);
}
