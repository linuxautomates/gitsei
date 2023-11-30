import React from "react";
import "./table-row-actions.style.scss";
import { AntButtonComponent as AntButton } from "../ant-button/ant-button.component";
import { default as AntIcon } from "../ant-icon/ant-icon.component";
import { DeleteMenuItemWrapper as DeleteMenuItem } from "../ant-dlt-menu-item/ant-dlt-menu-item.wrapper";
import { SvgIconComponent as SvgIcon } from "../svg-icon/svg-icon.component";
import { Dropdown, Menu, Popconfirm, Tooltip, Button } from "antd";
import { PermissionIdentifier, ResourceType } from "@harness/microfrontends";
import { useParentProvider } from "contexts/ParentProvider";
import { ButtonVariation } from "@harness/uicore";

type action = {
  disabled?: boolean;
  type: string;
  id: string;
  description?: string;
  onClickEvent: Function;
  toolTip?: any;
  permission?: {
    permission: PermissionIdentifier;
    resource: {
      resourceType: ResourceType;
      resourceIdentifier: string;
    };
  };
};
type traProps = {
  actions: action[];
  hideBorder?: boolean;
};

const TableRowActionsComponent = (props: traProps) => {
  const {
    components: { RbacButton }
  } = useParentProvider();
  const { actions, hideBorder } = props;
  // TODO: maybe change back to 2 once delete popconfirm issue is fixed...
  const showLimit = 4;
  let optionElements: JSX.Element | JSX.Element[];
  const showAll = actions.length <= showLimit;

  const deleteConfirm = (axn: action, i: any) => {
    if (!axn.disabled) {
      return (
        <Popconfirm
          key={`row-action-${i}`}
          title={"Do you want to delete this item?"}
          onConfirm={e => axn.onClickEvent(axn.id)}
          okText={"Yes"}
          cancelText={"No"}>
          {axn.permission ? (
            <RbacButton
              icon="code-delete"
              variation={ButtonVariation.ICON}
              iconProps={{ size: 24 }}
              permission={axn.permission}
            />
          ) : (
            <Tooltip title={axn.toolTip}>
              <Button
                className={`ant-btn-outline ant-btn-delete ${hideBorder ? "withoutBorder" : "mx-5"}`}
                disabled={axn.disabled}
                icon={axn.type}
                style={{ pointerEvents: axn.disabled && axn?.toolTip ? "none" : "auto" }}
              />
            </Tooltip>
          )}
        </Popconfirm>
      );
    } else {
      return (
        <Tooltip title={axn.toolTip}>
          <Button
            className={`ant-btn-outline ant-btn-delete ${hideBorder ? "withoutBorder" : "mx-5"}`}
            type="link"
            disabled={axn.disabled}
            icon={axn.type}
            style={{ pointerEvents: axn.disabled && axn?.toolTip ? "none" : "auto" }}
          />
        </Tooltip>
      );
    }
  };
  const copyConfirm = (axn: any, i: any) => {
    if (!axn.disabled) {
      return (
        <Popconfirm
          key={`row-action-${i}`}
          title={"Do you want to clone this item?"}
          onConfirm={e => axn.onClickEvent(axn.id)}
          okText={"Yes"}
          cancelText={"No"}>
          {axn.permission ? (
            <RbacButton
              icon="copy"
              variation={ButtonVariation.ICON}
              iconProps={{ size: 24 }}
              permission={axn.permission}
            />
          ) : (
            <Tooltip title={axn.tooltip}>
              <Button
                className={`ant-btn-outline ${hideBorder ? "withoutBorder" : "mx-5"}`}
                disabled={axn.disabled}
                icon={axn.type}
                style={{ pointerEvents: axn.disabled && axn?.toolTip ? "none" : "auto" }}
              />
            </Tooltip>
          )}
        </Popconfirm>
      );
    } else {
      return (
        <Tooltip title={axn.tooltip}>
          <Button
            className={`ant-btn-outline ${hideBorder ? "withoutBorder" : "mx-5"}`}
            type="link"
            disabled={axn.disabled}
            icon={axn.type}
            style={{ pointerEvents: axn.disabled && axn?.toolTip ? "none" : "auto" }}
          />
        </Tooltip>
      );
    }
  };
  if (showAll) {
    optionElements = actions.map((axn, i) => {
      if (axn.type === "delete") {
        if (axn.disabled && axn.toolTip) {
          return (
            <Tooltip title={axn.toolTip} key={`action-button-${i}`}>
              <span style={{ cursor: axn.disabled ? "not-allowed" : "pointer" }}>{deleteConfirm(axn, i)}</span>
            </Tooltip>
          );
        }
        return <>{deleteConfirm(axn, i)}</>;
      } else if (axn.type === "copy") {
        if (axn.disabled && axn.toolTip) {
          return (
            <Tooltip title={axn.toolTip} key={`action-button-${i}`}>
              <span style={{ cursor: axn.disabled ? "not-allowed" : "pointer" }}>{copyConfirm(axn, i)}</span>
            </Tooltip>
          );
        }
        return <>{copyConfirm(axn, i)}</>;
      } else if (axn.type === "reset_color_palette") {
        return (
          <Tooltip title={axn.description} placement={"bottom"} key={`action-button-${i}`}>
            <Button
              className={`ant-btn-outline ant-btn-icon-only flex align-center justify-center ${
                hideBorder ? "withoutBorder" : "mx-5"
              }`}
              style={{ pointerEvents: axn.disabled && axn?.toolTip ? "none" : "auto", padding: "0" }}
              type={axn.disabled ? "link" : "default"}
              disabled={axn.disabled}
              onClick={(e: any) => {
                axn.onClickEvent(axn.id);
              }}>
              <SvgIcon icon="vector" style={{ width: "16px", height: "16px" }} />
            </Button>
          </Tooltip>
        );
      } else {
        return (
          <Tooltip title={axn.description} placement={"bottom"} key={`action-button-${i}`}>
            <Button
              key={`row-action-${i}`}
              className={`ant-btn-outline ${hideBorder ? "withoutBorder" : "mx-5"}`}
              type={axn.disabled ? "link" : "default"}
              disabled={axn.disabled}
              onClick={(e: any) => {
                axn.onClickEvent(axn.id);
              }}
              icon={axn.type}
            />
          </Tooltip>
        );
      }
    });
  } else {
    optionElements = (
      <Menu>
        {actions.map((axn, i) => {
          if (axn.type === "delete") {
            return (
              <DeleteMenuItem
                title="Do you want to delete this item?"
                key={`action-menuitem-${i}`}
                onConfirm={() => axn.onClickEvent(axn.id)}
                okText="Yes"
                cancelText="No">
                <AntIcon type={axn.type} /> {axn.description}
              </DeleteMenuItem>
            );
          }
          return (
            <Tooltip title={axn?.toolTip}>
              <Menu.Item
                disabled={axn.disabled}
                key={`action-menuitem-${i}`}
                onClick={e => {
                  axn.onClickEvent(axn.id);
                }}>
                <AntIcon type={axn.type} /> {axn.description}
              </Menu.Item>
            </Tooltip>
          );
        })}
      </Menu>
    );
  }

  return (
    <>
      {showAll ? (
        <div className="actionsWrapper"> {optionElements} </div>
      ) : (
        <Dropdown overlay={optionElements} placement="bottomRight" trigger={["click"]}>
          <AntButton>
            <AntIcon type={"ellipsis"} />
          </AntButton>
        </Dropdown>
      )}
    </>
  );
};

export default TableRowActionsComponent;
