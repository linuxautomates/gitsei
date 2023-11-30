import React, { useMemo } from "react";
import WidgetConfigurationWrapper from "./WidgetConfigurationWrapper";
import SingleReportConfiguration from "../configuration/report/SingleReportConfiguration";
import { RestWidget } from "../../../../classes/RestDashboards";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { WidgetType } from "../../../../dashboard/helpers/helper";
import SingleTableConfiguration from "../configuration/table/SingleTableConfiguration";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";

interface WidgetConfigurationProps {
  dashboardId: string;
  widgetId: string;
}

const SingleWidgetConfiguration: React.FC<WidgetConfigurationProps> = ({ dashboardId, widgetId }) => {
  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });

  const widgetType = widget?.widget_type || "";
  const isTable = [WidgetType.CONFIGURE_WIDGET, WidgetType.CONFIGURE_WIDGET_STATS].includes(widgetType);

  const renderConfig = useMemo(() => {
    if (isTable) {
      return <SingleTableConfiguration widgetId={widgetId} dashboardId={dashboardId} />;
    }
    return <SingleReportConfiguration widgetId={widgetId} dashboardId={dashboardId} />;
  }, [isTable]);

  return (
    <WidgetConfigurationWrapper widgetId={widgetId} dashboardId={dashboardId}>
      {renderConfig}
    </WidgetConfigurationWrapper>
  );
};

export default SingleWidgetConfiguration;
