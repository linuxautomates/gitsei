import React, { useCallback, useMemo } from "react";
import CustomSelectWrapper from "shared-resources/components/custom-select/CustomSelectWrapper";
import { getModificationValue, modificationMappedValues } from "./helper";

interface BATimeRangeFilterProps {
  filters: any;
  config: { options: any[]; filterKey: string; label: string };
  onFilterValueChange?: (value: any, type?: any, exclude?: boolean) => void;
}

const BATimeRangeFilter: React.FC<BATimeRangeFilterProps> = ({ config, filters, onFilterValueChange }) => {
  const options = useMemo(() => config.options.map((i: any) => ({ label: i.label, value: i.id })), [config]);
  const value = useMemo(() => getModificationValue(filters?.[config.filterKey], config.options), [filters, config]);
  const handleChange = useCallback(
    (value: any) => {
      onFilterValueChange && onFilterValueChange(modificationMappedValues(value, config.options), config.filterKey);
    },
    [config, filters]
  );

  return (
    <CustomSelectWrapper
      mode={"default"}
      selectLabel={config.label}
      value={value}
      options={options}
      onChange={handleChange}
    />
  );
};

export default React.memo(BATimeRangeFilter);
