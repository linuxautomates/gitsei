import React from "react";
import MultipleReportConfiguration from "../configuration/report/MultiReportConfiguration";
import WidgetConfigurationWrapper from "./WidgetConfigurationWrapper";

interface CompositeWidgetConfigurationProps {
  dashboardId: string;
  widgetId: string;
}

const MultiWidgetConfiguration: React.FC<CompositeWidgetConfigurationProps> = ({ dashboardId, widgetId }) => {
  return (
    <WidgetConfigurationWrapper widgetId={widgetId} dashboardId={dashboardId}>
      <MultipleReportConfiguration widgetId={widgetId} dashboardId={dashboardId} />
    </WidgetConfigurationWrapper>
  );
};

export default MultiWidgetConfiguration;
