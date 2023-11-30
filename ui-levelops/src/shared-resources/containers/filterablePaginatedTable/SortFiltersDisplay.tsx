import { Button, Dropdown, Menu } from "antd";
import React, { useMemo } from "react";
import { default as AntIcon } from "shared-resources/components/ant-icon/ant-icon.component";
import { AntTextComponent as AntText } from "shared-resources/components/ant-text/ant-text.component";
import { SortOptions } from "./constants";
import { getSortFilters } from "./helper";
import "./SortFiltersDisplay.scss";

interface SortFilterDisplayProps {
  sortList: { index: string; order: SortOptions; isNumeric: boolean } | null;
  filters: Map<string, { key: string; value: string }>;
  resetFilters: () => void;
}

const SortFilterDisplay = (props: SortFilterDisplayProps) => {
  const { sortList, filters, resetFilters } = props;
  const sortFilters = getSortFilters(sortList, filters);

  const renderFiltersMenu = useMemo(() => {
    if (!sortFilters || sortFilters.sortFilterList.length < 3) {
      return null;
    }
    return (
      <Menu>
        {sortFilters.sortFilterList.slice(2).map((item: string) => {
          return <Menu.Item key={item}>{item}</Menu.Item>;
        })}
      </Menu>
    );
  }, [sortFilters]);

  const moreButton = useMemo(
    () =>
      sortFilters && sortFilters.sortFilterList.length > 2 ? (
        <Dropdown overlay={renderFiltersMenu} placement="bottomCenter" trigger={["click"]}>
          <Button type="link">
            <span>{`${sortFilters.sortFilterList.length - 2} More`}</span>
            <AntIcon type="down" />
          </Button>
        </Dropdown>
      ) : null,
    [sortFilters]
  );

  if (sortFilters) {
    return (
      <div className="sortFilterDisplay">
        <div>
          <AntText className="title">{sortFilters.title}</AntText>
          <AntText>{sortFilters.sortFilterList.slice(0, 2).join(", ")}</AntText>
          {moreButton}
        </div>
        <Button type="link" onClick={resetFilters}>
          Reset
        </Button>
      </div>
    );
  }
  return null;
};

export default SortFilterDisplay;
