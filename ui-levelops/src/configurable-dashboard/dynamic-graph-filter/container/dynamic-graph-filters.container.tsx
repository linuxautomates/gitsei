import React, { useEffect, useRef, useState } from "react";
import { get, isEqual } from "lodash";
import widgetConstants from "dashboard/constants/widgetConstants";
import { DynamicGraphFilter } from "dashboard/constants/applications/levelops.application";
import { APISelectFilter } from "../components/api-select-filter";
import { SelectFilter } from "../components/select.filter";
import { DateRangeFilter } from "../components/date-range.filter";
import { buildDataForFilterApiCalling } from "./helper";
import Spin from "antd/lib/spin";
import { useDispatch, useSelector } from "react-redux";
import { Form } from "antd";
import { AntSelect } from "shared-resources/components";
import { v1 as uuid } from "uuid";
import { toTitleCase } from "utils/stringUtils";
import { getIdsMap } from "reduxConfigs/actions/restapi/genericIdsMap.actions";
import { formClear } from "reduxConfigs/actions/formActions";
import { getMaxRangeFromReportType } from "dashboard/graph-filters/components/utils/getMaxRangeFromReportType";
import { levelOpsFiltersMap } from "./constants";

const maxOptions = [
  { label: "10", value: 10 },
  { label: "20", value: 20 },
  { label: "50", value: 50 },
  { label: "100", value: 100 }
];

interface DynamicGraphFiltersContainerProps {
  selectedReport: string;
  filters: any;
  onFilterValueChange: (value: any, type?: any) => void;
  onMaxRecordsSelection?: (value: any) => void;
  maxRecords?: any;
}

