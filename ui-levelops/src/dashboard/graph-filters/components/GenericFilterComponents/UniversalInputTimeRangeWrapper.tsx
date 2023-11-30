import { getFilterValue } from "helper/widgetFilter.helper";
import { InputRangeFilterData, LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useCallback, useMemo } from "react";
import { InputRangeFilter } from "shared-resources/components";

interface UniversalInputTimeRangeWrapperProps {
  filterProps: LevelOpsFilter;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
  onExcludeFilterChange: (key: string, value: boolean) => void;
  handlePartialValueChange: (key: string, value: any) => void;
  handleRemoveFilter: (key: string) => void;
}

const UniversalInputTimeRangeWrapper: React.FC<UniversalInputTimeRangeWrapperProps> = props => {
  const { filterProps, onFilterValueChange } = props;
  const { label, beKey, filterMetaData, allFilters, apiFilterProps } = filterProps;

  const value = getFilterValue(allFilters, beKey, true)?.value;

  const { greaterThanKey, lessThanKey } = filterMetaData as InputRangeFilterData;

  const onChange = useCallback(
    (value: any) => {
      onFilterValueChange(value, beKey);
    },
    [beKey, onFilterValueChange]
  );

  const apiFilters = useMemo(() => {
    if (apiFilterProps) {
      return apiFilterProps({
        ...filterProps,
        handleSwitchValueChange: props.onExcludeFilterChange,
        handlePartialValueChange: props.handlePartialValueChange,
        handleRemoveFilter: props.handleRemoveFilter
      });
    }
    return {};
  }, [apiFilterProps, filterProps]);

  return (
    <InputRangeFilter
      greaterThanKey={greaterThanKey}
      lessThanKey={lessThanKey}
      label={label}
      value={value}
      onChange={onChange}
      withDelete={apiFilters.withDelete}
    />
  );
};

export default UniversalInputTimeRangeWrapper;
