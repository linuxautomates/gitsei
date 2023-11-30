import {
  RESET_WIDGET_LIBRARY,
  RESET_WIDGET_LIBRARY_FILTERS,
  REST_REPORT_LIST,
  TABLE_LIBRARY_REPORT_LIST,
  WIDGET_LIBRARY_CATEGORY_SORT,
  WIDGET_LIBRARY_CATEGORY_UPDATED,
  WIDGET_LIBRARY_FILTER_UPDATED,
  WIDGET_LIBRARY_LIST_UPDATED,
  WIDGET_LIBRARY_SEARCH_QUERY_UPDATED
} from "reduxConfigs/actions/actionTypes";
import {
  getAllCompactReports,
  getFilteredCompactList,
  getFilteredCompactListByCategory,
  listOverWriteHelper
} from "../../dashboard/pages/explore-widget/reportHelper";

const DEFAULT_FILTERS = {
  applications: [],
  categories: [],
  search_query: "",
  supported_only: false
};

const INITIAL_STATE = {
  list: getAllCompactReports(),
  filtered_list: getAllCompactReports(),
  filters: DEFAULT_FILTERS,
  categories_list: getFilteredCompactListByCategory(getAllCompactReports()),
  sort: ""
};

const widgetLibraryReducer = (state = INITIAL_STATE, action: { type: string; data: any }) => {
  switch (action.type) {
    case WIDGET_LIBRARY_FILTER_UPDATED:
      return {
        ...state,
        categories_list: getFilteredCompactListByCategory(
          state.list,
          { ...action.data, search_query: state.filters.search_query },
          state.sort as any
        ),
        filtered_list: getFilteredCompactList(
          state.list,
          { ...action.data, search_query: state.filters.search_query },
          state.sort as any
        ),
        filters: {
          ...state.filters,
          applications: action.data.applications,
          categories: action.data.categories,
          supported_only: action.data.supported_only || false
        }
      };
    case WIDGET_LIBRARY_SEARCH_QUERY_UPDATED:
      return {
        ...state,
        categories_list: getFilteredCompactListByCategory(
          state.list,
          { ...state.filters, search_query: action.data },
          state.sort as any
        ),
        filtered_list: getFilteredCompactList(
          state.list,
          { ...state.filters, search_query: action.data },
          state.sort as any
        ),
        filters: {
          ...state.filters,
          search_query: action.data
        }
      };
    case WIDGET_LIBRARY_CATEGORY_UPDATED:
      return {
        ...state,
        filters: {
          ...state.filters,
          categories: action.data
        }
      };
    case WIDGET_LIBRARY_CATEGORY_SORT:
      return {
        ...state,
        categories_list: getFilteredCompactListByCategory(state.list, state.filters, action.data),
        filtered_list: getFilteredCompactList(state.list, state.filters, action.data),
        sort: action.data
      };
    case WIDGET_LIBRARY_LIST_UPDATED:
      return {
        ...state,
        list: action.data,
        filtered_list: getFilteredCompactList(action.data, state.filters, state.sort as any),
        categories_list: getFilteredCompactListByCategory(action.data, state.filters, state.sort as any)
      };
    case RESET_WIDGET_LIBRARY:
      return INITIAL_STATE;
    case TABLE_LIBRARY_REPORT_LIST:
      return {
        ...state,
        list: [...state.list, ...action.data],
        filtered_list: getFilteredCompactList([...state.list, ...action.data], state.filters, state.sort as any),
        categories_list: getFilteredCompactListByCategory(
          [...state.list, ...action.data],
          state.filters,
          state.sort as any
        )
      };

    case RESET_WIDGET_LIBRARY_FILTERS:
      return {
        ...state,
        filters: DEFAULT_FILTERS,
        sort: ""
      };

    case REST_REPORT_LIST:
      return {
        ...state,
        list: listOverWriteHelper(state.list, action.data),
        categories_list: getFilteredCompactListByCategory(
          listOverWriteHelper(state.list, action.data),
          state.filters,
          state.sort as any
        )
      };
    default:
      return state;
  }
};

export default widgetLibraryReducer;
