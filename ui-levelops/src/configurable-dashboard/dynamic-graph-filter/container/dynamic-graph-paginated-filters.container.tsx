import React, { useCallback, useMemo, useState, useContext } from "react";
import { Checkbox, Form } from "antd";
import { filter, get, map } from "lodash";
import { v1 as uuid } from "uuid";
import { DynamicGraphFilter } from "dashboard/constants/applications/levelops.application";
import widgetConstants from "dashboard/constants/widgetConstants";
import { getMaxRangeFromReportType } from "dashboard/graph-filters/components/utils/getMaxRangeFromReportType";
import { AntSelect, AntText, CustomFormItemLabel } from "shared-resources/components";
import { toTitleCase } from "utils/stringUtils";
import DateRangeFilter from "../components/date-range.filter";
import DynamicGraphFilterComponent from "../components/dynamic-graph-filter.component";
import SelectFilter from "../components/select.filter";
import { WidgetTabsContext } from "dashboard/pages/context";
import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { levelOpsFiltersMap } from "./constants";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";
import { CheckboxData } from "model/filters/levelopsFilters";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";

const maxOptions = [
  { label: "10", value: 10 },
  { label: "20", value: 20 },
  { label: "50", value: 50 },
  { label: "100", value: 100 }
];

interface DynamicGraphPaginatedFiltersProps {
  selectedReport: string;
  filters: any;
  metaData: any;
  onMetadataChange?: (value: any, type: any) => void;
  onFilterValueChange: (value: any, type?: any) => void;
  onMaxRecordsSelection?: (value: any) => void;
  maxRecords?: any;
}

