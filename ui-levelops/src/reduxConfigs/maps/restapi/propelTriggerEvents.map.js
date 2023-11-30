import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapPropelTriggerEventsToProps = dispatch => {
  return {
    propelTriggerEventsGet: (id, complete = null) => dispatch(actionTypes.propelTriggerEventsGet(id, complete)),
    propelTriggerEventsList: (filters, complete = null) =>
      dispatch(actionTypes.propelTriggerEventsList(filters, complete))
  };
};
