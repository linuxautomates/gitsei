import { RestWidget } from "classes/RestDashboards";
import { WidgetType } from "./helper";

export const getWidgetsFromType = (widgets: RestWidget[], type: WidgetType, includeDraft = false) => {
  let _widgets = [
    ...widgets.filter(
      (widget: { widget_type: string; hidden: boolean }) => widget.widget_type.includes(type) && !widget.hidden
    )
  ];

  if (type === WidgetType.GRAPH) {
    const configWidgets = widgets.filter((widget: any) => widget.widget_type === WidgetType.CONFIGURE_WIDGET);
    _widgets = [..._widgets, ...configWidgets];
  }

  _widgets = _widgets.sort((a, b) => a.order - b.order);
  if (!includeDraft) {
    _widgets = _widgets.filter((widget: RestWidget) => !widget.draft);
  }
  return _widgets;
};
