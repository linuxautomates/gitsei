import React from "react";
import MultiTimeSeriesReportConfiguration from "../configuration/report/MultiTimeSeriesConfiguration";
import WidgetConfigurationWrapper from "./WidgetConfigurationWrapper";

interface CompositeWidgetConfigurationProps {
  dashboardId: string;
  widgetId: string;
}

const MultiTimeWidgetConfiguration: React.FC<CompositeWidgetConfigurationProps> = ({ dashboardId, widgetId }) => {
  return (
    <WidgetConfigurationWrapper widgetId={widgetId} dashboardId={dashboardId}>
      <MultiTimeSeriesReportConfiguration widgetId={widgetId} dashboardId={dashboardId} />
    </WidgetConfigurationWrapper>
  );
};

export default MultiTimeWidgetConfiguration;
