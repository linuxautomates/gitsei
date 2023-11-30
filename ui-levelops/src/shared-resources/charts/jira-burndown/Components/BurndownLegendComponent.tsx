import { capitalize } from "lodash";
import React from "react";
import "./burndownLegend.styles.scss";

const BurndownLegendComponent: React.FC<{ statusList: any[]; mapping: any }> = ({ statusList, mapping }) => {
  return (
    <div className="legend-parent">
      {statusList.map((curData: any) => {
        const text = typeof curData === "string" ? curData : curData.text;
        const color = typeof curData === "string" ? mapping[curData] : mapping[curData.status];
        return (
          <div className="legend-container">
            <p className="legend-container-capsule" style={{ backgroundColor: color }}></p>
            <span className="legend-container-text">{capitalize(text)}</span>
          </div>
        );
      })}
    </div>
  );
};

export default BurndownLegendComponent;
