import { Dropdown } from "antd";
import Menu from "antd/lib/menu";
import React, { useMemo } from "react";
import { AntButton, AntCheckbox, Button, SvgIcon } from "../components";
import { onFilterChange } from "./helper";

interface FiltersDropdownProps {
  filters: Object;
  setFilters: (id: string, filters: Object) => void;
  widgetId: string;
}

export const FiltersDropdown = (props: FiltersDropdownProps) => {
  const { filters, setFilters, widgetId } = props;
  const data = (filters as any)[widgetId];

  const transformKey = (key: string) => {
    let keyName = key;
    if (keyName.includes("bar")) {
      keyName = keyName.substr(0, keyName.length - 3);
    } else if (keyName.includes("area") || keyName.includes("line")) {
      keyName = keyName.substr(0, keyName.length - 4);
    }
    return keyName.replace(/_/g, " ");
  };

  const menu = (
    <Menu>
      {data &&
        Object.keys(data).length > 1 &&
        Object.keys(data).map((key: string, index: number) => (
          <Menu.Item key={`filter-${index}`}>
            <AntCheckbox
              checked={(data as any)[key]}
              style={{ textTransform: "capitalize" }}
              onChange={(input: any) => onFilterChange(key, input.target.checked, filters, setFilters, widgetId)}>
              {transformKey(key)}
            </AntCheckbox>
          </Menu.Item>
        ))}
    </Menu>
  );


  return (
    <Dropdown overlay={menu} trigger={["click"]} placement="bottomRight">
      <Button className="widget-extras">
        <SvgIcon icon={"widgetFiltersIcon"} />
      </Button>
    </Dropdown>
  );
};
