import { call, put, select, take, takeLatest } from "redux-saga/effects";
import * as actionTypes from "reduxConfigs/actions/restapi";
import {
  CSV_DOWLOAD_DRILLDOWN,
  CSV_DOWLOAD_SAMPLE_USER,
  CSV_DOWLOAD_TRIAGE_GRID_VIEW,
  CSV_DOWLOAD_USER
} from "../actions/actionTypes";
import { get, orderBy, uniq, uniqBy, capitalize } from "lodash";
import { convertEpochToDate } from "../../utils/dateUtils";
import { saveAs } from "file-saver";
import { notification } from "antd";
import { csvDataTransformer } from "dashboard/helpers/csv-transformers/csvDataTransformer";
import { paginationEffectSaga } from "./paginationSaga";
import { getData } from "../../utils/loadingUtils";
import { BASE_UI_URL } from "helper/envPath.helper";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { getIsStandaloneApp } from "helper/helper";

const restapiState = state => state.restapiReducer;

const maxRowsAllowedInCsv = 10000;

export function* csvDownloadDrilldownEffectSaga(action) {
  const fileName = `${action.uri}-drilldown.csv`;
  try {
    let csvText = "";
    const headers = (action.columns || []).map(col => col.title);
    csvText = headers.join(",").concat("\n");
    // now after the headers are done, start paginating
    // make the first call to determine the total number of calls to be made
    const complete = `COMPLETE_${action.uri}_drilldown`;
    let page = 0;
    let totalPages = 999;
    let filters = action.filters;
    filters.page_size = 1000;
    const maxPages = maxRowsAllowedInCsv / filters.page_size;
    notification.info({
      message: "Download Started."
    });
    while (totalPages > page) {
      if (page === maxPages) {
        notification.info({
          message: `Downloading only first ${maxRowsAllowedInCsv} rows.`
        });
        break;
      }
      filters.page = page;
      yield put(actionTypes.genericList(action.uri, "list", filters, complete, "csv", false));
      yield take(complete);
      const csvState = yield select(restapiState);
      if (get(csvState, [action.uri, "list", "csv", "error"], false) === true) {
        return;
      }
      const tableData = get(csvState, [action.uri, "list", "csv", "data", "records"], []);
      const csvData = tableData.map(record => {
        return action.columns
          .map(col => {
            const csvValue = csvDataTransformer(action, col.key, record);
            return csvValue;
          })
          .join(",");
      });
      csvText = csvText.concat(csvData.join("\n")).concat("\n");
      if (page === 0) {
        // this is the first page, so set the metadata stuff correctly here
        const metadata = get(csvState, [action.uri, "list", "csv", "data", "_metadata"], {});
        totalPages = Math.ceil(metadata.total_count / filters.page_size);
      }
      page += 1;
    }
    const file = new File([csvText], fileName, { type: "text/csv;charset=utf-8" });
    saveAs(file);
  } catch (e) {}

  yield put(actionTypes.restapiClear(action.uri, "list", "csv"));
}

export function* csvDownloadDrilldownWatcherSaga() {
  yield takeLatest([CSV_DOWLOAD_DRILLDOWN], csvDownloadDrilldownEffectSaga);
}

export function* csvDownloadTriageGridViewEffectSaga(action) {
  const fileName = `${action.uri}-drilldown.csv`;
  let dataToWrite = "";
  const allCSVData = [];
  try {
    // now after the headers are done, start paginating
    // make the first call to determine the total number of calls to be made
    const complete = `COMPLETE_${action.uri}_drilldown`;
    let page = 0;
    let totalPages = 99;
    let filters = action.filters;
    filters.page_size = 100;
    const maxPages = maxRowsAllowedInCsv / filters.page_size;
    while (totalPages > page) {
      if (page === maxPages) {
        notification.info({
          message: `Downloading only first ${maxRowsAllowedInCsv} rows.`
        });
        break;
      }
      filters.page = page;
      yield put(actionTypes.genericList(action.uri, "list", filters, complete, "csv", false));
      yield take(complete);
      const csvState = yield select(restapiState);
      if (get(csvState, [action.uri, "list", "csv", "error"], false) === true) {
        return;
      }
      const tableData = get(csvState, [action.uri, "list", "csv", "data", "records"], []);
      allCSVData.push(...tableData);
      if (page === 0) {
        // this is the first page, so set the metadata stuff correctly here
        const metadata = get(csvState, [action.uri, "list", "csv", "data", "_metadata"], {});
        totalPages = Math.ceil(metadata.total_count / filters.page_size);
      }
      page += 1;
    }
  } catch (e) {}

  allCSVData.map(record => {
    if (record && record.aggs && record.aggs.length) {
      record.aggs.map(({ key, totals }) => {
        record[`dynamic_column_aggs_${key}`] = totals;
      });
    }
  });

  let cols = [];
  allCSVData.map(record => {
    const dynamicColumnKeys = Object.keys(record).filter(key => key.includes("dynamic_column_"));
    dynamicColumnKeys.map((dynamicColumn, i) => {
      const alreadyExists = cols.find(column => {
        return column.key === dynamicColumn;
      });
      if (!alreadyExists) {
        cols.push({
          title: convertEpochToDate(dynamicColumn.replace("dynamic_column_aggs_", ""), "LL"),
          key: dynamicColumn
        });
      }
    });
    cols = orderBy(cols, ["key"], ["desc"]);
  });

  dataToWrite = ["name", cols.map(item => `"${item.title}",,`)].join(",").concat("\n"); // header
  const subHeader = ["", cols.map(item => "Success,Failed,Aborted")].join(",").concat("\n");
  dataToWrite = dataToWrite + subHeader;
  allCSVData.forEach(row => {
    const calRow = [
      row.name,
      cols.map(item => {
        const cell = row[item.key];
        if (!!cell) {
          const success = cell["SUCCESS"] || "";
          const failure = cell["FAILURE"] || "";
          const aborted = cell["ABORTED"] || "";
          return `${success},${failure},${aborted}`;
        } else {
          return ",,";
        }
      })
    ].join(",");
    dataToWrite = dataToWrite + calRow.concat("\n");
  });
  yield put(actionTypes.restapiClear(action.uri, "list", "csv"));
  const file = new File([dataToWrite], fileName, { type: "text/csv;charset=utf-8" });
  saveAs(file);
}

