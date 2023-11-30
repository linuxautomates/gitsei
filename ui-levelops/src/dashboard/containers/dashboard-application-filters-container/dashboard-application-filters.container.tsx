import { RestDashboard } from "classes/RestDashboards";
import DashboardApplicationFilters from "dashboard/components/dashboard-application-filters/DashboardApplicationFilters";
import { get } from "lodash";
import React, { useCallback, useMemo } from "react";
import { useDispatch } from "react-redux";
import { widgetBulkUpdate } from "reduxConfigs/actions/restapi";
import { getDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import queryString from "query-string";
import { useLocation } from "react-router-dom";
interface DashboardApplicationFiltersContainerProps {
  dashboardId: any;
  showApplicationModal: boolean;
  toggleApplicationFilters: (param?: boolean) => void;
}

const DashboardApplicationFiltersContainer: React.FC<DashboardApplicationFiltersContainerProps> = (
  props: DashboardApplicationFiltersContainerProps
) => {
  const { dashboardId, toggleApplicationFilters, showApplicationModal } = props;
  const dashboard: RestDashboard = useParamSelector(getDashboard, { dashboard_id: dashboardId });

  const integration_ids = useMemo(() => get(dashboard, ["query", "integration_ids"], []), [dashboard]);
  const location = useLocation();
  const queryParamOU = queryString.parse(location.search).OU as string;
  const dispatch = useDispatch();

  const handleCancel = useCallback(() => toggleApplicationFilters(), []);

  const handleOk = useCallback(
    (filters: any, update: boolean = false) => {
      handleCancel();
      dispatch(widgetBulkUpdate(dashboardId, filters));
    },
    [dashboardId, handleCancel]
  );

  return (
    <DashboardApplicationFilters
      filters={dashboard?.global_filters}
      jiraOrFilters={dashboard?.jira_or_query}
      ou_ids={queryParamOU ? [queryParamOU] : dashboard?.ou_ids}
      integrationIds={integration_ids}
      ouUserFilterDesignations={dashboard?.ou_user_filter_designation}
      showOUOverrides
      onCancel={handleCancel}
      onOk={handleOk}
      visible={showApplicationModal}
      viewMode
    />
  );
};

export default DashboardApplicationFiltersContainer;
