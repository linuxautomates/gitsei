import React, { useContext, useState } from "react";
import { DatePicker, Dropdown, Icon, Input, Menu, Select, Tooltip } from "antd";
import { EditableContext } from "./Configure";
import { get } from "lodash";
import moment from "moment";
import { getDateValue, validateCell } from "./helper";
import "./ConfigurationTablesConfigureModal.scss";

const { Option } = Select;

const ConfigureTableBodyCell: React.FC = (props: any) => {
  const { rowsEdit, setRowsEdit } = useContext(EditableContext);

  const [showDropdownMap, setShowDropdownMap] = useState<{ [x: string]: boolean }>({});

  const saveCellValue = (value: any) => {
    props.setRowAction("cell_val", props.index, value);
    toggleEdit(false);
  };

  const getCellValue = () => {
    const value = get(props, ["value", "length"], 0);
    return value > 0 ? props.value : "";
  };

  const isDate = props.inputType === "date";

  const renderInput = () => {
    switch (props.inputType) {
      case "string":
        return (
          <Input
            defaultValue={getCellValue()}
            style={{ width: "80%", border: "none", boxShadow: "none" }}
            onPressEnter={() => toggleEdit(false)}
            onChange={e => props.setRowAction("cell_val", props.index, e.target.value)}
            onBlur={() => toggleEdit(false)}
          />
        );
      case "boolean":
        return (
          <Select
            style={{ width: "80%" }}
            value={getCellValue()}
            showArrow={false}
            onBlur={(value: any) => saveCellValue(value)}
            onChange={(value: any) => saveCellValue(value)}>
            <Option value={"False"}>False</Option>
            <Option value={"True"}>True</Option>
          </Select>
        );
      case "date":
        return (
          <DatePicker
            allowClear={false}
            style={{ width: "80%" }}
            value={moment(getDateValue(getCellValue()))}
            onChange={(date, dateString) => saveCellValue(dateString)}
            format={"MM/DD/YYYY"}
          />
        );
      case "single-select":
        return (
          <Select
            style={{ width: "80%" }}
            showArrow={false}
            value={getCellValue()}
            showSearch
            onBlur={(value: any) => saveCellValue(value)}
            onChange={(value: any) => saveCellValue(value)}>
            {(props.options || []).map((item: any, index: number) => (
              <Option key={index} value={item}>
                {item}
              </Option>
            ))}
          </Select>
        );
      default:
        return (
          <Input
            defaultValue={getCellValue()}
            style={{ width: "80%", border: "none", boxShadow: "none" }}
            onPressEnter={() => toggleEdit(false)}
            onChange={e => props.setRowAction("cell_val", props.index, e.target.value)}
            onBlur={() => toggleEdit(false)}
          />
        );
    }
  };

  const menu = () => {
    return (
      <Menu>
        <Menu.Item onClick={() => props.setColumnAction("addCol", props.colIndex)}>Insert Column</Menu.Item>
        <Menu.Item onClick={() => props.setColumnAction("deleteCol", props.colIndex)} disabled={props.readOnly}>
          Delete Column
        </Menu.Item>
        <Menu.Divider />
        <Menu.Item onClick={() => props.setRowAction("addRow", props.index)}>Insert Row</Menu.Item>
        <Menu.Item onClick={() => props.setRowAction("deleteRow", props.index)}>Delete Row</Menu.Item>
      </Menu>
    );
  };

  const valid = () => {
    return props.colRequired ? validateCell(props.inputType, props.value, props.options || []) : true;
  };

  const toggleEdit = (value: boolean) => {
    setRowsEdit(props.index, value);
  };

  const getEditing = () => {
    return get(rowsEdit, [props.index], false);
  };

  const renderValue = isDate ? getDateValue(getCellValue()) : getCellValue();

  const childNode = getEditing() ? (
    <div style={{ width: "100%", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
      {!props.readOnly && renderInput()}
      {props.readOnly && <Tooltip title={renderValue}>{renderValue}</Tooltip>}
    </div>
  ) : (
    <div style={{ height: "1.38rem", width: "100%", overflow: "hidden" }} onClick={() => toggleEdit(true)}>
      <Tooltip title={renderValue}>{renderValue}</Tooltip>
    </div>
  );

  return (
    <td
      className={"ellipsis-row"}
      onMouseEnter={() => setShowDropdownMap((dMap: { [x: string]: boolean }) => ({ ...dMap, [props.index]: true }))}
      onMouseLeave={() => setShowDropdownMap((dMap: { [x: string]: boolean }) => ({ ...dMap, [props.index]: false }))}
      style={{
        width: props.width,
        height: "4rem",
        backgroundColor: props.readOnly ? "#F5F5F5" : getEditing() ? "#fff" : !valid() ? "#FFE8E0" : "",
        cursor: props.readOnly ? "text" : ""
      }}>
      <div style={{ position: "relative", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
        {childNode}
        {showDropdownMap[props.index] && (
          <Dropdown overlay={menu} overlayStyle={{ width: "12em" }} trigger={["click"]}>
            <Icon type={"down"} onClick={e => e.preventDefault()} />
          </Dropdown>
        )}
      </div>
    </td>
  );
};

export default ConfigureTableBodyCell;
