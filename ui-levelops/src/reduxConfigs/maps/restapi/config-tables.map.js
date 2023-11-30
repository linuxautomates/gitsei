import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapConfigTablesToProps = dispatch => {
  return {
    configTablesList: (filters, id = "0") => dispatch(actionTypes.configTablesList(filters, id)),
    configTablesGet: (tableId, complete = null) => dispatch(actionTypes.configTablesGet(tableId, complete)),
    configTablesCreate: (tableData, complete = null) => dispatch(actionTypes.configTablesCreate(tableData, complete)),
    configTablesUpdate: (tableId, tableData, complete = null) =>
      dispatch(actionTypes.configTablesUpdate(tableId, tableData, complete)),
    configTablesDelete: tableId => dispatch(actionTypes.configTablesDelete(tableId)),
    configTablesBulkDelete: ids => dispatch(actionTypes.configTablesBulkDelete(ids))
  };
};
