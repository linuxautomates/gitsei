import React, { useCallback, useMemo, useState } from "react";
import { Tabs } from "antd";

import "./ConfigurationTabs.scss";

import { RestWidget } from "../../../../../classes/RestDashboards";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { WidgetTabsContext } from "../../../../../dashboard/pages/context";
import { WIDGET_CONFIGURATION_KEYS } from "../../../../../constants/widgets";
import TableFilters from "../table/TableFilters";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { MISCELLENEOUS_REPORTS } from "dashboard/reports/miscellaneous/constant";
import PropeloTableFilters from "../table/PropeloTableReportFilters";

const { TabPane } = Tabs;

interface TableConfigurationTabsProps {
  dashboardId: string;
  widgetId: string;
}

const TableConfigurationTabs: React.FC<TableConfigurationTabsProps> = ({ dashboardId, widgetId }) => {
  const [activeKey, setActiveKey] = useState<WIDGET_CONFIGURATION_KEYS>(WIDGET_CONFIGURATION_KEYS.FILTERS);

  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });

  const handleTabChange = useCallback((key: string) => {
    setActiveKey(key as WIDGET_CONFIGURATION_KEYS);
  }, []);

  const memoizedContextValue = useMemo(() => {
    return {
      isVisibleOnTab: (tab: any) => {
        return activeKey === tab;
      }
    };
  }, [activeKey]);

  if (!widget?.type) {
    return null;
  }

  return (
    <WidgetTabsContext.Provider value={memoizedContextValue}>
      <div className="report-configuration-tabs">
        <Tabs defaultActiveKey={activeKey} onChange={handleTabChange}>
          <TabPane tab="Filters" key={WIDGET_CONFIGURATION_KEYS.FILTERS}>
            {MISCELLENEOUS_REPORTS.TABLE_REPORTS === widget?.type ? (
              <PropeloTableFilters dashboardId={dashboardId} widgetId={widgetId} />
            ) : (
              <TableFilters dashboardId={dashboardId} widgetId={widgetId} />
            )}
          </TabPane>
        </Tabs>
      </div>
    </WidgetTabsContext.Provider>
  );
};

export default React.memo(TableConfigurationTabs);
