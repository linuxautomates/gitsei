import { all, call, put, take, takeEvery } from "redux-saga/effects";
import { DEV_PRODUCTIVITY_SCORE_REPORT_LIST } from "../actions/actionTypes";
import { get } from "lodash";
import { restapiData, restapiLoading } from "../actions/restapi";
import RestapiService from "../../services/restapiService";
import { handleRestApiError } from "./restapiSaga";
import { REPORT_REQUIRES_OU } from "constants/formWarnings";

const DEV_PROD_OU_SCORE_REPORT_URI = "dev_productivity_org_unit_score_report";
const DEV_PROD_USER_SCORE_REPORT_URI = "dev_productivity_user_score_report";

export function* devProductivityScoreReportSaga(action: any): any {
  const restService = new RestapiService();

  const actionId = action?.id;
  yield put(restapiLoading(true, action.uri, action.method, actionId));

  const interval = get(action, ["data", "filter", "interval"], "LAST_MONTH");
  const ou_ids = get(action, ["data", "filter", "ou_ids"], []);
  const ou_ref_ids = get(action, ["data", "filter", "ou_ref_ids"], []);
  const user_id_type = get(action, ["data", "filter", "user_id_type"], "ou_user_ids");
  const user_id_list = get(action, ["data", "filter", "user_id_list"], []);
  const page = get(action, ["data", "page"], 0);
  const page_size = get(action, ["data", "page_size"], 10);
  let sort = get(action, ["data", "sort"], []);

  if (!sort[0]) {
    sort = null;
  }

  const paginationAndSortFilter = { page, page_size: page_size - 1 };

  const user_filter = {
    ...paginationAndSortFilter,
    sort,
    filter: {
      interval,
      ou_ids,
      ou_ref_ids,
      user_id_type,
      user_id_list
    }
  };

  const org_filter = {
    filter: {
      interval,
      ou_ref_ids
    }
  };

  const calls = [
    {
      index: 0,
      uri: DEV_PROD_OU_SCORE_REPORT_URI,
      filter: org_filter,
      method: "list"
    },
    {
      index: 1,
      uri: DEV_PROD_USER_SCORE_REPORT_URI,
      filter: user_filter,
      method: "list"
    }
  ];

  let response: any;

  try {
    if (ou_ref_ids.length || ou_ids.length) {
      response = yield all(calls.map(_call => call(get(restService, [_call.uri, _call.method]), _call.filter)));
      if (!!response) {
        const data: any = {
          records: [],
          count: 0,
          _metadata: {}
        };
        calls.forEach(_call => {
          let selectedRecords = response[_call.index]?.data?.records || [];
          if (_call.uri === DEV_PROD_OU_SCORE_REPORT_URI) {
            if (ou_ref_ids.length) {
              selectedRecords = selectedRecords.filter((record: any) => ou_ref_ids.includes(record.org_ref_id));
            } else {
              selectedRecords = selectedRecords.filter((record: any) => ou_ids.includes(record.org_id));
            }
          }
          data.records = [...data.records, ...selectedRecords];
        });
        data.count = response[1]?.data?.count || 0;
        data._metadata = {
          ...data._metadata,
          ...response[1]?.data?._metadata
        };
        yield put(restapiData(data, action.uri, action.method, actionId));
      }
    } else {
      const data: any = {
        records: [],
        count: 0,
        _metadata: {
          message: REPORT_REQUIRES_OU
        }
      };
      yield put(restapiData(data, action.uri, action.method, actionId));
    }

    yield put(restapiLoading(false, action.uri, action.method, actionId));
    if (!!action?.complete) {
      yield put({ type: action.complete });
    }
  } catch (e) {
    yield call(handleRestApiError, e, { ...action, showNotfication: false }, response);
    yield put(restapiLoading(false, action.uri, action.method, actionId));
  }
}

export function* devProductivityScoreReportWatcherSaga() {
  yield takeEvery([DEV_PRODUCTIVITY_SCORE_REPORT_LIST], devProductivityScoreReportSaga);
}
