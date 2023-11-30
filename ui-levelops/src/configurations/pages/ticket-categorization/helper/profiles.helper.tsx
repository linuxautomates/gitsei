import { Button, Dropdown, Menu } from "antd";
import { Link } from "react-router-dom";
import { RestTicketCategorizationScheme } from "classes/RestTicketCategorizationScheme";
import { TRELLIS_SECTION_MAPPING } from "configurations/pages/TrellisProfile/constant";
import { get, map } from "lodash";
import React from "react";
import { AntRadio, AntTag, AntText, SvgIcon, TableRowActions, TitleWithCount } from "shared-resources/components";
import { baseColumnConfig } from "utils/base-table-config";
import { toTitleCase } from "utils/stringUtils";

export const buildActions = (
  id: string,
  handleMenuClick: (action: any, id: string) => void,
  actions: Array<any>,
  record: any
) =>
  actions.map(action => ({
    type: action === PROFILE_ACTIONS.CLONE ? "copy" : action,
    id,
    description: toTitleCase(action),
    disabled: action === PROFILE_ACTIONS.DELETE && (record.predefined_profile || record.defaultScheme),
    onClickEvent: (id: string) => handleMenuClick(action, id)
  }));

export const defaultColumn = (
  dataIndex: string,
  handleMenuClick: (action: any, id: string) => void,
  isReadonly: boolean
) => ({
  ...baseColumnConfig("SET TO DEFAULT", "default", { width: "15%" }),
  dataIndex,
  render: (isDeault: boolean, record: any) => {
    if (record.is_new) {
      return null;
    }

    const onDefaultClick = isReadonly ? () => {} : () => handleMenuClick("set-default", record.id);

    return isDeault ? (
      <AntTag key="default" color="purple">
        <span>DEFAULT</span>
      </AntTag>
    ) : (
      <AntTag key="non-default" style={isReadonly ? {} : { cursor: "pointer" }} onClick={onDefaultClick}>
        <span>SET TO DEFAULT</span>
      </AntTag>
    );
  }
});

const renderCategories = (categories: Array<any>) => {
  if ((categories || []).length === 0) return <div />;
  return (
    <Menu onClick={() => null}>
      {map(categories || [], (category, index) => (
        <Menu.Item key={category?.id || index}>
          {get(TRELLIS_SECTION_MAPPING, [category?.name || ""], category?.name || "")}
        </Menu.Item>
      ))}
    </Menu>
  );
};

export const categoriesColumn = (dataIndex: string, key: string = "categories", filterFn?: (obj: any) => boolean) => ({
  ...baseColumnConfig(key.toUpperCase(), key, { width: "20%" }),
  dataIndex,
  render: (categories: any, record: any) => {
    if (record.is_new) return null;
    const filteredCategories = filterFn ? categories.filter(filterFn) : categories;
    return (
      <div style={{ display: "flex" }}>
        <Dropdown placement="bottomRight" overlay={renderCategories(filteredCategories)} trigger={["hover"]}>
          <span className="profile-card-content-categories-tag">
            <SvgIcon icon={"vieweye"} style={{ height: "16px", width: "16px", margin: "3px" }} />
          </span>
        </Dropdown>
        {filteredCategories?.length}
      </div>
    );
  }
});

export const nameColumn = (data: RestTicketCategorizationScheme[], hrefBuilder: (configId: string) => any) => ({
  ...baseColumnConfig("PROFILE NAME", "name", { width: "30%" }),
  title: <TitleWithCount title="PROFILE NAME" count={(data || []).length} showZero={true} />,
  render: (
    value: boolean | React.ReactChild | React.ReactFragment | React.ReactPortal | null | undefined,
    record: any
  ) => (
    <>
      <AntText className={"pl-10"}>
        <Link className={"ellipsis"} to={hrefBuilder(record.id)}>
          {value}
        </Link>
      </AntText>
      {record.predefined_profile && (
        <AntTag key="default" style={{ marginLeft: "12px" }}>
          <span>PREDEFINED</span>
        </AntTag>
      )}
    </>
  )
});

const externalLinkStyle = {
  width: 16,
  height: 16,
  align: "center"
};

export const modalNameColumn = (data: any[], hrefBuilder: (configId: string) => any, profileType: string) => ({
  ...baseColumnConfig("Profile Name", "name", { width: "30%" }),
  title: <TitleWithCount title={`${profileType} profile name`} count={(data || []).length} showZero={true} />,
  render: (
    value: boolean | React.ReactChild | React.ReactFragment | React.ReactPortal | null | undefined,
    record: any
  ) => (
    <div className="modal-column">
      <AntRadio value={record.id} style={{ margin: 0 }}>
        <AntText className={"pl-10 ellipsis"}>{value}</AntText>
      </AntRadio>
      {record.predefined_profile && (
        <AntTag key="default" style={{ marginLeft: "3px" }}>
          <span>PREDEFINED</span>
        </AntTag>
      )}
      <Link className={"external-link"} to={hrefBuilder(record.id)} target={"_blank"} rel="noopener noreferrer">
        <SvgIcon icon="externalLink" className="external-link-icon" />
      </Link>
    </div>
  )
});

export const descriptionColumn = () => baseColumnConfig("DESCRIPTION", "description", { width: "25%" });

export const actionColumn = (actions: any[], handleMenuClick: (key: string, configId: string) => void) => ({
  ...baseColumnConfig("ACTIONS", "id"),
  render: (id: string, record: any) => (
    <TableRowActions actions={buildActions(id, handleMenuClick, actions, record)} hideBorder={true} />
  )
});

export const workflowProfileTypeColumn = () => ({
  ...baseColumnConfig("WORKFLOW PROFILE TYPE", "is_new", { width: "20%" }),
  render: (id: string, record: any) => {
    return <>{record.is_new ? <AntText>DORA profile </AntText> : <AntText>Velocity lead time profile</AntText>}</>;
  }
});

export const PROFILE_ACTIONS = {
  EDIT: "edit",
  CLONE: "clone",
  DELETE: "delete",
  RESET_COLOR_PALETTE: "reset_color_palette"
};
