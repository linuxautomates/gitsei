import * as React from "react";
import { AntSelect } from "shared-resources/components";
import { Form } from "antd";
import { TimeRangeAbsoluteRelativeWrapperComponent } from "./time-range-abs-rel-wrapper.component";
import { get } from "lodash";
import { STAT_TIME_BASED_FILTER } from "../../constants/filter-key.mapping";
import widgetConstants from "dashboard/constants/widgetConstants";

interface StatTimeRangeFiltersProps {
  filters: any;
  onFilterValueChange?: (value: any, type?: any, exclude?: boolean, addToMetaData?: any) => void;
  onTimeFilterValueChange?: (value: any, type?: any, rangeType?: string) => void;
  onRangeTypeChange: (key: string, value: any) => void;
  metaData?: any;
  reportType: string;
  onMetadataChange?: (value: any, type: any) => void;
  dashboardMetaData?: any;
  onSingleStatTypeFilterChange?: (value: string, removeKey: string) => void;
}

const StatTimeRangeFiltersComponent: React.FC<StatTimeRangeFiltersProps> = ({
  filters,
  onFilterValueChange,
  onRangeTypeChange,
  metaData,
  reportType,
  onMetadataChange,
  dashboardMetaData,
  onTimeFilterValueChange,
  onSingleStatTypeFilterChange
}) => {
  const widgetConstant = get(widgetConstants, [reportType, STAT_TIME_BASED_FILTER], {
    options: [],
    getFilterLabel: () => {},
    getFilterKey: () => {}
  });

  const options = widgetConstant.options;

  const getFilterLabel = () => {
    return widgetConstant.getFilterLabel({ filters, metaData }) || `${filters.across.replaceAll("_", " ")} in`;
  };

  const getFilterKey = () => {
    return widgetConstant.getFilterKey({ filters, metaData }) || `${filters.across}_at`;
  };

  const onTypeChange = (value: string) => {
    const key = getFilterKey();
    onSingleStatTypeFilterChange && onSingleStatTypeFilterChange(value, key);
  };

  return (
    <>
      <Form.Item label="Time Range Type" colon={false}>
        <AntSelect options={options} onChange={onTypeChange} value={filters.across || widgetConstant.defaultValue} />
      </Form.Item>
      <TimeRangeAbsoluteRelativeWrapperComponent
        key={`stat_${filters.across}_at`}
        label={getFilterLabel()}
        filterKey={getFilterKey()}
        metaData={metaData}
        filters={filters}
        onFilterValueChange={(data: any, key: string) => {
          onTimeFilterValueChange && onTimeFilterValueChange(data, key);
        }}
        onMetadataChange={onMetadataChange}
        onTypeChange={onRangeTypeChange}
        dashboardMetaData={dashboardMetaData}
      />
    </>
  );
};

export default StatTimeRangeFiltersComponent;
