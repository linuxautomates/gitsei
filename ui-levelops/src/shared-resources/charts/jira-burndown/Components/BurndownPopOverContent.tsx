import { capitalize } from "lodash";
import React, { useMemo, useRef } from "react";
import { AntButton } from "shared-resources/components";
import BurndownLegendComponent from "./BurndownLegendComponent";
import "./BurndownPopover.styles.scss";

const BurndownPopOverContent: React.FC<{ [x: string]: any }> = ({
  mapping,
  dataKeys,
  payload,
  onClick,
  notShowActionButton
}) => {
  const totalTicketsRef = useRef<number>(0);

  const statusList = useMemo(() => {
    let totalTickets = 0;
    const curStatusList = dataKeys.map((key: string) => {
      totalTickets += payload[key];
      return {
        text: `${payload[key]} ${capitalize(key)}`,
        status: key
      };
    });
    totalTicketsRef.current = totalTickets;
    return curStatusList;
  }, [dataKeys, payload]);

  return (
    <div className="popover-container">
      <div className="popover-container-upper">
        <p className="popover-container-upper-title">{totalTicketsRef.current}</p>
        <p className="popover-container-upper-subtitle">Tickets</p>
      </div>
      <div className="popover-container-lower">
        <div className="popover-container-lower-legends">
          <BurndownLegendComponent statusList={statusList} mapping={mapping} />
        </div>
        <div
          className="popover-container-lower-button_container"
          style={{ display: notShowActionButton ? "none" : "" }}>
          <AntButton className="report-button" onClick={onClick}>
            View Report
          </AntButton>
        </div>
      </div>
    </div>
  );
};

export default React.memo(BurndownPopOverContent);
