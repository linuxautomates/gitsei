import { ADD_ERROR, CLEAR_ERROR } from "../actions/actionTypes";

const INITIAL_STATE = {
  error: { error: false, error_header: "", error_message: "" },
  onHide: () => {}
};

const errorReducer = (state = INITIAL_STATE, action) => {
  switch (action.type) {
    case ADD_ERROR:
      if (state.error.error) {
        return state;
      }
      return { ...state, error: action.payload };

    case CLEAR_ERROR:
      return INITIAL_STATE;

    default:
      return state;
  }
};

export default errorReducer;
