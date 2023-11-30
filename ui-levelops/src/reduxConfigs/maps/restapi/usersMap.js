import * as actionTypes from "reduxConfigs/actions/restapi";

export const mapUsersToProps = dispatch => {
  return {
    usersCreate: (user, id = 0, complete = null) => dispatch(actionTypes.usersCreate(user, id, complete)),
    usersDelete: id => dispatch(actionTypes.usersDelete(id)),
    usersUpdate: (id, user) => dispatch(actionTypes.usersUpdate(id, user)),
    usersBulkUpdate: (data, id = "0") => dispatch(actionTypes.usersBulkUpdate(data, id)),
    usersGet: id => dispatch(actionTypes.usersGet(id)),
    usersList: (filters, id = "0") => dispatch(actionTypes.usersList(filters, id)),
    me: () => dispatch(actionTypes.me()),
    usersGetOrCreate: (users, id = "0", complete = null) => dispatch(actionTypes.usersGetOrCreate(users, id, complete))
  };
};
