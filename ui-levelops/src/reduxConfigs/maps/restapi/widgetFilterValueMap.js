import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapWidgetFilterValueToProps = dispatch => {
  return {
    widgetFilterValuesGet: (uri, filters, complete = null, id = "0") =>
      dispatch(actionTypes.widgetFilterValuesGet(uri, filters, complete, id))
  };
};

export const mapGenericWidgetFilterValueToProps = dispatch => {
  return {
    genericWidgetFilterValuesGet: (supportedFilters, integrationIds, id = "0") =>
      dispatch(actionTypes.genericWidgetFilterValuesGet(supportedFilters, integrationIds, id))
  };
};
