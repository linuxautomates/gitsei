import React, { useCallback, useMemo } from "react";
import { Button, Dropdown, Menu } from "antd";
import { AntCheckboxComponent as AntCheckbox } from "shared-resources/components/ant-checkbox/ant-checkbox.component";
import { default as AntIcon } from "shared-resources/components/ant-icon/ant-icon.component";
import { transformKey } from "../../helper";
import "./lead-time-filter-dropdown.scss";

interface LeadTimeFilterDropdownProps {
  filters: any;
  setFilters: (filters: any) => void;
}

const LeadTimeFilterDropdown: React.FC<LeadTimeFilterDropdownProps> = props => {
  const { filters, setFilters } = props;

  const filterKeys = useMemo(() => (filters && Object.keys(filters).length > 1 ? Object.keys(filters) : []), [filters]);
  const hasFilters = useMemo(() => filters && Object.keys(filters).length > 1, [filters]);

  const selectedFiltersCount = useMemo(
    () => Object.keys(filters || {}).filter((key: string) => !!filters[key]).length,
    [filters]
  );

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

  const renderFiltersMenu = useMemo(() => {
    return (
      <Menu className="lead-time-filters-dropdown">
        {filterKeys.map((key: string, index: number) => {
          return (
            <Menu.Item key={`filter-${index}`} onClick={({ domEvent }) => domEvent.stopPropagation()}>
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
        <Menu.Item key="reset-filters" onClick={({ domEvent }) => domEvent.stopPropagation()}>
          <Button type="link" className="reset-btn" onClick={handleResetFilters}>
            Reset Legend
          </Button>
        </Menu.Item>
      </Menu>
    );
  }, [filters, setFilters]);

  if (!hasFilters) return null;

  return (
    <div className="lead-time-legend-container">
      <Dropdown overlay={renderFiltersMenu} placement="bottomCenter" trigger={["click"]}>
        <Button type="link" className="stages-btn">
          <span>Stages</span>
          <span className="stages-count">{`(${selectedFiltersCount} / ${filterKeys.length})`}</span>
          <AntIcon type="down" className="down-arrow" />
        </Button>
      </Dropdown>
    </div>
  );
};

export default React.memo(LeadTimeFilterDropdown);
