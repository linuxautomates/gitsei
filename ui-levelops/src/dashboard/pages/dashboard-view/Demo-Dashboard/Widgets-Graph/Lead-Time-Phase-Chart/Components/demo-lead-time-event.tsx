import React, { useMemo } from "react";
import cx from "classnames";

interface LeadTimeEventProps {
  isStartEvent?: boolean;
  isEndEvent?: boolean;
  title: string;
  stageId: string;
  isActive?: boolean;
  showMore?: boolean;
}

const DemoLeadTimeEventComponent: React.FC<LeadTimeEventProps> = props => {
  const { stageId, title, isStartEvent, isEndEvent, isActive, showMore } = props;

  const renderPoint = useMemo(() => {
    let point: any;

    if (isStartEvent) {
      point = (
        <div className="event-label" style={{ marginRight: "1px", marginTop: "-5px" }}>
          Start
        </div>
      );
    } else if (isEndEvent) {
      point = (
        <div>
          <div className="end-upper-node-arrow"></div>
          <div className="end-lower-node-arrow"></div>
          <div className="event-label" style={{ marginTop: "-5px" }}>
            End
          </div>
        </div>
      );
    }

    return <div className={cx("point-container", stageId)}>{point}</div>;
  }, [stageId, isStartEvent, isEndEvent, showMore]);

  return (
    <div
      className={cx("lead-time-event", {
        "active-event": isActive,
        "start-event": isStartEvent,
        "end-event": isEndEvent
      })}>
      {renderPoint}
    </div>
  );
};

export default DemoLeadTimeEventComponent;
