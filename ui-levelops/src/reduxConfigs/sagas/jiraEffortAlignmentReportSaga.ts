import { cloneDeep, forEach, get, uniqBy, unset } from "lodash";
import { basicActionType, basicMappingType } from "dashboard/dashboard-types/common-types";
import { all, call, select, put, takeEvery } from "redux-saga/effects";
import { JIRA_EI_ALIGNMENT_REPORT } from "reduxConfigs/actions/actionTypes";

import {
  aligmentDataType,
  alignmentActiveConfig,
  categoryAlignmentConfig,
  categoryAllocationType,
  EICategoryTypes
} from "dashboard/dashboard-types/BAReports.types";
import {
  ACTIVE_WORK_UNIT_FILTER_KEY,
  TICKET_CATEGORIZATION_SCHEMES_KEY
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { restapiEffectSaga } from "./restapiSaga";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";
import {
  restapiClear,
  restapiData,
  restapiError,
  restapiErrorCode,
  restapiLoading
} from "reduxConfigs/actions/restapi";
import { getError, getErrorCode } from "utils/loadingUtils";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

function* jiraEffortAlignmentEffect(action: basicActionType<basicMappingType<any>>): any {
  const JIRA_DATAPOINTS_APICALL_ID = `JIRA_DATAPOINTS_APICALL_ID_${action.id}`;
  let filters = cloneDeep(action.data);
  const isIssueManagement = action?.extra?.application === IntegrationTypes.AZURE;
  const effortProfileKey = "workitem_ticket_categorization_scheme";
  unset(filters, ["filter", ACTIVE_WORK_UNIT_FILTER_KEY]);

  try {
    let categoryList: EICategoryTypes[] = [];
    let finalApidata: aligmentDataType = {} as aligmentDataType;
    const selectedProfileId = get(filters, ["filter", TICKET_CATEGORIZATION_SCHEMES_KEY], undefined);

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

    let apiCalls: basicActionType<any>[] = [
      { type: "", id: JIRA_DATAPOINTS_APICALL_ID, uri: action.uri, method: "list", data: filters }
    ];

    yield call(restapiEffectSaga, {
      id: selectedProfileId,
      uri: "ticket_categorization_scheme",
      method: "get"
    });

    yield all(apiCalls.map(ccall => call(restapiEffectSaga, ccall)));

    yield put(restapiError(false, action.uri, action.method, action.id));

    let restState = yield select(restapiState);
    let apiError = false,
      errorCode = "";

    apiCalls.forEach(call => {
      if (getError(restState, action.uri, action.method, call.id)) {
        errorCode = getErrorCode(restState, action.uri, action.method, call.id);
        apiError = true;
      }
    });

    if (apiError) {
      console.log("apiError errorcode", action.id, "-", errorCode);
      yield put(restapiError(true, action.uri, action.method, action.id as any));
      yield put(restapiErrorCode(errorCode, action.uri, action.method, action.id as any));
      yield put(restapiLoading(false, action.uri, action.method, action.id));
      yield all(apiCalls.map(call => put(restapiClear(call.uri, call.method, call.id))));
      return;
    }

    let profile: any = undefined;
    if (selectedProfileId) {
      profile = get(restState, ["ticket_categorization_scheme", "get", selectedProfileId], undefined);
      if (profile) {
        categoryList = Object.values(get(profile, ["data", "config", "categories"], {}));
        uniqBy(categoryList, "name");
      }
    }
    let activeWorkRecords: alignmentActiveConfig[] = get(
      restState,
      [action.uri, action.method, JIRA_DATAPOINTS_APICALL_ID, "data", "records"],
      []
    );

    let activeRecord: alignmentActiveConfig = {} as alignmentActiveConfig;

    if (activeWorkRecords.length) {
      activeRecord = activeWorkRecords[0];
    }

    finalApidata.total_alignment_score = activeRecord?.alignment_score ?? 0;
    let ncategories: categoryAlignmentConfig[] = [];

    forEach(Object.keys(activeRecord?.category_allocations), key => {
      const categoryConfig: categoryAllocationType = activeRecord?.category_allocations?.[key] ?? {};
      let category = categoryList.find(category => category.name === key);
      if (key === "Other") {
        category = get(profile, ["data", "config", "uncategorized"], undefined);
      }
      let idealRange = undefined;
      let color: string | undefined = "";
      if (category) {
        idealRange = category?.goals?.ideal_range;
        color = category?.color;

        // hiding those categories which has alignment_score = 0 || ideal_range is not defined
        if (categoryConfig?.alignment_score > 0 && Object.keys(idealRange ?? {}).length) {
          ncategories.push({
            id: category?.id ?? category?.name ?? key,
            name: category?.name ?? key,
            config: {
              ...categoryConfig,
              ideal_range: idealRange,
              color: color
            }
          });
        }
      }
    });
    finalApidata.categories = ncategories;
    finalApidata.profileId = selectedProfileId;
    yield put(restapiData({ records: finalApidata }, action.uri, action.method, action.id));
    yield put(restapiError(false, action.uri, action.method, action.id));
    yield put(restapiLoading(false, action.uri, action.method, action.id));
    yield all(apiCalls.map(call => put(restapiClear(call.uri, call.method, call.id))));
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

export function* jiraEffortAlignmentWatcher() {
  yield takeEvery(JIRA_EI_ALIGNMENT_REPORT, jiraEffortAlignmentEffect);
}
