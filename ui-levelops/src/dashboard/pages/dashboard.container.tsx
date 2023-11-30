import React, { ReactNode, useEffect } from "react";
import { useSelector } from "react-redux";
// import { useHistory } from "react-router-dom";

import "./dashboard.style.scss";
import Loader from "../../components/Loader/Loader";
import { AntCol, AntRow } from "../../shared-resources/components";
import { defaultDashboardSelector } from "reduxConfigs/selectors/dashboardSelector";
import { getDefaultDashboardPath } from "../../utils/dashboardUtils";
import { DashboardIntegrations, DashboardOverview } from "../containers";
import { useParams } from "react-router-dom";
import { ProjectPathProps } from "classes/routeInterface";

interface DashboardContainerProps {
  securityDash?: boolean;
  enableDashboardRoutes?: boolean;
  history?: any;
  children: ReactNode;
}

const DashboardContainer: React.FC<DashboardContainerProps> = ({
  history,
  securityDash,
  enableDashboardRoutes,
  children
}) => {
  const defaultDashboardState = useSelector(defaultDashboardSelector);
  const projectParams = useParams<ProjectPathProps>();

  useEffect(() => {
    if (securityDash) {
      return;
    }
    const defaultDashboard = defaultDashboardState?.data;
    if (enableDashboardRoutes && defaultDashboard) {
      // Redirecting to the default dashboard...
      const defaultDashboardUrl = getDefaultDashboardPath(projectParams, defaultDashboard.id);
      history && history.push(defaultDashboardUrl);
    }
  }, [defaultDashboardState]); // eslint-disable-line react-hooks/exhaustive-deps

  if (!securityDash && enableDashboardRoutes && defaultDashboardState?.loading) {
    return <Loader />;
  }

  return (
    <div className={"new-dashboard"}>
      <AntRow type={"flex"} justify={"space-between"}>
        <AntCol span={24}>{children}</AntCol>
        <AntCol span={24}>
          <DashboardOverview />
        </AntCol>
        <AntCol span={24}>
          <DashboardIntegrations />
        </AntCol>
      </AntRow>
    </div>
  );
};

export default DashboardContainer;
