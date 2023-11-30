import moment from "moment";
import { issueContextTypes, severityTypes } from "bugsnag";
import { TICKET_CATEGORIZATION_UNIT_FILTER_KEY } from "dashboard/constants/bussiness-alignment-applications/constants";
import { handleError } from "helper/errorReporting.helper";
import { get, unset, forEach, cloneDeep, set } from "lodash";
import { Entity } from "model/entities/entity";
import { all, call, put, select, takeEvery, takeLatest } from "redux-saga/effects";
import { AZURE_PROGRAM_PROGRESS_REPORT } from "reduxConfigs/actions/actionTypes";
import {
  restapiData,
  restapiError,
  restapiLoading,
  restapiClear,
  restapiErrorCode
} from "reduxConfigs/actions/restapi";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { getDecimalPercantageRoundValue } from "shared-resources/charts/graph-stat-chart/helper";
import { getError, getErrorCode } from "utils/loadingUtils";
import { restapiEffectSaga } from "./restapiSaga";
import { listOfKeysSupported } from "./constant";

const restapiState = (state: any) => state.restapiReducer;
const getStoryPointsRecord = (store: any, apiCall: any, action: any) => {
  let _storyPoint: any = [];
  (apiCall || [])?.forEach((call: any) => {
    const integration_id = call?.id?.split("_")?.[3];
    const data = get(store, [call.uri, call.method, call.id, "data"], []);
    const records = get(data, ["records", "0", action?.data?.across, "records"], []);
    const newRecords = records.map((record: any) => {
      return {
        ...record,
        integration_id: integration_id
      };
    });
    _storyPoint = [..._storyPoint, ...newRecords];
  });
  return _storyPoint;
};
const getApiCallsIntegrationBased = (filters: any, prefixUuid: string) => {
  const integrationIds = get(filters, ["filter", "integration_ids"], []);
  return integrationIds.map((integrationId: string) => {
    const uuid = `${prefixUuid}${integrationId}`;
    return {
      data: { ...filters, filter: { ...filters?.filter, integration_ids: [integrationId] } },
      id: uuid,
      method: "list",
      uri: "issue_management_story_point_report"
    };
  });
};

const getAllChartData = (allFeatureRecords: any, completedStoryPoints: any, totalStoryPoints: any) => {
  return allFeatureRecords.map((feature: any, index: number) => {
    const id = get(feature, "workitem_id", 0);
    const featureIntegrationId = get(feature, "integration_id", 0);
    const storyPointsCompleted =
      (completedStoryPoints || []).find(
        (record: any) => record?.key === id && record?.integration_id === featureIntegrationId
      )?.total_story_points ?? 0;
    const storyPointsTotal =
      (totalStoryPoints || []).find(
        (record: any) => record?.key === id && record?.integration_id === featureIntegrationId
      )?.total_story_points ?? 0;
    const _totalWorkitems =
      (totalStoryPoints || []).find(
        (record: any) => record?.key === id && record?.integration_id === featureIntegrationId
      )?.total_tickets ?? 0;
    const _completedWorkitems =
      (completedStoryPoints || []).find(
        (record: any) => record?.key === id && record?.integration_id === featureIntegrationId
      )?.total_tickets ?? 0;
    const _pendingWorkitems = _totalWorkitems - _completedWorkitems;
    const now = moment().utc().startOf("day").valueOf();
    const dueDate = get(feature, ["custom_fields", "Microsoft.VSTS.Scheduling.TargetDate"], undefined);
    let status = get(feature, "status", "-");
    const workItemResolvedAt = get(feature, "workitem_resolved_at", undefined);
    if (dueDate && !workItemResolvedAt) {
      status = now < dueDate ? `${status} / Early` : `${status} / Delayed`;
    }
    if (workItemResolvedAt && dueDate) {
      status = workItemResolvedAt < dueDate ? `${status} / Early` : `${status} / Delayed`;
    }
    return {
      ...feature,
      completed_points: storyPointsCompleted,
      workitems_ratio: storyPointsCompleted,
      total_points: storyPointsTotal,
      workitems: _totalWorkitems || 0,
      due_date: dueDate,
      fe_status: status,
      workitem_completed: _completedWorkitems || 0,
      workitem_pending: _pendingWorkitems || 0,
      workitem_effort: get(feature, ["attributes", "effort"], 0)
    };
  });
};
const getWorkItemListFilter = (filters: any) => {
  const _filters = cloneDeep(filters);
  set(_filters, ["filter", "workitem_types"], ["Feature"]);
  set(_filters, ["filter", "include_sprint_full_names"], true);
  set(_filters, ["page_size"], 200);
  unset(_filters, ["filter", "workitem_parent_workitem_types"]);
  unset(_filters, ["across"]);
  return _filters;
};

