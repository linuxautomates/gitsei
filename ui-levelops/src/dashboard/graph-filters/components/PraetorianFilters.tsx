import React from "react";
import { Form } from "antd";
import { AntInput } from "shared-resources/components";
import { getMaxRangeFromReportType } from "./utils/getMaxRangeFromReportType";
import { TimeRangeAbsoluteRelativeWrapperComponent } from "./time-range-abs-rel-wrapper.component";
import { timeFilterKey } from "./helper";

interface PraetorianFiltersProps {
  filters: any;
  metaData?: any;
  application: string;
  report: string;
  onFilterValueChange: (value: any, type?: any) => void;
  onRangeTypeChange: (key: string, value: any) => void;
  onTimeFilterValueChange: (value: any, type?: any, rangeType?: string) => void;
  onMetadataChange?: (value: any, type: any) => void;
  dashboardMetaData?: any;
}

const PraetorianFilters: React.FC<PraetorianFiltersProps> = props => {
  const {
    filters,
    report,
    onFilterValueChange,
    metaData,
    application,
    onRangeTypeChange,
    onTimeFilterValueChange,
    onMetadataChange,
    dashboardMetaData
  } = props;

  const maxRange = getMaxRangeFromReportType(report);

  return (
    <>
      <Form.Item key={"last_reports"} label={"Last Reports"}>
        <AntInput
          type="number"
          value={filters.n_last_reports || 1}
          style={{ width: "100%" }}
          onChange={(e: number) => onFilterValueChange(e, "n_last_reports")}
        />
      </Form.Item>
      <TimeRangeAbsoluteRelativeWrapperComponent
        key="ingested_in"
        label="Ingested In"
        filterKey={timeFilterKey(application, "ingested_at", report)}
        metaData={metaData}
        filters={filters}
        onFilterValueChange={onTimeFilterValueChange}
        onMetadataChange={onMetadataChange}
        dashboardMetaData={dashboardMetaData}
        onTypeChange={onRangeTypeChange}
        maxRange={maxRange}
      />
    </>
  );
};

export default PraetorianFilters;
