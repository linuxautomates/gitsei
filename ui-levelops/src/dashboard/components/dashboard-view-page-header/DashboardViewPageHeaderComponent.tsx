import { SECURITY } from "dashboard/constants/constants";
import React, { useCallback, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import DashboardHeader from "../dashboard-header/DashboardHeader";
import "./dashboardViewPageHeader.styles.scss";
import { DashboardViewSecondaryHeader } from "..";
import { secondaryFilterExist } from "./helper";
import { dashboardTimeUpdate, newDashboardUpdate } from "reduxConfigs/actions/restapi";
import { useHistory, useParams } from "react-router-dom";
import { getDashboardsPage } from "constants/routePaths";
import { ProjectPathProps } from "classes/routeInterface";
import { getIsStandaloneApp } from "helper/helper";

interface DashboardViewPageHeaderComponentProps {
  dashboardId: any;
  widgetsLoading: boolean;
  generatingReportProgress: boolean;
  handleExport: () => void;
  openSettings: (param: boolean) => void;
  openNotesModal?: (visibility: boolean) => void;
  handleShowApplicationFilterModal: (param?: boolean) => void;
  queryparamOU?: string | undefined;
}
const DashboardViewPageHeaderComponent: React.FC<DashboardViewPageHeaderComponentProps> = ({
  dashboardId,
  widgetsLoading,
  generatingReportProgress,
  handleExport,
  openSettings,
  handleShowApplicationFilterModal,
  openNotesModal,
  queryparamOU
}) => {
  const dashboard = useSelector(selectedDashboard);
  const isParameterizedHeader = true;
  const [popOverVisible, setPopOverVisible] = useState<boolean>(false);
  const dispatch = useDispatch();
  const history = useHistory();
  const projectParams = useParams<ProjectPathProps>();
  const onFilerValueChange = useCallback(
    (value: any, key: string) => {
      setPopOverVisible(false);
      const form = {
        metadata: {
          ...dashboard?._metadata,
          [key]: value
        }
      };
      if (key === "dashboard_time_range_filter") {
        dispatch(
          dashboardTimeUpdate(dashboardId, { dashboard_time_range_filter: value, metaData: dashboard?._metadata })
        );
      } else if (key === "ou_ids") {
        const queryValue = value[0] ? `OU=${value[0]}` : undefined;
        history.push({ pathname: `${getDashboardsPage(projectParams)}/${dashboardId}`, search: queryValue });
        dispatch(newDashboardUpdate(dashboardId, form));
      } else {
        dispatch(newDashboardUpdate(dashboardId, form));
      }
    },
    [dashboard?._metadata]
  );

  return (
    <div className={getIsStandaloneApp() ? "dashboard-view-page-header-container" : ""}>
      <div className="dashboad-secondary-header-wrapper">
        {dashboard?.type !== SECURITY && (
          <DashboardHeader
            dashboardId={dashboardId}
            widgetsLoading={widgetsLoading}
            generatingReportProgress={generatingReportProgress}
            handleExport={handleExport}
            openSettings={openSettings}
            handleShowApplicationFilterModal={handleShowApplicationFilterModal}
            openNotesModal={openNotesModal}
            isParameterizedHeader={isParameterizedHeader}
            onFilerValueChange={onFilerValueChange}
            queryparamOU={queryparamOU}
            metaData={dashboard?._metadata}
            popOverVisible={popOverVisible}
            setPopOverVisible={setPopOverVisible}
          />
        )}
        {secondaryFilterExist(dashboard?._metadata) && (
          <DashboardViewSecondaryHeader
            popOverVisible={popOverVisible}
            setPopOverVisible={setPopOverVisible}
            onFilerValueChange={onFilerValueChange}
            dashboardId={dashboardId}
            metaData={dashboard?._metadata}
          />
        )}
      </div>
    </div>
  );
};

export default DashboardViewPageHeaderComponent;
