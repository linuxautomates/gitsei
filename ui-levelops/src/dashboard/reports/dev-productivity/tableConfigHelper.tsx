import { Icon, Tooltip } from "antd";
import { legendColorByScore } from "dashboard/helpers/devProductivityRating.helper";
import React from "react";
import { getTimeForTrellisProfile } from "utils/dateUtils";
import "./tableConfigHelper.scss";

export const displayValues = (item: any) => (
  <div className="dev-productivity-score-table">
    {typeof item === "object" ? (
      <div className="blurred">{item.hiddenValue ?? "NA"}</div>
    ) : (
      <div style={{ cursor: "pointer" }}>
        <span className="values" style={{ backgroundColor: `${legendColorByScore(item)}` }} />
        {item ?? "NA"}
      </div>
    )}
  </div>
);

export const displayFirstRow = (item: any) => (
  <div className="dev-productivity-score-table">
    {typeof item === "object" ? (
      <div className="first-row-blurred">{item.hiddenValue ?? "NA"}</div>
    ) : (
      <div className="first-row-value" style={{ backgroundColor: `${legendColorByScore(item)}` }}>
        {item ?? "NA"}
      </div>
    )}
  </div>
);

export const lastUpdatedAt = (text: string, unixTime: number) => {
  return (
    <span className="last-updated-cell">
      <div>{text}</div>
      <div className="last-updated-at">
        <Tooltip title="Last updated at">
          <span className="cal-icon">
            <Icon type="calendar" />
          </span>
        </Tooltip>{" "}
        {getTimeForTrellisProfile(unixTime)}
      </div>
    </span>
  );
};
