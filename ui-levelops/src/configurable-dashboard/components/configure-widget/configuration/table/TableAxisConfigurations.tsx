import React from "react";
import { get } from "lodash";
import { useDispatch } from "react-redux";

import ConfigTableWidgetStat from "../../../configure-widget-modal/config-table-widget-modal/ConfigTableWidgetStat";
import ConfigTableWidgetAxis from "../../../configure-widget-modal/config-table-widget-modal/ConfigTableWidgetAxis";
import { _widgetUpdate } from "reduxConfigs/actions/restapi/widgetActions";
import { RestWidget } from "../../../../../classes/RestDashboards";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { _dashboardWidgetSelector } from "reduxConfigs/selectors/dashboardSelector";
import { WidgetType } from "../../../../../dashboard/helpers/helper";
import { useTables } from "../../../../../custom-hooks";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";

interface TableAxisConfigurationProps {
  widgetId: string;
  dashboardId: string;
}

const TableAxisConfiguration: React.FC<TableAxisConfigurationProps> = ({ dashboardId, widgetId }) => {
  const dispatch = useDispatch();

  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });

  const isStat = widget.widget_type === WidgetType.CONFIGURE_WIDGET_STATS;

  const selectedTable = get(widget, ["metadata", "tableId"], "");

  const [tableGetLoading, tableGetData] = useTables("get", selectedTable, true, undefined, [selectedTable]);

  const getXAxisOptions = () => {
    if (tableGetLoading || !tableGetData) {
      return [];
    } else {
      const columns = get(tableGetData, ["columns"], []);
      return columns.map((column: any) => {
        return {
          key: get(column, ["id"], ""),
          label: get(column, ["title"], "")
        };
      });
    }
  };

  const setDebouncedValues = (key: string, value: any) => {
    widget.metadata = { ...(widget.metadata || {}), [key]: value };
    dispatch(_widgetUpdate(dashboardId, widgetId, { ...widget.json }));
  };

  const setValues = (key: string, value: any) => {
    let data = widget;
    data.metadata = { ...(data.metadata || {}), [key]: value };
    if (key === "yAxis" && Array.isArray(value)) {
      let newFilters: { [x: string]: string } = {};
      getXAxisOptions().forEach((yA: any) => {
        if (Object.keys(data.query || {}).includes(yA.key))
          newFilters = {
            ...newFilters,
            [`${yA.key}`]: get(data.query, [yA.key], "")
          };
      });
      data.query = newFilters;
    }
    dispatch(_widgetUpdate(dashboardId, widgetId, { ...data.json }));
  };

  const getYAxisOptions = () => {
    const selectedXAxis = get(widget, ["metadata", "xAxis"], undefined);
    return getXAxisOptions().filter((option: { key: any }) => option.key !== selectedXAxis);
  };

  const getYAxis = () => {
    return get(widget, ["metadata", "yAxis"], []);
  };

  const addYAxis = () => {
    const newYAxis = [...getYAxis(), { key: "", value: "", label: "" }];
    setValues("yAxis", newYAxis);
  };

  const setYAxis = (key: string, value: any, yIndex: number) => {
    const newYAxis = getYAxis().map((item: { key: string; value: string }, index: number) => {
      if (yIndex === index) {
        return {
          ...item,
          [key]: value
        };
      }
      return item;
    });
    setValues("yAxis", newYAxis);
  };

  const getGroupBy = () => {
    return getWidgetDataValue(["metadata", "groupBy"], false);
  };

  const getWidgetDataValue = (path: string[], defaultValue?: any) => {
    return get(widget, path, defaultValue);
  };

  return (
    <div style={{ width: "100%", display: "flex", justifyContent: "space-between" }}>
      {isStat ? (
        <ConfigTableWidgetStat options={getXAxisOptions()} value={getYAxis()[0] || {}} setAxis={setYAxis} />
      ) : (
        <ConfigTableWidgetAxis
          setValues={setValues}
          xAxisValue={get(widget, ["metadata", "xAxis"], undefined)}
          yAxisOptions={getYAxisOptions()}
          xAxisOptions={getXAxisOptions()}
          yAxis={getYAxis()}
          setYAxis={setYAxis}
          addYAxis={addYAxis}
          setGroupBy={(value: boolean) => setDebouncedValues("groupBy", value)}
          groupBy={getGroupBy()}
          setStackBy={(value: string[]) => setDebouncedValues("stackBy", value)}
          stackBy={getWidgetDataValue(["metadata", "stackBy"])}
        />
      )}
    </div>
  );
};

export default React.memo(TableAxisConfiguration);
