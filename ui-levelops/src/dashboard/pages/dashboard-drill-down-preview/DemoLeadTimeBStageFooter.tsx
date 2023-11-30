import React from "react";
import { AntText } from "shared-resources/components";
import { toTitleCase } from "utils/stringUtils";
import "shared-resources/containers/server-paginated-table/components/Table-Footer/Lead-time-stage-footer.style.scss";

interface DemoLeadTimeByStageFooterProps {}

export const DemoLeadTimeByStageFooter: React.FC<DemoLeadTimeByStageFooterProps> = props => {
  const indicators = ["good", "slow", "acceptable", "missing"];
  return (
    <div className="lead-time-stage-footer" style={{ marginLeft: "20px" }}>
      {indicators.map((value: string, index: number) => {
        if (index === indicators.length - 1) {
          return (
            <div className="lead-time-stage-div">
              <div className={`stage-by-footer-${value}`}></div>
              <AntText>{toTitleCase(value)}</AntText>
            </div>
          );
        }
        if (value) {
          return (
            <div className="lead-time-stage-div">
              <div className={`stage-by-footer-${value}`}></div>
              <AntText>{toTitleCase(value)}</AntText>
            </div>
          );
        }
        return null;
      })}
    </div>
  );
};

export default DemoLeadTimeByStageFooter;
