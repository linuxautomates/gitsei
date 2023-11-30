import React, { useCallback } from "react";
import { AntSelect } from "../../../../../shared-resources/components";

interface ReportsDropdownProps {
  mode?: string;
  dashboardId: string;
  widgetType: string;
  className: string;
  value: any;
  onChange: any;
  placeHolder?: string;
}

const timeFilterOptions = [
  {
    label: "Quaterly",
    value: "quarter"
  },
  {
    label: "Monthly",
    value: "month"
  },
  {
    label: "Weekly",
    value: "week"
  }
];

const MultiTimeSeriesTimeFilterDropdown: React.FC<ReportsDropdownProps> = ({
  mode,
  className,
  value,
  onChange,
  placeHolder = "Select A Report"
}) => {
  const handleReportFilter = useCallback((value: string, option: { label: string; value: string }) => {
    return (option?.label as string)?.toLowerCase().includes(value?.toLowerCase());
  }, []);

  return (
    <AntSelect
      autoFocus
      mode={mode ? mode : "default"}
      className={className}
      value={value ? value : undefined}
      placeholder={placeHolder}
      options={timeFilterOptions}
      showSearch
      showArrow
      onOptionFilter={handleReportFilter}
      onChange={onChange}
    />
  );
};

export default MultiTimeSeriesTimeFilterDropdown;
