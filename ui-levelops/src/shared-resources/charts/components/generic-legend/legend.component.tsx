import React, { useCallback, useMemo } from "react";
import { Button, Dropdown, Menu } from "antd";
import { AntCheckbox, AntIcon } from "../../../components";
import {
  transformKey,
  getFilterWidth,
  CHECKBOX_PADDING,
  CHECKBOX_WIDTH,
  CHECKBOX_SPACE,
  MORE_BUTTON_WIDTH,
  RESET_BUTTON_WIDTH
} from "../../helper";
import "./legend.component.scss";

interface LegendComponentProps {
  filters: any;
  setFilters: (filters: any) => void;
  width: number;
}

const LegendComponent: React.FC<LegendComponentProps> = props => {
  const { filters, setFilters, width: containerWidth } = props;

  const filterKeys = useMemo(() => (filters && Object.keys(filters).length > 1 ? Object.keys(filters) : []), [filters]);

  const hasFilters = useMemo(() => filters && Object.keys(filters).length > 1, [filters]);

  const paddingAndMargin = CHECKBOX_PADDING + CHECKBOX_WIDTH + CHECKBOX_SPACE;

  const pivotIndex = useMemo(() => {
    let width = RESET_BUTTON_WIDTH + MORE_BUTTON_WIDTH + 20;
    for (let index = 0; index < filterKeys.length; index++) {
      const key = filterKeys[index];
      width = width + getFilterWidth(transformKey(key || "")) + paddingAndMargin;
      if (width >= containerWidth) {
        return index;
      }
    }
    return filterKeys.length;
  }, [filters, containerWidth]);

  const listFilterKeys = useMemo(() => (hasFilters ? filterKeys.slice(0, pivotIndex) : []), [filters, containerWidth]);

  const dropdownFilterKeys = useMemo(() => (hasFilters ? filterKeys.slice(pivotIndex) : []), [filters, containerWidth]);

  const handleResetFilters = useCallback(() => {
    let updatedFilters = {};
    Object.keys(filters).forEach(filter => {
      updatedFilters = {
        ...updatedFilters,
        [filter]: true
      };
    });
    setFilters(updatedFilters);
  }, [filters, setFilters]);

  const handleFilterChange = useCallback(
    (key: string, value: boolean) => {
      if (filters && Object.keys(filters).filter(key => (filters as any)[key]).length === 1 && !value) {
        return;
      }

      if (filters && (filters as any)[key] !== value) {
        const updatedFilters = { ...filters, [key]: value };
        return setFilters(updatedFilters);
      }

      setFilters({ ...(filters || {}), [key]: value });
    },
    [filters, setFilters]
  );

  const renderResetButton = useMemo(() => {
    return (
      <Button type="link" className="reset-btn" onClick={handleResetFilters}>
        Reset Legend
      </Button>
    );
  }, [filters, setFilters]);

  const renderFiltersList = useMemo(() => {
    return (
      <>
        {listFilterKeys.map((key: string, index: number) => {
          return (
            <AntCheckbox
              key={`filter-${index}`}
              className={"legend-filter-checkbox"}
              indeterminate={filters?.[key]}
              checked={filters?.[key]}
              onChange={(input: any) => handleFilterChange(key, input.target.checked)}>
              {transformKey(key)}
            </AntCheckbox>
          );
        })}
      </>
    );
  }, [filters, setFilters, containerWidth]);

  const renderFiltersMenu = useMemo(() => {
    return (
      <Menu>
        {dropdownFilterKeys.map((key: string, index: number) => {
          return (
            <Menu.Item key={`filter-${index}`}>
              <AntCheckbox
                indeterminate={filters[key]}
                checked={filters[key]}
                className={"legend-filter-checkbox"}
                onChange={(input: any) => handleFilterChange(key, input.target.checked)}>
                {transformKey(key)}
              </AntCheckbox>
            </Menu.Item>
          );
        })}
      </Menu>
    );
  }, [filters, setFilters, containerWidth]);

  const renderFiltersDropdown = useMemo(() => {
    if (!dropdownFilterKeys.length) {
      return null;
    }

    return (
      <Dropdown overlay={renderFiltersMenu} placement="bottomCenter" trigger={["click"]}>
        <Button type="link" className="more-btn">
          <span>More</span>
          <AntIcon type="down" className="down-arrow" />
        </Button>
      </Dropdown>
    );
  }, [filters, setFilters, containerWidth]);

  if (!hasFilters) return null;

  return (
    <div className="legend-filters">
      <div className="legend-filters-container">
        {renderFiltersList}
        {renderFiltersDropdown}
      </div>
      {renderResetButton}
    </div>
  );
};

export default React.memo(LegendComponent);
