
// Follwing beter way to write reducers pattern: https://redux.js.org/usage/reducing-boilerplate#generating-reducers
// Gives better code reading and maintaining as compare to switch case structure e.g requiredFieldReducer
export function createReducer(initialState, handlers) {
  return function reducer(state = initialState, action) {
    if (handlers.hasOwnProperty(action.type)) {
      return handlers[action.type](state, action);
    } else {
      return state;
    }
  };
}