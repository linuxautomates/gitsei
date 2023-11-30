import React, { useMemo } from "react";
import { getMaxRangeFromReportType } from "./utils/getMaxRangeFromReportType";
import { TimeRangeAbsoluteRelativeWrapperComponent } from "./time-range-abs-rel-wrapper.component";

interface JenkinsJobTimeFilterProps {
  filters: any;
  application: string;
  report: string;
  onFilterValueChange: (value: any, type?: any, rangeType?: string) => void;
  onRangeTypeChange: (key: string, value: any) => void;
  metaData?: any;
  filterKey?: string;
  onMetadataChange?: (value: any, type: any) => void;
  dashboardMetaData?: any;
}

const JenkinsJobTimeFilter: React.FC<JenkinsJobTimeFilterProps> = (props: JenkinsJobTimeFilterProps) => {
  const {
    filters,
    onFilterValueChange,
    report,
    onRangeTypeChange,
    metaData,
    filterKey,
    onMetadataChange,
    dashboardMetaData
  } = props;

  // Limit the range for trend reports to 90 days.
  const maxRange = useMemo(() => getMaxRangeFromReportType(report), [report]);

  return (
    <TimeRangeAbsoluteRelativeWrapperComponent
      key="job_end_date"
      label="Job End Date"
      filterKey={filterKey || `end_time`}
      metaData={metaData}
      filters={filters}
      onFilterValueChange={onFilterValueChange}
      onTypeChange={onRangeTypeChange}
      maxRange={maxRange}
      onMetadataChange={onMetadataChange}
      dashboardMetaData={dashboardMetaData}
    />
  );
};

export default React.memo(JenkinsJobTimeFilter);
