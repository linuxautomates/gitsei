import React, { useCallback, useMemo } from "react";
import { Button, Dropdown, Menu } from "antd";
import { LegendProps } from "recharts";
import { AntCheckboxComponent as AntCheckbox } from "shared-resources/components/ant-checkbox/ant-checkbox.component";
import { default as AntIcon } from "shared-resources/components/ant-icon/ant-icon.component";
import { calcLegendPivotIndex, transformKey } from "../../helper";
import "./chart-legend.style.scss";
import { toTitleCase } from "../../../../utils/stringUtils";

type CustomLegendType = { key: string; color: string; label: string };

interface ChartLegendProps extends LegendProps {
  filters: any;
  allowLabelTransform?: boolean;
  setFilters: (filters: any) => void;
  legends: CustomLegendType[];
}

const chartLegendSortingComparator = (value1: CustomLegendType, value2: CustomLegendType) => {
  const key1 = value1?.key?.replace(/_/g, " ").trim().toLowerCase();
  const key2 = value2?.key?.replace(/_/g, " ").trim().toLowerCase();
  if (key1 < key2) return -1;
  if (key1 > key2) return 1;
  return 0;
};

const NewChartLegendComponent: React.FC<ChartLegendProps> = (props: ChartLegendProps) => {
  const { filters, setFilters, width, allowLabelTransform, legends } = props;

  const slicedPayload = ((legends || []).slice(0, (legends || []).length) || []).sort(chartLegendSortingComparator);

  const filterKeys = useMemo(() => (filters && Object.keys(filters).length > 1 ? Object.keys(filters) : []), [filters]);
  const hasFilters = useMemo(() => filters && Object.keys(filters).length > 1, [filters]);

  const legendFormatter = useCallback(
    value => {
      if (allowLabelTransform === false) {
        return value;
      }
      return value ? toTitleCase(transformKey(value)) : "_";
    },
    [allowLabelTransform]
  );

  const pivotIndex = useMemo(() => {
    return calcLegendPivotIndex(filterKeys, legendFormatter, width);
  }, [filters, width]);

  const listFilterKeys = useMemo(
    () => (hasFilters ? (slicedPayload || []).slice(0, pivotIndex) : []),
    [legends, filters, width]
  );

  const dropdownFilterKeys = useMemo(
    () => (hasFilters ? (slicedPayload || []).slice(pivotIndex) : []),
    [legends, filters, width]
  );

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

  const renderResetButton = useMemo(
    () => (
      <Button type="link" className="reset-btn" onClick={handleResetFilters}>
        Reset Legend
      </Button>
    ),
    [filters, setFilters]
  );

  const renderFiltersList = useMemo(() => {
    return (
      <>
        {listFilterKeys.map((entry: CustomLegendType) => {
          const { key, color, label } = entry;
          return (
            <AntCheckbox
              key={`filter-${key}`}
              className={`legend-checkbox`}
              style={{ textTransform: allowLabelTransform === false ? "" : "capitalize", "--tick-color": color }}
              indeterminate={filters[key]}
              checked={filters[key]}
              onChange={(input: any) => handleFilterChange(key, input.target.checked)}>
              {legendFormatter(label)}
            </AntCheckbox>
          );
        })}
      </>
    );
  }, [legends, filters, setFilters, width, allowLabelTransform]);

  const renderFiltersMenu = useMemo(() => {
    return (
      <Menu>
        {dropdownFilterKeys.map((entry: any) => {
          const { key, color, label } = entry;

          return (
            <Menu.Item key={`filter-${key}`} onClick={({ domEvent }) => domEvent.stopPropagation()}>
              <AntCheckbox
                className={"legend-checkbox"}
                style={{ textTransform: allowLabelTransform === false ? "" : "capitalize", "--tick-color": color }}
                indeterminate={filters[key]}
                checked={filters[key]}
                onChange={(input: any) => handleFilterChange(key, input.target.checked)}>
                {legendFormatter(label)}
              </AntCheckbox>
            </Menu.Item>
          );
        })}
      </Menu>
    );
  }, [legends, filters, setFilters, width, allowLabelTransform]);

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
  }, [legends, filters, setFilters, width]);

  if (!hasFilters) return null;

  return (
    <div className="chart-legend-container">
      <div className="legend-filters-container">
        {renderFiltersList}
        {renderFiltersDropdown}
      </div>
      {renderResetButton}
    </div>
  );
};

export default NewChartLegendComponent;
