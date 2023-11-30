import React, { useEffect, useMemo } from "react";
import "./demo-dashboard-header-wrapper.style.scss";
import DemoDashboardHeader from "./demo-dashboard-header";
interface DemoDashboardWrapperProps {
  dashboardId?: any;
  queryparamOU: string | undefined;
  dashboardName: string;
}

const DemoDashboardWrapper: React.FC<DemoDashboardWrapperProps> = ({ queryparamOU, dashboardName, dashboardId }) => {
  return (
    <div className="demo-dashboard-view-page-header-container">
      <div style={{ visibility: "hidden" }} />
      {/* <AntRow>{queryparamOU && <DashboardOUHeader demoDashboard={true} />}</AntRow> */}
      <DemoDashboardHeader queryparamOU={queryparamOU} dashboardId={dashboardId} />
    </div>
  );
};

export default DemoDashboardWrapper;
