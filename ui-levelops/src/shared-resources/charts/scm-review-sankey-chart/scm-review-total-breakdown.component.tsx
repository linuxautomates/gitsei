import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { SCMReviewCollaborationStateType } from "dashboard/reports/scm/scm-review-collaboration/scm-review-collaboration-report.enum";
import { map } from "lodash";
import React from "react";
import { AntCheckbox } from "shared-resources/components";
import { collabStateColorMapping } from "./constant";

interface SCMReviewTotalBreakdownProps {
  totalBreakdown: basicMappingType<number>;
}

const SCMReviewTotalBreakdownComponent: React.FC<SCMReviewTotalBreakdownProps> = ({ totalBreakdown }) => {
  return (
    <div className="total-breakdown-container">
      {map(Object.keys(totalBreakdown), (key: SCMReviewCollaborationStateType) => (
        <div key={key} className="breakdown-element">
          <span className="text">{totalBreakdown[key]}</span>
          <div className="label">
            <div
              key={key}
              className="breakdown-checkbox"
              style={{ backgroundColor: (collabStateColorMapping as any)[key?.replaceAll("-", "_")] }}
            />
            <span className="checkbox-text">{key?.replaceAll("-", " ")}</span>
          </div>
        </div>
      ))}
    </div>
  );
};

export default SCMReviewTotalBreakdownComponent;