const DynamicGraphPaginatedFilters: React.FC<DynamicGraphPaginatedFiltersProps> = (
  props: DynamicGraphPaginatedFiltersProps
) => {
  const { isVisibleOnTab } = useContext(WidgetTabsContext);
  // const isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) = tabType === WIDGET_CONFIGURATION_KEYS.METRICS;
  // const isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) = tabType === WIDGET_CONFIGURATION_KEYS.FILTERS;
  // const isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.AGGREGATIONS) = tabType === WIDGET_CONFIGURATION_KEYS.AGGREGATIONS;

  const { selectedReport } = props;

  const [activePopkey, setActivePopupKey] = useState<string>();

  const getWidgetConstant = useCallback(
    (key: any) => get(widgetConstants, [selectedReport, key], undefined),
    [selectedReport]
  );

  const supportedFilters: Array<DynamicGraphFilter> = useMemo(
    () => getWidgetConstant("supported_filters"),
    [selectedReport]
  );

  const apiCallsArray: any[] = useMemo(() => {
    return map(
      filter(supportedFilters, (sf: DynamicGraphFilter) => sf.filterType.includes("api")),
      (sf: DynamicGraphFilter) => ({
        id: `${sf.uri}_${uuid()}`,
        apiName: sf.uri,
        apiMethod: "list",
        filters: { page_size: 10 }
      })
    );
  }, [supportedFilters]);

  const getSpecificFilter = useCallback(
    (uri: string, label: string) => supportedFilters.find(sf => sf.uri === uri && sf.label === label),
    [supportedFilters]
  );

  const getSpecificApi = useCallback(
    (uri: string) => apiCallsArray.find(api => api.apiName === uri),
    [supportedFilters]
  );

  const maxRange = useMemo(() => getMaxRangeFromReportType(selectedReport), [selectedReport]);

  const mappedFilters = useMemo(() => {
    if (Array.isArray(supportedFilters)) {
      return supportedFilters.map((filter: DynamicGraphFilter) => {
        return {
          ...filter,
          selectedValue: get(props.filters, [filter.filterField], undefined)
        } as DynamicGraphFilter;
      });
    }
    return [];
  }, [props.filters, supportedFilters]);

  const renderFilters = useMemo(
    () => (position: "left" | "right") => {
      return (
        mappedFilters
          .filter((filter: DynamicGraphFilter) => filter.position === position)
          // eslint-disable-next-line array-callback-return
          .map((filter: DynamicGraphFilter, index: number) => {
            const componentKey = `${filter.filterField}-${index}`;
            switch (filter.filterType) {
              case "apiSelect":
              case "apiMultiSelect":
                return (
                  <DynamicGraphFilterComponent
                    key={componentKey}
                    api={getSpecificApi(filter.uri as string)}
                    onFilterValueChange={props.onFilterValueChange}
                    filters={props.filters}
                    activePopkey={activePopkey}
                    handleActivePopkey={key => setActivePopupKey(key)}
                    supportedFilter={getSpecificFilter(filter.uri as any, filter.label) as any}
                  />
                );
              case "select":
              case "multiSelect":
                return (
                  <SelectFilter
                    key={componentKey}
                    filter={filter}
                    onChange={(value: any) =>
                      props.onFilterValueChange(
                        filter.filterField === "completed" ? value === "true" : value,
                        filter.filterField
                      )
                    }
                  />
                );
              case "dateRange":
                return (
                  <DateRangeFilter
                    key={componentKey}
                    maxRange={maxRange}
                    filter={filter}
                    onChange={(value: any) => props.onFilterValueChange(value, filter.filterField)}
                  />
                );
            }
          })
      );
    },
    [mappedFilters, supportedFilters, activePopkey]
  );

  const hasStacks = useMemo(() => (getWidgetConstant("stack_filters") || []).length > 0, [selectedReport]);

  const hasAcross = useMemo(
    () => (getWidgetConstant("xaxis") || false) && (getWidgetConstant("across") || []).length > 0,
    [selectedReport]
  );

  const showMax = useMemo(() => getWidgetConstant("show_max") || false, [selectedReport]);

  const getMappedLabel = useCallback((label: string) => toTitleCase(get(levelOpsFiltersMap, [label], label)), []);

  const getMappedOptions = useCallback(
    (options: Array<any>) => options.map(option => ({ label: getMappedLabel(option), value: option })),
    []
  );

  return (
    <div className="flex justify-space-between w-100">
      <div className="w-100">
        {isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.SETTINGS) && getWidgetConstant("chart_type") === ChartType.BAR && (
          <Form.Item label={<CustomFormItemLabel label={ShowValueOnBarConfig.label} />}>
            <Checkbox
              checked={props.metaData[ShowValueOnBarConfig.beKey]}
              onChange={e => props.onMetadataChange?.(e.target.checked, ShowValueOnBarConfig.beKey)}>
              <AntText className="mr-1">
                {(ShowValueOnBarConfig?.filterMetaData as CheckboxData)?.checkboxLabel}{" "}
              </AntText>
            </Checkbox>
          </Form.Item>
        )}
        {isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) && renderFilters("left")}
        {isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.METRICS) && hasStacks && (
          <Form.Item key={`stack-filter`} label={"Stacks"}>
            <AntSelect
              showArrow={true}
              value={get(props.filters, ["stacks"], [])}
              options={getMappedOptions(get(widgetConstants, [props.selectedReport, "stack_filters"], []))}
              // mode={"multiple"}
              allowClear={true}
              onChange={(value: any, options: any) => props.onFilterValueChange(value ? [value] : [], "stacks")}
            />
          </Form.Item>
        )}
        {isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.AGGREGATIONS) && hasAcross && (
          <Form.Item key={`across`} required label={"X-Axis"}>
            <AntSelect
              showArrow={true}
              value={get(props.filters, ["across"], [])}
              options={getMappedOptions(get(widgetConstants, [props.selectedReport, "across"], []))}
              mode={"default"}
              onChange={(value: any, options: any) => props.onFilterValueChange(value, "across")}
            />
          </Form.Item>
        )}
        {showMax && isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.AGGREGATIONS) && (
          <Form.Item key={`max-entries`} label={"Max X-Axis Entries"}>
            <AntSelect
              options={maxOptions}
              defaultValue={props.maxRecords}
              onSelect={(value: any) => props.onMaxRecordsSelection?.(value)}
            />
          </Form.Item>
        )}
        {isVisibleOnTab(WIDGET_CONFIGURATION_KEYS.FILTERS) && renderFilters("right")}
      </div>
    </div>
  );
};

export default React.memo(DynamicGraphPaginatedFilters);
