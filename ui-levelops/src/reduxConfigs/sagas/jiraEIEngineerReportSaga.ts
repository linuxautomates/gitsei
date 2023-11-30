import { cloneDeep, forEach, get, map, set, uniq, uniqBy, unset } from "lodash";
import { basicActionType, basicMappingType } from "dashboard/dashboard-types/common-types";
import { all, put, call, select, takeEvery } from "redux-saga/effects";
import { JIRA_EI_ACTIVE_ENGINEER_REPORT, JIRA_EI_COMPLETED_ENGINEER_REPORT } from "reduxConfigs/actions/actionTypes";
import {
  restapiClear,
  restapiData,
  restapiError,
  restapiLoading,
  restapiErrorCode
} from "reduxConfigs/actions/restapi";
import {
  ACTIVE_WORK_UNIT_FILTER_KEY,
  BA_COMPLETED_WORK_STATUS_BE_KEY,
  BA_EFFORT_ATTRIBUTION_BE_KEY,
  BA_HISTORICAL_ASSIGNEES_STATUS_BE_KEY,
  BA_IN_PROGRESS_STATUS_BE_KEY,
  TICKET_CATEGORIZATION_SCHEMES_KEY,
  TICKET_CATEGORIZATION_UNIT_FILTER_KEY,
  WORKITEM_STATUS_CATEGORIES_ADO
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { restapiEffectSaga } from "./restapiSaga";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";
import {
  categoryAllocationType,
  completedEffortRecord,
  completedEffortRecordType,
  EICategoryTypes
} from "dashboard/dashboard-types/BAReports.types";
import { Dict } from "types/dict";
import { EffortUnitType } from "dashboard/constants/enums/jira-ba-reports.enum";
import { effortInvestmentDonutBarColors } from "shared-resources/charts/chart-themes";
import { CATEGORY_DEFAULT_BACKGORUND_COLOR } from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";
import { RestWidget } from "../../classes/RestDashboards";
import { getWidget } from "../selectors/widgetSelector";
import { convertToDays } from "../../utils/timeUtils";
import { getError, getErrorCode } from "utils/loadingUtils";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

const getPercentageIn100rs = (value: number) => (value * 100).toFixed(2);

const UNCATEGORIZED = "Uncategorized";
const REMAINING_ALLOCATION_KEY = "Remaining Allocation";

function* jiraEICompletedEngineerReportEffectSaga(action: basicActionType<any>): any {
  const widget: RestWidget = yield select(getWidget, { widget_id: action?.id });
  const JIRA_COMPLETED_DATAPOINTS_APICALL_ID = `JIRA_COMPLETED_DATAPOINTS_APICALL_ID_${action.id}`;
  const filters = cloneDeep(action.data);
  const unit = get(filters, ["filter", TICKET_CATEGORIZATION_UNIT_FILTER_KEY], undefined);
  const isCommitCount = [EffortUnitType.COMMIT_COUNT, EffortUnitType.AZURE_COMMIT_COUNT].includes(unit);
  const isIssueManagement = action?.extra?.application === IntegrationTypes.AZURE;
  const ticketCategoryKey = isIssueManagement ? "workitem_ticket_categories" : "ticket_categories";
  const effortProfileKey = isIssueManagement
    ? "workitem_ticket_categorization_scheme"
    : TICKET_CATEGORIZATION_SCHEMES_KEY;

  unset(filters, ["filter", TICKET_CATEGORIZATION_UNIT_FILTER_KEY]);
  unset(filters, ["filter", ACTIVE_WORK_UNIT_FILTER_KEY]);

  try {
    let categoryList: EICategoryTypes[] = [];
    let apiCallsToClear: basicActionType<any>[] = [];
    let categoryColorMapping: basicMappingType<string> = {};
    const selectedProfileId = get(filters, ["filter", TICKET_CATEGORIZATION_SCHEMES_KEY], undefined);

    yield call(restapiEffectSaga, {
      id: selectedProfileId,
      uri: "ticket_categorization_scheme",
      method: "get"
    });

    let restState = yield select(restapiState);

    if (selectedProfileId) {
      const profile = get(restState, ["ticket_categorization_scheme", "get", selectedProfileId], undefined);
      if (profile) {
        categoryList = Object.values(get(profile, ["data", "config", "categories"], {}));
        categoryList.push({ name: UNCATEGORIZED });
        uniqBy(categoryList, "name");
        /** handling color of a uncategorized category */
        set(categoryColorMapping, [UNCATEGORIZED], get(profile, ["data", "config", "uncategorized", "color"]));
      }
    }

    let apiCalls: basicActionType<any>[] = [];

    forEach(categoryList, (category, index) => {
      /** handling color mapping for categories */
      if (category?.name !== UNCATEGORIZED) {
        categoryColorMapping[category?.name] =
          category?.color || effortInvestmentDonutBarColors[index % (categoryList ?? []).length];
      }

      const nameKey = category?.name === UNCATEGORIZED ? "Other" : category?.name;
      let apiFilters = {
        ...(filters || {}),
        filter: {
          ...get(filters, ["filter"], {}),
          [ticketCategoryKey]: [nameKey]
        }
      };

      // handling OU enabled
      const usingOU = !!get(apiFilters || {}, ["ou_ids"], undefined);
      if (usingOU) {
        apiFilters = {
          ...(apiFilters || {}),
          ou_ids: get(apiFilters || {}, ["ou_ids"], []),
          ou_user_filter_designation: get(apiFilters || {}, ["ou_user_filter_designation"], {})
        };
      }

      if (isIssueManagement) {
        apiFilters = {
          ...(apiFilters || {}),
          filter: {
            ...get(apiFilters, ["filter"], {}),
            [effortProfileKey]: selectedProfileId,
            workitem_status_categories: WORKITEM_STATUS_CATEGORIES_ADO
          }
        };
        let keysToUnset = [TICKET_CATEGORIZATION_SCHEMES_KEY];
        forEach(keysToUnset, key => {
          unset(apiFilters, ["filter", key]);
        });
      }

      if (isCommitCount) {
        apiFilters = {
          ...(apiFilters || {}),
          across: "author",
          filter: {
            ...get(apiFilters ?? {}, ["filter"], {})
          }
        };
        let keysToUnset = ["issue_resolved_at", "workitem_resolved_at", BA_EFFORT_ATTRIBUTION_BE_KEY];
        forEach(keysToUnset, key => {
          unset(apiFilters, ["filter", key]);
        });
      }

      apiCalls.push({
        data: apiFilters,
        id: `${JIRA_COMPLETED_DATAPOINTS_APICALL_ID}-${category?.name}`,
        uri: action.uri,
        method: action.method
      } as basicActionType<any>);
    });

    yield all(map(apiCalls, apiCall => call(restapiEffectSaga, apiCall)));
    restState = yield select(restapiState);
    let apiError = false,
      errorCode = "";
    apiCalls.forEach(call => {
      if (getError(restState, call.uri, call.method, call.id)) {
        errorCode = getErrorCode(restState, call.uri, call.method, call.id);

        apiError = true;
      }
    });

    if (apiError) {
      yield put(restapiError(true, action.uri, action.method, action.id));
      yield put(restapiErrorCode(errorCode, action.uri, action.method, action.id as any));
      yield put(restapiLoading(false, action.uri, action.method, action.id));
      apiCallsToClear.push(...apiCalls);
      yield all(apiCallsToClear.map(call => put(restapiClear(call.uri, call.method, call.id))));
      return;
    }

    let assigneeToCategoryScoreMapping: Dict<string, basicMappingType<number>> = {};
    const convertTotalToDays = ["effort_investment_time_spent", "azure_effort_investment_time_spent"].includes(
      widget?.query?.uri_unit
    );
    forEach(categoryList, category => {
      const id = `${JIRA_COMPLETED_DATAPOINTS_APICALL_ID}-${category?.name}`;
      const records: completedEffortRecord[] = get(restState, [action.uri, action.method, id, "data", "records"], []);
      forEach(records, record => {
        const prevMapping = (assigneeToCategoryScoreMapping as any)[record?.key];
        let defaultTotalValue =
          parseFloat((record?.total || 0)?.toString()) * parseFloat((record?.fte || 0)?.toString());
        if (widget?.query?.uri_unit === "tickets_report") {
          defaultTotalValue = Math.ceil(defaultTotalValue);
        }
        const totalValue = get(record, ["effort"], defaultTotalValue);
        (assigneeToCategoryScoreMapping as any)[record?.key] = {
          ...prevMapping,
          [category?.name]: {
            total: convertTotalToDays ? convertToDays(totalValue ?? 0, true) : (totalValue ?? 0).toFixed(2),
            fte: record?.fte ?? 0
          }
        };
      });
    });

    let finalApidata: completedEffortRecordType[] = map(Object.keys(assigneeToCategoryScoreMapping), key => {
      const remainingAllocation =
        1 -
        Object.values((assigneeToCategoryScoreMapping as any)?.[key] || {}).reduce(
          (acc: number, next: any) => acc + parseFloat(next?.fte || 0),
          0
        );

      const allocationSummary = {
        ...((assigneeToCategoryScoreMapping as any)?.[key] || {}),
        [REMAINING_ALLOCATION_KEY]: Math.max(remainingAllocation, 0).toString()
      };

      let newFixedAllocationSummary: any = {};
      forEach(Object.keys(allocationSummary), key => {
        if (key !== REMAINING_ALLOCATION_KEY) {
          const fte = getPercentageIn100rs(parseFloat(allocationSummary[key]?.fte || 0));
          newFixedAllocationSummary[key] = `${allocationSummary[key]?.total} | ${fte}`;
          return;
        }
        newFixedAllocationSummary[key] = getPercentageIn100rs(parseFloat(allocationSummary[key] || 0));
      });

      return {
        engineer: key,
        allocation_summary: newFixedAllocationSummary
      } as completedEffortRecordType;
    });

    const categories = [...uniq(categoryList.map(category => category.name)), REMAINING_ALLOCATION_KEY];
    categoryColorMapping[REMAINING_ALLOCATION_KEY] = CATEGORY_DEFAULT_BACKGORUND_COLOR;

    yield put(
      restapiData(
        { records: { apidata: finalApidata, categories, categoryColorMapping } },
        action.uri,
        action.method,
        action.id
      )
    );

    yield put(restapiError(false, action.uri, action.method, action.id));
    yield put(restapiLoading(false, action.uri, action.method, action.id));
    apiCallsToClear.push(...apiCalls);
    yield all(apiCallsToClear.map(call => put(restapiClear(call.uri, call.method, call.id))));
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });
  }
}

