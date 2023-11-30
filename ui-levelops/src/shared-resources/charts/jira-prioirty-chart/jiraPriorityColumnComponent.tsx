import { Icon } from "antd";
import React, { useMemo } from "react";
import { AntPopover, NameAvatar } from "shared-resources/components";
import { newUXColorMapping } from "../chart-themes";
import { PriorityTypes } from "./helper";
import "./jiraPriorityColumn.styles.scss";

const JiraPriorityColumnComponent: React.FC<{
  priority: string;
  teamAllocationTrend: number;
  teamAllocations: string[];
}> = ({ priority, teamAllocationTrend, teamAllocations }) => {
  const getPrioirtyInitials = useMemo(() => {
    switch (priority) {
      case PriorityTypes.HIGHEST:
        return "HH";
      case PriorityTypes.LOWEST:
        return "LL";
      default:
        return priority?.charAt(0);
    }
  }, [priority]);

  const getArrowType = useMemo(() => {
    return teamAllocationTrend > 0 ? "arrow-up" : teamAllocationTrend < 0 ? "arrow-down" : "";
  }, [teamAllocationTrend]);

  const renderPopOverContent = useMemo(() => {
    if ((teamAllocations || []).length > 0) {
      return (
        <div
          className="avatar-name-container"
          style={{ overflowY: (teamAllocations || []).length > 3 ? "scroll" : "hidden" }}>
          {(teamAllocations || []).map((name: any, index: number) => {
            return (
              <div key={index} className="avatar-name-component">
                <div className="name-avatar-container">
                  <NameAvatar name={name} />
                </div>
                <div className="text-container">
                  <p className="avatar-text">{name}</p>
                </div>
              </div>
            );
          })}
        </div>
      );
    }
    return null;
  }, [teamAllocations]);

  return (
    <div className="jira-priority-column-container">
      <div
        className="prioirty-status"
        style={{
          backgroundColor: newUXColorMapping[priority]
        }}>
        {getPrioirtyInitials}
      </div>
      <div className="team-count-container">
        <div className="user-container">
          <Icon type="user" className="user-container-icon" />
          {(teamAllocations || []).length ? (
            <AntPopover trigger="hover" placement="right" content={renderPopOverContent}>
              <p className="user-container-text">{(teamAllocations || []).length}</p>
            </AntPopover>
          ) : (
            <p className="user-container-text">0</p>
          )}
        </div>
        <Icon type={getArrowType} className="arrow-icon-style" />
      </div>
    </div>
  );
};

export default JiraPriorityColumnComponent;
