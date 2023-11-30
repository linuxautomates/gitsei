import React, { useEffect } from "react";
import { useDispatch, useSelector } from "react-redux";
import { Skeleton } from "antd";
import { getWorkItemForm } from "reduxConfigs/selectors/formSelector";
import { formClear } from "reduxConfigs/actions/formActions";
import { workItemFlow } from "reduxConfigs/actions/workitemFlowActions";
import SmartTicketEditContainer from "../containers/smart-ticket-edit/SmartTicketEditContainer";
interface SmartTicketEditChangeContainerProps {
  vanityId: any;
  workId: any;
  history: any;
  onReroute: (url: string) => void;
  onRefresh: () => void;
}

export const SmartTicketEditChangeContainer: React.FC<SmartTicketEditChangeContainerProps> = (
  props: SmartTicketEditChangeContainerProps
) => {
  const dispatch = useDispatch();
  const workItemState = useSelector(state => getWorkItemForm(state));

  useEffect(() => {
    dispatch(formClear("workitem_form"));
    dispatch(workItemFlow(props.vanityId));
  }, [props.vanityId]);

  if (workItemState === undefined || workItemState.loading || props.vanityId === undefined) {
    return <Skeleton paragraph={{ rows: 10 }} active={true} />;
  }
  return <SmartTicketEditContainer {...props} workItem={workItemState} />;
};

export default SmartTicketEditChangeContainer;
