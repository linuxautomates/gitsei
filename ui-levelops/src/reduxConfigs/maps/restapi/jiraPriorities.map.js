import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapJiraPrioritiesToProps = dispatch => {
  return {
    jiraPrioritiesUpdate: (id, item, complete = null) => dispatch(actionTypes.jiraPrioritiesUpdate(id, item, complete)),
    jiraPrioritiesList: (filters, id = "0") => dispatch(actionTypes.jiraPrioritiesList(filters, id))
  };
};
