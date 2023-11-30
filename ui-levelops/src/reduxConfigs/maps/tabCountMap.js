import { workbenchTabClear, workbenchTabCounts } from "../actions/tabCountActions";

export const mapTabCountStatetoProps = state => {
  return {
    tab_counts: state.tabCountReducer
  };
};

export const mapTabCountDispatchtoProps = dispatch => {
  return {
    workbenchTabCounts: filters => dispatch(workbenchTabCounts(filters)),
    workbenchTabClear: () => dispatch(workbenchTabClear())
  };
};
