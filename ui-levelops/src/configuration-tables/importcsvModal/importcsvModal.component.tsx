import * as React from "react";
import {
  AntButton,
  AntCol,
  AntInput,
  AntModal,
  AntSelect,
  AntText,
  TableRowActions
} from "../../shared-resources/components";
import { Icon, Upload, message, Table, DatePicker, Form, Input } from "antd";
import "./importcsvModal.component.scss";
import { useEffect, useState } from "react";
import { tableColumn } from "./table-config";
import moment from "moment";
import { ModalProps } from "antd/lib/modal";
import { readString } from "react-papaparse";
import PresetColumnTypeModal from "configuration-tables/PresetColumnTypeModal";
import { COLUMN_TYPE_OPTIONS, TableColumnTypes } from "./constant";
import { get } from "lodash";

const { Dragger } = Upload;

interface CSVImportModalProps extends ModalProps {
  onImportComplete: (data: { name: string; columns: any[]; rows: any[] }) => void;
  getValidateStatus: () => "success" | "warning" | "error" | "validating" | "";
  getError: () => string;
  setNameFieldBlur: (value: boolean) => void;
  setTableName: (value: any) => void;
  tableName: string;
  reImporting: boolean;
}

export const CSVImportModal: React.FC<CSVImportModalProps> = props => {
  const [fileData, setFileData] = useState<any>("");
  const [tableHeaderData, setHeaderTableData] = useState<any[]>([]);
  const [removedColumns, setRemovedColumns] = useState<number[]>([]);
  const [presetModalColumnId, setPresetModalColumnId] = useState<string | undefined>(undefined);
  const [presetOptions, setPresetOptions] = useState<Record<string, string[]>>({});

  const handlePresetModalClose = (id = presetModalColumnId) => {
    setPresetModalColumnId(undefined);
    if (id) {
      setPresetOptions(prev => ({ ...prev, [id]: [] }));
    }
  };

  const handlePresetModalSave = (options: string[]) => {
    if (presetModalColumnId) {
      setPresetOptions(prev => ({ ...prev, [presetModalColumnId]: options }));
      setPresetModalColumnId(undefined);
    }
  };

  const onFieldChange = (name: string, value: string, id: string) => {
    const index = tableHeaderData.findIndex((item: any) => item.id === id);
    const item = tableHeaderData[index];
    const _updatedData = [
      ...tableHeaderData.slice(0, index),
      { ...item, [name]: value },
      ...tableHeaderData.slice(index + 1)
    ];
    setHeaderTableData(_updatedData);
    if (name === "type") {
      if (value === TableColumnTypes.PRESET) {
        setPresetModalColumnId(id);
      } else if (presetOptions.hasOwnProperty(id)) {
        handlePresetModalClose(id);
      }
    }
  };

  const getPressetOptions = (colId: string) => {
    return get(presetOptions, [colId], []).map(o => ({ label: o, value: o }));
  };

  const getDefaultHeader = (id: number, name: string) => {
    return {
      id: id + 1,
      name,
      type: "string",
      required: "no",
      default: "",
      columnTypeOptions: COLUMN_TYPE_OPTIONS,
      readOnly: "no"
    };
  };

  useEffect(() => {
    if (fileData) {
      const headers = fileData[0];
      const _headerData = headers.map((label: string, index: number) => getDefaultHeader(index, label));
      setHeaderTableData(_headerData);
    }
  }, [fileData]);

  useEffect(() => {
    if (props.visible === false) {
      setFileData("");
    }
  }, [props.visible]);

  const onRemoveHandler = (id: number) => {
    const index = tableHeaderData.findIndex(_data => _data.id === id);
    if (index !== -1) {
      setRemovedColumns((columns: any) => [...columns, index]);
    }
  };

  const buildActionOptions = (props: any) => {
    const actions = [
      {
        type: "delete",
        id: props.id,
        description: "Delete",
        onClickEvent: onRemoveHandler
      }
    ];
    return <TableRowActions actions={actions} />;
  };

  const uploadProps = {
    name: "file",
    multiple: true,
    showUploadList: false,
    accept: ".csv",
    customRequest: (data: any) => {
      const _fileName = data && data.file && data.file.name;
      const _extension = _fileName.split(".")[1];

      if (_extension !== "csv") {
        message.error("Upload a .csv file");
        return;
      }

      const fileReader = new FileReader();
      fileReader.onload = (e: any) => {
        if (e && e.target) {
          const _dataarray = readString(e.target.result as string).data;
          setFileData(_dataarray);
        }
      };

      fileReader.readAsText(data.file);
    },
    style: { width: "45vw" }
  };

  const mappedColumns = tableColumn.map(column => {
    if (column.key === "id") {
      return {
        ...column,
        render: (item: string, record: any, index: number) => buildActionOptions(record)
      };
    }

    if (column.key === "type") {
      return {
        ...column,
        render: (value: string, record: any) => (
          <AntSelect
            style={{ width: "100%" }}
            value={value}
            options={record.columnTypeOptions}
            onSelect={(value: any, options: any) => onFieldChange("type", value, record.id)}
          />
        )
      };
    }

    if (column.key === "default") {
      return {
        ...column,
        render: (value: string, record: any, index: number) => {
          const isInvalid = record.required === "yes" && record.readOnly === "yes" && !record.default;
          if (record.type === "boolean") {
            return (
              <AntSelect
                className={isInvalid ? "selectError" : ""}
                style={{ width: "100%" }}
                value={value}
                options={[
                  { label: "Yes", value: "yes" },
                  { label: "No", value: "no" }
                ]}
                onSelect={(value: any, options: any) => onFieldChange("default", value, record.id)}
              />
            );
          }
          if (record.type === "date") {
            return (
              <DatePicker
                className={isInvalid ? "dateError" : ""}
                style={{ width: "100%" }}
                value={value ? moment.unix(parseInt(value)) : null}
                // @ts-ignore
                onChange={(date: any) => onFieldChange("default", Math.ceil(date?.valueOf() / 1000), record.id)}
              />
            );
          }
          if (record?.type === TableColumnTypes.PRESET) {
            return (
              <AntSelect
                options={getPressetOptions(record.id)}
                onChange={(v: string) => onFieldChange("default", v, record.id)}
                value={value}
                className="w-100"
              />
            );
          }
          return (
            <AntInput
              style={{ borderColor: isInvalid ? "red" : "" }}
              id={`default_${index}`}
              placeholder="Type here..."
              value={value}
              onChange={(e: any) => onFieldChange("default", e.target.value, record.id)}
            />
          );
        }
      };
    }

    if (column.key === "required") {
      return {
        ...column,
        render: (value: string, record: any) => (
          <AntSelect
            style={{ width: "100%" }}
            value={value}
            options={[
              { label: "Yes", value: "yes" },
              { label: "No", value: "no" }
            ]}
            onSelect={(value: any, option: any) => onFieldChange("required", value, record.id)}
          />
        )
      };
    }

    if (column.key === "readOnly") {
      return {
        ...column,
        render: (value: string, record: any) => (
          <AntSelect
            style={{ width: "100%" }}
            value={value}
            options={[
              { label: "Yes", value: "yes" },
              { label: "No", value: "no" }
            ]}
            onSelect={(value: any, option: any) => onFieldChange("readOnly", value, record.id)}
          />
        )
      };
    }
    return column;
  });

  const importData = () => {
    let rows = fileData
      .slice(1)
      .filter((v: any) => v.length > 0 && v.some((item: string) => !!item))
      .map((d: string[]) => {
        return d.filter((v: string, index: number) => !removedColumns.includes(index));
      });

    rows = rows.map((row: any, id: number) =>
      row.reduce((acc: any, next: string, index: number) => ({ ...acc, [`column_${index + 1}`]: next }), { id })
    );

    // start index with one
    const columns = tableHeaderData
      .filter((value: any, index: number) => !removedColumns.includes(index))
      .map((col: any, index: number) => ({
        title: col.name,
        key: `column_${index + 1}`,
        index: index,
        id: `${index}`,
        editable: true,
        inputType: col.type,
        dataIndex: `column_${index + 1}`,
        required: col.required === "yes",
        defaultValue: col.default,
        readOnly: col.readOnly === "yes",
        options: get(presetOptions, [col?.id], [])
      }));

    const importedData = { name: props.tableName, columns, rows };

    props.onImportComplete(importedData);
  };

  const importButtonValidation = () => {
    if (!fileData || (!props.tableName && !props.reImporting)) {
      return true;
    }

    const requiredCols = tableHeaderData.filter(item => item.required === "yes" || item.readOnly === "yes");

    return requiredCols.filter((item: any) => !item.default).length > 0;
  };

  return (
    <>
      <PresetColumnTypeModal
        visible={!!presetModalColumnId}
        onCancel={handlePresetModalClose}
        onOk={handlePresetModalSave}
      />
      <AntModal
        {...props}
        title={props.reImporting ? "Re-import CSV" : "Import CSV"}
        wrapClassName={"import-csv-modal"}
        destroyOnClose={true}
        maskClosable={false}
        footer={[
          <AntButton key="back" onClick={props.onCancel}>
            Cancel
          </AntButton>,
          <AntButton key="submit" type="primary" disabled={importButtonValidation()} onClick={importData}>
            Import
          </AntButton>
        ]}
        width="60vw">
        <div className={`content ${fileData ? "content-table" : ""}`}>
          {!fileData && (
            <Dragger {...uploadProps} height={320}>
              <p className="ant-upload-drag-icon">
                <Icon type="cloud-upload" />
              </p>
              <AntText style={{ fontSize: "20px" }} type="secondary">
                CSV upload
              </AntText>
              <p className="ant-upload-text">Drag and Drop to upload or click to browse</p>
            </Dragger>
          )}
          {fileData && !props.reImporting && (
            <AntCol span={24}>
              <Form layout="vertical">
                <Form.Item
                  label="Table name"
                  validateStatus={props.getValidateStatus()}
                  required
                  hasFeedback={true}
                  help={props.getValidateStatus() === "error" && props.getError()}>
                  <Input
                    name={"Name"}
                    onFocus={() => props.setNameFieldBlur(true)}
                    onChange={props.setTableName}
                    value={props.tableName}
                  />
                </Form.Item>
              </Form>
            </AntCol>
          )}
          {fileData && (
            <Table
              style={{ alignSelf: "flex-start", width: "100%" }}
              columns={mappedColumns}
              dataSource={tableHeaderData.filter((value: any, index: number) => !removedColumns.includes(index))}
              pagination={false}
            />
          )}
        </div>
      </AntModal>
    </>
  );
};
