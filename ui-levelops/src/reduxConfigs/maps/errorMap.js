import { addError, clearError } from "../actions/errorActions";

export const mapErrorStatetoProps = state => {
  return {
    error: state.errorReducer.error
  };
};

export const mapErrorDispatchtoProps = dispatch => {
  return {
    addError: error => dispatch(addError(error)),
    clearError: () => dispatch(clearError())
  };
};
