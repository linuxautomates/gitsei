import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapRestapiToProps = (dispatch: any) => {
  return {
    restapiClearAll: () => dispatch(actionTypes.restapiClearAll()),
    restapiClear: (uri: string, method: string, id?: string | number) =>
      dispatch(actionTypes.restapiClear(uri, method, id)),
    restapiLoading: (loading: boolean, uri: string, method: string, id: string | number | undefined) =>
      dispatch(actionTypes.restapiLoading(loading, uri, method, id))
  };
};

export type REST_API_CLEAR_TYPE = (uri: string, method: string, id?: string | number) => any;
