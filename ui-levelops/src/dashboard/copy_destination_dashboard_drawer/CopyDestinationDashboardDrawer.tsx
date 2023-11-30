import React, { useCallback, useMemo, useState } from "react";
import { Drawer, Typography } from "antd";
import "./CopyDestinationDashboardDrawer.styles.scss";
import DestinationDashboardListPage from "./DestinationDashboardListPage";
import { AntButton } from "shared-resources/components";
import { useDispatch } from "react-redux";
import { copyWidgetToDashboard } from "reduxConfigs/actions/restapi";
import { useHistory, useParams } from "react-router-dom";
import { WebRoutes } from "routes/WebRoutes";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { copyDashboardGetStatus } from "reduxConfigs/selectors/dashboardSelector";
import { useEffect } from "react";
import { CopyDestinationDashboardConfig } from "dashboard/dashboard-types/Dashboard.types";
import queryString from "query-string";
import { ProjectPathProps } from "classes/routeInterface";
interface CopyDestinationDashboardDrawerProps {
  showCopyDestinationDashboardsDrawer: string | undefined;
  handleCopyDestinationDashboardsDrawer: () => void;
}
const { Title, Text } = Typography;
const initialSelectedDashboardConfig = {
  selectedDashboard: "",
  selectedPage: -1,
  selectedRowIndex: -1
};
const CopyDestinationDashboardDrawer: React.FC<CopyDestinationDashboardDrawerProps> = ({
  showCopyDestinationDashboardsDrawer,
  handleCopyDestinationDashboardsDrawer
}) => {
  const [page, setPage] = useState<number>(1);
  const [selectedDashboardConfig, setSelectedDashboardConfig] =
    useState<CopyDestinationDashboardConfig>(initialSelectedDashboardConfig);
  const [searchQuery, setSearchQuery] = useState<string>("");
  const [intitialQueryParams, setInitialQueryParams] = useState<string>("");
  const newCopyWidgetId = useParamSelector(copyDashboardGetStatus, {
    dashboard_id: selectedDashboardConfig.selectedDashboard
  });
  const history = useHistory();
  const params = useParams();
  const initialDashboardId = (params as any).id;
  const dispatch = useDispatch();
  const projectParams = useParams<ProjectPathProps>();

  useEffect(() => {
    const OU = queryString.parse(history?.location?.search)?.OU;
    const workspaceId = queryString.parse(history?.location?.search)?.workspace_id;
    const prevData = `prev_OU=${OU}&workspace_id=${workspaceId}`;
    setInitialQueryParams(prevData);
  }, []);

  useEffect(() => {
    if (newCopyWidgetId) {
      const search = `?OU=${newCopyWidgetId?.OU?.id}&${intitialQueryParams}`;
      history.push(
        WebRoutes.dashboard.widgets.widgetsRearrangeWithCopyInProgress(
          projectParams,
          selectedDashboardConfig.selectedDashboard,
          initialDashboardId,
          search
        )
      );
    }
  }, [newCopyWidgetId, selectedDashboardConfig]);

  const handlePageChange = (page: number) => {
    setPage(page);
  };

  const handleRowClick = (id: string, rowIndex: number) => {
    setSelectedDashboardConfig({ selectedDashboard: id, selectedRowIndex: rowIndex, selectedPage: page });
  };

  const handleSearchChange = (searchQuery: string) => {
    setSearchQuery(searchQuery);
  };

  const handleSelectClick = () => {
    dispatch(
      copyWidgetToDashboard(selectedDashboardConfig.selectedDashboard, showCopyDestinationDashboardsDrawer || "")
    );
  };

  const handleDrawerClose = () => {
    handleSearchChange("");
    setSelectedDashboardConfig(initialSelectedDashboardConfig);
    handleCopyDestinationDashboardsDrawer();
  };

  const handleRowClassName = useCallback(
    (record: any, index: number) => {
      return index === selectedDashboardConfig.selectedRowIndex && page === selectedDashboardConfig.selectedPage
        ? "dashboard_selected_row"
        : "";
    },
    [selectedDashboardConfig, page]
  );

  const renderDrawerTitle = useMemo(
    () => (
      <div className="drawer_title_container">
        <Title level={3} className="title">
          Copy Widget to Another Insight
        </Title>
        <Text className="description">Select the destination insight</Text>
      </div>
    ),
    []
  );
  return (
    <Drawer
      title={renderDrawerTitle}
      visible={!!showCopyDestinationDashboardsDrawer}
      closable
      className="copy-destination-dashboards"
      onClose={handleDrawerClose}>
      <DestinationDashboardListPage
        rowClassName={handleRowClassName}
        handleRowClick={handleRowClick}
        handlePageChange={handlePageChange}
        handleSearchChange={handleSearchChange}
        searchQuery={searchQuery}
        page={page}
      />
      <div className="footer">
        <AntButton type="ghost" onClick={handleDrawerClose} className="footer_cancel_button">
          Cancel
        </AntButton>
        <AntButton type="primary" onClick={handleSelectClick} disabled={!selectedDashboardConfig.selectedDashboard}>
          Select
        </AntButton>
      </div>
    </Drawer>
  );
};

export default CopyDestinationDashboardDrawer;
