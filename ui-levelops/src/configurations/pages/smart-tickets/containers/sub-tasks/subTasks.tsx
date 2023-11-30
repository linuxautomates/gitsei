import React from "react";
import { useSelector } from "react-redux";
import { List } from "antd";
import Loader from "components/Loader/Loader";
import { TicketInfo } from "configurations/pages/smart-tickets/components";
import { RestWorkItem } from "classes/RestWorkItem";
import { subTicketsListState } from "reduxConfigs/selectors/restapiSelector";
import { get } from "lodash";

interface SubTasksProps {
  workItemId: any;
  workItemChildren: any[];
  onRefresh: () => void;
}

export const SubTasksContainer: React.FC<SubTasksProps> = props => {
  const subTickets = useSelector(state => subTicketsListState(state, { workItemId: props.workItemId }));

  const workItems = () => {
    return get(subTickets, ["data", "records"], []);
  };

  const renderTickets = () => {
    return (
      <List
        dataSource={workItems()}
        renderItem={(ticket: any) => (
          <List.Item key={ticket.id} style={{ border: "0px" }}>
            <TicketInfo ticket={new RestWorkItem(ticket)} onRefresh={props.onRefresh} />
          </List.Item>
        )}
      />
    );
  };

  const isLoading = () => {
    if (!props.workItemChildren || props.workItemChildren.length === 0) {
      return false;
    }
    return subTickets.loading;
  };

  if (isLoading()) {
    return <Loader />;
  }

  return <>{workItems().length > 0 && <div className="sub-tickets">{renderTickets()}</div>}</>;
};

export default SubTasksContainer;
