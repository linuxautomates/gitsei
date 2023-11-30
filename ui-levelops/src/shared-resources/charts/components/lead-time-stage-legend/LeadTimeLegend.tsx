import React, { useCallback, useMemo } from "react";
import { AntCheckboxComponent as AntCheckbox } from "shared-resources/components/ant-checkbox/ant-checkbox.component";
import { filterLabelMapping, legendColorMapping } from "custom-hooks/helpers/leadTime.helper";
import { toTitleCase } from "utils/stringUtils";
import { get } from "lodash";

interface LeadTimeFilterDropdownProps {
  filters: any;
  setFilters: (filters: any) => void;
  hideMissing?: boolean;
}

const LeadTimeLegend: React.FC<LeadTimeFilterDropdownProps> = props => {
  const { filters, setFilters, hideMissing = false } = props;

  const filterKeys = useMemo(() => (filters && Object.keys(filters).length > 1 ? Object.keys(filters) : []), [filters]);
  const hasFilters = useMemo(() => filters && Object.keys(filters).length > 1, [filters]);

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

  const renderFiltersList = useMemo(() => {
    return (
      <>
        {filterKeys.map((entry: string, index: number) => {
          if (hideMissing && entry === "missing") {
            return null;
          }
          if (index === filterKeys.length - 1) {
            return (
              <div className="flex">
                <div className="separator mr-15" />
                <AntCheckbox
                  key={`filter-${entry}`}
                  className={`legend-checkbox`}
                  style={{ "--tick-color": legendColorMapping?.[entry] }}
                  indeterminate={filters[entry]}
                  checked={filters[entry]}
                  onChange={(input: any) => handleFilterChange(entry, input.target.checked)}>
                  {toTitleCase(get(filterLabelMapping, entry, entry))}
                </AntCheckbox>
              </div>
            );
          }
          return (
            <AntCheckbox
              key={`filter-${entry}`}
              className={`legend-checkbox`}
              style={{ "--tick-color": legendColorMapping?.[entry] }}
              indeterminate={filters[entry]}
              checked={filters[entry]}
              onChange={(input: any) => handleFilterChange(entry, input.target.checked)}>
              {toTitleCase(get(filterLabelMapping, entry, entry))}
            </AntCheckbox>
          );
        })}
      </>
    );
  }, [filters, setFilters]);

  if (!hasFilters) return null;

  return renderFiltersList;
};

export default React.memo(LeadTimeLegend);
