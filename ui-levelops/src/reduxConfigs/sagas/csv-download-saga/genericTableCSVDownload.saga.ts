import { issueContextTypes, severityTypes } from "bugsnag";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { saveAs } from "file-saver";
import { handleError } from "helper/errorReporting.helper";
import { get } from "lodash";
import { call, put, select, takeLatest } from "redux-saga/effects";
import { GENERIC_TABLE_CSV_DOWNLOAD } from "reduxConfigs/actions/actionTypes";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { restapiState } from "reduxConfigs/selectors/restapiSelector.js";

import { paginationEffectSaga } from "../paginationSaga";

export type genericTableCSVDownloadActionType = {
  type: string;
  uri: string;
  method: string;
  queryparams?: basicMappingType<string>;
  data: {
    transformer: ((data: any) => any) | undefined;
    filters: { page_size: number; page: number; [x: string]: any };
    columns: any[];
    derive?: boolean;
    shouldDerive?: any;
    jsxHeaders?: { title: string; key: string }[];
  };
};

const mappedID = (uri: string) => `${uri}_csv`;

export function* genericTableCSVDownloadEffectSage(action: genericTableCSVDownloadActionType): any {
  const {
    uri,
    method,
    data: { filters, transformer, columns, derive, shouldDerive, jsxHeaders }
  } = action;

  try {
    let csvText = "";
    const headers = [...(jsxHeaders || []).map(col => col.title), ...(columns || []).map(col => col.title)];
    let mergedJSXHeadersColumns = [
      ...(jsxHeaders || []).map(col => {
        if (col.title && col.key) return { title: col.title, key: col.key };
      }),
      ...(columns || []).map(col => {
        if (col.title && col.key) return { title: col.title, key: col.key };
      })
    ].filter(col => !!col);

    csvText = headers.join(",").concat("\n");
    let page = 0;
    let totalPages = 999;
    filters.page_size = 1000;
    while (totalPages > page) {
      filters.page = page;
      yield call(paginationEffectSaga, {
        uri,
        method,
        filters,
        id: mappedID(uri),
        derive,
        deriveOnly: shouldDerive,
        payload: { queryParam: action?.queryparams ?? {} }
      });
      const csvState = yield select(restapiState);
      const tableData = get(csvState, [uri, "list", mappedID(uri), "data", "records"], []);
      const csvData = transformer?.({ apiData: tableData, columns, jsxHeaders: mergedJSXHeadersColumns, filters });
      csvText = csvText.concat(csvData.join("\n")).concat("\n");
      if (page === 0) {
        // this is the first page, so set the metadata stuff correctly here
        const metadata = get(csvState, [uri, "list", mappedID(uri), "data", "_metadata"], {});
        totalPages = Math.ceil(metadata.total_count / filters.page_size);
      }
      page += 1;
    }
    const file = new File([csvText], `${uri}-drilldown.csv`, { type: "text/csv;charset=utf-8" });
    saveAs(file);
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.FILES,
        data: { e, action }
      }
    });
  }
  yield put(actionTypes.restapiClear(uri, method, mappedID(uri)));
}

export function* genericTableCSVDownloadWatcherSaga() {
  yield takeLatest([GENERIC_TABLE_CSV_DOWNLOAD], genericTableCSVDownloadEffectSage);
}