const DynamicGraphFiltersContainer: React.FC<DynamicGraphFiltersContainerProps> = (
  props: DynamicGraphFiltersContainerProps
) => {
  const [apiSelectState, setApiSelectState] = useState<{ [x: string]: any }>({});
  const [filtersApiLoading, setFiltersApiLoading] = useState<boolean>(false);
  const [filtersApiData, setFiltersApiData] = useState<any>({});
  const formName = useRef("");

  const { selectedReport } = props;
  const maxRange = getMaxRangeFromReportType(selectedReport);
  const reportRef = useRef<string>(selectedReport);

  const apisForCalling = buildDataForFilterApiCalling(props.selectedReport, props.filters);

  const dispatch = useDispatch();
  const formState = useSelector((state: any) => state.formReducer);

  useEffect(() => {
    if (!isEqual(reportRef.current, selectedReport)) {
      reportRef.current = selectedReport;
      setApiSelectState({});
    }
  }, [selectedReport]);

  useEffect(() => {
    if (Object.keys(apisForCalling).length && !filtersApiLoading && !formName.current?.length) {
      const name = `graph_filters_${uuid()}`;
      dispatch(getIdsMap(name, apisForCalling as any));
      setFiltersApiLoading(true);
      formName.current = name;

      return () => {
        dispatch(formClear(name));
      };
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (filtersApiLoading) {
      const data = formState[formName.current] || {};
      setFiltersApiData(data);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [formState]);

  useEffect(() => {
    if (Object.keys(filtersApiData).length) {
      let selectedValues: any = {};
      Object.keys(filtersApiData).forEach(key => {
        const data = get(filtersApiData, [key], []);
        if (key === "questionnaire_template_ids") {
          selectedValues = {
            ...selectedValues,
            questionnaire_template_id: data.map((res: any) => ({ key: res.id, label: res.name }))
          };
        }
        if (key === "product_ids") {
          selectedValues = {
            ...selectedValues,
            product_ids: data.map((res: any) => ({ key: res.id, label: res.name }))
          };
        }
        if (key === "tag_ids") {
          selectedValues = {
            ...selectedValues,
            tags: data.map((res: any) => ({ key: res.id, label: res.name }))
          };
        }

        if (key === "user_ids") {
          selectedValues = {
            ...selectedValues,
            assignees: data.map((res: any) => ({ key: res.id, label: res.email }))
          };
        }
        if (key === "state_ids") {
          selectedValues = {
            ...selectedValues,
            states: data.map((res: any) => ({ key: res.id, label: res.name }))
          };
        }
      });
      setApiSelectState(selectedValues);
      setFiltersApiLoading(false);
    }
  }, [filtersApiData]);

  const onRestApiChange = (value: any, type: string) => {
    setApiSelectState((prev: any) => ({ ...prev, [type]: value }));
    if (typeof value === "object" && !Array.isArray(value)) {
      const key = get(value, "key", undefined);
      if (type === "reporters") {
        props.onFilterValueChange(value, type);
      } else {
        props.onFilterValueChange(key, type);
      }
    } else if (typeof value === "string") {
      props.onFilterValueChange(value, type);
    } else if (Array.isArray(value)) {
      let values: any[] = [];
      value.forEach(val => {
        if (typeof val === "object" && !Array.isArray(val)) {
          const key = get(val, "key", undefined);
          if (type === "reporters") {
            values.push(val);
          } else {
            values.push(key);
          }
        } else if (typeof val === "string") {
          values.push(val);
        }
      });
      props.onFilterValueChange(values, type);
    }
  };

  const getWidgetConstant = (key: string) => {
    return get(widgetConstants, [selectedReport, key], undefined);
  };

  const mappedFilters = () => {
    const supportedFilters = getWidgetConstant("supported_filters");
    if (Array.isArray(supportedFilters)) {
      return supportedFilters.map((filter: DynamicGraphFilter) => {
        return {
          ...filter,
          selectedValue: get(props.filters, [filter.filterField], undefined)
        } as DynamicGraphFilter;
      });
    }
    return [];
  };

  const getRestApiSelectValue = (field: string) => {
    if (field === "reporters") {
      return props.filters[field];
    }
    return apiSelectState[field];
  };

  const renderFilters = (type: string) => {
    const filters = mappedFilters();
    if (filters.length) {
      return (
        filters
          .filter((filter: DynamicGraphFilter) => filter.position === type)
          // eslint-disable-next-line array-callback-return
          .map((filter: DynamicGraphFilter) => {
            switch (filter.filterType) {
              case "apiSelect":
              case "apiMultiSelect":
                return (
                  <APISelectFilter
                    filter={filter}
                    value={getRestApiSelectValue(filter.filterField)}
                    onChange={(value: any) => onRestApiChange(value, filter.filterField)}
                  />
                );
              case "select":
              case "multiSelect":
                return (
                  <SelectFilter
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
                    maxRange={maxRange}
                    filter={filter}
                    onChange={(value: any) => props.onFilterValueChange(value, filter.filterField)}
                  />
                );
            }
          })
      );
    }
  };

  if (filtersApiLoading) {
    return <Spin className="centered" />;
  }

  const hasStacks = get(widgetConstants, [props.selectedReport, "stack_filters"], []).length > 0;

  const hasAcross =
    get(widgetConstants, [props.selectedReport, "xaxis"], false) &&
    get(widgetConstants, [props.selectedReport, "across"], []).length > 0;

  const showMax = get(widgetConstants, [props.selectedReport, "show_max"], false);

  const getMappedLabel = (label: string) => {
    return toTitleCase(get(levelOpsFiltersMap, [label], label));
  };

  const getMappedOptions = (options: Array<any>) => {
    return options.map(option => ({ label: getMappedLabel(option), value: option }));
  };

  return (
    <div
      style={{
        display: "flex",
        width: "100%",
        marginTop: "0.6rem",
        justifyContent: "space-between",
        height: "14.725rem"
      }}>
      <div style={{ width: "48%", overflowY: "scroll" }}>
        {renderFilters("left")}
        {hasStacks && (
          <Form.Item key={uuid()} label={"Stacks"}>
            <AntSelect
              showArrow={true}
              value={get(props.filters, ["stacks"], [])}
              options={getMappedOptions(get(widgetConstants, [props.selectedReport, "stack_filters"], []))}
              // mode={"multiple"}
              allowClear={true}
              onChange={(value: any, options: any) => props.onFilterValueChange([value], "stacks")}
            />
          </Form.Item>
        )}
      </div>
      <div style={{ width: "48%", overflowY: "scroll" }}>
        {hasAcross && (
          <Form.Item key={uuid()} required label={"X-Axis"}>
            <AntSelect
              showArrow={true}
              value={get(props.filters, ["across"], [])}
              options={getMappedOptions(get(widgetConstants, [props.selectedReport, "across"], []))}
              mode={"default"}
              allowClear={true}
              onChange={(value: any, options: any) => props.onFilterValueChange(value, "across")}
            />
          </Form.Item>
        )}
        {showMax && (
          <Form.Item label={"Max X-Axis Entries"}>
            <AntSelect
              options={maxOptions}
              defaultValue={props.maxRecords}
              onSelect={(value: any) => props.onMaxRecordsSelection?.(value)}
            />
          </Form.Item>
        )}
        {renderFilters("right")}
      </div>
    </div>
  );
};

export default DynamicGraphFiltersContainer;
