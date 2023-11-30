import React, { useEffect, useState } from "react";
import { Select } from "antd";
import { useDispatch, useSelector } from "react-redux";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { configTableState } from "reduxConfigs/selectors/restapiSelector";
import { get } from "lodash";

const { Option } = Select;

interface ConfigTableColumnComponentProps {
  onChange: (value: any) => void;
  layout?: any;
  value?: any;
  key: string;
  suggestions?: Array<any>;
  index?: number;
  field_values?: any;
}

export const ConfigTableColumnComponent = (props: ConfigTableColumnComponentProps) => {
  const dispatch = useDispatch();
  const key = props.key || "0";
  const { onChange, value, field_values } = props;

  const rest_api = useSelector(state =>
    configTableState(state, key, get(field_values, ["table_id", "value", 0, "key"], "0"))
  );
  const [tableSelect, setTableSelect] = useState<any>(undefined);
  const [columnSelect, setColumnSelect] = useState<any>(undefined);
  const [columns, setColumns] = useState<any>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const propTableId = get(field_values, ["table_id", "value", 0, "key"], undefined);
    // setTableSelect({ key: propTableId });
    if (propTableId !== undefined) {
      const columnId = get(value, ["column_id"], undefined);
      setColumnSelect(columnId);
      //   setLoading(true);
      //   dispatch(actionTypes.genericGet("config_tables", `${propTableId}?expand=schema`));
    }
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
        setColumnSelect(undefined);
        // @ts-ignore
        dispatch(actionTypes.restapiClear("config-tables", "get", `${tableSelect.key}?expand=schema`));
      }
      setTableSelect({ key: propTableId });
      //const columnId = get(value, ["column_id"], undefined);
      //setColumnSelect(columnId);
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
    if (loading) {
      // do the get for the schema here
      if (!rest_api.get.loading && !rest_api.get.error && rest_api.get.data !== undefined) {
        const data = get(rest_api, ["get", "data"], {});
        const dataColumns = get(data, ["schema", "columns"], {});
        updateColumns(dataColumns);
        setLoading(false);
        setTableSelect({ key: data.id, label: data.name });
      } else if (!rest_api.get.loading && rest_api.get.error) {
        console.log("rest api error");
        setLoading(false);
        setTableSelect(undefined);
      }
    }
  }, [rest_api]); // eslint-disable-line react-hooks/exhaustive-deps

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

  const onColumnChange = (option: string) => {
    setColumnSelect(option);
    const data = get(rest_api, ["get", "data"], {});
    const dataColumns = get(data, ["schema", "columns"], {});
    const schema = generateSchema(dataColumns, tableSelect.key);
    onChange({
      ...value,
      column_id: option,
      schema: schema
    });
  };

  return (
    <>
      <Select onChange={onColumnChange} value={columnSelect} disabled={loading || !tableSelect || !tableSelect.key}>
        {columns.map((col: any) => (
          <Option key={col.id}>{col.display_name}</Option>
        ))}
      </Select>
    </>
  );
};
