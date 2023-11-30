import {
  CSV_DOWLOAD_DRILLDOWN,
  CSV_DOWLOAD_SAMPLE_USER,
  CSV_DOWLOAD_TRIAGE_GRID_VIEW,
  CSV_DOWLOAD_USER,
  DEV_PRODUCTIVITY_CSV_DOWNLOAD,
  RAW_STATS_CSV_DOWNLOAD
} from "./actionTypes";

export const csvDownloadDrilldown = (uri, method, filters, columns) => ({
  type: CSV_DOWLOAD_DRILLDOWN,
  uri: uri,
  method: method,
  filters: filters,
  columns: columns
});

export const csvDownloadTriageGridView = (uri, method, filters, columns) => ({
  type: CSV_DOWLOAD_TRIAGE_GRID_VIEW,
  uri: uri,
  method: method,
  filters: filters,
  columns: columns
});

export const csvDownloadUser = (uri, method, data) => ({
  type: CSV_DOWLOAD_USER,
  uri: uri,
  method: method,
  data: data
});

export const csvDownloadDevProductivity = (dashboardId, widgetId, queryparams = {}) => ({
  type: DEV_PRODUCTIVITY_CSV_DOWNLOAD,
  dashboardId: dashboardId,
  widgetId: widgetId,
  queryparams: queryparams
});

export const csvDownloadRawStats = (dashboardId, widgetId, queryparams = {}) => ({
  type: RAW_STATS_CSV_DOWNLOAD,
  dashboardId: dashboardId,
  widgetId: widgetId,
  queryparams: queryparams
});

export const csvDownloadSampleUser = () => ({ type: CSV_DOWLOAD_SAMPLE_USER });
