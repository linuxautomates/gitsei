import { Form, Input } from "antd";
import { DEFAULT_MAX_RECORDS, DEFAULT_MAX_STACKED_ENTRIES } from "dashboard/constants/constants";
import { get, isArray } from "lodash";
import React from "react";
import { CustomSelect, AntFormItem, AntSelect } from "shared-resources/components";
import { TimeRangeAbsoluteRelativeWrapperComponent } from "../../../../dashboard/graph-filters/components/time-range-abs-rel-wrapper.component";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";

interface ConfigTableWidgetFilterProps {
  filters: any;
  yAxis: Array<{ label: string; value: string; key: string }>;
  columns: Array<any>;
  setSelectedFilters: (value: any) => void;
  onFilterValueChange: (key: string, value: any, isMetaData?: boolean) => void;
  setYUnit: (value: string) => void;
  yUnit: string;
  isStat: boolean;
  groupBy: boolean;
  filterOptions: any;
  maxStackedEntries: number;
  onMaxStackedEntriesSelection: (...args: any) => any;
  maxRecords?: any;
  onMaxRecordsSelection?: (value: any) => void;
  dashboardMetadata?: any;
  widgetMetadata?: any;
  onTimeFilterValueChange?: (value: any, type?: any, rangeType?: string) => void;
  onTimeRangeTypeChange?: (key: string, value: any) => void;
  widgetType?: string;
}

const ConfigTableWidgetFilter: React.FC<ConfigTableWidgetFilterProps> = props => {
  const isMaxStackEntriesProvided = !!getWidgetConstant(props.widgetType, ["maxStackEntries"], false);

  const getFilterLabel = (key: any) => {
    const filter = props.columns.find(option => option.key === key);
    return filter ? filter.label : "";
  };

  const getFilterType = (key: any) => {
    const filter = props.columns.find(option => option.key === key);
    return filter ? filter.type : "text";
  };

  return (
    <div style={{ width: "100%" }}>
      {!props.groupBy && (
        <Form.Item label="Unit" style={{ width: "100%", marginTop: "1.5rem" }}>
          <Input defaultValue={props.yUnit} onChange={e => props.setYUnit(e.target.value)} />
        </Form.Item>
      )}
      <Form.Item label="Filters" style={{ width: "100%", marginTop: props.groupBy ? "1.5rem" : "0" }}>
        <CustomSelect
          valueKey="key"
          labelKey="label"
          labelCase="none"
          mode="multiple"
          createOption={false}
          onChange={props.setSelectedFilters}
          value={Object.keys(props.filters)}
          options={props.columns}
        />
      </Form.Item>
      <div
        className={"flex direction-column"}
        style={{ maxHeight: "10rem", overflowY: "scroll", alignItems: "center" }}>
        {Object.keys(props.filters).map(filter => {
          const type = getFilterType(filter);
          const value = get(props.filters, [filter], "");
          if (type === "date") {
            return (
              <TimeRangeAbsoluteRelativeWrapperComponent
                key={filter}
                label={getFilterLabel(filter)}
                filterKey={filter}
                metaData={props.widgetMetadata}
                filters={props.filters}
                onFilterValueChange={(value: any, type?: any, rangeType?: string) =>
                  props.onTimeFilterValueChange?.(value, type, rangeType)
                }
                onMetadataChange={(value, type) => props.onFilterValueChange(type, value, true)}
                onTypeChange={(key: string, value: any) => props.onTimeRangeTypeChange?.(key, value)}
                dashboardMetaData={props.dashboardMetadata}
                className="table-time-filter"
              />
            );
          }

          return (
            <Form.Item key={`filter-${filter}`} label={getFilterLabel(filter)} style={{ width: "90%" }}>
              <CustomSelect
                options={props.filterOptions[filter]}
                mode="multiple"
                value={isArray(value) ? value : []}
                onChange={(value: any) => props.onFilterValueChange(filter, value)}
                createOption={false}
                labelCase="none"
              />
            </Form.Item>
          );
        })}
      </div>
      {!props.isStat && (
        <AntFormItem label={"Max X-Axis Entries"}>
          <AntSelect
            options={[
              { label: "10", value: 10 },
              { label: "20", value: 20 },
              { label: "50", value: 50 },
              { label: "100", value: 100 }
            ]}
            defaultValue={props.maxRecords || DEFAULT_MAX_RECORDS}
            onSelect={(value: any) => props.onMaxRecordsSelection?.(value)}
          />
        </AntFormItem>
      )}
      {props.groupBy && !isMaxStackEntriesProvided && (
        <AntFormItem label="Max Stacked Entries">
          <AntSelect
            options={[
              { label: "5", value: 5 },
              { label: "10", value: 10 },
              { label: "15", value: 15 },
              { label: "20", value: 20 }
            ]}
            defaultValue={props.maxStackedEntries || DEFAULT_MAX_STACKED_ENTRIES}
            onSelect={(value: any) => props.onMaxStackedEntriesSelection?.(value)}
          />
        </AntFormItem>
      )}
    </div>
  );
};

export default ConfigTableWidgetFilter;
