import React, { useEffect, useMemo, useState } from "react";
import { Col } from "antd";
import { configsList, _widgetUpdateCall } from "reduxConfigs/actions/restapi";
import { WIDGET_MIN_HEIGHT } from "dashboard/constants/helper";
import DemoWidget from "./demo-widget";
import DemoDashboardGraphsContainer from "../Demo-Dashboard-Graph/demo-dashboard-graph.container";
import { get } from "lodash";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { GLOBAL_SETTINGS_UUID } from "dashboard/constants/uuid.constants";
import Loader from "components/Loader/Loader";
import { useDispatch, useSelector } from "react-redux";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";

interface DemoWidgetContainerProps {
  minHeight?: string;
  widgetSpan?: number;
  id: string;
  widgetConfig: any;
  widgetType: string;
  dashboardId: string;
  drilldownSelected: boolean;
  handleShowDrillDownPreview: (widgetId: string) => void;
}

const DemoWidgetContainer: React.FC<DemoWidgetContainerProps> = (props: DemoWidgetContainerProps) => {
  const { id, widgetSpan, widgetConfig, widgetType, dashboardId, handleShowDrillDownPreview, drilldownSelected } =
    props;

  const widgetData = useSelector(state =>
    get(state, ["restapiReducer", "selected-dashboard", "metadata", "demo_data", id], {})
  );
  const [scmGlobalSettingsLoading, setSCMGlobalSettingsLoading] = useState(true);
  const dispatch = useDispatch();
  const reportType = useMemo(() => {
    return get(widgetData, ["type"], "");
  }, [widgetData]);

  const globalSettingsState = useParamSelector(getGenericRestAPISelector, {
    uri: "configs",
    method: "list",
    uuid: GLOBAL_SETTINGS_UUID
  });

  useEffect(() => {
    const data = get(globalSettingsState, "data", {});
    if (Object.keys(data).length > 0) {
      setSCMGlobalSettingsLoading(false);
    } else {
      // id is defined as number in configsList, which shouldn't be the case
      dispatch(configsList({}, GLOBAL_SETTINGS_UUID as any));
    }
  }, []);

  useEffect(() => {
    const loading = get(globalSettingsState, "loading", true);
    if (!loading) {
      setSCMGlobalSettingsLoading(false);
    }
  }, [globalSettingsState]);

  const handleChartClick = (data: any) => {
    handleShowDrillDownPreview(data);
  };

  const renderWidget = (
    <Col span={widgetSpan} id={id}>
      <DemoWidget
        id={id as any}
        dashboardId={dashboardId}
        width={widgetConfig?.width}
        title={widgetData?.title}
        description={widgetConfig?.description}
        drilldownSelected={drilldownSelected}
        widgetType={widgetType}
        reportType={reportType}
        widgetData={widgetData}
        height={getWidgetConstant(widgetConfig?.type, WIDGET_MIN_HEIGHT, "100%")}>
        {scmGlobalSettingsLoading ? (
          <Loader />
        ) : (
          <DemoDashboardGraphsContainer
            widgetConfig={widgetConfig}
            reportType={reportType}
            widgetData={widgetData}
            id={id}
            dashboardId={dashboardId}
            widgetType={widgetType}
            onChartClick={handleChartClick}
          />
        )}
      </DemoWidget>
    </Col>
  );

  return renderWidget;
};

export default DemoWidgetContainer;
