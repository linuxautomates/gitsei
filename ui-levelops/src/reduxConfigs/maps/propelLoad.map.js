import * as actions from "reduxConfigs/actions/propelLoad.actions";

export const mapPropelLoadDispatchToProps = dispatch => {
  return {
    propelFetch: id => dispatch(actions.propelFetch(id)),
    propelNew: index => dispatch(actions.propelNew(index))
  };
};
