import { Divider, Radio } from "antd";
import { RadioChangeEvent } from "antd/lib/radio";
import {
  EXCLUDE_NUMBER_SETTINGS_OPTIONS,
  EXCLUDE_SETTINGS_OPTIONS
} from "configurations/pages/TrellisProfile/constant";
import React, { useEffect, useMemo, useState } from "react";
import { AntButtonComponent as AntButton } from "shared-resources/components/ant-button/ant-button.component";
import { AntColComponent as AntCol } from "shared-resources/components/ant-col/ant-col.component";
import { AntInputComponent as AntInput } from "shared-resources/components/ant-input/ant-input.component";
import { default as AntRow } from "shared-resources/components/ant-row/ant-row.component";
import { AntTextComponent as AntText } from "shared-resources/components/ant-text/ant-text.component";
import { NUMERIC_SORT_OPTIONS, SortOptions, STRING_SORT_OPTIONS } from "./constants";
import cx from "classnames";
import "./ColumnFilterDropdown.scss";

interface ColumnFilterDropdownProps {
  index: string;
  sortList: { index: string; order: SortOptions } | null;
  setSort: (index: string, order: SortOptions, isNumeric?: boolean) => void;
  filters: Map<string, { key: string; value: string }>;
  setFilters: (filter: Map<string, { key: string; value: string }>) => void;
  isNumeric?: boolean;
  resetSort: (index: string) => void;
}

const ColumnFilterDropdown = (props: ColumnFilterDropdownProps) => {
  const { index, sortList, setSort, filters, setFilters, isNumeric, resetSort } = props;
  const [filterValue, setFilterValue] = useState<string>("");
  const [filterKey, setFilterKey] = useState<string>("");

  const sortValue = useMemo(() => {
    if (sortList?.index === index) {
      return sortList.order;
    }
  }, [sortList]);

  useEffect(() => {
    const filter = filters.get(index);
    if (filter) {
      setFilterKey(filter.key);
      setFilterValue(filter.value);
    } else {
      setFilterKey("");
      setFilterValue("");
    }
  }, [filters]);

  const onFilterKeyChange = (event: RadioChangeEvent) => {
    setFilterKey(event.target.value);
    setFilterValue("");
  };

  const onFilterValueChange = (e: any) => {
    const value = isNumeric ? e : e.target.value;
    if ((isNumeric && value >= 0) || !isNumeric) setFilterValue(value);
  };

  const onSortChange = (sortValue: SortOptions) => {
    setSort(props.index, sortValue, isNumeric);
  };

  const saveFilter = () => {
    const value = typeof filterValue === "string" ? filterValue.trim() : filterValue;
    const isValid = typeof filterValue === "string" ? !!value : filterValue !== "";
    if (isValid && filterKey) {
      const filtersCopy = new Map(filters);
      filtersCopy.set(index, {
        key: filterKey,
        value
      });
      setFilters(filtersCopy);
    }
  };

  const resetFilter = () => {
    const filtersCopy = new Map(filters);
    filtersCopy.delete(index);
    setFilterValue("");
    setFilterKey("");
    setFilters(filtersCopy);
    resetSort(index);
  };

  const sortOption = useMemo(() => {
    const SORT_OPTIONS = isNumeric ? NUMERIC_SORT_OPTIONS : STRING_SORT_OPTIONS;
    return (
      <div className="sections">
        <AntText className="m-b10 header-color">SORT</AntText>
        {SORT_OPTIONS.map((option: any) => (
          <div
            onClick={() => onSortChange(option.value)}
            className={cx("sort-input-div", { selected: sortValue == option.value })}>
            <AntText>{option.label}</AntText>
          </div>
        ))}
      </div>
    );
  }, [sortValue, isNumeric]);

  const filtersOption = useMemo(() => {
    const { isNumeric } = props;
    const EXCLUDE_OPTIONS = isNumeric ? EXCLUDE_NUMBER_SETTINGS_OPTIONS : EXCLUDE_SETTINGS_OPTIONS;
    return (
      <div className="sections">
        <AntText className="header-color">FILTER</AntText>
        <Radio.Group onChange={onFilterKeyChange} value={filterKey}>
          {EXCLUDE_OPTIONS.map((option: any) => (
            <AntRow justify={"space-between"} className="flex align-bottom">
              <AntCol className="text-align-left" span={12}>
                <Radio value={option.value}>{option.label}</Radio>
              </AntCol>
              <AntCol span={12}>
                <AntInput
                  disabled={option.value !== filterKey}
                  type={isNumeric ? "number" : "text"}
                  className="input-width"
                  onChange={onFilterValueChange}
                  value={option.value === filterKey ? filterValue : ""}
                />
              </AntCol>
            </AntRow>
          ))}
        </Radio.Group>
      </div>
    );
  }, [filterKey, onFilterKeyChange]);

  const footButtons = useMemo(
    () => (
      <div className="flex direction-row-reverse">
        <AntButton type="primary" onClick={saveFilter}>
          Apply Filter
        </AntButton>
        <AntButton type="link" onClick={resetFilter}>
          Reset
        </AntButton>
      </div>
    ),
    [saveFilter, resetFilter]
  );

  return (
    <div className="column-filter-dropdown">
      {sortOption}
      {filtersOption}
      <Divider className="m-0 m-b10" />
      {footButtons}
    </div>
  );
};

export default ColumnFilterDropdown;
