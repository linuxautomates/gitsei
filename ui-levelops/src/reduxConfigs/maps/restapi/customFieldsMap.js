import * as actionTypes from "reduxConfigs/actions/restapi";
import { getCustomFieldsListSelector } from "reduxConfigs/selectors/restapiSelector";

export const mapCustomFieldsDispatchToProps = dispatch => ({
  customFieldsList: filters => dispatch(actionTypes.customFieldsList(filters)),
  customFieldsUpdate: (id, fields) => dispatch(actionTypes.customFieldsUpdate(id, fields))
});

export const mapCustomFieldsStateToProps = state => ({
  custom_fields: getCustomFieldsListSelector(state)
});
