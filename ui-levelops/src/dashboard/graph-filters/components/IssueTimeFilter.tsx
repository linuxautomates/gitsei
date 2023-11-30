import React from "react";
import { TimeRangeAbsoluteRelativeWrapperComponent } from "./time-range-abs-rel-wrapper.component";

interface IssueTimeFilterComponentProps {
  filters: any;
  onFilterValueChange: (value: any, type?: any, rangeType?: string) => void;
  onRangeTypeChange: (key: string, value: any) => void;
  metaData?: any;
  label: string;
  filterKey: string;
  options: { id: string; label: string; mFactor: number }[];
  onDelete?: (key: string) => void;
}

// Note: Use only for non stat widgets
const IssueTimeFilterComponent: React.FC<IssueTimeFilterComponentProps> = ({
  filters,
  onFilterValueChange,
  onRangeTypeChange,
  metaData,
  label,
  filterKey,
  options,
  onDelete
}) => {
  return (
    <TimeRangeAbsoluteRelativeWrapperComponent
      key={`issue_time_filter_${filterKey}`}
      label={label?.toUpperCase()}
      filterKey={filterKey}
      metaData={metaData}
      filters={filters}
      onFilterValueChange={onFilterValueChange}
      onTypeChange={onRangeTypeChange}
      onDelete={onDelete}
      useMapping={false}
    />
  );
};

export default IssueTimeFilterComponent;