const getEffortListFilter = (filters: any) => {
  const _filters = cloneDeep(filters);
  set(_filters, ["filter", "workitem_types"], ["Feature"]);
  unset(_filters, ["filter", "workitem_parent_workitem_types"]);
  set(_filters, ["across"], "none");
  unset(_filters, ["filter", "ticket_categorization_scheme"]);
  unset(_filters, "ou_user_filter_designation");
  unset(_filters, "widget_id");
  return _filters;
};

const getRegularStoryPointsFilters = (filters: any, workItemIds: string[]) => {
  const _filters = cloneDeep(filters);
  const filterKeys = Object.keys(get(_filters, "filter", {}));
  filterKeys.forEach((key: string) => {
    if (!listOfKeysSupported.includes(key)) {
      unset(_filters, ["filter", key]);
    }
  });

  unset(_filters, ["filter", "workitem_parent_workitem_types"]);
  set(_filters, ["filter", "workitem_parent_workitem_ids"], workItemIds);
  set(_filters, ["across_limit"], (workItemIds ?? []).length);
  return _filters;
};

const getStatStoryPointFilters = (filters: any, ids: string[]) => {
  const _filters = {
    ...(filters || {}),
    across: "none",
    filter: {
      ...get(filters, ["filter"], {}),
      workitem_parent_workitem_ids: ids
    }
  };

  const filterKeys = Object.keys(get(_filters, "filter", {}));
  filterKeys.forEach((key: string) => {
    if (!listOfKeysSupported.includes(key)) {
      unset(_filters, ["filter", key]);
    }
  });
  unset(_filters, "ou_user_filter_designation");
  unset(_filters, "widget_id");
  return _filters;
};

