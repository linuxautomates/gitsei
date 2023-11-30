import React, { useCallback, useMemo, useState } from "react";
import cx from "classnames";
import { convertToDay } from "custom-hooks/helpers/leadTime.helper";
import { toTitleCase } from "utils/stringUtils";
import { Tooltip } from "antd";
import { AntTagComponent as AntTag } from "shared-resources/components/ant-tag/ant-tag.component";
import { AntTextComponent as AntText } from "shared-resources/components/ant-text/ant-text.component";
import { Cell, Pie, PieChart } from "recharts";
import { renderActiveShape } from "./helper";
import { BACKGROUND_COLOR_MAPPLING, COLOR_MAPPLING } from "../constants";
import { DoraStageComponentProps } from "../types";
import { get } from "lodash";
import { AcceptanceTimeUnit } from "classes/RestVelocityConfigs";

const DoraStageComponent: React.FC<DoraStageComponentProps> = (props: DoraStageComponentProps) => {
  const { stage, onClick, isActivePhase, buckets, dataKey, metrics } = props;
  const [activeIndex, setActiveIndex] = useState<number | number[]>(0);
  const [pieDisplay, setPieDisplay] = useState<boolean>(false);
  const stageValue = Math.round(convertToDay(get(stage, [metrics], 0), AcceptanceTimeUnit.SECONDS));
  const rating = get(stage, ["velocity_stage_result", "rating"], "");
  const unit = stageValue > 1 ? "Days" : "Day";

  const handleClick = useCallback(() => {
    onClick && onClick(stage.key);
  }, [stage, onClick]);

  const showColorFills = (index: number) => {
    if (Array.isArray(activeIndex)) {
      return activeIndex.includes(index) || isActivePhase;
    }
    return activeIndex === index || isActivePhase;
  };

  const onMouseEnter = () => {
    setPieDisplay(true);
    setActiveIndex((buckets || []).map((item: any, index: number) => index));
  };

  const onMouseLeave = () => {
    setPieDisplay(false);
  };

  const onPieEnter = useCallback(
    (_, index) => {
      setActiveIndex(index);
    },
    [setActiveIndex]
  );

  const renderContent = useMemo(
    () => (
      <div className="phase-container" onClick={handleClick}>
        <Tooltip title={stage.key?.toUpperCase()}>
          <p className="phase-title">{stage.key}</p>
        </Tooltip>
        <div className={cx("phase-circle", { "active-phase-item": isActivePhase })}>
          <div className={cx("left-line", stage.id)} />
          <div className="upper-node-arrow"></div>
          <div className="lower-node-arrow"></div>
          <div
            onMouseEnter={onMouseEnter}
            onMouseLeave={onMouseLeave}
            style={{
              borderColor: isActivePhase ? COLOR_MAPPLING[rating] : "",
              background: isActivePhase ? BACKGROUND_COLOR_MAPPLING[rating] : ""
            }}
            className="label-container">
            {(pieDisplay || isActivePhase) && (
              <div className="pie-chart-custom-wrapper">
                <PieChart width={isActivePhase ? 200 : 350} height={isActivePhase ? 200 : 300}>
                  <Pie
                    onMouseEnter={onPieEnter}
                    isAnimationActive={false}
                    activeIndex={activeIndex}
                    activeShape={isActivePhase ? undefined : renderActiveShape}
                    data={buckets}
                    cx={isActivePhase ? 96 : 171}
                    cy={isActivePhase ? 94 : 144}
                    innerRadius={50}
                    outerRadius={59}
                    paddingAngle={1}
                    cornerRadius={5}
                    dataKey={dataKey}>
                    {(buckets || []).map((entry: any, index: number) => {
                      return (
                        <Cell
                          key={`cell-${index}`}
                          fill={showColorFills(index) ? COLOR_MAPPLING[entry.rating] : "transparent"}
                        />
                      );
                    })}
                  </Pie>
                </PieChart>
              </div>
            )}
            <div className="stage-value-wrapper" onMouseEnter={onMouseEnter}>
              <AntText className="phase-value">{stageValue}</AntText>
              <AntText className="phase-unit">{toTitleCase(unit)}</AntText>
              <AntTag className="stage-review" color={COLOR_MAPPLING[rating]}>
                {toTitleCase(rating)}
              </AntTag>
            </div>
          </div>
          <div className={cx("right-line", stage.id)} />
        </div>
      </div>
    ),
    [stage, onClick, activeIndex, pieDisplay, isActivePhase, metrics]
  );

  return <div className="dora-stage-content">{renderContent}</div>;
};

export default DoraStageComponent;
