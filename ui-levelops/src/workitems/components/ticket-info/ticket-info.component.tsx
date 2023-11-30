import React from "react";
import { Typography } from "antd";
import { AntAvatar, AntCol, AntRow, AntTooltip } from "shared-resources/components";
import { StatusSelect } from "workitems/components";
import "./ticket-info.style.scss";
import { getSettingsPage } from "constants/routePaths";

const { Text } = Typography;
export const TicketInfoComponent = (props: any) => {
  return (
    <AntRow style={{ width: "100%" }} type="flex" align="middle">
      <AntCol span={16}>
        <a href={`${getSettingsPage()}/workitems?workitem=${props.id}`}>{props.title}</a>
      </AntCol>
      <AntCol span={4}>
        <Text type="secondary">Assignee:</Text>
        <AntTooltip title={props.assignee}>
          <AntAvatar size="small" className="f-12">
            {(props.assignee?.substring(0, 2) || "").toUpperCase()}
          </AntAvatar>
        </AntTooltip>
      </AntCol>
      <AntCol span={4}>
        <div className="flex justify-end">
          <StatusSelect />
        </div>
      </AntCol>
    </AntRow>
  );
};