function* azureBAProgramProgressReportEffectSaga(action: any): any {
  const actionId = action.id;
  const AZURE_EPIC_PRIORITY_APICALL_ID = `AZURE_PROGRAM_EPIC_PRIORITY_APICALL_ID_${actionId}`;
  const AZURE_TOTAL_STORY_POINTS_APICALL_ID = `AZURE_PROGRAM_TOTAL_STORY_POINTS_APICALL_ID_${actionId}`;
  const AZURE_COMPLETED_STORY_POINTS_APICALL_ID = `AZURE_PROGRAM_COMPLETED_STORY_POINTS_APICALL_ID_${actionId}`;
  const AZURE_ESTIMATED_STORY_POINTS_APICALL_ID = `AZURE_PROGRAM_ESTIMATED_STORY_POINTS_APICALL_ID_${actionId}`;

  let filters = action.data;
  unset(filters, ["filter", TICKET_CATEGORIZATION_UNIT_FILTER_KEY]);
  try {
    let apiCallsToclear: any[] = [];
    let regularStoryPointsFilters: any = {};
    let workItemCount: number = 0;
    let totalStoryPoints: number = 0;
    let completedStoryPoints: number = 0;
    let estimatedStoryPoints: number = 0;
    let pendingStoryPoints: number = 0;
    let percentageCompleteStoryPoints: number | string = 0;
    let apiError = false,
      errorCode = "";
    const workItemListFilters = getWorkItemListFilter(filters);
    let sortedRecords: Entity[] = [];
    let apiCallsAllStoryBasedOnIntegration = [];
    let apiCallsCompletedStoryBasedOnIntegration = [];

    let listCallObj = {
      data: workItemListFilters,
      uri: "issue_management_list",
      method: "list",
      id: AZURE_EPIC_PRIORITY_APICALL_ID
    };

    let allWorkitemListRecords: Entity[] = [];

    /** getting all workitem list records ar once */
    let page = 0,
      hasNext = true;

    while (hasNext) {
      listCallObj.data = {
        ...listCallObj?.data,
        page
      };
      yield call(restapiEffectSaga, listCallObj);
      let restapi = yield select(restapiState);
      const workitemListDataState = get(
        restapi,
        ["issue_management_list", "list", AZURE_EPIC_PRIORITY_APICALL_ID, "data"],
        {}
      );
      const workitemListStateRecords = get(workitemListDataState, ["records"], []);
      const metadata = get(workitemListDataState, ["_metadata"], {});
      hasNext = get(metadata, ["has_next"], false);
      allWorkitemListRecords = [...allWorkitemListRecords, ...workitemListStateRecords];
      page = page + 1;
    }
    let restApiState = yield select(restapiState);

    /* if any api call fails, get error and errorcode and dispatch error so error handling can be handled in BAWidgetAPi Wrapper */
    forEach([listCallObj], apiCall => {
      if (getError(restApiState, apiCall.uri, apiCall.method, apiCall.id)) {
        errorCode = getErrorCode(restApiState, action.uri, action.method, apiCall.id);
        apiError = true;
      }
    });
    if (apiError) {
      yield put(restapiError(true, action.uri, action.method, action.id as any));
      yield put(restapiErrorCode(errorCode, action.uri, action.method, action.id as any));
      yield put(restapiLoading(false, action.uri, action.method, action.id));
      yield put(restapiClear(listCallObj.uri, listCallObj.method, listCallObj.id));
      return;
    }

    const workItemFeatureListState = get(
      restApiState,
      ["issue_management_list", "list", AZURE_EPIC_PRIORITY_APICALL_ID],
      undefined
    );
    /* If list iteam count is zero then no need to call other API all header stat will be shown zero */
    if (allWorkitemListRecords.length !== 0) {
      const workItemIds = allWorkitemListRecords.map((workItem: any) => workItem?.workitem_id);
      const statStoryPointFilters = getStatStoryPointFilters(filters, workItemIds);
      const statEffortPointsFilters = getEffortListFilter(filters);
      const totalStoryPointsApiObject = {
        data: statStoryPointFilters,
        uri: "issue_management_story_point_report",
        method: "list",
        id: AZURE_TOTAL_STORY_POINTS_APICALL_ID
      };
      const completedStoryPointsApiObject = {
        data: {
          ...statStoryPointFilters,
          filter: {
            ...statStoryPointFilters.filter,
            workitem_status_categories: ["Done", "Resolved", "Closed", "Completed"]
          }
        },
        uri: "issue_management_story_point_report",
        method: "list",
        id: AZURE_COMPLETED_STORY_POINTS_APICALL_ID
      };
      const estimatedStoryPointsApiObject = {
        data: statEffortPointsFilters,
        uri: "issue_management_effort_report",
        method: "list",
        id: AZURE_ESTIMATED_STORY_POINTS_APICALL_ID
      };

      const headerApiCalls = [totalStoryPointsApiObject, completedStoryPointsApiObject, estimatedStoryPointsApiObject];

      yield all(headerApiCalls.map((apiObject: any) => call(restapiEffectSaga, apiObject)));

      let newApiState = yield select(restapiState);

      forEach([...headerApiCalls], apiCall => {
        if (getError(newApiState, apiCall.uri, apiCall.method, apiCall.id)) {
          errorCode = getErrorCode(newApiState, apiCall.uri, apiCall.method, apiCall.id);
          apiError = true;
        }
      });
      if (apiError) {
        yield put(restapiError(true, action.uri, action.method, action.id as any));
        yield put(restapiErrorCode(errorCode, action.uri, action.method, action.id as any));
        yield put(restapiLoading(false, action.uri, action.method, action.id));
        return;
      }

      const totalStoryPointState = get(
        newApiState,
        ["issue_management_story_point_report", "list", AZURE_TOTAL_STORY_POINTS_APICALL_ID],
        undefined
      );
      const completedStoryPointState = get(
        newApiState,
        ["issue_management_story_point_report", "list", AZURE_COMPLETED_STORY_POINTS_APICALL_ID],
        undefined
      );
      const estimatedStoryPointState = get(
        newApiState,
        ["issue_management_effort_report", "list", AZURE_ESTIMATED_STORY_POINTS_APICALL_ID],
        undefined
      );
      workItemCount = workItemFeatureListState?.data._metadata?.total_count || 0;
      totalStoryPoints = get(
        totalStoryPointState,
        ["data", "records", "0", "none", "records", 0, "total_story_points"],
        0
      );
      completedStoryPoints = get(
        completedStoryPointState,
        ["data", "records", "0", "none", "records", 0, "total_story_points"],
        0
      );
      estimatedStoryPoints = get(
        estimatedStoryPointState,
        ["data", "records", "0", "none", "records", 0, "total_effort"],
        0
      );
      pendingStoryPoints = totalStoryPoints - completedStoryPoints;

      percentageCompleteStoryPoints = getDecimalPercantageRoundValue(completedStoryPoints, totalStoryPoints, 1);

      regularStoryPointsFilters = getRegularStoryPointsFilters(filters, workItemIds);
      apiCallsAllStoryBasedOnIntegration = getApiCallsIntegrationBased(
        regularStoryPointsFilters,
        `${actionId}_all_story_`
      );
      const completedStoryFilters = {
        ...regularStoryPointsFilters,
        filter: {
          ...regularStoryPointsFilters?.filter,
          workitem_status_categories: ["Done", "Resolved", "Closed", "Completed"]
        }
      };
      apiCallsCompletedStoryBasedOnIntegration = getApiCallsIntegrationBased(
        completedStoryFilters,
        `${actionId}_completed_story_`
      );

      yield all(apiCallsCompletedStoryBasedOnIntegration.map((curCall: any) => call(restapiEffectSaga, curCall)));
      yield all(apiCallsAllStoryBasedOnIntegration.map((curCall: any) => call(restapiEffectSaga, curCall)));

      let apiState = yield select(restapiState);
      /* if any api call fails, get error and errorcode and dispatch error so error handling can be handled in BAWidgetAPi Wrapper */
      forEach([...apiCallsAllStoryBasedOnIntegration, ...apiCallsCompletedStoryBasedOnIntegration], apiCall => {
        if (getError(apiState, apiCall.uri, apiCall.method, apiCall.id)) {
          errorCode = getErrorCode(apiState, apiCall.uri, apiCall.method, apiCall.id);
          apiError = true;
        }
      });
      if (apiError) {
        yield put(restapiError(true, action.uri, action.method, action.id as any));
        yield put(restapiErrorCode(errorCode, action.uri, action.method, action.id as any));
        yield put(restapiLoading(false, action.uri, action.method, action.id));
        apiCallsToclear.push(...apiCallsAllStoryBasedOnIntegration, ...apiCallsCompletedStoryBasedOnIntegration);
        yield all(apiCallsToclear.map(call => put(genericRestAPISet({}, call.uri, call.method, call.id))));
        return;
      }
      const _completedStoryPointsRecords = getStoryPointsRecord(
        apiState,
        apiCallsCompletedStoryBasedOnIntegration,
        action
      );
      const _totalStoryPointRecords = getStoryPointsRecord(apiState, apiCallsAllStoryBasedOnIntegration, action);

      let resultRecords = yield getAllChartData(
        allWorkitemListRecords,
        _completedStoryPointsRecords,
        _totalStoryPointRecords
      );
      sortedRecords = (resultRecords || [])?.sort((a: any, b: any) => a?.due_date - b?.due_date);
    }
    const reportHeaderInfoData = [
      {
        label: "Features",
        value: (workItemCount || 0).toLocaleString()
      },
      {
        label: "Feature Effort",
        value: (estimatedStoryPoints || 0).toLocaleString(),
        infoIcon: true,
        infoValue: "Total effort at the feature level."
      },
      {
        label: "Total Points",
        value: (totalStoryPoints || 0).toLocaleString()
      },
      {
        label: "Pending Points",
        value: (pendingStoryPoints || 0).toLocaleString()
      },
      {
        label: "Completed Points",
        value: (completedStoryPoints || 0).toLocaleString()
      },
      {
        label: "% Completed",
        value: (percentageCompleteStoryPoints === "NaN" ? 0 : percentageCompleteStoryPoints).toLocaleString(),
        infoIcon: true,
        infoValue:
          "Percentage of story points completed for child workitems based on their Status (Done, Resolved, Closed, or Completed)."
      }
    ];

    yield put(
      restapiData(
        { records: sortedRecords, reportHeaderInfoData: reportHeaderInfoData },
        action.uri,
        action.method,
        actionId
      )
    );
    yield put(restapiError(false, action.uri, action.method, action.id));
    yield put(restapiLoading(false, action.uri, action.method, actionId));
    apiCallsToclear = [
      ...apiCallsToclear,
      ...apiCallsAllStoryBasedOnIntegration,
      ...apiCallsCompletedStoryBasedOnIntegration
    ];
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

export function* azureBAProgramProgressReportWatcher() {
  yield takeEvery([AZURE_PROGRAM_PROGRESS_REPORT], azureBAProgramProgressReportEffectSaga);
}
