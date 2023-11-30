import React, { useCallback, useEffect, useMemo } from "react";
import _, { cloneDeep, get, isArray, isEmpty, uniq } from "lodash";
import { useDispatch } from "react-redux";
import { RestDashboard, RestWidget } from "classes/RestDashboards";
import { useTables } from "custom-hooks";
import { widgetUpdate } from "reduxConfigs/actions/restapi/widgetActions";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { getDashboard } from "reduxConfigs/selectors/dashboardSelector";
import moment from "moment";
import { Checkbox, Form, Select } from "antd";
import { TimeRangeAbsoluteRelativeWrapperComponent } from "dashboard/graph-filters/components/time-range-abs-rel-wrapper.component";
import { AntSelect, CustomSelect } from "shared-resources/components";
import { IGNORE_TABLE_FILTERS, TableFiltersBEKeys } from "./constant";
import { unsetKeysFromObject } from "utils/commonUtils";
import "./propeloTableReportFilters.styles.scss";

interface PropeloTableFiltersProps {
  widgetId: string;
  dashboardId: string;
}

const { Option } = Select;
const PropeloTableFilters: React.FC<PropeloTableFiltersProps> = ({ dashboardId, widgetId }) => {
  const dispatch = useDispatch();

  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });
  const dashboard: RestDashboard = useParamSelector(getDashboard, { dashboard_id: dashboardId });
  const selectedTableId = get(widget, ["metadata", "tableId"], "");
  const [tableGetLoading, tableGetData] = useTables("get", selectedTableId, true, undefined, [selectedTableId]);
  const dashboardMetadata = get(dashboard, "_metadata", {});
  const widgetMetadata = useMemo(() => get(widget, "metadata", {}), [widget]);

  const getXAxisOptions = useMemo(() => {
    if (tableGetLoading || !tableGetData) {
      return [];
    } else {
      const columns = get(tableGetData, ["columns"], []);
      return columns.map((column: any) => {
        return {
          key: get(column, ["id"], ""),
          label: get(column, ["title"], ""),
          column_key: get(column, ["key"], ""),
          type: get(column, ["inputType"], "text")
        };
      });
    }
  }, [tableGetData, tableGetLoading]);

  const hasOUIdColumn = useMemo(
    () => !!getXAxisOptions?.find((option: { column_key: string }) => option?.column_key === "ou_id"),
    [getXAxisOptions, tableGetLoading]
  );

  useEffect(() => {
    onFilterValueChange("ou_id_column_exists", hasOUIdColumn, true);
  }, [hasOUIdColumn]);

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
      let newQuery: Record<string, string> = IGNORE_TABLE_FILTERS.reduce((acc, next: string) => {
        return {
          ...(acc ?? {}),
          [next]: get(widget?.query, [next])
        };
      }, {});

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
    let data: RestWidget = widget;
    let newQuery = cloneDeep(widget?.query ?? {});
    let newMetadata = cloneDeep(widget?.metadata ?? {});
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

    dispatch(widgetUpdate(widgetId, data?.json));
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

  const filterOptions = useMemo(() => {
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
  }, [tableGetLoading, tableGetData]);

  const filters = useMemo(() => get(widget, ["query"], {}), [widget]);

  const getFilterLabel = (key: any) => {
    const filter = getXAxisOptions?.find((option: { key: string }) => option?.key === key);
    return filter ? filter.label : "";
  };

  const getFilterType = (key: any) => {
    const filter = getXAxisOptions?.find((option: { key: string }) => option?.key === key);
    return filter ? filter.type : "text";
  };

  const columns: string[] = useMemo(() => get(filters, [TableFiltersBEKeys.COLUMNS], []), [filters]);

  const handleColumnSelectionChange = (v: string[]) => {
    if (v.includes("all") && v.length > 1) v = v.filter(o => o !== "all");
    if (isEmpty(v)) v = ["all"];
    onFilterValueChange(TableFiltersBEKeys.COLUMNS, v);
  };

  const validationStatus = useMemo(() => {
    if (!tableGetLoading && tableGetData) {
      const showOUBasedData = get(filters, [TableFiltersBEKeys.SHOW_VALUES_OF_SELECTED_OU], false);
      if (showOUBasedData && hasOUIdColumn === false) return "error";
    }
    return "";
  }, [hasOUIdColumn, filters, tableGetData, tableGetLoading]);

  const help = useMemo(() => {
    if (validationStatus === "error")
      return "You have selected an option that requires a special column called OU_ID to be present. Please update your table";
  }, [validationStatus]);

  const sanitizedFilters = useMemo(() => {
    let nFilters = cloneDeep(filters);
    return unsetKeysFromObject(["across", ...IGNORE_TABLE_FILTERS], nFilters);
  }, [filters]);

  return (
    <div className="w-100 flex justify-space-between direction-column">
      <Form.Item label="" className="w-100" validateStatus={validationStatus} help={help}>
        <Checkbox
          disabled={!selectedTableId}
          onChange={e => onFilterValueChange(TableFiltersBEKeys.SHOW_VALUES_OF_SELECTED_OU, e.target.checked)}
          checked={get(filters, [TableFiltersBEKeys.SHOW_VALUES_OF_SELECTED_OU], false)}>
          Show values for selected collection and children
        </Checkbox>
      </Form.Item>
      <Form.Item label="Columns" className="w-100">
        <AntSelect
          mode="multiple"
          value={columns}
          onChange={handleColumnSelectionChange}
          allowClear={true}
          className="table-column-selection">
          {[
            <Option key="all" disabled={columns.length > 0 && !columns.includes("all")}>
              All
            </Option>,
            ...getXAxisOptions?.map((option: { label: string; key: string }) => (
              <Option key={option.key}>{option.label}</Option>
            ))
          ]}
        </AntSelect>
      </Form.Item>
      <div className="w-100">
        <Form.Item label="Filters" className="w-100">
          <CustomSelect
            valueKey="key"
            labelKey="label"
            labelCase="none"
            mode="multiple"
            createOption={false}
            onChange={setSelectedFilters}
            value={Object.keys(sanitizedFilters)}
            options={getXAxisOptions}
          />
        </Form.Item>
        <div
          className={"flex direction-column"}
          style={{ maxHeight: "10rem", overflowY: "scroll", alignItems: "center" }}>
          {Object.keys(sanitizedFilters).map(filter => {
            const type = getFilterType(filter);
            const value = get(filters, [filter], "");
            if (type === "date") {
              return (
                <TimeRangeAbsoluteRelativeWrapperComponent
                  key={filter}
                  label={getFilterLabel(filter)}
                  filterKey={filter}
                  metaData={widgetMetadata}
                  filters={filters}
                  onFilterValueChange={(value: any, type?: any, rangeType?: string) =>
                    handleTimeFilterChange(value, type, rangeType)
                  }
                  onMetadataChange={(value, type) => onFilterValueChange(type, value, true)}
                  onTypeChange={(key: string, value: any) => handleTimeRangeTypeChange(key, value)}
                  dashboardMetaData={dashboardMetadata}
                  className="table-time-filter"
                />
              );
            }

            return (
              <Form.Item key={`filter-${filter}`} label={getFilterLabel(filter)} style={{ width: "90%" }}>
                <CustomSelect
                  options={filterOptions[filter]}
                  mode="multiple"
                  value={isArray(value) ? value : []}
                  onChange={(value: any) => onFilterValueChange(filter, value)}
                  createOption={false}
                  labelCase="none"
                />
              </Form.Item>
            );
          })}
        </div>
      </div>
    </div>
  );
};

export default PropeloTableFilters;
