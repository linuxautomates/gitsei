import React, { useMemo } from "react";
import { Descriptions } from "antd";

import { WORKITEM_TICKET_TYPE_SNIPPET, WORKITEM_TYPE_AUTOMATED, WORKITEM_TYPE_MANUAL } from "classes/RestWorkItem";
import { AntCol } from "shared-resources/components";
import "./WorkItemDescription.styles.scss";
interface WorkItemDescriptionsProps {
  workItem: any;
}

const WorkItemDescriptions: React.FC<WorkItemDescriptionsProps> = ({ workItem }) => {
  const style = useMemo(() => ({ marginTop: "10px", padding: "10px" }), []);

  if (!workItem) {
    return null;
  }

  const descriptions: any[] = [];

  switch (workItem.type) {
    case WORKITEM_TYPE_AUTOMATED:
      descriptions.push({
        label: "Reason",
        value: workItem.reason || ""
      });
      descriptions.push({
        label: "Cloud Owner",
        value: workItem.cloud_owner
      });
      break;
    case WORKITEM_TYPE_MANUAL:
      if (workItem.ticket_type === WORKITEM_TICKET_TYPE_SNIPPET) {
        descriptions.push({
          label: "Reason",
          value: workItem.reason || ""
        });
      }
      break;
  }

  if (!descriptions.length) {
    return null;
  }

  return (
    <AntCol span={24}>
      <div className="border-class" style={style}>
        <Descriptions title={workItem.type} layout="vertical" bordered={false} column={2} colon={false} size="small">
          {descriptions.map((description: any, index: number) => (
            <Descriptions.Item
              className="automated-description"
              key={`description-${index}`}
              label={description.label}
              // @ts-ignore
              colon={false}>
              {description.value}
            </Descriptions.Item>
          ))}
        </Descriptions>
      </div>
    </AntCol>
  );
};

export default WorkItemDescriptions;
