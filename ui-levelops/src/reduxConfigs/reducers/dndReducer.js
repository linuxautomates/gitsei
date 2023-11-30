import {
  ADD_DASHBOARD_DRAG,
  ADD_DASHBOARD_DROP,
  CLEAR_DASHBOARD_DRAG,
  CLEAR_DASHBOARD_DROP
} from "../actions/actionTypes";

const INITIAL_STATE = {
  drag: null,
  drop: null
};

const dndReducer = (state = INITIAL_STATE, action) => {
  switch (action.type) {
    case ADD_DASHBOARD_DRAG:
      return { drag: action.payload, drop: null };

    case ADD_DASHBOARD_DROP:
      return { ...state, drop: action.payload };

    case CLEAR_DASHBOARD_DRAG:
      return { ...state, drag: null };

    case CLEAR_DASHBOARD_DROP:
      return { ...state, drop: null };

    default:
      return INITIAL_STATE;
  }
};

export default dndReducer;
