import React, { useMemo } from "react";
import { AntCol } from "shared-resources/components";
import { RestDashboard } from "classes/RestDashboards";
import DemoWidgetListContainer from "./demo-widget-list.container";
import { WidgetType } from "dashboard/helpers/helper";
import { get } from "lodash";
import widgetConstants from "dashboard/constants/widgetConstants";

interface DemoDashboardWidgetsContainerProps {
  dashboard: RestDashboard;
  dashboardId: string;
}

const DemoDashboardWidgetsContainer: React.FC<DemoDashboardWidgetsContainerProps> = (
  props: DemoDashboardWidgetsContainerProps
) => {
  const { dashboard } = props;

  const demoData = useMemo(() => {
    return Object.keys(dashboard?.metadata?.demo_data || {}).reduce((acc: any, next: any) => {
      return [...acc, { [next]: dashboard?.metadata?.demo_data[next] }];
    }, []);
  }, [dashboard]);

  const statWidgetsData = useMemo(() => {
    return demoData
      .filter((widget: any) => {
        const value: any = Object.values(widget)[0];
        const suppportedWidgetType = get(widgetConstants, [value?.type, "supported_widget_types", "0"], "");
        if (suppportedWidgetType?.includes("stats")) {
          return true;
        }
        return false;
      })
      .sort((a: any, b: any) => {
        const valueA: any = Object.values(a || {})[0];
        const valueB: any = Object.values(b || {})[0];
        return valueA?.order - valueB?.order;
      });
  }, [demoData]);

  const graphWidgetsData = useMemo(() => {
    return demoData
      .filter((widget: any) => {
        const value: any = Object.values(widget)[0];
        const suppportedWidgetType = get(widgetConstants, [value?.type, "supported_widget_types", "0"], "");
        if (suppportedWidgetType?.includes("stats")) {
          return false;
        }
        return true;
      })
      .sort((a: any, b: any) => {
        const valueA: any = Object.values(a || {})[0];
        const valueB: any = Object.values(b || {})[0];
        return valueA?.order - valueB?.order;
      });
  }, [demoData]);

  return (
    <AntCol span={24}>
      <div className="dashboard-widgets-container">
        <DemoWidgetListContainer
          demoData={statWidgetsData}
          widgetType={WidgetType.STATS}
          dashboardId={props.dashboardId}
        />
        <DemoWidgetListContainer
          demoData={graphWidgetsData}
          widgetType={WidgetType.GRAPH}
          dashboardId={props.dashboardId}
        />
      </div>
    </AntCol>
  );
};

export default DemoDashboardWidgetsContainer;
