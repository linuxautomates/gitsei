import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapAzurePrioritiesToProps = (dispatch: any) => {
  return {
    azurePrioritiesUpdate: (id: any, item: any, complete: any = null) =>
      dispatch(actionTypes.azurePrioritiesUpdate(id, item, complete))
  };
};
