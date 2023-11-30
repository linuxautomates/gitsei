import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { Button, Collapse, Form, Icon } from "antd";
import { debounce, get } from "lodash";

import "./MultiReportConfiguration.scss";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { dashboardWidgetChildrenSelector } from "reduxConfigs/selectors/dashboardSelector";
import { RestWidget } from "classes/RestDashboards";
import { WidgetType } from "dashboard/helpers/helper";
import { AntText, AntButton, AntInput, AntIcon } from "shared-resources/components";
import ReportsDropdown from "./../dropdown/ReportsDropdown";
import ReportConfigurationTabs from "./../tabs/ReportConfigurationTabs";
import {
  multiReportWidgetReportAdd,
  multiReportWidgetReportDelete,
  multiReportWidgetReportNameUpdate
} from "reduxConfigs/actions/restapi";
import { getReportNameByKey } from "utils/reportListUtils";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { restapiClear, setSelectedChildId } from "reduxConfigs/actions/restapi/restapiActions";
import WidgetInfoDrawer from "dashboard/pages/explore-widget/widget-info-drawer/WidgetInfoDrawer";
import widgetConstants from "dashboard/constants/widgetConstants";

const { Panel } = Collapse;

interface MultiReportConfigurationProps {
  dashboardId: string;
  widgetId: string;
}