export function* csvDownloadTriageGridViewWatcherSaga() {
  yield takeLatest([CSV_DOWLOAD_TRIAGE_GRID_VIEW], csvDownloadTriageGridViewEffectSaga);
}
const mappedID = uri => `${uri}_csv`;

export function* usersCSVDownloadEffectSaga(action) {
  const {
    uri,
    method,
    data: { filters, transformer, derive, shouldDerive, jsxHeaders }
  } = action;

  try {
    let page = 0;
    let totalPages = 999;
    const version = filters?.version || 1;
    const payload = {
      queryParam: {
        version
      }
    };

    delete filters["version"];

    filters.page_size = 1000;
    let tableData = [];
    while (totalPages > page) {
      filters.page = page;
      yield call(paginationEffectSaga, {
        uri,
        method,
        filters,
        id: mappedID(uri),
        derive,
        deriveOnly: shouldDerive,
        payload
      });
      const csvState = yield select(restapiState);
      tableData = [...(tableData || []), ...get(csvState, [uri, "list", mappedID(uri), "data", "records"], [])];
      if (page === 0) {
        // this is the first page, so set the metadata stuff correctly here
        const metadata = get(csvState, [uri, "list", mappedID(uri), "data", "_metadata"], {});
        totalPages = Math.ceil(metadata.total_count / filters.page_size);
      }
      page += 1;
    }

    const USER_SCHEMA_URI = "org_users_schema";
    const METHOD = "get";
    const COMPLETE = "COMPLETE_CSV_SCHEMA";
    const SCHEMA_ID = "csv_schema";

    yield put(actionTypes.genericList(USER_SCHEMA_URI, METHOD, {}, COMPLETE, SCHEMA_ID, false, { version }));
    yield take(COMPLETE);
    const csvSchemaState = yield select(restapiState);

    if (get(csvSchemaState, [USER_SCHEMA_URI, METHOD, SCHEMA_ID, "error"], false) === true) {
      return;
    }

    const schemaData = getData(csvSchemaState, USER_SCHEMA_URI, METHOD, SCHEMA_ID);
    const schemaRecords = get(schemaData, ["fields"], []);

    const fixedColumns = [
      {
        key: "full_name",
        title: "Full_Name"
      },
      {
        key: "email",
        title: "Email"
      }
    ];

    const additionColumns = schemaRecords
      .filter(item => !["full_name", "email", "integration", "start_date"].includes(item.key))
      .sort((a, b) => a.index - b.index)
      .map(item => {
        return {
          key: `additional@${item.key}`,
          title: capitalize(item.key),
          type: item.type
        };
      });

    const missingColumns = tableData.reduce((acc, next) => {
      const fields = Object.keys(get(next, "additional_fields", {}));
      const _columns = fields.map(field => ({
        key: `additional@${field}`,
        title: capitalize(field)
      }));
      return uniqBy([...acc, ..._columns], "key");
    }, []);

    const _columns = uniqBy([...additionColumns, ...missingColumns], "key");

    const integrationColumns = tableData.reduce((acc, next) => {
      const _columns = (next.integration_user_ids || []).map(item => ({
        key: `integration@${item.name}`,
        title: `Integration: ${capitalize(item.name)}`
      }));
      return uniqBy([...acc, ..._columns], "key");
    }, []);

    const updatedColumns = [...(fixedColumns || []), ...(_columns || []), ...(integrationColumns || [])];

    const headers = [...(jsxHeaders || []), ...updatedColumns].map(col => col.title);

    let csvText = "";
    csvText = headers.join(",").concat("\n");

    if (transformer) {
      const csvData = transformer?.({ apiData: tableData, columns: updatedColumns, jsxHeaders });
      csvText = csvText.concat(csvData.join("\n")).concat("\n");
    }

    const file = new File([csvText], `user_${version}.csv`, { type: "text/csv;charset=utf-8" });
    saveAs(file);
  } catch (e) {
    handleError({
      bugsnag: {
        message: e?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.FILES,
        data: { e, action }
      }
    });
  }
  yield put(actionTypes.restapiClear(uri, method, mappedID(uri)));
}

export function* usersSampleCSVDownloadEffectSaga() {
  try {
    const baseUrl = getIsStandaloneApp() ? BASE_UI_URL : `${BASE_UI_URL}/sei/static`
    saveAs(`${baseUrl}/sample/sample_user.csv`, "sample_user.csv");
  } catch (e) {
    handleError({
      bugsnag: {
        message: e?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.FILES,
        data: { e }
      }
    });
  }
}

export function* usersCSVDownloadWatcherSaga() {
  yield takeLatest([CSV_DOWLOAD_USER], usersCSVDownloadEffectSaga);
}

export function* usersSampleCSVDownloadWatcherSaga() {
  yield takeLatest([CSV_DOWLOAD_SAMPLE_USER], usersSampleCSVDownloadEffectSaga);
}
