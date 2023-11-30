import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Dropdown, Menu } from "antd";
import { AntCheckboxComponent as AntCheckbox } from "shared-resources/components/ant-checkbox/ant-checkbox.component";
import { default as AntIcon } from "shared-resources/components/ant-icon/ant-icon.component";
import "./DoraStageLegend.style.scss";
import { transformKey } from "shared-resources/charts/helper";

interface DoraStageFilterDropdownProps {
  filters: any;
  setFilters: (filters: any) => void;
}

const DoraStageFilterDropdown: React.FC<DoraStageFilterDropdownProps> = props => {
  const { filters, setFilters } = props;

  const filterKeys = useMemo(() => (filters && Object.keys(filters).length > 1 ? Object.keys(filters) : []), [filters]);
  const hasFilters = useMemo(() => filters && Object.keys(filters).length > 1, [filters]);
  const [stages, setStages] = useState(filters);
  const [visible, setVisible] = useState(false);
  const selectedFiltersCount = useMemo(
    () => Object.keys(filters || {}).filter((key: string) => !!filters[key]).length,
    [filters]
  );

  useEffect(() => {
    setStages(filters);
  }, [filters]);

  const handleResetFilters = useCallback(() => {
    let updatedFilters = {};
    Object.keys(stages).forEach(filter => {
      updatedFilters = {
        ...updatedFilters,
        [filter]: true
      };
    });
    setStages(updatedFilters);
  }, [stages, setStages]);

  const handleSaveFilters = useCallback(() => {
    setFilters(stages);
    setVisible(false);
  }, [stages, setFilters]);

  const handleFilterChange = useCallback(
    (key: string, value: boolean) => {
      if (stages && Object.keys(stages).filter(key => (stages as any)[key]).length === 1 && !value) {
        return;
      }

      if (stages && (stages as any)[key] !== value) {
        const updatedFilters = { ...stages, [key]: value };
        return setStages(updatedFilters);
      }

      setStages({ ...(stages || {}), [key]: value });
    },
    [stages, setStages]
  );

  const renderFiltersMenu = useMemo(() => {
    return (
      <div className="dora-stage-filters-dropdown">
        <Menu>
          {filterKeys.map((key: string, index: number) => {
            return (
              <Menu.Item key={`filter-${index}`} onClick={({ domEvent }) => domEvent.stopPropagation()}>
                <AntCheckbox
                  indeterminate={stages[key]}
                  checked={stages[key]}
                  className={"dora-stage-legend-filter-checkbox"}
                  onChange={(input: any) => handleFilterChange(key, input.target.checked)}>
                  {transformKey(key)}
                </AntCheckbox>
              </Menu.Item>
            );
          })}
        </Menu>
        <div className="dora-legend-filter-footer">
          <Button type="link" className="reset-btn" onClick={handleResetFilters}>
            Reset Legend
          </Button>
          <Button type="primary" className="save-btn" onClick={handleSaveFilters}>
            Save
          </Button>
        </div>
      </div>
    );
  }, [stages, filters, setFilters]);

  const handleVisibleChange = useCallback(
    (bool: boolean) => {
      setVisible(bool);
      setStages(filters);
    },
    [filters]
  );

  if (!hasFilters) return null;

  return (
    <div className="dora-stage-legend-container">
      <Dropdown
        onVisibleChange={handleVisibleChange}
        visible={visible}
        overlay={renderFiltersMenu}
        placement="bottomCenter"
        trigger={["click"]}>
        <Button type="link" className="stages-btn">
          <span>Stages</span>
          <span className="stages-count">{`(${selectedFiltersCount} / ${filterKeys.length})`}</span>
          <AntIcon type="down" className="down-arrow" />
        </Button>
      </Dropdown>
    </div>
  );
};

export default React.memo(DoraStageFilterDropdown);
