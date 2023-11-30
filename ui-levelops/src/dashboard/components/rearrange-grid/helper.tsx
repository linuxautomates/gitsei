import { Layout } from "react-grid-layout";
import { RestWidget } from "../../../classes/RestDashboards";
import { WidgetType } from "../../helpers/helper";

const STAT_WIDGET_HEIGHT = 3;
const WIDGET_HEIGHT = 5;
const NOTES_WIDGET_HEIGHT = 4;

export interface CustomLayout extends Layout {
  add: boolean;
  data?: RestWidget;
}

export interface WidgetOrderConfig {
  id: string;
  order: number;
  width: "half" | "full";
}

const getWidgetHeight = (type: WidgetType) => {
  switch (type) {
    case WidgetType.CONFIGURE_WIDGET:
    case WidgetType.COMPOSITE_GRAPH:
    case WidgetType.GRAPH:
      return WIDGET_HEIGHT;
    case WidgetType.GRAPH_NOTES:
    // return NOTES_WIDGET_HEIGHT;
    case WidgetType.CONFIGURE_WIDGET_STATS:
    case WidgetType.STATS:
    case WidgetType.STATS_NOTES:
      return STAT_WIDGET_HEIGHT;
    default:
      return WIDGET_HEIGHT;
  }
};

export const makeLayout = (widgets: RestWidget[], widgetType: string): CustomLayout[] => {
  let row = 0;
  let col = 0;
  let _y = 0;
  let last_row = 0;
  const colCount = widgetType === "stats" ? 4 : 2;
  const sortedData = widgets.filter((widget: any) => !widget.hidden).sort((a: any, b: any) => a.order - b.order);
  const customLayout: CustomLayout[] = sortedData.map((item: RestWidget, index: number) => {
    const widgetHeight = getWidgetHeight(item.widget_type);
    if (index > 0 && sortedData[index - 1].width !== "full") {
      col = col + 1;
    }

    if (index > 0 && sortedData[index - 1].width === "full") {
      col = 0;
      row = row + 1;
    }

    if (col === colCount) {
      col = 0;
      row = row + 1;
    }

    if (col === 1 && item.width === "full") {
      col = 0;
      row = row + 1;
    }

    if (last_row < row) {
      _y = _y + widgetHeight;
      last_row = row;
    }

    let w = item.width === "full" ? 2 : 1;

    if (item.widget_type === WidgetType.STATS_NOTES) {
      w = 4;
    }

    return {
      i: item.id,
      x: col,
      y: _y,
      w,
      h: widgetHeight,
      add: false,
      data: item,
      minW: 1,
      maxW: 2,
      name: item.name,
      position: index,
      isDraggable: true,
      isResizable: true
    };
  });
  // customLayout.push(getAddButtonLayout(customLayout, widgetType));
  return customLayout;
};

export const getAddButtonLayout = (_actualWidgets: CustomLayout[], widgetType: string) => {
  const widgetHeight = widgetType === "stats" ? STAT_WIDGET_HEIGHT : WIDGET_HEIGHT;

  let x = 0;

  if (!!_actualWidgets.length) {
    const _lastWidget = _actualWidgets[_actualWidgets.length - 1];
    const { w: _width, x: _x } = _lastWidget;

    if (widgetType !== "stats") {
      if (_width === 1 && _x === 0) {
        x = 1;
      }
    } else {
      // for stats which has 4 per row, all of same width
      if (_x === 3) {
        x = 0;
      } else {
        x = _x + 1;
      }
    }
  }

  return {
    i: "add",
    x: x,
    y: Infinity,
    w: 1,
    h: widgetHeight,
    isDraggable: false,
    add: true,
    minW: 1,
    maxW: 2
  } as CustomLayout;
};