const MultiReportConfiguration: React.FC<MultiReportConfigurationProps> = ({ dashboardId, widgetId }) => {
  const [selectedWidgetId, setSelectedWidgetId] = useState<string | undefined>(undefined);
  const [selectedReport, setSelectedReport] = useState<string | undefined>();
  const [addNewReport, setAddNewReport] = useState<boolean>(false);
  const [widgetDetailId, setWidgetDetailId] = useState<string | undefined>();

  const dispatch = useDispatch();
  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });

  const [showWidgetInfoDrawer, setShowWidgetInfoDrawer] = useState<boolean>(false);

  const childWidgetIds = widget.children || [];

  const childWidgets: RestWidget[] = useParamSelector(dashboardWidgetChildrenSelector, {
    dashboard_id: dashboardId,
    widget_id: widgetId
  });

  const childWidget = useMemo(() => {
    return childWidgets.find((w: RestWidget) => w.id === selectedWidgetId);
  }, [selectedWidgetId]);

  const childWidgetType = childWidget?.widget_type || WidgetType.GRAPH;

  useEffect(() => {
    if (Array.isArray(childWidgetIds) && childWidgetIds.length > 0) {
      setSelectedWidgetId(childWidgetIds[0]);
      dispatch(setSelectedChildId(childWidgetIds[0]));

      // clearing store to refresh the widgets when navigating back to dashboard view
      childWidgets.forEach(widget => {
        const uri = get(widgetConstants, [widget.type, "uri"], "");
        const method = get(widgetConstants, [widget.type, "method"], "");
        dispatch(restapiClear(uri, method, widget.id));
      });
    }
  }, []);

  useEffect(() => {
    if (addNewReport) {
      setSelectedReport(undefined);
      setAddNewReport(false);
      setSelectedWidgetId(childWidgetIds[0]);
      dispatch(setSelectedChildId(childWidgetIds[0]));
    }
  }, [childWidgets]);

  const handleReportSelect = useCallback(
    (value: string) => {
      setSelectedReport(value);
    },
    [widgetId]
  );

  const onAddSelectReport = () => {
    dispatch(multiReportWidgetReportAdd(widgetId, selectedReport as string));
  };

  const onItemDelete = useCallback(
    (e: any, value: string) => {
      e.stopPropagation();
      e.preventDefault();
      dispatch(multiReportWidgetReportDelete(widgetId, value));
    },
    [widgetId]
  );

  const handleSelectedReportChange = useCallback((key: string | string[]) => {
    if (!key) {
      setSelectedWidgetId(undefined);
      return;
    }
    const _key = typeof key === "string" ? key : key[0];
    setSelectedWidgetId(_key);
    dispatch(setSelectedChildId(_key));
  }, []);

  const updateName = useCallback(
    debounce((childId: string, value: string) => {
      dispatch(multiReportWidgetReportNameUpdate(childId, value));
    }, 300),
    []
  );

  const onShowWidgetClick = useCallback((e: any, id: string) => {
    e.stopPropagation();
    e.preventDefault();
    setShowWidgetInfoDrawer(true);
    setWidgetDetailId(id);
  }, []);

  const activeKey = useMemo(() => (selectedWidgetId ? [selectedWidgetId] : []), [selectedWidgetId]);

  const collapseStyle = useMemo(
    () => ({
      maxHeight: `calc(100% - ${addNewReport ? 176 : 40}px`,
      overflow: "auto",
      width: "100%"
    }),
    [addNewReport]
  );

  const reports = useMemo(() => {
    const panels = childWidgets.map(({ name, id: childWidgetId, type }: RestWidget, index: number) => {
      const header = !name ? `Report ${index + 1} - ${getReportNameByKey(type)}` : name; // TODO: styling.
      return (
        <Panel
          header={header}
          key={childWidgetId}
          extra={
            <>
              <Icon type="delete" onClick={(e: any) => onItemDelete(e, childWidgetId)} />
              <AntIcon
                type="question-circle"
                style={{ marginLeft: "8px" }}
                onClick={(e: any) => onShowWidgetClick(e, childWidgetId)}
              />
            </>
          }>
          <Form.Item label="Data Series Name">
            <AntInput defaultValue={header} onChange={(e: any) => updateName(childWidgetId, e.target.value)} />
          </Form.Item>
          <ReportConfigurationTabs widgetId={childWidgetId} dashboardId={dashboardId} />
        </Panel>
      );
    });
    return (
      <Collapse
        className={"trend-report-collapse"}
        style={collapseStyle}
        accordion
        activeKey={activeKey}
        onChange={handleSelectedReportChange}>
        {panels}
      </Collapse>
    );
  }, [childWidgets, selectedWidgetId, collapseStyle]);

  const widgetInfoDrawer = useMemo(() => {
    const childWidget = childWidgets.find(widget => widget.id === widgetDetailId);

    if (!showWidgetInfoDrawer || !childWidget) {
      return null;
    }

    const compactInfo: any = { key: childWidget?.reportType, name: childWidget?.reportName };
    return (
      <WidgetInfoDrawer
        reportTheme={compactInfo}
        visible
        onClose={() => {
          setShowWidgetInfoDrawer(false);
        }}
      />
    );
  }, [showWidgetInfoDrawer, widgetDetailId, childWidgets]);

  return (
    <div className="h-100 overflow-hidden">
      <div className="report-label">
        <AntText className="label">{"Reports"}</AntText>
        <AntText className="report-count">{`${childWidgetIds.length}`}</AntText>
        <div className="flex-1" />
        <AntButton type="link" className="report-add-button" onClick={() => setAddNewReport(true)}>
          + Add report
        </AntButton>
      </div>
      {addNewReport && (
        <div className="multi-report-list-dd">
          <div className="flex">
            <ReportsDropdown
              placeHolder="Select a Multi-Metric Report"
              dashboardId={dashboardId}
              widgetType={WidgetType.COMPOSITE_GRAPH}
              className="report-dropdown"
              value={selectedReport}
              onChange={handleReportSelect}
            />
            <Button icon="check" className="select-report-icon" onClick={onAddSelectReport} />
          </div>
          <AntText className="select-report-info">
            Note: Multi-metric (Time Series Trends) reports allows you to layer Trend Reports to compare data across
            multiple metrics.
          </AntText>
        </div>
      )}
      {reports}
      {childWidgets.length === 0 && (
        <AntText class="multi-report-info">Use + Add report button above to create new report.</AntText>
      )}
      {widgetInfoDrawer}
    </div>
  );
};

export default MultiReportConfiguration;
