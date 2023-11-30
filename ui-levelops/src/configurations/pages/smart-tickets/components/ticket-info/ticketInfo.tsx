import React, { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Tooltip } from "antd";
// @ts-ignore
import { useDispatch } from "react-redux";
import { AntCol, AntRow, AntText, NameAvatarList } from "shared-resources/components";
import { SelectRestapi } from "shared-resources/helpers";
import { RestWorkItem } from "classes/RestWorkItem";
import "./ticket-info.style.scss";
import { workItemUdpate } from "reduxConfigs/actions/restapi/workitemActions";
import { debounce } from "lodash";
import { getWorkitemDetailPage } from "constants/routePaths";

interface TicketInfoComponentProps {
  ticket: RestWorkItem;
  onRefresh: () => void;
}

const TicketInfoComponent: React.FC<TicketInfoComponentProps> = props => {
  const dispatch = useDispatch();
  const [ticket, setTicket] = useState<null | any>(null);
  const debounceRefresh = debounce(props.onRefresh, 500);
  useEffect(() => {
    if (!ticket || ticket.id !== props.ticket.id) {
      setTicket(props.ticket);
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const assigneesAvatar = () => {
    return <NameAvatarList names={props.ticket.assignees.map((assignee: any) => assignee.user_email) || []} />;
  };

  useEffect(() => {
    if (ticket === null || props.ticket.status === ticket.status) {
      return;
    }
    dispatch(workItemUdpate(ticket.id, ticket));
    debounceRefresh();
  }, [ticket]); // eslint-disable-line react-hooks/exhaustive-deps

  const handleStatusChange = (option: any) => {
    setTicket((ticket: any) => {
      const updatedData = { ...ticket.json(), status: option.label, state_id: option.key };
      return new RestWorkItem(updatedData);
    });
  };

  return (
    <AntRow style={{ width: "100%" }} type="flex" align="top" gutter={[10, 0]} justify={"space-between"}>
      <AntCol span={6}>
        <AntText>
          <Link to={`${getWorkitemDetailPage()}?workitem=${props.ticket?.vanity_id}`}>{props.ticket?.vanity_id}</Link>
        </AntText>
      </AntCol>
      <AntCol span={10}>
        <div className={"ellipsis"}>
          <Tooltip title={props.ticket?.title}>{props.ticket?.title}</Tooltip>
        </div>
      </AntCol>
      <AntCol span={4}>{assigneesAvatar()}</AntCol>
      <AntCol span={4}>
        <SelectRestapi
          style={{ width: "100%" }}
          placeholder="Select Status..."
          mode="single"
          uri="states"
          searchField="name"
          value={{
            key: ticket?.state_id,
            label: ticket?.status
          }}
          onChange={handleStatusChange}
          labelinValue={true}
          allowClear={false}
        />
      </AntCol>
    </AntRow>
  );
};

export default TicketInfoComponent;