function* jiraEIActiveEngineerReportEffectSaga(action: basicActionType<any>): any {
  const widget: RestWidget = yield select(getWidget, { widget_id: action?.id });
  let filters = cloneDeep(action.data);
  const JIRA_ACTIVE_DATAPOINTS_APICALL_ID = `JIRA_ACTIVE_DATAPOINTS_APICALL_ID_${action.id}`;
  const isIssueManagement = action?.extra?.application === IntegrationTypes.AZURE;
  const effortProfileKey = "workitem_ticket_categorization_scheme";
  const selectedProfileId = get(filters, ["filter", TICKET_CATEGORIZATION_SCHEMES_KEY], undefined);
  const isCommitCount = [EffortUnitType.AZURE_COMMIT_COUNT, EffortUnitType.COMMIT_COUNT].includes(
    get(filters, ["filter", TICKET_CATEGORIZATION_UNIT_FILTER_KEY])
  );

  unset(filters, ["filter", TICKET_CATEGORIZATION_UNIT_FILTER_KEY]);
  unset(filters, ["filter", ACTIVE_WORK_UNIT_FILTER_KEY]);

  try {
    if (isIssueManagement) {
      filters = {
        ...(filters || {}),
        filter: {
          ...get(filters, ["filter"], {}),
          [effortProfileKey]: selectedProfileId
        }
      };
      let keysToUnset = [TICKET_CATEGORIZATION_SCHEMES_KEY];
      forEach(keysToUnset, key => {
        unset(filters, ["filter", key]);
      });
    }

    if (isCommitCount) {
      const timeRangeKey = isIssueManagement ? "workitem_resolved_at" : "issue_resolved_at";
      filters = {
        ...(filters || {}),
        filter: {
          ...get(filters, ["filter"], {}),
          [timeRangeKey]: get(filters, ["filter", "committed_at"])
        }
      };

      unset(filters, ["filter", "committed_at"]);
    }

    const keysToUnset = [
      BA_EFFORT_ATTRIBUTION_BE_KEY,
      BA_COMPLETED_WORK_STATUS_BE_KEY,
      BA_IN_PROGRESS_STATUS_BE_KEY,
      BA_HISTORICAL_ASSIGNEES_STATUS_BE_KEY
    ];

    forEach(keysToUnset, key => {
      unset(filters, ["filter", key]);
    });

    yield call(restapiEffectSaga, {
      id: JIRA_ACTIVE_DATAPOINTS_APICALL_ID,
      uri: action.uri,
      method: "list",
      data: filters
    });

    let restState = yield select(restapiState);
    const records = get(
      restState,
      [action.uri, action.method, JIRA_ACTIVE_DATAPOINTS_APICALL_ID, "data", "records"],
      []
    );

    let categories: string[] = [];
    let finalApidata: completedEffortRecordType[] = map(records, record => {
      const mapping: basicMappingType<number> = {};
      const categoryAllocations: basicMappingType<categoryAllocationType> = get(record, ["category_allocations"], {});
      forEach(Object.keys(categoryAllocations), category => {
        categories.push(category === "Other" ? UNCATEGORIZED : category);
        const totalAllocationEffort: categoryAllocationType = get(
          categoryAllocations,
          [category],
          {} as categoryAllocationType
        );
        let defaultTotalValue =
          parseFloat((totalAllocationEffort?.effort || 0)?.toString()) *
          parseFloat((totalAllocationEffort?.allocation || 0)?.toString());
        if (widget?.query?.active_work_unit.includes("tickets")) {
          defaultTotalValue = Math.ceil(defaultTotalValue);
        }
        const totalValue = get(totalAllocationEffort, ["effort"], defaultTotalValue);
        set(
          mapping,
          [category === "Other" ? UNCATEGORIZED : category],
          `${(totalValue ?? 0).toFixed(2)} | ${getPercentageIn100rs(totalAllocationEffort?.allocation ?? 0)}`
        );
      });
      return {
        engineer: get(record, ["key"], "") as string,
        allocation_summary: mapping,
        current_allocation: true
      };
    });

    /** Getting color as per categories */
    let categoryColorMapping: basicMappingType<string> = {};
    let categoryList: EICategoryTypes[] = [];

    yield call(restapiEffectSaga, {
      id: selectedProfileId,
      uri: "ticket_categorization_scheme",
      method: "get"
    });
    restState = yield select(restapiState);
    if (selectedProfileId) {
      const profile = get(restState, ["ticket_categorization_scheme", "get", selectedProfileId], undefined);
      if (profile) {
        categoryList = Object.values(get(profile, ["data", "config", "categories"], {}));
        uniqBy(categoryList, "name");
        /** handling color of a uncategorized category */
        set(categoryColorMapping, [UNCATEGORIZED], get(profile, ["data", "config", "uncategorized", "color"]));
      }
    }
    forEach(categoryList, (category, index) => {
      categoryColorMapping[category?.name] =
        category?.color || effortInvestmentDonutBarColors[index % (categories ?? []).length];
    });
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
        { records: { apidata: finalApidata, categories: uniq(categories), categoryColorMapping } },
        action.uri,
        action.method,
        action.id
      )
    );

    yield put(restapiError(false, action.uri, action.method, action.id));
    yield put(restapiLoading(false, action.uri, action.method, action.id));
    yield put(restapiClear(action.uri, action.method, JIRA_ACTIVE_DATAPOINTS_APICALL_ID));
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });
  }
}

export function* jiraEICompletedEngineerReportWatcherSaga() {
  yield takeEvery([JIRA_EI_COMPLETED_ENGINEER_REPORT], jiraEICompletedEngineerReportEffectSaga);
}

export function* jiraEIActiveEngineerReportWatcherSaga() {
  yield takeEvery([JIRA_EI_ACTIVE_ENGINEER_REPORT], jiraEIActiveEngineerReportEffectSaga);
}
