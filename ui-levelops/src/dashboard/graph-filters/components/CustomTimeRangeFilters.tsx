import * as React from "react";
import { map } from "lodash";
import { timeBasedFields } from "./helper";
import { TimeRangeAbsoluteRelativeWrapperComponent } from "./time-range-abs-rel-wrapper.component";

interface CustomTimeRangeFiltersProps {
  filters: any;
  application: string;
  onFilterValueChange: (value: any, type?: any, rangeType?: string, isCustom?: boolean) => void;
  onRangeTypeChange: (key: string, value: any) => void;
  metaData?: any;
  reportType?: string;
  fieldTypeList: { type: string; key: string; name: string }[];
  data: { [key: string]: {} }[];
  onMetadataChange?: (value: any, key: any) => void;
  dashboardMetaData?: any;
  onDelete?: (value: any) => void;
}

const CustomTimeRangeFiltersComponent: React.FC<CustomTimeRangeFiltersProps> = ({
  filters,
  application,
  onFilterValueChange,
  onRangeTypeChange,
  metaData,
  reportType,
  fieldTypeList,
  data,
  onMetadataChange,
  onDelete,
  dashboardMetaData
}) => {
  if (data.length === 0 || Object.keys(data).length == 0) {
    return null;
  }

  return (
    <div>
      {map(data, (item: any) => {
        //BE not support time based custom field for zendesk application
        if (!timeBasedFields(item, fieldTypeList) || ["zendesk"].includes(application)) {
          return null;
        }

        const _itemKey = Object.keys(item)[0];
        const _itemData = _itemKey.split("@")[0];
        const _itemName = _itemKey.split("@")[1];

        return (
          <TimeRangeAbsoluteRelativeWrapperComponent
            key={_itemData}
            onMetadataChange={onMetadataChange}
            dashboardMetaData={dashboardMetaData}
            label={_itemName}
            filterKey={_itemData}
            metaData={metaData}
            filters={filters.custom_fields || {}}
            onFilterValueChange={(value: any, key: string) => {
              onFilterValueChange?.(value, key, undefined, true);
            }}
            onDelete={onDelete}
            onTypeChange={onRangeTypeChange}
          />
        );
      })}
    </div>
  );
};

export default CustomTimeRangeFiltersComponent;
