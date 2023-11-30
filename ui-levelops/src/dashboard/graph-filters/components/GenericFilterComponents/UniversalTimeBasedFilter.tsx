import {
  AZURE_CUSTOM_FIELD_PREFIX,
  CUSTOM_FIELD_PREFIX,
  TESTRAILS_CUSTOM_FIELD_PREFIX
} from "dashboard/constants/constants";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useMemo } from "react";
import { TimeRangeAbsoluteRelativeWrapperComponent } from "../time-range-abs-rel-wrapper.component";
import { getMaxRangeFromReportType } from "../utils/getMaxRangeFromReportType";

export interface UniversalTimeBasedFilterProps {
  filterProps: LevelOpsFilter;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
  onExcludeFilterChange: (key: string, value: boolean) => void;
  handlePartialValueChange: (key: string, value: any) => void;
  handleTimeRangeTypeChange: (key: string, value: any) => void;
  handleTimeRangeFilterValueChange: (value: any, type?: any, rangeType?: string, isCustom?: boolean) => void;
  handleRemoveFilter: (key: string) => void;
  handleMetadataChange?: (key: any, value: any) => void;
}

const UniversalTimeBasedFilter: React.FC<UniversalTimeBasedFilterProps> = (props: UniversalTimeBasedFilterProps) => {
  const { filterProps } = props;
  const { label, beKey, allFilters, metadata, filterMetaData, deleteSupport, required } = filterProps;
  const { dashboardMetaData, reportType } = filterMetaData as any;

  const maxRange = getMaxRangeFromReportType(reportType);

  const isCustom =
    beKey.includes(CUSTOM_FIELD_PREFIX) ||
    beKey.includes(AZURE_CUSTOM_FIELD_PREFIX) ||
    beKey.includes(TESTRAILS_CUSTOM_FIELD_PREFIX);

  const mapFilterWithCustomFields = useMemo(
    () => ({ ...(allFilters ?? {}), ...(allFilters?.custom_fields ?? {}) }),
    [allFilters]
  );

  const isRequired = useMemo(() => {
    if (typeof required === "boolean") return required;
    if (required instanceof Function) return required({ filters: allFilters });
    return false;
  }, [required, allFilters]);

  return (
    <>
      <TimeRangeAbsoluteRelativeWrapperComponent
        key={beKey}
        label={label}
        filterKey={beKey}
        metaData={metadata}
        filters={mapFilterWithCustomFields}
        onFilterValueChange={(data: any, key: string) => {
          props.handleTimeRangeFilterValueChange?.(data, key, undefined, isCustom);
        }}
        required={isRequired}
        onMetadataChange={props.handleMetadataChange}
        onTypeChange={props.handleTimeRangeTypeChange!}
        onDelete={deleteSupport ? props.handleRemoveFilter : undefined}
        maxRange={maxRange}
        dashboardMetaData={dashboardMetaData}
      />
    </>
  );
};

export default React.memo(UniversalTimeBasedFilter);
