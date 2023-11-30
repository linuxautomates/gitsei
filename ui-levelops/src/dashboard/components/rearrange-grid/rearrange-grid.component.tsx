import React, { useEffect, useMemo } from "react";
import GridLayout, { Layout } from "react-grid-layout";

import { makeLayout, WidgetOrderConfig, CustomLayout } from "./helper";
import RearrangeWidgetComponent from "../rearrange-widget/rearrange-widget.component";
import { RestWidget } from "../../../classes/RestDashboards";
import { cloneDeep } from "lodash";

interface RearrangeGridProps {
  widgets: RestWidget[];
  widgetType: string;
  onLayoutChange: (newLayout: WidgetOrderConfig[]) => void;
  width: number;
}

const RearrangeGridComponent: React.FC<RearrangeGridProps> = ({ widgets, onLayoutChange, widgetType, width }) => {
  useEffect(() => {
    const draftWidget: RestWidget | undefined = (widgets || []).find(
      (widget: RestWidget) => widget?.draft && !widget?.hidden
    );
    if (draftWidget) {
      const draftElement = document.getElementById(draftWidget.id);
      draftElement?.scrollIntoView({ behavior: "smooth" });
    }
  }, []);

  const handleDragStop = (layout: Layout[]) => {
    const widgets: WidgetOrderConfig[] = layout
      .sort((a: Layout, b: Layout) => {
        if (a.y === b.y) {
          return a.x - b.x;
        } else {
          return a.y - b.y;
        }
      })
      .map((item: Layout, index: number) => {
        return {
          id: item.i,
          width: [2, 4].includes(item.w) ? "full" : "half",
          order: index + 1
        };
      });

    onLayoutChange(widgets);
  };

  const layout = useMemo(() => {
    return makeLayout(widgets, widgetType);
  }, [widgets, widgetType]);

  return (
    <div>
      <GridLayout
        key={widgetType}
        autoSize
        verticalCompact
        layout={layout}
        cols={widgetType === "stats" ? 4 : 2}
        onDragStop={handleDragStop}
        rowHeight={widgetType === "stats" ? 34 : 40}
        width={width}
        isDroppable={true}
        isResizable={true}
        isDraggable={true}
        className="layout">
        {layout.map((layout: CustomLayout) => (
          <div key={layout.i} id={layout.i} data-grid={layout}>
            <RearrangeWidgetComponent className={widgetType} layoutInfo={layout} />
          </div>
        ))}
      </GridLayout>
    </div>
  );
};

export default RearrangeGridComponent;
