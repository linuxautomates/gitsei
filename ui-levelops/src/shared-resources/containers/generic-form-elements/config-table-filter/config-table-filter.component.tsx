import React, { useEffect, useState } from "react";
import { Row, Col, Select, DatePicker, Button } from "antd";
import { useDispatch, useSelector } from "react-redux";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { configTableState } from "reduxConfigs/selectors/restapiSelector";
import { get, debounce } from "lodash";
// @ts-ignore
import { v1 as uuid } from "uuid";
import { AutosuggestContainer } from "../../autosuggest/autosuggest.container";
import moment from "moment";

const { Option } = Select;

interface ConfigTableFilterComponentProps {
  onChange: (value: any) => void;
  layout?: any;
  value?: any;
  key: string;
  suggestions?: Array<any>;
  index?: number;
  field_values?: any;
}

export const ConfigTableFilterComponent = (props: ConfigTableFilterComponentProps) => {
  const dispatch = useDispatch();
  const key = props.key || "0";
  const { onChange, value, suggestions, field_values } = props;

  const rest_api = useSelector(state =>
    configTableState(state, key, get(field_values, ["table_id", "value", 0, "key"], "0"))
  );
  const [tableSelect, setTableSelect] = useState<any>(undefined);
  const [filters, setFilters] = useState<Array<any>>([]);
  const [columns, setColumns] = useState<any>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const propFilters = get(value, ["filters"], []);
    const propTableId = get(field_values, ["table_id", "value", 0, "key"], undefined);
    if (!propTableId && propFilters.length > 0) {
      onChange({
        ...value,
        filters: []
      });
      setFilters([]);
    } else {
      setFilters(propFilters);
    }

    //setTableSelect({ key: propTableId });
    // if (propTableId !== undefined) {
    //   setLoading(true);
    //   dispatch(actionTypes.genericGet("config_tables", `${propTableId}?expand=schema`));
    // }
    return () => {
      // @ts-ignore
      //dispatch(actionTypes.restapiClear("config-tables", "get", `${propTableId}?expand=schema`));
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const propTableId = get(field_values, ["table_id", "value", 0, "key"], undefined);
    if (propTableId !== undefined && (!tableSelect || propTableId !== tableSelect.key)) {
      if (tableSelect !== undefined) {
        // table changed, not the initial load
        setColumns([]);
        setFilters([]);
        onChange({
          ...value,
          filters: []
        });
        // @ts-ignore
        dispatch(actionTypes.restapiClear("config-tables", "get", `${tableSelect.key}?expand=schema`));
      }
      setTableSelect({ key: propTableId });
      setLoading(true);
      dispatch(actionTypes.genericGet("config_tables", `${propTableId}?expand=schema`));
    }
  }, [field_values]); // eslint-disable-line react-hooks/exhaustive-deps

  const updateColumns = (dataColumns: any) => {
    const mappedColumns = Object.keys(dataColumns).reduce((acc: any, obj: any) => {
      acc.push(dataColumns[obj]);
      return acc;
    }, []);
    setColumns(mappedColumns);
    return mappedColumns;
  };

  useEffect(() => {
    const propTableId = get(field_values, ["table_id", "value", 0, "key"], undefined);
    if (loading && propTableId) {
      // do the get for the schema here
      const propFilters = get(value, ["filters"], []);
      if (!rest_api.get.loading && !rest_api.get.error && rest_api.get.data !== undefined) {
        const data = get(rest_api, ["get", "data"], {});
        const dataColumns = get(data, ["schema", "columns"], {});
        const mappedColumns = updateColumns(dataColumns);
        setFilters(
          propFilters
            .filter((fill: any) => fill.key)
            .map((fill: any) => ({
              ...fill,
              label: (mappedColumns.find((col: { id: string }) => col.id === fill.key) || {}).display_name || fill.key
            }))
        );
        setLoading(false);
        setTableSelect({ key: data.id, label: data.name });
      } else if (!rest_api.get.loading && rest_api.get.error) {
        console.log("rest api error");
        setLoading(false);
        setTableSelect(undefined);
        setFilters([]);
      }
    }
  }, [rest_api]); // eslint-disable-line react-hooks/exhaustive-deps

  const getValue = (index: number) => {
    const selectedFilter = filters[index];
    if (selectedFilter.key === undefined) {
      return null;
    }
    const selectedColumn = columns.find((col: { id: any }) => col.id === selectedFilter.key);
    if (!selectedColumn) {
      return null;
    }
    switch (selectedColumn.type) {
      case "preset":
        let options = (selectedColumn.options || []).map((opt: string) => ({ key: opt, label: opt }));
        // @ts-ignore
        options.push(...(suggestions || []).map(suggestion => ({ key: suggestion.key, label: suggestion.node })));
        return (
          <Select value={selectedFilter.value} onChange={(val: string) => onFilterValueChange(index, val)}>
            {options.map((opt: any) => (
              <Option key={opt.key} value={opt.key}>
                {opt.label}
              </Option>
            ))}
          </Select>
        );
      case "date":
        // @ts-ignore
        return (
          <DatePicker
            value={moment.unix(selectedFilter.value)}
            // @ts-ignore
            onChange={(date: any) => onFilterValueChange(index, Math.ceil(date!.valueOf() / 1000))}
          />
        );
      case "text":
      default:
        return (
          <div style={{ marginRight: "5px" }}>
            <AutosuggestContainer
              value={selectedFilter.value}
              suggestions={suggestions}
              text_type={"text"}
              onChange={(value: any) => debouncedFilterValueChange(index, value, false)}
              onBlur={() => updateFilterValues()}
            />
          </div>
        );
      // return (
      //   <Input
      //     defaultValue={selectedFilter.value}
      //     key={selectedFilter.id}
      //     onChange={(e: any) => debouncedFilterValueChange(index, e.currentTarget.value)}
      //   />
      // );
    }
  };

  const generateSchema = (cols: any, tableId: string) => {
    let fields = Object.keys(cols).reduce((acc: any, obj: string) => {
      acc[obj] = {
        key: cols[obj].key,
        content_type: "string",
        value_type: "string"
      };
      return acc;
    }, {});
    fields.id = {
      key: "id",
      content_type: "id:config_row",
      value_type: "string"
    };
    return {
      content_type: `config-tables/${tableId}`,
      key: tableId,
      fields: fields,
      value_type: "json_blob"
    };
  };

  const onFilterValueChange = (index: number, v: any, update: boolean = true) => {
    let newFilters = filters;
    newFilters[index] = {
      ...filters[index],
      value: v
    };
    setFilters(newFilters);
    if (update) {
      const data = get(rest_api, ["get", "data"], {});
      const dataColumns = get(data, ["schema", "columns"], {});
      const schema = generateSchema(dataColumns, tableSelect.key);
      onChange({
        ...value,
        filters: newFilters,
        schema: schema
      });
    }
  };

  const updateFilterValues = () => {
    const data = get(rest_api, ["get", "data"], {});
    const dataColumns = get(data, ["schema", "columns"], {});
    const schema = generateSchema(dataColumns, tableSelect.key);
    onChange({
      ...value,
      filters: filters,
      schema: schema
    });
  };

  const debouncedFilterValueChange = debounce(onFilterValueChange, 500);

  const onFilterKeyChange = (index: number, k: any) => {
    let newFilters = filters;
    newFilters[index] = {
      ...newFilters[index],
      key: k,
      value: undefined
    };
    setFilters(newFilters);
    const data = get(rest_api, ["get", "data"], {});
    const dataColumns = get(data, ["schema", "columns"], {});
    const schema = generateSchema(dataColumns, tableSelect.key);
    onChange({
      ...value,
      filters: newFilters,
      schema: schema
    });
  };

  const onDeleteFilter = (index: number) => {
    //let newFilters = filters;
    filters.splice(index, 1);
    setFilters(filters);
    const data = get(rest_api, ["get", "data"], {});
    const dataColumns = get(data, ["schema", "columns"], {});
    const schema = generateSchema(dataColumns, tableSelect.key);
    onChange({
      ...value,
      filters: filters,
      schema: schema
    });
  };

  const onAddFilter = () => {
    let newFilters = filters;
    newFilters.push({
      label: undefined,
      key: undefined,
      value: undefined
    });
    setFilters(newFilters);
    const data = get(rest_api, ["get", "data"], {});
    const dataColumns = get(data, ["schema", "columns"], {});
    const schema = generateSchema(dataColumns, tableSelect ? tableSelect.key : undefined);
    onChange({
      ...value,
      filters: newFilters,
      schema: schema
    });
  };

  return (
    <>
      {!loading &&
        tableSelect !== undefined &&
        tableSelect.key !== undefined &&
        (filters || []).map((fill: any, index: number) => (
          <Row gutter={[5, 5]} key={uuid()}>
            <Col span={11}>
              <Select
                loading={loading}
                key={index}
                value={{ label: fill.label, key: fill.key }}
                labelInValue={true}
                onChange={(option: any) => onFilterKeyChange(index, option.key)}>
                {columns.map((col: { id: string | number | undefined; display_name: string }) => (
                  <Option key={col.id} value={col.id}>
                    {col.display_name}
                  </Option>
                ))}
              </Select>
            </Col>
            <Col span={12}>{getValue(index)}</Col>
            <Col span={1}>
              <Button icon={"delete"} type={"link"} onClick={(e: any) => onDeleteFilter(index)} />
            </Col>
          </Row>
        ))}
      <Row>
        <Col>
          <Button type={"link"} icon={"plus"} onClick={onAddFilter}>
            Add Filter
          </Button>
        </Col>
      </Row>
    </>
  );
};
