import React, { useCallback } from "react";
import _, { get, uniq } from "lodash";
import { useDispatch } from "react-redux";
import { RestDashboard, RestWidget } from "classes/RestDashboards";
import { useTables } from "custom-hooks";
import ConfigTableWidgetStat from "configurable-dashboard/components/configure-widget-modal/config-table-widget-modal/ConfigTableWidgetStat";
import ConfigTableWidgetAxis, {
  YAxisType
} from "configurable-dashboard/components/configure-widget-modal/config-table-widget-modal/ConfigTableWidgetAxis";
import ConfigTableWidgetFilter from "../../../configure-widget-modal/config-table-widget-modal/ConfigTableWidgetFilter";
import { widgetUpdate } from "reduxConfigs/actions/restapi/widgetActions";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { uniqYAxisWithOrderPreserved } from "./helper";
import { getDashboard } from "reduxConfigs/selectors/dashboardSelector";
import moment from "moment";
import { colorPalletteShades } from "shared-resources/charts/chart-themes";
import { sanitizeObjectCompletely } from "utils/commonUtils";

interface TableFiltersProps {
  widgetId: string;
  dashboardId: string;
}

const TableFilters: React.FC<TableFiltersProps> = ({ dashboardId, widgetId }) => {
  const dispatch = useDispatch();

  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });

  const dashboard: RestDashboard = useParamSelector(getDashboard, { dashboard_id: dashboardId });

  const selectedTable = get(widget, ["metadata", "tableId"], "");

  const [tableGetLoading, tableGetData] = useTables("get", selectedTable, true, undefined, [selectedTable]);

  const widgetGroupBy = get(widget, ["metadata", "groupBy"], false);

  const dashboardMetadata = get(dashboard, "_metadata", {});

  const widgetMetadata = get(widget, "metadata", {});

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
    if (key === "xAxis") {
      const selectedXAxis = getXAxisOptions().find((item: { key: string }) => item?.key === value);
      data.metadata = { ...(data.metadata || {}), xAxisType: get(selectedXAxis, ["type"], "") };
    }
    dispatch(widgetUpdate(widgetId, { ...data.json }));
  };

  const setYAxis = (key: string, value: any, yIndex: number) => {
    const newYAxis = getYAxis().map((item: YAxisType, index: number) => {
      if (yIndex === index) {
        let extraContentForBarGraph = {};
        if (key === "value" && value === "bar") {
          const alreadyUsedColors: string[] = getYAxis()?.map((y: { display_color: string }) =>
            y?.display_color?.toLowerCase()
          );
          const defaultDisplayColor = colorPalletteShades.filter(c => !alreadyUsedColors.includes(c?.toLowerCase()));
          extraContentForBarGraph = {
            display_color: defaultDisplayColor?.length ? defaultDisplayColor[0] : colorPalletteShades[0]
          };
        } else if (key === "value" && value !== "bar") {
          extraContentForBarGraph = {
            display_color: ""
          };
        }

        return sanitizeObjectCompletely({
          ...item,
          [key]: value,
          ...extraContentForBarGraph
        });
      }
      return item;
    });
    setValues("yAxis", newYAxis);
  };

  const getXAxisOptions = () => {
    if (tableGetLoading || !tableGetData) {
      return [];
    } else {
      const columns = get(tableGetData, ["columns"], []);
      return columns.map((column: any) => {
        return {
          key: get(column, ["id"], ""),
          label: get(column, ["title"], ""),
          type: get(column, ["inputType"], "text")
        };
      });
    }
  };

  const setDebouncedValues = (key: string, value: any) => {
    widget.metadata = { ...(widget.metadata || {}), [key]: value };
    dispatch(widgetUpdate(widgetId, { ...widget.json }));
  };

  const getYAxis = () => {
    return get(widget, ["metadata", "yAxis"], []);
  };

  const isDateTypeColumn = useCallback(
    (key: string) => {
      const columns = tableGetData?.columns ?? [];
      const _column = columns.find((col: any) => col.id === key);
      return _column?.inputType === "date";
    },
    [tableGetData]
  );

  const setSelectedFilters = (value: any) => {
    if (Array.isArray(value)) {
      let newQuery: { [x: string]: string } = {};
      value.forEach(key => {
        let defaultQuery: any = [];
        if (isDateTypeColumn(key)) {
          defaultQuery = {
            $lt: moment().unix().toString(),
            $gt: moment().subtract(30, "days").unix().toString()
          };
        }
        newQuery = {
          ...newQuery,
          [key]: get(widget, ["query", key], defaultQuery)
        };
      });
      dispatch(widgetUpdate(widgetId, { ...widget.json, query: newQuery }));
    }
  };

  const onFilterValueChange = (key: string, value: any, isMetaData?: boolean) => {
    let data: any = widget;
    let newQuery = {
      ...data.query
    };
    let newMetadata = {
      ...data.metadata
    };

    if (isMetaData) {
      data.metadata = {
        ...newMetadata,
        [key]: value
      };
    } else {
      data.query = {
        ...newQuery,
        [key]: value
      };
    }

    dispatch(widgetUpdate(widgetId, data.json));
  };

  const handleTimeFilterChange = (value: any, type?: any, rangeType?: string) => {
    let data: any = widget;

    data.query = {
      ...data.query,
      [type]: value.absolute
    };

    data.metadata = {
      ...(data.metadata || {}),
      range_filter_choice: {
        ...(data.metadata?.range_filter_choice || {}),
        [type]: { type: value.type, relative: value.relative }
      }
    };
    dispatch(widgetUpdate(widgetId, data.json));
  };

  const handleTimeRangeTypeChange = (key: string, value: any) => {
    const data: any = widget;
    const metadata = {
      ...(data.metadata || {}),
      range_filter_choice: {
        ...(data.metadata?.range_filter_choice || {}),
        [key]: value
      }
    };
    dispatch(widgetUpdate(widgetId, { ...data.json, metadata: metadata }));
  };

  const filterOptions = () => {
    if (tableGetLoading || !tableGetData) {
      return [];
    } else {
      const columns = get(tableGetData, ["columns"], []);
      const rows = get(tableGetData, ["rows"], []);
      return columns.reduce((acc: any, item: any) => {
        return {
          ...acc,
          [item.id]: uniq(rows.reduce((acc: string[], row: any) => [...acc, row[item.dataIndex]], []))
        };
      }, {});
    }
  };

  const getWidgetDataValue = (path: string[], defaultValue?: any) => {
    return get(widget, path, defaultValue);
  };

  const getYAxisOptions = () => {
    const selectedXAxis = get(widget, ["metadata", "xAxis"], undefined);
    const selectedYAxis = getYAxis().map((yaxis: YAxisType) => yaxis.key);
    const totalKeysToFilter = [...selectedYAxis, selectedXAxis];
    return getXAxisOptions().filter((option: { key: any }) => !totalKeysToFilter.includes(option.key));
  };

  const addYAxis = () => {
    const newYAxis = [...getYAxis(), { key: "", value: "", label: "" }];
    setValues("yAxis", uniqYAxisWithOrderPreserved(newYAxis));
  };

  const updateMaxRecords = (value: any) => {
    widget.max_records = value;
    dispatch(widgetUpdate(widgetId, { ...widget.json }));
  };

  return (
    <div style={{ width: "100%", display: "flex", justifyContent: "space-between", flexDirection: "column" }}>
      {widget?.isStatFromTable ? (
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
          groupBy={widgetGroupBy}
          setStackBy={(value: string[]) => setDebouncedValues("stackBy", value)}
          stackBy={getWidgetDataValue(["metadata", "stackBy"])}
          onFilterValueChange={onFilterValueChange}
          widgetMetadata={widgetMetadata}
          widgetType={widget.type}
        />
      )}
      <ConfigTableWidgetFilter
        filters={get(widget, ["query"], {})}
        yAxis={getYAxis()}
        columns={getXAxisOptions()}
        setSelectedFilters={setSelectedFilters}
        onFilterValueChange={onFilterValueChange}
        setYUnit={(value: string) => setDebouncedValues("yUnit", value)}
        yUnit={get(widget, ["metadata", "yUnit"], "")}
        isStat={widget?.isStatFromTable}
        groupBy={!!widgetGroupBy}
        filterOptions={filterOptions()}
        maxStackedEntries={getWidgetDataValue(["metadata", "max_stacked_entries"])}
        onMaxStackedEntriesSelection={(value: number) => setDebouncedValues("max_stacked_entries", value)}
        maxRecords={getWidgetDataValue(["metadata", "max_records"])}
        onMaxRecordsSelection={(value: number) => {
          updateMaxRecords(value);
        }}
        dashboardMetadata={dashboardMetadata}
        widgetMetadata={widgetMetadata}
        onTimeFilterValueChange={handleTimeFilterChange}
        onTimeRangeTypeChange={handleTimeRangeTypeChange}
        widgetType={widget.type}
      />
    </div>
  );
};

export default React.memo(TableFilters);
