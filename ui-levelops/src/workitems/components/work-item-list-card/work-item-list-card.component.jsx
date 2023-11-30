import React from "react";
import * as PropTypes from "prop-types";
import { AntAvatar, AntBadge, AntText } from "shared-resources/components";

export class WorkItemListCardComponent extends React.PureComponent {
  constructor(props) {
    super(props);
    this.onClickHandler = this.onClickHandler.bind(this);
  }

  onClickHandler() {
    this.props.onSelectEvent(this.props.workItem.id);
  }

  render() {
    const { name, assignee, color, ticketId } = this.props.workItem;
    return (
      <div className="w-100" onClick={this.onClickHandler} role="presentation">
        <div className="flex direction-column overflow-hidden">
          <AntText strong className="mb-40 ticket-name" ellipsis>
            {name}
          </AntText>
          <div className="flex justify-space-between align-bottom">
            <AntBadge color={color} text={ticketId} />
            <AntAvatar size="small" style={{ backgroundColor: `${color}` }} className="f-12">
              {assignee.substring(0, 2).toUpperCase()}
            </AntAvatar>
          </div>
        </div>
      </div>
    );
  }
}

WorkItemListCardComponent.propTypes = {
  workItem: PropTypes.object.isRequired,
  onSelectEvent: PropTypes.func.isRequired
};
