import { Form } from "antd";
import { LevelOpsFilter, StatTimeRangeFilterData } from "model/filters/levelopsFilters";
import * as React from "react";
import { AntSelect, NewCustomFormItemLabel } from "shared-resources/components";
import { TimeRangeAbsoluteRelativeWrapperComponent } from "../time-range-abs-rel-wrapper.component";

interface UniversalStatTimeRangeFiltersProps {
  filterProps: LevelOpsFilter;
  handleTimeRangeFilterValueChange?: (value: any, type?: any, rangeType?: string) => void;
  handleTimeRangeTypeChange?: (key: string, value: any) => void;
  handleMetadataChange?: (value: any, type: any) => void;
  handleSingleStatTypeFilterChange?: (value: string, removeKey: string) => void;
  handleRemoveFilter: (key: string) => void;
}

const UniversalStatTimeRangeFilterWrapper: React.FC<UniversalStatTimeRangeFiltersProps> = ({
  filterProps,
  handleTimeRangeTypeChange,
  handleMetadataChange,
  handleTimeRangeFilterValueChange,
  handleSingleStatTypeFilterChange,
  handleRemoveFilter
}) => {
  const { allFilters, metadata, filterMetaData, required, defaultValue, apiFilterProps } = filterProps;
  const { dashboardMetaData, options, filterLabel, filterKey } = filterMetaData as StatTimeRangeFilterData;

  const getFilterLabel = () => {
    if (typeof filterLabel === "function") return filterLabel({ allFilters, metadata });
    return filterLabel || "";
  };

  const getFilterKey = () => {
    if (typeof filterKey === "function") return filterKey({ allFilters, metadata });
    return filterKey || "";
  };

  const onTypeChange = (value: string) => {
    const key = getFilterKey();
    handleSingleStatTypeFilterChange?.(value, key);
  };

  const apiFilters = React.useMemo(() => {
    if (apiFilterProps) {
      return apiFilterProps({
        ...filterProps,
        handleRemoveFilter
      });
    }
    return {};
  }, [apiFilterProps, filterProps]);

  const isRequired = React.useMemo(() => {
    if (typeof required === "boolean") return required;
    if (required instanceof Function) return required({ filters: allFilters });
    return false;
  }, [required, allFilters]);

  return (
    <>
      <Form.Item
        label={<NewCustomFormItemLabel required={isRequired} label={"Time Range Type"} {...apiFilters} />}
        colon={false}>
        <AntSelect options={options} onChange={onTypeChange} value={allFilters.across || defaultValue} />
      </Form.Item>
      <TimeRangeAbsoluteRelativeWrapperComponent
        key={`stat_${allFilters.across}_at`}
        label={getFilterLabel()}
        filterKey={getFilterKey()}
        metaData={metadata}
        filters={allFilters}
        required={isRequired}
        onFilterValueChange={(data: any, key: string) => handleTimeRangeFilterValueChange?.(data, key)}
        onMetadataChange={handleMetadataChange}
        onTypeChange={handleTimeRangeTypeChange!}
        dashboardMetaData={dashboardMetaData}
      />
    </>
  );
};

export default UniversalStatTimeRangeFilterWrapper;
