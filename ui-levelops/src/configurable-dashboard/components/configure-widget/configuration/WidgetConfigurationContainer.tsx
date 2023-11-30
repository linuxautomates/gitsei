import React, { useCallback, useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";

import { RestDashboard, RestWidget } from "../../../../classes/RestDashboards";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { WidgetType } from "../../../../dashboard/helpers/helper";
import NewWidgetFilterComponent from "../../NewWidgetFilterComponent";
import { _widgetUpdate } from "reduxConfigs/actions/restapi";
import { getChildWidget } from "reduxConfigs/selectors/widgetSelector";

interface WidgetConfigurationContainerProps {
  dashboardId: string;
  widgetId: string;
  isParentTabData?: boolean;
  advancedTabState?: {
    value: boolean;
    callback: any;
  };
}

const WidgetConfigurationContainer: React.FC<WidgetConfigurationContainerProps> = ({
  dashboardId,
  widgetId,
  isParentTabData,
  advancedTabState
}) => {
  const dispatch = useDispatch();

  const dashboard: RestDashboard | null = useSelector(selectedDashboard);

  const widget: RestWidget = useParamSelector(getChildWidget, { widget_id: widgetId });

  const widgetType = useMemo(() => widget?.widget_type || WidgetType.GRAPH, [widget]);
  const globalApplicationFilters = dashboard?.global_filters;
  const globalFilters = dashboard?.query;
  const dashboardMetaData = dashboard?.metadata;

  const graphType = widget?.widget_type || WidgetType.GRAPH;

  const updateAndReload = useCallback(
    (updatedWidgetData: RestWidget, shouldUpdateType: boolean = false) => {
      if (!shouldUpdateType) {
        updatedWidgetData.type = widget.type;
      }
      dispatch(_widgetUpdate(dashboardId, widgetId, updatedWidgetData.json));
    },
    [widget]
  );

  return (
    <NewWidgetFilterComponent
      graphType={graphType}
      widgetData={widget}
      updateAndReload={updateAndReload}
      globalFilters={globalFilters}
      widgetType={widgetType}
      globalApplicationFilters={globalApplicationFilters}
      dashboardMetaData={dashboardMetaData}
      isParentTabData={isParentTabData}
      advancedTabState={advancedTabState}
    />
  );
};

export default WidgetConfigurationContainer;
