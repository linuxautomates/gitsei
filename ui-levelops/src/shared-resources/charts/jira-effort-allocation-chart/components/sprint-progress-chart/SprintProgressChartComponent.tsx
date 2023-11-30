import { Tooltip } from "antd";
import React, { useMemo } from "react";
import { AntProgress } from "shared-resources/components";
import TooltipWithTruncatedTextComponent from "shared-resources/components/tooltip-with-truncated-text/TooltipWithTruncatedTextComponent";
import "./sprintProgressChart.styles.scss";

const SprintProgressChartComponent: React.FC<{
  sprintData: any[];
  dependencyIds: string[];
  colorMapping: any;
  unit: string;
}> = ({ sprintData, dependencyIds, colorMapping, unit }) => {
  const renderSprintCard = useMemo(() => {
    return sprintData.map((sprint: any, index: number) => {
      return (
        <div
          className="sprint-progress-container-card"
          key={sprint?.name}
          style={{ paddingLeft: index === 0 ? "2rem" : "" }}>
          <div className="mb-20">
            <TooltipWithTruncatedTextComponent
              title={sprint?.name || ""}
              allowedTextLength={24}
              textClassName="sprint-progress-container-card_name"
            />
          </div>
          <div className="sprint-progress-container-card_content">
            <p className="main-text">{sprint?.total_completed_points}</p>
            <span className="support-text">{unit}</span>
          </div>
          <div className="sprint-progress-container-card_progress">
            {dependencyIds.map(id => {
              return (
                <Tooltip
                  title={!!sprint[id] ? `${id || ""} : ${sprint[`${id}_${unit}`]} ${unit}` : null}
                  placement="topLeft">
                  <AntProgress
                    percent={sprint?.[id]}
                    strokeColor={colorMapping[id]}
                    key={id}
                    showInfo={false}
                    strokeWidth="1rem"
                  />
                </Tooltip>
              );
            })}
          </div>
        </div>
      );
    });
  }, [dependencyIds]);

  return <div className="sprint-progress-container">{renderSprintCard}</div>;
};

export default SprintProgressChartComponent;
