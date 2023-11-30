import React from "react";
import { Form } from "antd";
import { InputRangeFilter } from "shared-resources/components";
import { timeFilterKey } from "./helper";
import { getMaxRangeFromReportType } from "./utils/getMaxRangeFromReportType";
import { getFilterValue } from "../../../configurable-dashboard/helpers/helper";
import { TimeRangeAbsoluteRelativeWrapperComponent } from "./time-range-abs-rel-wrapper.component";

interface SynkFiltersProps {
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

const SynkFilters: React.FC<SynkFiltersProps> = props => {
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
      <Form.Item key={"score_range"}>
        <InputRangeFilter
          value={getFilterValue(filters, "score_range")}
          label={"Priority Score"}
          onChange={(value: any) => onFilterValueChange(value, "score_range")}
        />
      </Form.Item>
      <TimeRangeAbsoluteRelativeWrapperComponent
        key="disclosure_time"
        label="Disclosure Time"
        filterKey={timeFilterKey(application, "disclosure_range", report)}
        metaData={metaData}
        filters={filters}
        onFilterValueChange={onTimeFilterValueChange}
        onTypeChange={onRangeTypeChange}
        onMetadataChange={onMetadataChange}
        dashboardMetaData={dashboardMetaData}
        maxRange={maxRange}
      />
      <TimeRangeAbsoluteRelativeWrapperComponent
        key="publication_time"
        label="Publication Time"
        filterKey={timeFilterKey(application, "publication_range", report)}
        metaData={metaData}
        filters={filters}
        onFilterValueChange={onTimeFilterValueChange}
        onMetadataChange={onMetadataChange}
        onTypeChange={onRangeTypeChange}
        dashboardMetaData={dashboardMetaData}
        maxRange={maxRange}
      />
    </>
  );
};

export default SynkFilters;
