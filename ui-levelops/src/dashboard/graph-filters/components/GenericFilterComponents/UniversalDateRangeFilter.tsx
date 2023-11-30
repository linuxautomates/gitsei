import React, { useCallback, useMemo } from "react";
import { DatePicker, Form } from "antd";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { NewCustomFormItemLabel } from "shared-resources/components";
import { toTitleCase } from "utils/stringUtils";
import { RangePickerValue } from "antd/lib/date-picker/interface";
import { get } from "lodash";
import moment from "moment";
import { DATE_RANGE_FILTER_FORMAT } from "shared-resources/containers/server-paginated-table/containers/filters/constants";

interface UniversalDateRangeFilterProps {
  filterProps: LevelOpsFilter;
  onFilterValueChange: (value: any, key: string, type?: string) => void;
  handleRemoveFilter: (key: string) => void;
}

const { RangePicker } = DatePicker;

const UniversalDateRangeFilter: React.FC<UniversalDateRangeFilterProps> = props => {
  const { filterProps, onFilterValueChange, handleRemoveFilter } = props;
  const { label, beKey, allFilters, apiFilterProps } = filterProps;

  const apiFilters = useMemo(() => {
    if (apiFilterProps) {
      return apiFilterProps({
        ...filterProps,
        handleRemoveFilter
      });
    }
    return {};
  }, [apiFilterProps, filterProps]);

  const value = useMemo(() => {
    const dateRange = get(allFilters, [beKey], {});
    if (!Object.keys(dateRange).length) {
      return [];
    }

    return Object.keys(dateRange).map(key => {
      if (!dateRange[key].toString().includes("-")) {
        return moment.unix(dateRange[key]).subtract(moment().utcOffset(), "m");
      } else {
        return moment(dateRange[key], DATE_RANGE_FILTER_FORMAT);
      }
    });
  }, [allFilters]);

  const handleChange = useCallback(
    (dates: RangePickerValue, dateStrings: [string, string]) => {
      if (dates.length > 1) {
        return onFilterValueChange?.(
          {
            $gt: dateStrings[0],
            $lt: dateStrings[1]
          },
          beKey,
          "date"
        );
      } else {
        return onFilterValueChange(undefined, beKey, "date");
      }
    },
    [onFilterValueChange]
  );

  return (
    <Form.Item
      className={"custom-form-item"}
      label={
        <div style={{ display: "flex", width: "100%" }}>
          <NewCustomFormItemLabel label={toTitleCase(label)} {...apiFilters} />
        </div>
      }>
      <RangePicker className="universal-date-picker" onChange={handleChange} allowClear={true} value={value as any} />
    </Form.Item>
  );
};

export default UniversalDateRangeFilter;
