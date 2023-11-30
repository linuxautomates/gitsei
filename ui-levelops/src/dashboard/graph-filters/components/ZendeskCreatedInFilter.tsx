import React from "react";
import { getMaxRangeFromReportType } from "./utils/getMaxRangeFromReportType";
import { TimeRangeAbsoluteRelativeWrapperComponent } from "./time-range-abs-rel-wrapper.component";

interface ZendeskCreatedInFilterProps {
  metadata: any;
  filters: any;
  reportType: string;
  onFilterValueChange?: (value: any, type?: any, rangeType?: string) => void;
  onTimeRangeTypeChange?: (key: string, value: any) => void;
  onMetadataChange?: (value: any, type: any) => void;
  dashboardMetaData?: any;
}

const ZendeskCreatedInFilter: React.FC<ZendeskCreatedInFilterProps> = (props: ZendeskCreatedInFilterProps) => {
  const {
    filters,
    metadata,
    reportType,
    onFilterValueChange,
    onTimeRangeTypeChange,
    onMetadataChange,
    dashboardMetaData
  } = props;

  const maxRange = getMaxRangeFromReportType(reportType);

  if (!onTimeRangeTypeChange || !onFilterValueChange) {
    return null;
  }

  return (
    <TimeRangeAbsoluteRelativeWrapperComponent
      key="zendesk_time_range_filters"
      label="Zendesk Ticket Created in"
      filterKey={"created_at"}
      metaData={metadata}
      filters={filters}
      onFilterValueChange={onFilterValueChange}
      onMetadataChange={onMetadataChange}
      onTypeChange={onTimeRangeTypeChange}
      maxRange={maxRange}
      dashboardMetaData={dashboardMetaData}
    />
  );
};

export default ZendeskCreatedInFilter;
