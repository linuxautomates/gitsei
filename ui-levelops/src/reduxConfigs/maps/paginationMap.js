import { paginationClear, paginationGet, paginationSet } from "../actions/paginationActions";

export const mapPaginationStatetoProps = state => {
  return {
    pagination_data: state.paginationReducer.pagination_data,
    pagination_loading: state.paginationReducer.pagination_loading,
    pagination_error: state.paginationReducer.pagination_error,
    pagination_pages: state.paginationReducer.pagination_pages,
    pagination_total: state.paginationReducer.pagination_total
  };
};

export const mapPaginationDispatchtoProps = dispatch => {
  return {
    paginationGet: (uri, method, filters, id = "0", derive = true, deriveOnly = "all", complete = null, payload = {}) =>
      dispatch(paginationGet(uri, method, filters, id, derive, deriveOnly, complete, payload)),
    paginationSet: (uri, method, promiseToken) => dispatch(paginationSet(uri, method, promiseToken)),
    paginationClear: () => dispatch(paginationClear())
  };
};
