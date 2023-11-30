import {
  RESET_WIDGET_LIBRARY,
  RESET_WIDGET_LIBRARY_FILTERS,
  REST_REPORT_LIST,
  SET_REPORTS,
  TABLE_LIBRARY_REPORT_LIST,
  WIDGET_LIBRARY_CATEGORY_SORT,
  WIDGET_LIBRARY_CATEGORY_UPDATED,
  WIDGET_LIBRARY_FILTER_UPDATED,
  WIDGET_LIBRARY_LIST_UPDATED,
  WIDGET_LIBRARY_SEARCH_QUERY_UPDATED,
  WIDGET_REPORTS_LIST
} from "./actionTypes";
import CompactReport from "../../model/report/CompactReport";

export const loadReports = () => ({
  type: WIDGET_REPORTS_LIST
});

export const setReports = () => ({
  type: SET_REPORTS
});

export const updateWidgetLibraryList = (data: {
  applications: string[];
  categories: string[];
  supported_only: boolean;
}) => ({
  type: WIDGET_LIBRARY_FILTER_UPDATED,
  data
});

export const updateWidgetLibrarySearchQuery = (data: string) => ({
  type: WIDGET_LIBRARY_SEARCH_QUERY_UPDATED,
  data
});

export const updateWidgetLibraryCategory = (data: string[]) => ({
  type: WIDGET_LIBRARY_CATEGORY_UPDATED,
  data
});

export const updateWidgetLibrarySort = (data: string) => ({
  type: WIDGET_LIBRARY_CATEGORY_SORT,
  data
});

export const resetWidgetLibraryState = () => ({
  type: RESET_WIDGET_LIBRARY
});

export const setWidgetLibraryList = (data: CompactReport[]) => ({
  type: WIDGET_LIBRARY_LIST_UPDATED,
  data
});

export const tableReportList = (data: any) => ({
  type: TABLE_LIBRARY_REPORT_LIST,
  data
});

export const resetWidgetLibraryFilters = () => ({
  type: RESET_WIDGET_LIBRARY_FILTERS
});

export const restReportList = (data: CompactReport[]) => ({
  type: REST_REPORT_LIST,
  data
});
