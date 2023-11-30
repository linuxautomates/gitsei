import React from "react";
import { AntTitle } from "shared-resources/components";
import cx from "classnames";
import ScoreCardSecondaryHeaderComponent from "../dashboard-view-page-secondary-header/ScoreCardSecondaryHeader";
import "./ScoreCardDashboardHeader.scss";
import { getIsStandaloneApp } from "helper/helper";

interface ScorecardDashHeaderProps {
  dashboardTitle: string;
  dashboardTimeRange: any;
  onFilterValueChange: (value: any) => void;
  orgListOptions?: Array<Record<string, any>>;
  currentOu?: string | string[] | null | undefined;
  handleOuChange?: (param: string) => void;
  newTrellisProfile?: boolean;
  ouView?: boolean;
}

const ScoreCardDashboardHeader: React.FC<ScorecardDashHeaderProps> = ({
  dashboardTimeRange,
  onFilterValueChange,
  dashboardTitle,
  orgListOptions,
  currentOu,
  handleOuChange,
  newTrellisProfile,
  ouView
}) => {
  return (
    <div
      className={cx("score-card-header-container", {
        standaloneStyle: getIsStandaloneApp()
      })}>
      <div className="score-card-header-wrapper flex">
        <div className="flex ">
          <AntTitle level={4} className="dashboard-name">
            {dashboardTitle}
          </AntTitle>
          {!newTrellisProfile && <div className="version-chip">BETA</div>}
        </div>
        <div>
          <ScoreCardSecondaryHeaderComponent
            dashboardTimeRange={dashboardTimeRange}
            onFilterValueChange={onFilterValueChange}
            orgListOptions={orgListOptions}
            currentOu={currentOu}
            handleOuChange={handleOuChange}
            ouView={ouView}
          />
        </div>
      </div>
    </div>
  );
};

export default ScoreCardDashboardHeader;
