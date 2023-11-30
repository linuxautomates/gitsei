import moment from "moment";
import { cloneDeep, forEach, get, set, unset } from "lodash";
import { all, put, takeEvery, call, select } from "redux-saga/effects";
import { JIRA_EFFORT_INVESTMENT_STAT } from "reduxConfigs/actions/actionTypes";
import {
  restapiData,
  restapiError,
  restapiLoading,
  restapiErrorCode,
  restapiClear
} from "reduxConfigs/actions/restapi";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { basicActionType, basicMappingType, basicRangeType } from "dashboard/dashboard-types/common-types";
import {
  eiSingleStateFiltersDefaultMapping,
  EISingleStatFiltersType
} from "./saga-types/effortInvestmentStatSaga.constant";
import { restapiEffectSaga } from "./restapiSaga";
import {
  BA_EFFORT_ATTRIBUTION_BE_KEY,
  TICKET_CATEGORIZATION_SCHEMES_KEY,
  WORKITEM_STATUS_CATEGORIES_ADO
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { EICategoryTypes } from "dashboard/dashboard-types/BAReports.types";
import { newEffortInvestmentSingleStatTransformer } from "custom-hooks/helpers/graphStatHelper";
import { UNCATEGORIZED_ID_SUFFIX } from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";
import { commitCountCompletedWorkURIs, timeRangeKeys } from "reduxConfigs/constants/effort-investment.constants";
import { getError, getErrorCode } from "utils/loadingUtils";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

const restapiState = (state: any) => state.restapiReducer;

const getFilters: (props: EISingleStatFiltersType) => basicMappingType<any> = (
  props = eiSingleStateFiltersDefaultMapping
) => {
  const {
    initialFilters,
    issueResolvedAtFilterKey,
    acrossValue,
    issueResolvedAtTimeRange,
    eiProfileKey,
    keysToUnset,
    statusCategoriesKey,
    statusCategoriesValue
  } = props;
  const usingOU = !!get(initialFilters, ["ou_ids"], undefined);

  let apiCallFilters: basicMappingType<any> = {
    across: acrossValue,
    filter: {
      ...get(initialFilters ?? {}, ["filter"], {})
    }
  };

  if (issueResolvedAtTimeRange) set(apiCallFilters, ["filter", issueResolvedAtFilterKey], issueResolvedAtTimeRange);

  if (eiProfileKey !== eiSingleStateFiltersDefaultMapping.eiProfileKey) {
    apiCallFilters = {
      ...apiCallFilters,
      filter: {
        ...get(apiCallFilters, ["filter"], {}),
        [eiProfileKey]: get(apiCallFilters, ["filter", eiSingleStateFiltersDefaultMapping.eiProfileKey], "")
      }
    };
  }

  if (statusCategoriesKey && statusCategoriesValue) {
    apiCallFilters = {
      ...apiCallFilters,
      filter: {
        ...get(apiCallFilters, ["filter"], {}),
        [statusCategoriesKey]: statusCategoriesValue
      }
    };
  }

  if (usingOU) {
    apiCallFilters = {
      ...(apiCallFilters || {}),
      ou_ids: get(initialFilters || {}, ["ou_ids"], []),
      ou_user_filter_designation: get(initialFilters || {}, ["ou_user_filter_designation"], {})
    };
  }

  forEach(keysToUnset, key => {
    unset(apiCallFilters, ["filter", key]);
  });
  return apiCallFilters;
};

const prepareFiltersOnTheBasisofCases = (
  uri: string = "",
  initialFilters: basicMappingType<any> = {},
  application = "jira"
): EISingleStatFiltersType => {
  let preparedFilters: EISingleStatFiltersType = {
    ...eiSingleStateFiltersDefaultMapping,
    initialFilters: initialFilters
  };

  const isIssueManagement = application === IntegrationTypes.AZURE;
  const isCommitCount = commitCountCompletedWorkURIs.includes(uri);

  if (isIssueManagement) {
    preparedFilters = {
      initialFilters: initialFilters,
      acrossValue: "ticket_category",
      issueResolvedAtFilterKey: "workitem_resolved_at",
      eiProfileKey: "workitem_ticket_categorization_scheme",
      statusCategoriesKey: "workitem_status_categories",
      statusCategoriesValue: WORKITEM_STATUS_CATEGORIES_ADO,
      keysToUnset: [
        ...eiSingleStateFiltersDefaultMapping.keysToUnset,
        eiSingleStateFiltersDefaultMapping.issueResolvedAtFilterKey,
        eiSingleStateFiltersDefaultMapping.eiProfileKey
      ]
    };
  }

  if (isCommitCount) {
    let moreKeysToUnset: string[] = [
      "workitem_resolved_at",
      eiSingleStateFiltersDefaultMapping.issueResolvedAtFilterKey,
      BA_EFFORT_ATTRIBUTION_BE_KEY
    ];

    if (isIssueManagement) {
      moreKeysToUnset.push(eiSingleStateFiltersDefaultMapping.eiProfileKey);
    }

    preparedFilters = {
      ...preparedFilters,
      issueResolvedAtFilterKey: "committed_at",
      keysToUnset: [...eiSingleStateFiltersDefaultMapping.keysToUnset, ...moreKeysToUnset]
    };
  }

  return preparedFilters;
};

const getBeforeIssueResolvedTimeRange = (filters: basicMappingType<any>, timeRangeKey: string) => {
  const curTimeRange: basicRangeType = get(filters, ["filter", timeRangeKey], {
    $gt: moment.utc().subtract(4, "months").startOf("day").unix().toString(),
    $lt: moment.utc().unix().toString()
  });

  let newTimeRange: basicRangeType = { $lt: "", $gt: "" };
  if (Object.keys(curTimeRange).length) {
    let gt = typeof curTimeRange.$gt === "string" ? parseInt(curTimeRange.$gt) : curTimeRange.$gt;
    let lt = typeof curTimeRange.$lt === "string" ? parseInt(curTimeRange.$lt) : curTimeRange.$lt;
    newTimeRange = {
      $lt: gt.toString(),
      $gt: (2 * gt - lt).toString()
    };
  }
  return newTimeRange;
};

const getTimeRangeKey = (application: string) => {
  if (application === IntegrationTypes.AZURE) return timeRangeKeys.AZURE;
  if (application === "scm") return timeRangeKeys.SCM;
  return timeRangeKeys.JIRA;
};
/** @summary*/
/**  Doing 2 api calls with across "ticket_category" and "issue_resolved_at" time ranges
 *   @variation in api call 2 is, we use "issue_resolved_at" time ranges "before" the current time range.
 * */

function* jiraEffortInvestmentStatEffectSaga(action: basicActionType<basicMappingType<any>>): any {
  const CURRENT_CALL_ID = `${action.id}-current`,
    BEFORE_CALL_ID = `${action.id}-before`;
  try {
    let apiCallsToClear: any[] = [],
      categories: EICategoryTypes[] = [],
      curRangeData: Array<{ key: string; fte: number }> = [],
      beforeRangeData: Array<{ key: string; fte: number }> = [];

    const isCommitCount = commitCountCompletedWorkURIs.includes(action?.uri);
    const selectedSchemeId = get(action?.data ?? {}, ["filter", TICKET_CATEGORIZATION_SCHEMES_KEY], undefined);
    const timeRangeKey = getTimeRangeKey(isCommitCount ? "scm" : action?.extra?.application);
    const initialApiCalls: Omit<basicActionType<basicMappingType<any>>, "type">[] = [
      {
        id: selectedSchemeId,
        uri: "ticket_categorization_scheme",
        method: "get"
      },
      {
        uri: action.uri,
        id: CURRENT_CALL_ID,
        data: getFilters(
          prepareFiltersOnTheBasisofCases(action?.uri, cloneDeep(action?.data ?? {}), action?.extra?.application)
        ),
        method: action.method
      },
      {
        uri: action.uri,
        id: BEFORE_CALL_ID,
        data: getFilters(
          prepareFiltersOnTheBasisofCases(
            action?.uri,
            {
              ...(action?.data ?? {}),
              filter: {
                ...get(action?.data ?? {}, ["filter"], {}),
                [timeRangeKey]: getBeforeIssueResolvedTimeRange(cloneDeep(action?.data ?? {}), timeRangeKey)
              }
            },
            action?.extra?.application
          )
        ),
        method: action.method
      }
    ];

    /** making api call */
    yield all(initialApiCalls.map(apiCall => call(restapiEffectSaga, apiCall)));
    yield put(restapiError(false, action.uri, action.method, action.id));
    const restState: basicMappingType<any> = yield select(restapiState);

    /* if any api call fails, get error and errorcode and dispatch error so error handling can be handled in BAWidgetAPi Wrapper */
    let apiError = false,
      errorCode = "";
    forEach(initialApiCalls, (apiCall, index) => {
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

    /** getting data */
    forEach(initialApiCalls, (apiCall, index) => {
      switch (index) {
        case 0:
          if (selectedSchemeId) {
            const scheme = get(restState, [apiCall.uri, apiCall.method, apiCall.id], undefined);
            if (scheme) {
              categories = Object.values(get(scheme, ["data", "config", "categories"], {}));
              categories.push({
                name: "Other",
                goals: get(scheme ?? {}, ["data", "config", "uncategorized", "goals"]),
                id: UNCATEGORIZED_ID_SUFFIX
              });
            }
          }
          break;
        case 1:
          curRangeData = get(restState, [apiCall.uri, apiCall.method, apiCall.id, "data", "records"], []);
          break;
        case 2:
          beforeRangeData = get(restState, [apiCall.uri, apiCall.method, apiCall.id, "data", "records"], []);
          break;
      }
    });

    const transformedData = newEffortInvestmentSingleStatTransformer(curRangeData, beforeRangeData, categories);
    yield put(restapiData({ records: transformedData }, action.uri, action.method, action?.id));
    yield put(restapiError(false, action.uri, action.method, action.id));
    yield put(restapiLoading(false, action.uri, action.method, action?.id));
    apiCallsToClear.push(...initialApiCalls.slice(1));
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

    yield put(restapiError(true, action.uri, action.method, action?.id));
    yield put(restapiLoading(false, action.uri, action.method, action?.id));
  }
}

export function* jiraEffortInvestmentStatWatcherSaga() {
  yield takeEvery([JIRA_EFFORT_INVESTMENT_STAT], jiraEffortInvestmentStatEffectSaga);
}
