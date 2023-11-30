import { Avatar, Tooltip } from "antd";
import React from "react";
import { NameAvatarComponent as NameAvatar } from "../name-avatar/name-avatar.component";
import "./name-avatar.scss";

export const NameAvatarListComponent = props => {
  if (!props.names) {
    return null;
  }
  let assigneesToDisplay = props.names;
  const noOfAssignees = props.names.length;
  let remainingAssignees;
  if (noOfAssignees > props.namesShowFactor) {
    assigneesToDisplay = props.names.slice(0, props.namesShowFactor);
    remainingAssignees = noOfAssignees - props.namesShowFactor;
  }

  const getClassName = () => {
    if (noOfAssignees === 1) {
      return "avatar-group-secondary";
    } else if (noOfAssignees === 2) {
      return "avatar-group-secondary";
    } else return "avatar-group-ternary";
  };

  const classes = () => {
    return `${props.classRequired ? getClassName() : ""} ${props.className}`;
  };

  return (
    <div className={classes()}>
      {assigneesToDisplay.map(assignee => {
        return <NameAvatar name={assignee} />;
      })}
      {remainingAssignees && (
        <Tooltip title={props.names.slice(2, props.names.length).join(",")}>
          <Avatar size="small" style={{ backgroundColor: "grey" }} className="f-12">
            +{remainingAssignees}
          </Avatar>
        </Tooltip>
      )}
    </div>
  );
};

NameAvatarListComponent.defaultProps = {
  classRequired: true,
  className: "",
  namesShowFactor: 2
};
