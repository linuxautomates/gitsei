import * as actionTypes from "reduxConfigs/actions/restapi";

export const maptriageRulesToProps = dispatch => {
  return {
    triageRulesCreate: item => dispatch(actionTypes.triageRulesCreate(item)),
    triageRulesDelete: id => dispatch(actionTypes.triageRulesDelete(id)),
    triageRulesUpdate: (id, item) => dispatch(actionTypes.triageRulesUdpate(id, item)),
    triageRulesGet: id => dispatch(actionTypes.triageRulesGet(id)),
    triageRulesList: (filters, id = "0") => dispatch(actionTypes.triageRulesList(filters, id)),
    triageRulesBulkDelete: ids => dispatch(actionTypes.triageRulesBulkDelete(ids))
  };
};
