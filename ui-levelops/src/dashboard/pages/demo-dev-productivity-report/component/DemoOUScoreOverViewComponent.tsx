import React from "react";
import queryParser from "query-string";
import { useHistory, useLocation } from "react-router-dom";
import { Divider } from "antd";
import { calculateTotalEngineerScoreMapping } from "dashboard/pages/scorecard/helper";
import { scoreOverviewUtilType } from "dashboard/dashboard-types/engineerScoreCard.types";
import "../../dev-productivity-report/component/ouScoreOverViewComponent.styles.scss";
import { useDispatch } from "react-redux";

interface DemoOUScoreOverViewComponentProps {
  data: any;
}
const DemoOUScoreOverViewComponent: React.FC<DemoOUScoreOverViewComponentProps> = ({ data }) => {
  const ouScoreTransformedData: scoreOverviewUtilType = calculateTotalEngineerScoreMapping([data]);
  return (
    <div className="ou-score-overview-component">
      <div className="summary-container">
        <p className="title">Summary</p>
        <div className="summary-fields">
          <div className="field-elements">
            <p className="field">Managers</p>
            {/* <div className="value">{(orgUnit?.managers || []).map(manager => manager?.full_name).join(", ")}</div> */}
          </div>
        </div>
        <div className="org-unit-overview">
          <span>Collection Defintion</span>
          {/* <Popover content={renderPopoverContent} placement="bottomLeft">
            <Icon type="question-circle" theme="filled" />
          </Popover> */}
        </div>
        <p className="ou-edit-link">Collection Settings</p>
        <Divider />
      </div>
      <div className="productivity-container">
        <p className="title">Trellis Score</p>
        <div className="score-container" style={{ backgroundImage: ouScoreTransformedData?.color }}>
          <div className="score">{ouScoreTransformedData.finalScore ?? 0}</div>
          <div className="score-desc">Overall Score</div>
        </div>
        <p className="ou-edit-link">Trellis Score Settings</p>
      </div>
    </div>
  );
};

export default DemoOUScoreOverViewComponent;
