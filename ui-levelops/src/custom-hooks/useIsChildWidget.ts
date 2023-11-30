import { RestWidget } from "classes/RestDashboards";
import { WidgetType } from "dashboard/helpers/helper";
import { filter } from "lodash";
import { useMemo } from "react";
import { dashboardWidgetsSelector } from "reduxConfigs/selectors/dashboardSelector";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";

export const useIsChildWidget = (dashboardId: string, widgetId: string): boolean => {
  const widgets: RestWidget[] = useParamSelector(dashboardWidgetsSelector, {
    dashboard_id: dashboardId
  });

  const compositeWidgets = useMemo(
    () => filter(widgets, wid => wid.widget_type === WidgetType.COMPOSITE_GRAPH),
    [widgets]
  );

  const isChild = useMemo(() => {
    const currWidget: any = filter(compositeWidgets, wid => wid?.metadata?.children?.includes(widgetId))?.[0];
    if (currWidget) {
      // widget is considered a composite child widget
      // when there are more than 1 reports added
      // else it is treated as a single widget
      return currWidget?.metadata?.children?.length > 1;
    }
    return false;
  }, [compositeWidgets, widgetId]);

  return isChild;
};

export default useIsChildWidget;
