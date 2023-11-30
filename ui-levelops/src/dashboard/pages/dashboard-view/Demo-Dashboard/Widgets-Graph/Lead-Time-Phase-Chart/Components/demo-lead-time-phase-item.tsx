import React, { useMemo, useCallback } from "react";
import cx from "classnames";
import { getTimeAndIndicator } from "custom-hooks/helpers/leadTime.helper";
import { toTitleCase } from "utils/stringUtils";
import { AntTag, AntText } from "shared-resources/components";

interface LeadTimePhaseItemProps {
  phase: any;
  isActivePhase?: boolean;
  isEndEvent?: any;
  onClick?: (v: any) => void;
  showMore?: boolean;
}

const DemoLeadTimePhaseItem: React.FC<LeadTimePhaseItemProps> = props => {
  const { phase, isActivePhase, onClick, showMore } = props;
  const { duration, lower_limit, upper_limit, rating } = phase;
  const {
    duration: value,
    unit,
    color,
    backgroudColor,
    rating: _rating
  } = getTimeAndIndicator(duration, lower_limit, upper_limit);

  const handleClick = useCallback(() => {
    const { id, name } = phase;
    onClick && onClick({ phaseId: id, name });
  }, [phase, onClick]);

  const renderContent = useMemo(
    () => (
      <div className="demo-phase-container" onClick={handleClick}>
        <p className="phase-title">{phase.name}</p>
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
    [phase, isActivePhase, showMore]
  );

  return <div className={cx("lead-time-phase-item", { "active-phase-item": isActivePhase })}>{renderContent}</div>;
};

export default DemoLeadTimePhaseItem;
