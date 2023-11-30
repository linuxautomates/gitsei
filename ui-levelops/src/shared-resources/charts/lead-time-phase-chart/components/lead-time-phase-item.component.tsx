import React, { useCallback, useMemo } from "react";
import cx from "classnames";
import { getTimeAndIndicator } from "custom-hooks/helpers/leadTime.helper";
import { toTitleCase } from "utils/stringUtils";
import { AntTag, AntText } from "shared-resources/components";
import { Icon, Tooltip } from "antd";

interface LeadTimePhaseItemProps {
  phase: any;
  onClick?: (v: any) => void;
  isActivePhase?: boolean;
  isEndEvent?: any;
}

const LeadTimePhaseItem: React.FC<LeadTimePhaseItemProps> = props => {
  const { phase, onClick, isActivePhase } = props;
  const { duration, lower_limit, upper_limit } = phase;
  const {
    duration: value,
    unit,
    color,
    backgroudColor,
    rating
  } = getTimeAndIndicator(duration, lower_limit, upper_limit);

  const handleClick = useCallback(() => {
    onClick && onClick(phase.name);
  }, [phase, onClick]);

  const renderContent = useMemo(
    () => (
      <div className="phase-container" onClick={handleClick}>
        <div className="phase-header">
          <p className="phase-title">{phase.name}</p>
          {phase?.info_message &&
            <Tooltip title={phase.info_message}>
              <Icon className="phase-info" type="info-circle" theme="outlined" />
            </Tooltip>
          }
        </div>
        <div className={cx("phase-circle", { "active-phase-item": isActivePhase })}>
          <div className={cx("left-line", phase.id)} />
          <div className="upper-node-arrow"></div>
          <div className="lower-node-arrow"></div>
          <div
            style={{ borderColor: isActivePhase ? color : "", background: isActivePhase ? backgroudColor : "" }}
            className="label-container">
            <AntText className="phase-value">{value}</AntText>
            <AntText className="phase-unit">{toTitleCase(unit)}</AntText>
            <AntTag className="stage-review" color={color}>
              {toTitleCase(rating)}
            </AntTag>
          </div>
          <div className={cx("right-line", phase.id)} />
        </div>
      </div>
    ),
    [phase, onClick]
  );

  return <div className={cx("lead-time-phase-item", { "active-phase-item": isActivePhase })}>{renderContent}</div>;
};

export default LeadTimePhaseItem;
