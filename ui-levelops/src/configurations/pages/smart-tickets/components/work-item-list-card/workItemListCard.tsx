import React from "react";
import { AntParagraph, NameAvatarList } from "shared-resources/components";
import "./work-item-list-card.style.scss";
import { tableCell } from "utils/tableUtils";

interface WorkItemListProps {
  workItem: any;
  onSelectEvent: (value: any) => void;
}

const WorkItemListCardComponent: React.FC<WorkItemListProps> = props => {
  const { title, assignees, vanity_id, status } = props.workItem;

  const onClickHandler = () => {
    props.onSelectEvent(props.workItem.vanity_id);
  };

  const renderAssigneesAvatar = () => {
    if (!assignees) {
      return null;
    }
    return <NameAvatarList names={assignees.map((assignee: any) => assignee.user_email)} />;
  };

  return (
    <div className="w-100" onClick={onClickHandler} role="presentation">
      <div className="flex direction-column overflow-hidden">
        <AntParagraph className="ticket-name" ellipsis={{ rows: 2 }}>
          {title && title}
        </AntParagraph>
        <div className="flex justify-space-between align-bottom">
          {tableCell("status_badge", {
            value: status,
            name: props.workItem.parent_id ? `${vanity_id} (sub issue)` : vanity_id
          })}
          {renderAssigneesAvatar()}
        </div>
      </div>
    </div>
  );
};

export default WorkItemListCardComponent;
