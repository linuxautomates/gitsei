import React, { useState } from "react";
import { Table } from "antd";
import "./ConfigurationTablesConfigureModal.scss";
import ConfigureTableHeaderCell from "./ConfigureTableHeaderCell";
import ConfigureTableBodyCell from "./ConfigureTableBodyCell";
import PresetColumnTypeModal from "./PresetColumnTypeModal";
import DefaultValueModal from "./DefaultValueModal";
import { get } from "lodash";

export const EditableContext = React.createContext({
  rowsEdit: {},
  setRowsEdit: (id: string | number, edit: boolean) => {}
});

const components = {
  header: {
    cell: ConfigureTableHeaderCell
  },
  body: {
    cell: ConfigureTableBodyCell
  }
};

interface ConfigureProps {
  columns: Array<any>;
  rows: Array<any>;
  edit: boolean;
  setColumnAction: (action: string, colId: string, value?: any) => void;
  setRowAction: (action: string, rowId: string, value?: any) => void;
  setColumns: (data: Array<any>) => void;
  setRows: (data: Array<any>) => void;
  paginationConfig: any;
  filtersData: any;
}

const ConfigureTable: React.FC<ConfigureProps> = (props: ConfigureProps) => {
  const [openPresetColumnTypeModal, setOpenPresetColumnTypeModal] = useState<number>();
  const [openDefaultModal, setDefaultModal] = useState<string>();
  const [defaultRequired, setDefaultRequired] = useState<boolean>(false);
  const [metaData, setMetaData] = useState<{ colId: string; value: any; key: string }>({
    colId: "",
    value: undefined,
    key: ""
  });

  const [resizeFactorMap, setResizeFactorMap] = useState<any>({});
  const [initialWidthMap, setInitialWidthMap] = useState<any>({});

  const setColumnAction = (action: string, colId: string, value?: any) => {
    const key = action.split("_")[1];
    if (["required", "readOnly"].includes(key) && value === true) {
      const col = props.columns.find((c: any) => c.dataIndex === colId);
      if (
        (key === "required" && col.readOnly && !col.defaultValue) ||
        (key === "readOnly" && col.required && !col.defaultValue)
      ) {
        setDefaultRequired(true);
        setDefaultModal(`${col.dataIndex}_${col.inputType}`);
        setMetaData({ colId, value, key });
        return;
      }
    }

    props.setColumnAction(action, colId, value);
  };

  const openDefaultValueModal = (itemKey: string) => {
    const dataIndex = itemKey.split("_")[1];
    const col = props.columns.find((c: any) => c.dataIndex === `column_${dataIndex}`);
    if (col.required && col.readOnly) {
      setDefaultRequired(true);
    }
    setDefaultModal(itemKey);
  };

  const handleResize = (index: string, event: any, data: any) => {
    const width = get(data, ["size", "width"], 0);
    const updatedColumns = props.columns.map((col: any, colIndex: any) => {
      if (col.dataIndex === index) {
        return {
          ...col,
          width
        };
      } else return col;
    });

    const factor = width - getInitialWidth(index);

    if (factor < 0) {
      let newInitials = initialWidthMap;
      props.columns.forEach((col: any) => {
        if (col.dataIndex !== index) {
          const ele = document.getElementById(col.dataIndex);
          if (ele) {
            newInitials = {
              ...newInitials,
              [`${col.dataIndex}`]: ele.getBoundingClientRect().width
            };
          }
        }
      });
      setInitialWidthMap(newInitials);
    }

    setResizeFactorMap((rMap: any) => ({ ...rMap, [index]: factor }));

    props.setColumns(updatedColumns);
  };

  const getInitialWidth = (index: string) => {
    let initialWidth = get(initialWidthMap, [index], undefined);
    if (initialWidth) {
      return initialWidth;
    } else {
      const ele = document.getElementById(index);
      if (ele) {
        initialWidth = ele.getBoundingClientRect().width;
        setInitialWidthMap((iMap: any) => ({ ...iMap, [index]: initialWidth }));
        return initialWidth;
      }
    }
  };

  const mergedColumns = props.columns.map((col, colIndex) => {
    if (!col.editable) {
      return col;
    }

    // Don't allow editing of columns that have
    // a filter being applied against them.
    // Prevents bugs.
    const hasActiveFilter = !!(props.filtersData || []).find((filter: any) => filter.dataIndex === col.dataIndex);

    return {
      ...col,
      onHeaderCell: (hProps: any) => ({
        width: hProps.width || getInitialWidth(col.dataIndex),
        columns: props.columns,
        dataIndex: col.dataIndex,
        title: col.title,
        editing: props.edit,
        last: colIndex === props.columns.length - 1,
        first: colIndex === 0,
        index: col.dataIndex,
        colRequired: col.required,
        options: col.options || [],
        inputType: col.inputType,
        setColumnAction: setColumnAction,
        readOnly: col.readOnly || hasActiveFilter,
        onResize: (e: any, data: any) => handleResize(col.dataIndex, e, data),
        openDefaultModal: (itemKey: string) => openDefaultValueModal(itemKey),
        openPresetTypeModal: () => setOpenPresetColumnTypeModal(col.dataIndex)
      }),
      onCell: (record: any, rowIndex: number) => ({
        record,
        width: col.width || getInitialWidth(col.dataIndex),
        value: record[col.dataIndex],
        defaultValue: col.defaultValue,
        editing: props.edit,
        inputType: col.inputType,
        index: `row_${rowIndex}_${colIndex}`,
        last: colIndex === props.columns.length - 1,
        first: rowIndex === 0,
        colRequired: col.required,
        options: col.options || [],
        colIndex: col.dataIndex,
        setColumnAction: props.setColumnAction,
        setRowAction: props.setRowAction,
        readOnly: col.readOnly || hasActiveFilter
      })
    };
  });

  const savePresetOptions = (options: Array<string>) => {
    const colIndex = openPresetColumnTypeModal;
    const updatedColumns = props.columns.map((col: any, index: number) => {
      if (col.dataIndex === colIndex) {
        return {
          ...col,
          inputType: "single-select",
          options
        };
      }
      return col;
    });
    props.setColumns(updatedColumns);
    setOpenPresetColumnTypeModal(undefined);
  };

  const getDefaultValue = () => {
    const dataIndex = openDefaultModal && openDefaultModal.split("_")[1];
    const col = props.columns.find((col: any) => col.dataIndex === `column_${dataIndex}`);
    return col ? col.defaultValue : "";
  };

  const saveDefaultValue = (defaultValue: string) => {
    const dataIndex = openDefaultModal && openDefaultModal.split("_")[1];
    const updatedColumns = props.columns.map((col: any, index: number) => {
      if (col.dataIndex === `column_${dataIndex}`) {
        if (defaultRequired && metaData.colId) {
          return {
            ...col,
            defaultValue,
            [metaData.key]: metaData.value
          };
        }
        return {
          ...col,
          defaultValue
        };
      }
      return col;
    });

    let updatedRows = props.rows;
    if (defaultValue) {
      updatedRows = props.rows.map((row: any) => {
        return {
          ...row,
          [`column_${dataIndex}`]: row[`column_${dataIndex}`] || defaultValue
        };
      });
    }

    if (defaultRequired) {
      setDefaultRequired(false);
    }

    if (metaData.colId) {
      setMetaData({ colId: "", value: undefined, key: "" });
    }
    props.setRows(updatedRows);
    props.setColumns(updatedColumns);
    setDefaultModal(undefined);
  };

  const getWidth = () => {
    return `calc(100% + ${Object.values(resizeFactorMap)
      .filter((val: any) => val >= 0)
      .reduce((acc: number, item: any) => acc + item, 0)}px)`;
  };

  return (
    <div style={{ width: getWidth() }}>
      <Table
        rowKey={(record, index) => `${index}`}
        bordered
        columns={mergedColumns}
        components={components}
        pagination={props.paginationConfig}
        dataSource={props.rows}
      />
      <PresetColumnTypeModal
        visible={!!openPresetColumnTypeModal}
        onOk={savePresetOptions}
        onCancel={() => setOpenPresetColumnTypeModal(undefined)}
      />
      <DefaultValueModal
        itemKey={openDefaultModal as string}
        onCancel={() => !defaultRequired && setDefaultModal(undefined)}
        onOk={saveDefaultValue}
        visible={!!openDefaultModal}
        presetValue={getDefaultValue()}
        defaultRequired={defaultRequired}
      />
    </div>
  );
};

export default ConfigureTable;
