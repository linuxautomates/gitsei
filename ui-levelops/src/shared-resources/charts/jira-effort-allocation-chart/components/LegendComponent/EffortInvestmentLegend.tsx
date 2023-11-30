import React, { useCallback, useMemo } from "react";
import { Button } from "antd";
import { AntCheckbox, AntText } from "shared-resources/components";
import { transformKey } from "shared-resources/charts/helper";
import { capitalizeWord, toTitleCase } from "utils/stringUtils";
import "./EffortInvestmentLegend.scss";
import { Icon } from "@harness/uicore";

interface NewLegendComponentProps {
  filters: any;
  setFilters: any;
  data?: any;
  colors: any;
  disableAll?: boolean;
  wordToCapitalize?: string;
}
const NewLegendComponent: React.FC<NewLegendComponentProps> = (props: NewLegendComponentProps) => {
  const { filters, setFilters, colors, disableAll, wordToCapitalize } = props;

  const hasFilters = useMemo(() => filters && Object.keys(filters).length > 1, [filters]);
  const listKeys = Object.keys(colors);

  const legendFormatter = useCallback(value => {
    return value ? capitalizeWord(toTitleCase(transformKey(value)), wordToCapitalize) : "_";
  }, []);

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
      <div className="legend-checkbox">
        {listKeys.map((entry: any) => {
          const color = colors[entry];
          return (
            <AntCheckbox
              key={`filter-${entry}`}
              disabled={!!disableAll}
              className={`legend-checkbox`}
              style={{ "--tick-color": color }}
              indeterminate={filters[entry]}
              checked={filters[entry]}
              onChange={(input: any) => handleFilterChange(entry, input.target.checked)}>
              {legendFormatter(entry)}
            </AntCheckbox>
          );
        })}
      </div>
    );
  }, [filters, setFilters, colors, listKeys]);

  if (!hasFilters) return null;

  return (
    <div className="new-legend-container mt-10">
      {renderFiltersList}
      {renderResetButton}
    </div>
  );
};

export default NewLegendComponent;
