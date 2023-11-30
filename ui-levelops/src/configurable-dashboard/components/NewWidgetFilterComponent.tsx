import React from "react";

import "./ModalSettings.scss";
import { AntRow } from "../../shared-resources/components";
import { WidgetType } from "../../dashboard/helpers/helper";
import ConfigureTableWidget from "./configure-widget-modal/ConfigureTableWidget";
import ConfigureSingleWidget from "./configure-widget-modal/configureSingleWidget";

interface WidgetFilterComponentProps {
  graphType: WidgetType;
  widgetData: any;
  updateAndReload: any;
  globalFilters: any;
  widgetType: any;
  globalApplicationFilters: any;
  dashboardMetaData?: any;
  isParentTabData?: boolean;
  advancedTabState?: {
    value: boolean;
    callback: any;
  };
}

const NewWidgetFilterComponent: React.FC<WidgetFilterComponentProps> = ({
  graphType,
  widgetData,
  updateAndReload,
  globalFilters,
  globalApplicationFilters,
  widgetType,
  dashboardMetaData,
  isParentTabData,
  advancedTabState
}) => {
  let filters = null;
  switch (graphType) {
    case WidgetType.CONFIGURE_WIDGET_STATS:
      filters = <ConfigureTableWidget isStat widgetData={widgetData} updateWidget={updateAndReload} />;
      break;
    case WidgetType.CONFIGURE_WIDGET:
      filters = <ConfigureTableWidget isStat={false} widgetData={widgetData} updateWidget={updateAndReload} />;
      break;
    default:
      filters = (
        <ConfigureSingleWidget
          widgetData={widgetData}
          globalFilters={globalFilters}
          widgetType={widgetType}
          updateWidget={updateAndReload}
          trendOnly={false}
          graphType={graphType}
          globalApplicationFilters={globalApplicationFilters}
          dashboardMetaData={dashboardMetaData}
          isParentTabData={isParentTabData}
          advancedTabState={advancedTabState}
        />
      );
  }

  return <AntRow>{filters}</AntRow>;
};

export default React.memo(NewWidgetFilterComponent);
