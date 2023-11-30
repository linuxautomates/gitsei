import { workItemFlow } from "../actions/workitemFlowActions";

export const workitemFlowDispatchToProps = dispatch => {
  return {
    workItemFlow: id => dispatch(workItemFlow(id))
  };
};
