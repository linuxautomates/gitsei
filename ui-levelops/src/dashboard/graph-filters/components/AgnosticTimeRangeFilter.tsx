import React from "react";
import { timeFilterKey } from "./helper";
import { TimeRangeAbsoluteRelativeWrapperComponent } from "./time-range-abs-rel-wrapper.component";

interface AgnosticTimeRangeFilterProps {
  filters: any;
  application: string;
  onFilterValueChange: (value: any, type?: any, rangeType?: string) => void;
  onRangeTypeChange: (key: string, value: any) => void;
  metaData?: any;
  reportType: string;
  timeRangeFiltersSchema: { key: string; label: string }[];
  onMetadataChange?: (value: any, type: any) => void;
  dashboardMetaData?: any;
}

const AgnosticTimeRangeFilter: React.FC<AgnosticTimeRangeFilterProps> = props => {
  const {
    timeRangeFiltersSchema,
    filters,
    onFilterValueChange,
    application,
    onRangeTypeChange,
    metaData,
    reportType,
    onMetadataChange,
    dashboardMetaData
  } = props;

  return (
    <React.Fragment>
      {timeRangeFiltersSchema.map((filterSchema: any, index: number) => {
        return (
          <TimeRangeAbsoluteRelativeWrapperComponent
            key={`agnostic_time_range_${index}`}
            label={filterSchema.label}
            filterKey={timeFilterKey(application, filterSchema.dataKey, reportType)}
            metaData={metaData}
            filters={filters}
            onFilterValueChange={onFilterValueChange}
            onTypeChange={onRangeTypeChange}
            onMetadataChange={onMetadataChange}
            dashboardMetaData={dashboardMetaData}
          />
        );
      })}
    </React.Fragment>
  );
};

export default AgnosticTimeRangeFilter;
