import { addDashboardDrag, addDashboardDrop, clearDashboardDrag, clearDashboardDrop } from "../actions/dndActions";

export const mapDndStatetoProps = state => {
  return {
    drag: state.dndReducer.drag,
    drop: state.dndReducer.drop
  };
};

export const mapDndDispatchtoProps = dispatch => {
  return {
    addDashboardDrag: id => dispatch(addDashboardDrag(id)),
    addDashboardDrop: id => dispatch(addDashboardDrop(id)),
    clearDashboardDrag: () => dispatch(clearDashboardDrag()),
    clearDashboardDrop: () => dispatch(clearDashboardDrop())
  };
};
