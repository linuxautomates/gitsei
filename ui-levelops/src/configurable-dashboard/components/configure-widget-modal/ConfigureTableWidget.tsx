import { Col, Form, Row, Spin } from "antd";
import { useTables } from "custom-hooks";
import { cloneDeep, debounce, get, uniq } from "lodash";
import React, { useCallback, useState } from "react";
import { CustomSelect } from "shared-resources/components";
import ConfigTableWidgetAxis from "./config-table-widget-modal/ConfigTableWidgetAxis";
import ConfigTableWidgetFilter from "./config-table-widget-modal/ConfigTableWidgetFilter";
import ConfigTableWidgetStat from "./config-table-widget-modal/ConfigTableWidgetStat";
import { RestWidget } from "classes/RestDashboards";
import moment from "moment";

interface ConfigureTableWidgetProps {
  widgetData: any;
  updateWidget: (data: any) => void;
  isStat: boolean;
}

const ConfigureTableWidget: React.FC<ConfigureTableWidgetProps> = (props: ConfigureTableWidgetProps) => {
  const [widgetData, setWidgetData] = useState<any>(cloneDeep(props.widgetData) || {});
  const tableId = get(widgetData, ["metadata", "tableId"], "");

  const [tableListLoading, tableListData] = useTables("list");
  const [tableGetLoading, tableGetData] = useTables("get", tableId, true, undefined, [tableId]);

  const debounceUpdate = debounce(props.updateWidget, 100);

  const loading = () => {
    return (
      <div style={{ display: "flex", justifyContent: "center", alignItems: "center" }}>
        <Spin />
      </div>
    );
  };

  const getGroupBy = () => {
    return getWidgetDataValue(["metadata", "groupBy"], false);
  };

  const getWidgetDataValue = (path: string[], defaultValue?: any) => {
    return get(widgetData, path, defaultValue);
  };

  const getWidgetData = () => new RestWidget(widgetData.json);

  const setTable = (value: any) => {
    let data = getWidgetData();
    data.metadata = {
      ...(data.metadata || {}),
      tableId: value,
      xAxis: "",
      yAxis: props.isStat ? [{ key: "" }] : [],
      yUnit: "",
      groupBy: false
    };
    data.query = {};
    setWidgetData(data);
    props.updateWidget(data);
  };

  const setDebouncedValues = (key: string, value: any) => {
    let data = getWidgetData();
    setWidgetData(data);
    data.metadata = { ...(data.metadata || {}), [key]: value };
    debounceUpdate(data);
  };

  const setValues = (key: string, value: any) => {
    let data = getWidgetData();
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
    setWidgetData(data);
    props.updateWidget(data);
  };

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

  const getYAxisOptions = () => {
    const selectedXAxis = get(widgetData, ["metadata", "xAxis"], undefined);
    return getXAxisOptions().filter((option: { key: any }) => option.key !== selectedXAxis);
  };

  const getYAxis = () => {
    return get(widgetData, ["metadata", "yAxis"], []);
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
          [key]: get(widgetData, ["query", key], defaultQuery)
        };
      });
      updateFilters(newQuery);
    }
  };

  const updateFilters = (newQuery: any) => {
    let data = getWidgetData();
    data.query = newQuery;
    setWidgetData(data);
    props.updateWidget(data);
  };

  const onFilterValueChange = (key: string, value: any, isMetaData?: boolean) => {
    let data = getWidgetData();
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

    props.updateWidget(data);
  };

  const updateMaxRecords = (value: any) => {
    let data = getWidgetData();
    data.max_records = value;
    setWidgetData(data);
    props.updateWidget(data);
  };

  return (
    <div style={{ maxHeight: "400px" }}>
      <Row type={"flex"}>
        <Col span={24}>
          <Form layout="vertical" style={{ padding: "10px" }}>
            <Form.Item label="Tables" required>
              <CustomSelect
                valueKey="id"
                labelKey="name"
                labelCase="none"
                mode="default"
                createOption={false}
                onChange={setTable}
                value={get(widgetData, ["metadata", "tableId"], undefined)}
                options={tableListData || []}
                loading={tableListLoading}
              />
            </Form.Item>
            {tableGetLoading && loading()}
            {!tableGetLoading && tableGetData && (
              <div className="configure-widget-filters">
                <div className="configure-widget-filters__half">
                  {props.isStat ? (
                    <ConfigTableWidgetStat options={getXAxisOptions()} value={getYAxis()[0] || {}} setAxis={setYAxis} />
                  ) : (
                    <ConfigTableWidgetAxis
                      setValues={setValues}
                      xAxisValue={get(widgetData, ["metadata", "xAxis"], undefined)}
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
                <div className="configure-widget-filters__half">
                  <ConfigTableWidgetFilter
                    filters={get(widgetData, ["query"], {})}
                    yAxis={getYAxis()}
                    columns={getXAxisOptions()}
                    setSelectedFilters={setSelectedFilters}
                    onFilterValueChange={onFilterValueChange}
                    setYUnit={(value: string) => setDebouncedValues("yUnit", value)}
                    yUnit={get(widgetData, ["metadata", "yUnit"], "")}
                    isStat={props.isStat}
                    groupBy={!!getGroupBy()}
                    filterOptions={filterOptions()}
                    maxStackedEntries={getWidgetDataValue(["metadata", "max_stacked_entries"])}
                    onMaxStackedEntriesSelection={(value: number) => setDebouncedValues("max_stacked_entries", value)}
                    maxRecords={getWidgetDataValue(["metadata", "max_records"])}
                    onMaxRecordsSelection={(value: number) => {
                      updateMaxRecords(value);
                    }}
                  />
                </div>
              </div>
            )}
          </Form>
        </Col>
      </Row>
    </div>
  );
};

export default ConfigureTableWidget;
