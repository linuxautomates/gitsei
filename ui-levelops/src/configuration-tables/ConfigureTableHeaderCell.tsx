import React, { useContext, useEffect, useMemo, useState } from "react";
import { Dropdown, Icon, Input, Menu, Modal, Switch, Tooltip } from "antd";
import "./ConfigurationTablesConfigureModal.scss";
import ActionComponent from "./ActionComponent";
import { forEach, get } from "lodash";
import { EditableContext } from "./Configure";
import { validateHeaderCell } from "./helper";
import { Resizable } from "react-resizable";

const { confirm } = Modal;

const { SubMenu } = Menu;

const ConfigureTableHeaderCell: React.FC = (props: any) => {
  const { rowsEdit, setRowsEdit } = useContext(EditableContext);
  const [headerTitle, setHeaderTitle] = useState<string>("");
  const { onResize, width, columns } = props;

  const disableBaselineType = useMemo(() => {
    let cntOfBaseLineCols = 0;
    forEach(columns, col => {
      if (col.inputType === "base_line") {
        cntOfBaseLineCols += 1;
      }
    });
    return cntOfBaseLineCols >= 3;
  }, [columns]);

  useEffect(() => setHeaderTitle(props.title), [props]);

  const showConfirm = (type: string) => {
    confirm({
      title: "Changing the column type will affect the corresponding cell value. Are you sure?",
      onOk: () => {
        type === "single-select"
          ? props.openPresetTypeModal()
          : props.setColumnAction("col_inputType", props.index, type);
      }
    });
  };

  const setHeaderName = () => {
    if (headerTitle.trim().length > 0) {
      props.setColumnAction("col_title", props.index, headerTitle);
      toggleEdit(false);
      setHeaderTitle("");
    }
  };

  const getHighlighted = (type: string) => {
    return props.inputType === type ? "#e0f0ff" : "#fff";
  };

  const valid = () => {
    return validateHeaderCell(props.columns || [], props.index, headerTitle);
  };

  const menu = () => {
    return (
      <Menu>
        <SubMenu title={"Column Type"} disabled={props.readOnly}>
          <Menu.Item
            key="type:3"
            style={{ backgroundColor: getHighlighted("string") }}
            onClick={() => showConfirm("string")}>
            Text (Default)
          </Menu.Item>
          <Menu.Item
            key="type:3"
            style={{ backgroundColor: getHighlighted("base_line") }}
            onClick={() => showConfirm("base_line")}
            disabled={disableBaselineType}>
            Baseline
          </Menu.Item>
          <Menu.Item
            key="type:2"
            style={{ backgroundColor: getHighlighted("boolean") }}
            onClick={() => showConfirm("boolean")}>
            Boolean
          </Menu.Item>
          <Menu.Item
            key="type:1"
            style={{ backgroundColor: getHighlighted("date") }}
            onClick={() => showConfirm("date")}>
            Date
          </Menu.Item>
          <Menu.Item
            key="type:0"
            style={{ backgroundColor: getHighlighted("single-select") }}
            onClick={() => showConfirm("single-select")}>
            Preset
          </Menu.Item>
        </SubMenu>
        <Menu.Item key="1" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          Required{" "}
          <Switch
            checked={props.colRequired}
            onChange={checked => props.setColumnAction("col_required", props.index, checked)}
          />
        </Menu.Item>
        <Menu.Item key="2" style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          Read Only{" "}
          <Switch
            checked={props.readOnly}
            onChange={checked => props.setColumnAction("col_readOnly", props.index, checked)}
          />
        </Menu.Item>
        <Menu.Item key="type:0" onClick={() => props.openDefaultModal(`${props.index}_${props.inputType}`)}>
          Default Value
        </Menu.Item>
        <Menu.Divider />
        <Menu.Item
          key="3"
          onClick={() => props.setColumnAction("deleteCol", props.index)}
          style={{ display: "flex", alignItems: "center", color: "red" }}>
          <Icon type={"delete"} style={{ marginRight: "0.5em" }} onClick={e => e.preventDefault()} />
          Delete Column
        </Menu.Item>
      </Menu>
    );
  };

  const toggleEdit = (value: boolean) => {
    setRowsEdit(props.index, value);
  };

  const getEditing = () => {
    return get(rowsEdit, [props.index], false);
  };

  let childNode = getEditing() ? (
    <Input
      style={{ width: "90%", backgroundColor: "inherit", border: "none", boxShadow: "none" }}
      onPressEnter={() => setHeaderName()}
      onBlur={() => setHeaderName()}
      onChange={e => setHeaderTitle(e.target.value)}
      value={headerTitle}
    />
  ) : (
    <div onClick={() => toggleEdit(true)} style={{ width: "100%", fontWeight: "normal" }} className={"ellipsis"}>
      <Tooltip title={props.title}>
        <span className={props.colRequired ? "required" : ""}>{props.title}</span>
      </Tooltip>
    </div>
  );

  return (
    <Resizable
      width={width}
      className={"col-resize"}
      height={0}
      handle={
        <span
          className="react-resizable-handle"
          onClick={e => {
            e.stopPropagation();
          }}
        />
      }
      onResize={onResize}>
      <th
        id={props.index}
        style={{
          height: "4rem",
          borderTop: "1rem solid #eef3fe",
          backgroundColor: getEditing() ? "#fff" : !valid() ? "#FFE8E0" : "#e7ecf7"
        }}>
        <div
          style={{ display: "flex", flexDirection: "column", justifyContent: "space-between", position: "relative" }}>
          <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
            {childNode}
            <>
              <div style={{ marginLeft: "1rem" }}>
                <Dropdown overlay={menu} overlayStyle={{ width: "12em" }} trigger={["click"]}>
                  <Icon type={"down"} onClick={e => e.preventDefault()} />
                </Dropdown>
              </div>
              {props.first && (
                <ActionComponent
                  className={"col-dot-pos-first"}
                  onClick={() => props.setColumnAction("addCol", `col_-1`)}
                  index={props.index}
                />
              )}
              {!props.last && (
                <ActionComponent
                  className={"col-dot-pos"}
                  index={props.index}
                  onClick={() => props.setColumnAction("addCol", props.index)}
                />
              )}
              {props.last && (
                <ActionComponent
                  className={"col-dot-pos"}
                  style={{ right: "calc(-1rem - 1px)" }}
                  index={props.index}
                  onClick={() => props.setColumnAction("addCol", props.index)}
                />
              )}
            </>
          </div>
        </div>
      </th>
    </Resizable>
  );
};

export default ConfigureTableHeaderCell;
