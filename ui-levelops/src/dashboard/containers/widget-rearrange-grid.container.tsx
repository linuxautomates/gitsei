import React, { createRef, useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { Collapse } from "antd";
import { uniq } from "lodash";

import "./widget-rearrange-grid.style.scss";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { WidgetOrderConfig } from "dashboard/components/rearrange-grid/helper";
import RearrangeGridComponent from "../components/rearrange-grid/rearrange-grid.component";
import { makeWidgetsReversible, widgetsReOrder } from "reduxConfigs/actions/restapi/widgetActions";
import { WidgetType } from "../helpers/helper";
import { getWidgetsFromType } from "../helpers/dashboardWidgetTypeFilter.helper";
import { getWidgetsByDashboardId } from "reduxConfigs/selectors/widgetSelector";
import { RestWidget } from "../../classes/RestDashboards";

const { Panel } = Collapse;

interface WidgetRearrangeGridProps {
  dashboardId: string;
}

enum PANELS {
  STATS = "stats",
  REPORTS = "reports"
}

const WidgetRearrangeGridContainer: React.FC<WidgetRearrangeGridProps> = ({ dashboardId }) => {
  const [width, setWidth] = useState<number>(1200);
  const [activePanels, setActivePanels] = useState<string | string[]>([PANELS.REPORTS, PANELS.STATS]);
  const ref: any = createRef();
  const dispatch = useDispatch();

  const widgets: RestWidget[] = useParamSelector(getWidgetsByDashboardId, { dashboard_id: dashboardId });

  useEffect(() => {
    dispatch(makeWidgetsReversible(widgets.map((w: RestWidget) => w.id)));
  }, []);

  useEffect(() => {
    if (ref.current) {
      setWidth(ref.current.clientWidth - 51.2); // subtracting 25.6 for the padding ( 1.6 rem on each side)
    }
  }, [ref]);

  const statWidgets: RestWidget[] = useMemo(() => getWidgetsFromType(widgets, WidgetType.STATS, true), [widgets]);
  const graphWidgets: RestWidget[] = useMemo(() => getWidgetsFromType(widgets, WidgetType.GRAPH, true), [widgets]);

  useEffect(() => {
    const hasDraftWidget = graphWidgets.find(w => w.draft);
    if (hasDraftWidget || !statWidgets.length) {
      setActivePanels([PANELS.REPORTS]);
    }
  }, [graphWidgets, statWidgets]);

  const handleStatsOrderChange = (widgetOrderList: WidgetOrderConfig[]) => {
    dispatch(widgetsReOrder(dashboardId, WidgetType.STATS, widgetOrderList));
  };

  const handleGraphOrderChange = (widgetOrderList: WidgetOrderConfig[]) => {
    dispatch(widgetsReOrder(dashboardId, WidgetType.GRAPH, widgetOrderList));
  };

  const handleCollapseChange = useCallback((key: string | string[]) => {
    const activePanels = Array.isArray(key) ? key : [key];
    setActivePanels(uniq([...activePanels, PANELS.REPORTS]));
  }, []);

  return (
    <div className="h-100">
      <div className="rearrange-grid" ref={ref}>
        <Collapse activeKey={activePanels} onChange={handleCollapseChange} defaultActiveKey={activePanels}>
          <Panel header="Single Stats" key={PANELS.STATS}>
            <div className="position-relative">
              <RearrangeGridComponent
                key={"stats"}
                width={width}
                widgets={statWidgets}
                widgetType="stats"
                onLayoutChange={handleStatsOrderChange}
              />
            </div>
          </Panel>
          <Panel showArrow={false} header="Dashboard Widgets" key={PANELS.REPORTS}>
            <div className="position-relative">
              <RearrangeGridComponent
                key={"graph"}
                width={width}
                widgets={graphWidgets}
                widgetType="graph"
                onLayoutChange={handleGraphOrderChange}
              />
            </div>
          </Panel>
        </Collapse>
      </div>
    </div>
  );
};

export default WidgetRearrangeGridContainer;
