import React from "react";
import { AntTitle } from "shared-resources/components";
import DemoScoreCardSecondaryHeaderComponent from "./DemoScoreCardSecondaryHeaderComponent";

interface DemoScorecardDashHeaderProps {
  dashboardTitle: string;
  dashboardTimeRange: any;
}

const DemoScoreCardDashboardHeader: React.FC<DemoScorecardDashHeaderProps> = ({
  dashboardTimeRange,
  dashboardTitle
}) => {
  return (
    <div className="dashboard-view-page-header-container">
      <div className="dashboad-secondary-header-wrapper flex">
        <div className="flex ">
          <AntTitle level={4} className="dashboard-name">
            {dashboardTitle}
          </AntTitle>
          <div className="version-chip">BETA</div>
        </div>
        <div>
          <DemoScoreCardSecondaryHeaderComponent dashboardTimeRange={dashboardTimeRange} />
        </div>
      </div>
    </div>
  );
};

export default DemoScoreCardDashboardHeader;
