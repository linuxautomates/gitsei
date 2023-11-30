import React from "react";
import { Tooltip } from "antd";
import cx from "classnames";
import { AntPopover, AntText } from "shared-resources/components";
import { baseColumnConfig } from "utils/base-table-config";
import { toTitleCase } from "utils/stringUtils";
import { getTimeAndIndicator, getClassByPrVolume } from "custom-hooks/helpers/leadTime.helper";

export const velocityTableConfig = (onClick: (val: string) => void) => [
  {
    dataIndex: "name",
    width: 200,
    title: "Task Type",
    align: "left",
    render: (item: any) => (
      <div className="task-type-container">
        <AntText onClick={() => onClick(item)}>{toTitleCase(item)}</AntText>
      </div>
    )
  },
  {
    dataIndex: "avg_velocity",
    width: 128,
    title: "Avg. Velocity",
    align: "center",
    render: (item: any) => {
      return (
        <div className={cx("avg-velocity", `avg-velocity-${item.rating}`)}>
          <span className="velocity-value">{item.duration}</span>
          <span className="velocity-unit">{item.extraUnit}</span>
        </div>
      );
    }
  }
];

export const stageColumn = (
  stage: string,
  onClick: (task_type: string, stage_name: string) => any,
  activeCell?: string
) => ({
  ...baseColumnConfig("", stage),
  width: 180,
  align: "center",
  title: <Tooltip title={stage}>{(stage || "").toUpperCase()}</Tooltip>,
  render: (item: any, record: any) => {
    const { duration, unit, rating } = getTimeAndIndicator(item.duration, item.lower_limit, item.upper_limit);
    return (
      <div
        className={cx("stage-row", {
          "stage-row-active": activeCell === `${record.name}_${stage}`
        })}>
        <AntPopover
          overlayClassName={"stage-details-popover"}
          trigger={["hover", "click"]}
          content={
            <div className="stage-details">
              <div className="stage-header">
                <div className="flex items-center">
                  <AntText>Task Type :</AntText>
                  <AntText className="task-value">{toTitleCase(record.name)}</AntText>
                </div>
                <div className="flex items-center">
                  <AntText>Phase :</AntText>
                  <AntText className="phase-value">{stage}</AntText>
                </div>
              </div>
              <div className="stage-content">
                <div className="flex items-center">
                  <AntText className="pr-label">PRs :</AntText>
                  <AntText className="pr-value" onClick={() => onClick(record.name, stage)}>
                    {item.count}
                  </AntText>
                </div>
                <div className="flex items-center">
                  <AntText>Avg. Lead Time :</AntText>
                  <AntText className="velocity-value">{`${duration} ${unit}`}</AntText>
                </div>
              </div>
            </div>
          }>
          <div
            className={cx("pr-volume", `pr-volume-${getClassByPrVolume(item.count)}`, `pr-volume-${rating}`)}
            onClick={onClick(record.name, stage)}
          />
        </AntPopover>
      </div>
    );
  }
});
