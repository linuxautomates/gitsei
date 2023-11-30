import {
  PAGINATION_CLEAR,
  PAGINATION_DATA,
  PAGINATION_DONE_LOADING,
  PAGINATION_ERROR,
  PAGINATION_FILTERS,
  PAGINATION_LOADING,
  PAGINATION_SET
} from "../actions/actionTypes";

const INITIAL_STATE = {
  pagination_data: [],
  pagination_function: null,
  pagination_loading: true,
  pagination_promise_token: null,
  pagination_page_number: 0,
  pagination_page_size: 100,
  pagination_error: false,
  pagination_pages: 1,
  pagination_total: 0,
  pagination_filter: {},
  uri: null,
  method: null
};

const paginationReducer = (state = INITIAL_STATE, action) => {
  switch (action.type) {
    case PAGINATION_LOADING:
      return { ...state, pagination_loading: true, pagination_data: [] };
    case PAGINATION_DONE_LOADING:
      return { ...state, pagination_loading: false };
    case PAGINATION_SET:
      return {
        ...INITIAL_STATE,
        uri: action.uri,
        method: action.method,
        pagination_promise_token: action.promise_token
      };
    case PAGINATION_DATA:
      let pages = Math.ceil(action.data._metadata.total_count / action.data._metadata.page_size);
      return {
        ...state,
        pagination_total: action.data._metadata.total_count,
        pagination_data: action.data.records,
        pagination_error: false,
        pagination_pages: pages
      };
    case PAGINATION_ERROR:
      return { ...state, pagination_error: true };
    case PAGINATION_FILTERS:
      return { ...state, pagination_filter: action.filter };
    case PAGINATION_CLEAR:
      return { ...state, pagination_loading: true, pagination_data: [], pagination_total: 0 };
    default:
      return state;
  }
};

export default paginationReducer;
